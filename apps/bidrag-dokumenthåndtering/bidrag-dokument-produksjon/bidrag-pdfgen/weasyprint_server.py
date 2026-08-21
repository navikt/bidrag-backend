"""HTML -> PDF service backed by WeasyPrint.

Replaces the previous Gotenberg (Chromium + LibreOffice) service. The endpoints
mirror what ``BidragPdfGenConsumer`` calls, and cover the Gotenberg routes that
were in use:

    GET  /health              liveness/readiness probe
    POST /convert             text/html body -> application/pdf
    POST /flatten             application/pdf body -> flattened application/pdf
    POST /merge               multipart of PDFs -> single application/pdf

Chromium/Gotenberg behaviour that has to be emulated explicitly:

1. Gotenberg received ``header.html``/``footer.html`` as *separate* documents
   and stamped them onto every page, filling in the ``.pageNumber`` and
   ``.totalPages`` spans itself. WeasyPrint instead uses CSS running elements
   plus ``counter(page)``/``counter(pages)``, so those spans are mapped onto
   counters and ``#header``/``#footer`` are declared as running elements.
2. ``#header`` is ``position: relative`` in the template stylesheet, which keeps
   it in the normal flow and lets it swallow the whole first page. The compat
   stylesheet forces ``position: running(header)``.
3. ``scale: 4`` on the NAV logo is an unsupported shorthand in WeasyPrint; it is
   rewritten to ``transform: scale(4)``.
4. Chromium silently shrink-to-fits oversized content. WeasyPrint does not, so
   the templates must declare a page size that actually fits the layout (fixed
   in style.css by using real A4 rather than A4's point size used as pixels).
5. Gotenberg was given ``emulatedMediaType=print`` and ``preferCssPageSize=true``.
   WeasyPrint already defaults to the print media type and always honours the
   ``@page`` size, so neither needs configuring.

Note on stylesheet origin: ``stylesheets=`` is a *user* origin stylesheet, which
loses to author declarations. The compat rules therefore use ``!important``,
which is the only way a user stylesheet can win.
"""

from __future__ import annotations

import itertools
import logging
import os
import subprocess
import tempfile
from html.parser import HTMLParser
from typing import Any

from flask import Flask, Response, jsonify, request
from weasyprint import CSS, HTML, __version__ as weasyprint_version
from weasyprint.pdf import VARIANTS
from weasyprint.text.fonts import FontConfiguration
from weasyprint.urls import URLFetchingError

# PDF/A-3b was what the Gotenberg deployment produced, so keep it as the default
# for `pdfa=true`. "b" = basic conformance (visual fidelity, no tagging).
DEFAULT_PDFA_VARIANT = os.environ.get("DEFAULT_PDFA_VARIANT", "pdf/a-3b")

LOG_LEVEL = os.environ.get("LOG_LEVEL", "INFO").upper()
logging.basicConfig(
    level=LOG_LEVEL,
    format="%(asctime)s %(levelname)s %(name)s %(message)s",
)
log = logging.getLogger("weasyprint-server")

# WeasyPrint logs one line per resource it fetches; keep it quiet unless debugging.
logging.getLogger("weasyprint").setLevel(
    logging.DEBUG if LOG_LEVEL == "DEBUG" else logging.WARNING
)

app = Flask(__name__)

# Guard against a malicious/oversized payload exhausting the pod's memory.
MAX_CONTENT_LENGTH = int(os.environ.get("MAX_CONTENT_LENGTH", 64 * 1024 * 1024))
app.config["MAX_CONTENT_LENGTH"] = MAX_CONTENT_LENGTH

# The templates inline every asset as an SVG element or a data: URI, so no
# network access is needed. Blocking everything else prevents SSRF from
# template-injected markup and avoids multi-second stalls when WeasyPrint tries
# to resolve the root-relative asset links Remix emits.
ALLOW_REMOTE_RESOURCES = os.environ.get("ALLOW_REMOTE_RESOURCES", "false").lower() == "true"

