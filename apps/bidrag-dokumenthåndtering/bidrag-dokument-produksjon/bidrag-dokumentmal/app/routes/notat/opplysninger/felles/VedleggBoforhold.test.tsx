import { readFileSync } from "node:fs";
import { join } from "node:path";
import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it } from "vitest";
import { ThemeProvider } from "~/components/notat_felles/ThemeContext";
import {
  NotatProvider,
  RenderMode,
  RenderPDFVersion,
} from "~/components/notat_felles/NotatContext";
import VedleggBoforhold, {
  BoforholdTable,
} from "~/routes/notat/opplysninger/felles/VedleggBoforhold";
import { Bostatuskode, SivilstandskodePDL, VedtakNotatDto } from "~/types/Api";

describe("BoforholdTable", () => {
  it("viser ikke tabellen når det ikke finnes opplysninger", () => {
    expect(
      renderToStaticMarkup(
        <ThemeProvider styling="V2">
          <BoforholdTable data={[]} />
        </ThemeProvider>,
      ),
    ).toBe("");
  });

  describe("VedleggBoforhold", () => {
    it("viser barn, andre voksne og bidragsmottakers boforhold i dokumentrekkefølge", () => {
      const data: VedtakNotatDto = JSON.parse(
        readFileSync(
          join(__dirname, "../../../../../../data/notat/bidrag.json"),
          "utf8",
        ),
      );
      const barn = data.boforhold.barn[0];
      const periode = { fom: "2020-01", til: null };
      barn.opplysningerFraFolkeregisteret = [
        {
          periode,
          status: Bostatuskode.MED_FORELDER,
          statusVisningsnavn: "Barnets status",
        },
      ];
      data.boforhold.andreVoksneIHusstanden = {
        opplysningerBruktTilBeregning: [],
        opplysningerFraFolkeregisteret: [
          {
            periode,
            status: Bostatuskode.BOR_IKKE_MED_ANDRE_VOKSNE,
            statusVisningsnavn: "Voksenstatus",
          },
        ],
      };
      data.boforhold.boforholdBMSøknadsbarn = [
        {
          gjelderBarn: barn.gjelder,
          perioder: [
            {
              periode,
              status: Bostatuskode.MED_FORELDER,
              statusVisningsnavn: "Mottakerstatus",
            },
          ],
        },
      ];

      const html = renderToStaticMarkup(
        <NotatProvider
          data={data}
          styling="V2"
          renderMode={RenderMode.PDF}
          renderPDFVersion={RenderPDFVersion.V2}
          vedlegg={["boforhold"]}
        >
          <VedleggBoforhold />
        </NotatProvider>,
      );

      expect(html).toContain("Barnets status");
      expect(html).toContain("Voksenstatus");
      expect(html).toContain("Mottakerstatus");
      expect(html.indexOf("Barnets status")).toBeLessThan(
        html.indexOf("Voksenstatus"),
      );
      expect(html.indexOf("Voksenstatus")).toBeLessThan(
        html.indexOf("Mottakerstatus"),
      );
    });
  });

  it("viser perioder, status og detaljer om andre voksne i riktig rekkefølge", () => {
    const html = renderToStaticMarkup(
      <ThemeProvider styling="V2">
        <BoforholdTable
          data={[
            {
              periode: { fom: "2020-01", til: "2020-03" },
              status: Bostatuskode.BOR_IKKE_MED_ANDRE_VOKSNE,
              statusVisningsnavn: "Bor alene",
            },
            {
              periode: { fom: "2020-03", til: null },
              status: Bostatuskode.BOR_MED_ANDRE_VOKSNE,
              statusVisningsnavn: "Bor med andre voksne",
              detaljer: {
                totalAntallHusstandsmedlemmer: 2,
                husstandsmedlemmer: [
                  {
                    navn: "Beskyttet",
                    fødselsdato: "1980-01-02",
                    erBeskyttet: true,
                    harRelasjonTilBp: false,
                  },
                  {
                    navn: "Ubeskyttet",
                    fødselsdato: "1990-04-03",
                    erBeskyttet: false,
                    harRelasjonTilBp: false,
                  },
                ],
              },
            },
          ]}
        />
      </ThemeProvider>,
    );

    expect(html).toContain("Bor med andre voksne (2)");
    expect(html.indexOf("Bor alene")).toBeLessThan(
      html.indexOf("Bor med andre voksne"),
    );
    expect(html).toContain("Beskyttet");
    expect(html).toContain("03.04.1990");
    expect(html).not.toContain("Ubeskyttet");
    expect(html).toContain('colSpan="2"');
  });

  it("viser sivilstand uten detaljrad", () => {
    const html = renderToStaticMarkup(
      <ThemeProvider styling="V1">
        <BoforholdTable
          data={[
            {
              periode: { fom: "2021-01", til: null },
              status: SivilstandskodePDL.GIFT,
              statusVisningsnavn: "Gift",
            },
          ]}
        />
      </ThemeProvider>,
    );

    expect(html).toContain("Gift");
    expect(html).not.toContain("table_row_details");
  });
});
