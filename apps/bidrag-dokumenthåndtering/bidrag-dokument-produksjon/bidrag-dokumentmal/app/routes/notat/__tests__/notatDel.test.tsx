import { readFileSync } from "node:fs";
import { join } from "node:path";
import { renderToStaticMarkup } from "react-dom/server";
import { createRoutesStub } from "react-router";
import { afterAll, beforeAll, describe, expect, it, vi } from "vitest";
import { RenderPDFVersion } from "~/components/notat_felles/NotatContext";
import { NotatRequest } from "~/routes/common";
import { VedtakNotatDto } from "~/types/Api";
import NotatForskudd from "~/routes/notat/forskudd";
import NotatSærbidrag from "~/routes/notat/særbidrag";
import NotatBidrag from "~/routes/notat/bidrag";
import NotatBegrunnelseForskudd from "~/routes/notat/begrunnelse/forskudd/route";
import NotatBegrunnelseSærbidrag from "~/routes/notat/begrunnelse/særbidrag/route";
import NotatBegrunnelseBidrag from "~/routes/notat/begrunnelse/bidrag/route";
import NotatOpplysningerForskudd from "~/routes/notat/opplysninger/forskudd/route";
import NotatOpplysningerSærbidrag from "~/routes/notat/opplysninger/særbidrag/route";
import NotatOpplysningerBidrag from "~/routes/notat/opplysninger/bidrag/route";

const EKSEMPELDATA = join(__dirname, "../../../../../data/notat");
const FAST_DATO = new Date("2026-01-15T12:00:00Z");
const ROUTE_ID = "notat";
const LENKETEKST = "se vedlegg nr.";
const LENKE_TIL_VEDLEGG = 'href="#vedlegg';
const FERDIGSTILT = "Ferdigstilt";
const SAKSBEHANDLER = "Saksbehandler Saksbehandlersen";

/** Eksemplene har ikke fattet vedtak. Testene fatter vedtaket, slik at signaturen vises. */
function lesEksempel(fil: string): VedtakNotatDto {
  const data: VedtakNotatDto = JSON.parse(
    readFileSync(join(EKSEMPELDATA, fil), "utf8"),
  );
  return {
    ...data,
    vedtak: {
      ...data.vedtak,
      erFattet: true,
      fattetAvSaksbehandler: SAKSBEHANDLER,
      fattetTidspunkt: "2026-01-14T10:00:00",
    },
  };
}

function renderRoute(Notat: () => React.ReactNode, data: VedtakNotatDto) {
  const actionData: NotatRequest = {
    data,
    renderForPdf: true,
    renderPDFVersion: RenderPDFVersion.V2,
  };
  const Stub = createRoutesStub([
    { id: ROUTE_ID, path: "/", Component: Notat },
  ]);
  return renderToStaticMarkup(
    <Stub hydrationData={{ actionData: { [ROUTE_ID]: actionData } }} />,
  );
}

function antall(html: string, tekst: string) {
  return html.split(tekst).length - 1;
}

const notater = [
  {
    navn: "forskudd",
    fil: "forskudd_innvilget.json",
    komplett: NotatForskudd,
    begrunnelse: NotatBegrunnelseForskudd,
    opplysninger: NotatOpplysningerForskudd,
  },
  {
    navn: "særbidrag",
    fil: "saerbidrag.json",
    komplett: NotatSærbidrag,
    begrunnelse: NotatBegrunnelseSærbidrag,
    opplysninger: NotatOpplysningerSærbidrag,
  },
  {
    navn: "bidrag",
    fil: "bidrag.json",
    komplett: NotatBidrag,
    begrunnelse: NotatBegrunnelseBidrag,
    opplysninger: NotatOpplysningerBidrag,
  },
];

describe("notatdeler", () => {
  beforeAll(() => {
    vi.useFakeTimers({ toFake: ["Date"] });
    vi.setSystemTime(FAST_DATO);
  });
  afterAll(() => vi.useRealTimers());

  describe.each(notater)(
    "$navn",
    ({ fil, komplett, begrunnelse, opplysninger }) => {
      const data = lesEksempel(fil);
      const roller = data.roller.map((r) => r.navn).filter((n) => n != null);

      it("skal vise lenker til vedleggene og signaturen til slutt i hele notatet", () => {
        const html = renderRoute(komplett, data);

        expect(html).toContain(LENKETEKST);
        expect(html).toContain(LENKE_TIL_VEDLEGG);
        expect(antall(html, FERDIGSTILT)).toBe(1);
        expect(html.lastIndexOf(FERDIGSTILT)).toBeGreaterThan(
          html.lastIndexOf('id="vedlegg'),
        );
      });

      it("skal vise begrunnelsen med søknadsdetaljer og signatur, uten lenker til vedleggene", () => {
        const html = renderRoute(begrunnelse, data);

        expect(roller.length).toBeGreaterThan(0);
        expect(html).toContain(data.saksnummer);
        roller.forEach((navn) => expect(html).toContain(navn));
        expect(html).not.toContain(LENKETEKST);
        expect(html).not.toContain(LENKE_TIL_VEDLEGG);
        expect(antall(html, FERDIGSTILT)).toBe(1);
        expect(html).toContain(SAKSBEHANDLER);
      });

      it("skal vise opplysningene med søknadsdetaljer og signatur", () => {
        const html = renderRoute(opplysninger, data);

        expect(html).toContain(data.saksnummer);
        roller.forEach((navn) => expect(html).toContain(navn));
        expect(html).not.toContain(LENKE_TIL_VEDLEGG);
        expect(antall(html, FERDIGSTILT)).toBe(1);
        expect(html).toContain(SAKSBEHANDLER);
      });
    },
  );

  it("skal ikke vise signaturen når vedtaket ikke er fattet", () => {
    const data = lesEksempel("forskudd_innvilget.json");
    const html = renderRoute(NotatBegrunnelseForskudd, {
      ...data,
      vedtak: { ...data.vedtak, erFattet: false },
    });

    expect(html).not.toContain(FERDIGSTILT);
  });
});