COMPAT_CSS_SOURCE = """
/* (1)+(2) The templates render id="header"/id="footer", and style.css used to
   set `#header { position: relative }`, which kept the header in the normal
   flow so it swallowed the whole first page. `!important` is required because
   this is a *user* origin stylesheet and would otherwise lose to author rules. */
#header {
  position: running(header) !important;
}

#footer {
  position: running(footer) !important;
}

/* (1) Chromium filled these spans in its separate header/footer documents.
   WeasyPrint has no such magic, so map them onto the CSS page counters. */
.pageNumber::before {
  content: counter(page);
}

.totalPages::before {
  content: counter(pages);
}

/* (3) WeasyPrint does not implement the `scale` property at all, so the NAV
   logo's `scale: 4` is dropped and the logo renders at 1/4 size. Translate it
   into the `transform` shorthand, which WeasyPrint does support. Do NOT set
   transform-origin: `transform: scale()` and `scale` share the same default
   origin (50% 50%), and anchoring at `top right` pushes the logo off the page. */
.navlogo_new {
  transform: scale(4) !important;
}
"""

_font_config = FontConfiguration()
_compat_css = CSS(string=COMPAT_CSS_SOURCE, font_config=_font_config)


def _restricted_url_fetcher(url: str, timeout: int = 10, ssl_context: Any = None):
    """Allow only `data:` URIs unless remote resources are explicitly enabled."""
    if url.startswith("data:"):
        from weasyprint.urls import default_url_fetcher

        return default_url_fetcher(url, timeout=timeout, ssl_context=ssl_context)
    if ALLOW_REMOTE_RESOURCES:
        from weasyprint.urls import default_url_fetcher

        return default_url_fetcher(url, timeout=timeout, ssl_context=ssl_context)
    raise URLFetchingError(f"Blocked non-data URL: {url[:120]}")


def _parse_bool(value: str | None, default: bool = False) -> bool:
    if value is None:
        return default
    return value.strip().lower() in {"1", "true", "yes", "on"}


def _parse_float(value: str | None, default: float) -> float:
    """Parse a float, raising on garbage.

    Silently falling back to the default would let a typo'd `scale` produce a
    plausible-looking but wrongly sized PDF, which is far worse than a 400.
    """
    if value is None or value.strip() == "":
        return default
    try:
        return float(value)
    except ValueError as exc:
        raise ValueError(f"Not a number: '{value}'") from exc


class _HeadMetaParser(HTMLParser):
    """Collect <title> and <meta> from <head>.

    WeasyPrint only looks at `<meta name=...>`, but Remix's `meta()` export emits
    `<meta property="author">` / `<meta property="subject">`, so those values were
    silently dropped from the PDF metadata. Collect both spellings.
    """

    def __init__(self) -> None:
        super().__init__(convert_charrefs=True)
        self.meta: dict[str, str] = {}
        self.title: str | None = None
        self._in_title = False
        self._in_head = False

    def handle_starttag(self, tag: str, attrs: list[tuple[str, str | None]]) -> None:
        if tag == "head":
            self._in_head = True
        elif tag == "title":
            self._in_title = True
        elif tag == "meta":
            a = {k.lower(): (v or "") for k, v in attrs}
            key = a.get("name") or a.get("property")
            content = a.get("content")
            if key and content:
                self.meta.setdefault(key.strip().lower(), content.strip())

    def handle_endtag(self, tag: str) -> None:
        if tag == "head":
            self._in_head = False
        elif tag == "title":
            self._in_title = False

    def handle_data(self, data: str) -> None:
        if self._in_title and self.title is None:
            text = data.strip()
            if text:
                self.title = text


def _extract_head_meta(html_source: str) -> tuple[dict[str, str], str | None]:
    parser = _HeadMetaParser()
    try:
        parser.feed(html_source)
    except Exception:  # noqa: BLE001 - malformed markup must not fail the render
        log.warning("Could not parse <head> metadata", exc_info=True)
    return parser.meta, parser.title


def _apply_chromium_metadata(document, meta: dict[str, str], title: str | None) -> None:
    """Backfill metadata WeasyPrint did not pick up itself."""
    metadata = document.metadata
    if not metadata.title and title:
        metadata.title = title
    # `authors` is a *list* on WeasyPrint's DocumentMetadata.
    if not metadata.authors:
        author = meta.get("author")
        if author:
            metadata.authors = [author]
    # PDF's "Subject" maps to `description` in WeasyPrint.
    if not metadata.description:
        subject = meta.get("subject") or meta.get("description")
        if subject:
            metadata.description = subject
    if not metadata.keywords:
        keywords = meta.get("keywords")
        if keywords:
            metadata.keywords = [k.strip() for k in keywords.split(",") if k.strip()]


# WeasyPrint silently ignores unknown kwargs passed to `write_pdf`. The previous
# implementation passed `variant=`, which is NOT a valid option — the correct key
# is `pdf_variant` — so PDF/A output was a no-op. Fail loudly on unknown names.
_VALID_VARIANTS = set(VARIANTS)


