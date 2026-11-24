"""Prometheus metrics for the WeasyPrint server.

gunicorn runs several worker processes, so the metrics use prometheus_client's
multiprocess mode whenever PROMETHEUS_MULTIPROC_DIR is set (see gunicorn.conf.py
and DockerfileWeasyprint). Without it, e.g. under ``python weasyprint_server.py``,
the default single-process registry is used.

Each conversion phase records both wall-clock time and the CPU time of the
request thread. If wall time is much higher than CPU time, the render was
waiting for a CPU, not doing more work.
"""

from __future__ import annotations

import os
import time
from contextlib import contextmanager
from typing import Iterator

from prometheus_client import (
    CONTENT_TYPE_LATEST,
    REGISTRY,
    CollectorRegistry,
    Counter,
    Gauge,
    Histogram,
    generate_latest,
)
from prometheus_client import multiprocess

MULTIPROC_DIR_ENV = "PROMETHEUS_MULTIPROC_DIR"

PHASE_PARSE = "parse"
PHASE_RENDER = "render"
PHASE_WRITE_PDF = "write_pdf"
PHASE_QPDF = "qpdf"
PHASE_TOTAL = "total"

DOCUMENT_TYPE_HTML = "html"
DOCUMENT_TYPE_PDF = "pdf"

UNMATCHED_ENDPOINT = "unmatched"

_SECONDS_BUCKETS = (0.05, 0.1, 0.25, 0.5, 1, 2, 3, 5, 8, 13, 20, 30, 60)
_KIB = 1024
_SIZE_BUCKETS = tuple(kib * _KIB for kib in (10, 50, 100, 250, 500, 1000, 2500, 5000, 10000))
_PAGE_BUCKETS = (1, 2, 3, 5, 8, 13, 21, 34, 55, 89)

PHASE_SECONDS = Histogram(
    "pdfgen_phase_duration_seconds",
    "Wall-clock time per conversion phase",
    ["phase"],
    buckets=_SECONDS_BUCKETS,
)
PHASE_CPU_SECONDS = Histogram(
    "pdfgen_phase_cpu_seconds",
    "CPU time of the request thread per conversion phase (excludes qpdf, which runs as a subprocess)",
    ["phase"],
    buckets=_SECONDS_BUCKETS,
)
PAGES = Histogram(
    "pdfgen_pages",
    "Number of pages in each rendered PDF",
    buckets=_PAGE_BUCKETS,
)
DOCUMENT_BYTES = Histogram(
    "pdfgen_document_size_bytes",
    "Size of the HTML received and the PDF returned",
    ["type"],
    buckets=_SIZE_BUCKETS,
)
IN_PROGRESS = Gauge(
    "pdfgen_conversions_in_progress",
    "Conversions currently running, summed over all workers",
    multiprocess_mode="livesum",
)
REQUESTS = Counter(
    "pdfgen_requests",
    "Handled requests by endpoint and HTTP status",
    ["endpoint", "status"],
)


@contextmanager
def measure_phase(phase: str, timings: dict[str, float] | None = None) -> Iterator[None]:
    """Record wall and thread CPU time for ``phase``, also when it raises.

    If ``timings`` is given, the wall time in seconds is stored under ``phase``
    so the caller can log it.
    """
    wall_start = time.perf_counter()
    cpu_start = time.thread_time()
    try:
        yield
    finally:
        wall = time.perf_counter() - wall_start
        PHASE_SECONDS.labels(phase=phase).observe(wall)
        PHASE_CPU_SECONDS.labels(phase=phase).observe(time.thread_time() - cpu_start)
        if timings is not None:
            timings[phase] = wall


def record_request(endpoint: str | None, status: int) -> None:
    REQUESTS.labels(endpoint=endpoint or UNMATCHED_ENDPOINT, status=str(status)).inc()


def render_metrics() -> tuple[bytes, str]:
    """Return the exposition body and its content type."""
    if os.environ.get(MULTIPROC_DIR_ENV):
        registry = CollectorRegistry()
        multiprocess.MultiProcessCollector(registry)
        return generate_latest(registry), CONTENT_TYPE_LATEST
    return generate_latest(REGISTRY), CONTENT_TYPE_LATEST