def _normalise_variant(raw: str) -> str:
    candidate = raw.strip().lower().replace("_", "-")
    if not candidate.startswith("pdf/"):
        candidate = candidate.replace("pdfa-", "pdf/a-").replace("pdfua-", "pdf/ua-")
    if candidate not in _VALID_VARIANTS:
        raise ValueError(
            f"Unsupported PDF variant '{raw}'. "
            f"Supported: {', '.join(sorted(_VALID_VARIANTS))}"
        )
    return candidate


def _resolve_pdf_variant(
    pdfa: str | None, pdfua: str | None, variant_param: str | None = None
) -> str | None:
    """Map Gotenberg's pdfa/pdfua parameters onto a WeasyPrint PDF variant.

    Gotenberg accepted `pdfa=PDF/A-3b` and `pdfua=true`; the Kotlin consumer
    sends `pdfa=true|false`. An explicit `variant=` is also honoured and wins,
    so any WeasyPrint variant can be requested without a code change.
    """
    if variant_param and variant_param.strip():
        return _normalise_variant(variant_param)

    want_ua = _parse_bool(pdfua, False)

    variant: str | None = None
    if pdfa:
        raw = pdfa.strip().lower()
        if raw in {"1", "true", "yes", "on"}:
            variant = DEFAULT_PDFA_VARIANT
        elif raw in {"0", "false", "no", "off", ""}:
            variant = None
        else:
            variant = _normalise_variant(raw)

    if want_ua:
        if variant is None:
            return "pdf/ua-1"
        # PDF/A and PDF/UA cannot both be named in one variant. The accessible
        # ("a") conformance levels are tagged, which is what PDF/UA requires, so
        # upgrade the requested level to its accessible sibling where possible.
        accessible = variant[:-1] + "a"
        if accessible in _VALID_VARIANTS:
            return accessible
        log.warning("No accessible variant for %s; keeping it and ignoring pdfua", variant)
    return variant


def _run_qpdf(args: list[str], data: bytes) -> bytes:
    """Run qpdf on `data` and return the result.

    qpdf exits 0 on success, **3 on warnings** (output is still written and
    valid) and >=2 on real errors. Treating 3 as a failure would reject perfectly
    good PDFs, which is what a naive `check=True` does.
    """
    with tempfile.TemporaryDirectory() as tmp:
        src = os.path.join(tmp, "in.pdf")
        dst = os.path.join(tmp, "out.pdf")
        with open(src, "wb") as fh:
            fh.write(data)
        result = subprocess.run(
            ["qpdf", src, *args, dst],
            capture_output=True,
            timeout=int(os.environ.get("QPDF_TIMEOUT", "120")),
        )
        if result.returncode not in (0, 3):
            raise RuntimeError(
                f"qpdf failed ({result.returncode}): {result.stderr.decode('utf-8', 'replace')[:500]}"
            )
        if result.returncode == 3:
            log.warning("qpdf warnings: %s", result.stderr.decode("utf-8", "replace")[:500])
        with open(dst, "rb") as fh:
            return fh.read()


def _error(message: str, status: int) -> Response:
    log.warning("%s -> HTTP %s", message, status)
    return jsonify({"error": message, "status": status}), status


@app.get("/health")
def health() -> Response:
    return jsonify(
        {
            "status": "UP",
            "weasyprint": weasyprint_version,
            "remoteResourcesAllowed": ALLOW_REMOTE_RESOURCES,
        }
    )


@app.post("/convert")
def convert() -> Response:
    """Render an HTML document to PDF.

    Body:  the HTML document (text/html, UTF-8)
    Query: scale   float, default 1.0    zoom factor (Gotenberg `scale`)
           pdfa    bool or variant name  e.g. `true` or `PDF/A-3b`
           pdfua   bool, default false   produce a tagged, accessible PDF
           flatten bool, default false   flatten form fields in the same request
    """
    html_bytes = request.get_data()
    if not html_bytes:
        return _error("Empty request body; expected an HTML document", 400)

    # The consumer always sends UTF-8; fall back rather than 500 on odd bytes.
    charset = request.mimetype_params.get("charset") or "utf-8"
    try:
        html_source = html_bytes.decode(charset)
    except (LookupError, UnicodeDecodeError):
        log.warning("Could not decode body as %s, falling back to utf-8/replace", charset)
        html_source = html_bytes.decode("utf-8", "replace")

    try:
        scale = _parse_float(request.args.get("scale"), 1.0)
        if scale <= 0:
            raise ValueError(f"scale must be > 0, got {scale}")
        pdf_variant = _resolve_pdf_variant(
            request.args.get("pdfa"),
            request.args.get("pdfua"),
            request.args.get("variant"),
        )
    except ValueError as exc:
        return _error(str(exc), 400)

    try:
        html = HTML(
            string=html_source,
            base_url=request.args.get("baseUrl") or None,
            url_fetcher=_restricted_url_fetcher,
            # WeasyPrint already defaults to the print media type, which is what
            # Gotenberg's emulatedMediaType=print gave us. Be explicit anyway.
            media_type="print",
        )
        # render() and write_pdf() are split so metadata can be patched in between.
        document = html.render(
            stylesheets=[_compat_css],
            font_config=_font_config,
            presentational_hints=True,
        )
        meta, title = _extract_head_meta(html_source)
        _apply_chromium_metadata(document, meta, title)

        options: dict[str, Any] = {}
        if pdf_variant:
            # NB: the option is `pdf_variant`, not `variant` — WeasyPrint ignores
            # unknown kwargs silently, which made the old `variant=` a no-op.
            options["pdf_variant"] = pdf_variant
        pdf_bytes = document.write_pdf(zoom=scale, **options)
    except URLFetchingError as exc:
        return _error(f"Blocked or unreachable resource: {exc}", 400)
    except Exception as exc:  # noqa: BLE001 - surface a 500 with context
        log.exception("HTML to PDF conversion failed")
        return _error(f"Conversion failed: {exc}", 500)

    if _parse_bool(request.args.get("flatten"), False):
        try:
            pdf_bytes = _run_qpdf(["--flatten-annotations=all"], pdf_bytes)
        except Exception as exc:  # noqa: BLE001
            log.exception("Flatten after convert failed")
            return _error(f"Flatten failed: {exc}", 500)

    log.info(
        "Converted %d bytes of HTML to a %d byte PDF (scale=%s, variant=%s)",
        len(html_bytes),
        len(pdf_bytes),
        scale,
        pdf_variant,
    )
    return Response(pdf_bytes, mimetype="application/pdf")


@app.post("/flatten")
def flatten() -> Response:
    """Flatten annotations/form fields. Mirrors Gotenberg's pdfengines flatten."""
    pdf_bytes = request.get_data()
    if not pdf_bytes:
        return _error("Empty request body; expected a PDF", 400)
    if not pdf_bytes.startswith(b"%PDF-"):
        return _error("Request body is not a PDF", 400)
    try:
        flattened = _run_qpdf(["--flatten-annotations=all"], pdf_bytes)
    except Exception as exc:  # noqa: BLE001
        log.exception("Flatten failed")
        return _error(f"Flatten failed: {exc}", 500)
    return Response(flattened, mimetype="application/pdf")


@app.post("/merge")
def merge() -> Response:
    """Merge several PDFs into one, in the order they were uploaded.

    Provided for parity with the Gotenberg pdfengines merge route.
    """
    files = [f for f in itertools.chain.from_iterable(request.files.listvalues()) if f.filename]
    if len(files) < 2:
        return _error("Provide at least two PDF files to merge", 400)

    with tempfile.TemporaryDirectory() as tmp:
        paths = []
        for index, storage in enumerate(files):
            data = storage.read()
            if not data.startswith(b"%PDF-"):
                return _error(f"'{storage.filename}' is not a PDF", 400)
            path = os.path.join(tmp, f"{index:04d}.pdf")
            with open(path, "wb") as fh:
                fh.write(data)
            paths.append(path)

        out = os.path.join(tmp, "merged.pdf")
        result = subprocess.run(
            ["qpdf", "--empty", "--pages", *paths, "--", out],
            capture_output=True,
            timeout=int(os.environ.get("QPDF_TIMEOUT", "120")),
        )
        if result.returncode not in (0, 3):
            log.error("qpdf merge failed: %s", result.stderr.decode("utf-8", "replace")[:500])
            return _error("Merge failed", 500)
        with open(out, "rb") as fh:
            return Response(fh.read(), mimetype="application/pdf")


@app.errorhandler(413)
def payload_too_large(_exc) -> Response:
    return _error(f"Payload exceeds MAX_CONTENT_LENGTH ({MAX_CONTENT_LENGTH} bytes)", 413)


if __name__ == "__main__":
    # Development entrypoint only; production runs under gunicorn (see Dockerfile).
    app.run(host="0.0.0.0", port=int(os.environ.get("PORT", "8080")))
