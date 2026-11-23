import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it } from "vitest";
import {
  DokumentTabell,
  Tabellcelle,
  Tabelldetaljer,
  Tabellrad,
} from "~/components/DokumentTabell";
import {
  ThemeProvider,
  type Styling,
} from "~/components/notat_felles/ThemeContext";

const kolonner = [{ tittel: "Periode", bredde: "140px" }, { tittel: "Status" }];
const rader = [
  { periode: "2020", status: "Første", detaljer: null },
  { periode: "2021", status: "Andre", detaljer: "Detaljer" },
];

describe("DokumentTabell", () => {
  it("krever at celler og detaljer ligger i en tabell og at celler ligger i en rad", () => {
    expect(() => renderToStaticMarkup(<Tabellrad index={0} />)).toThrow(
      "Tabellrader og celler må brukes i DokumentTabell",
    );
    expect(() => renderToStaticMarkup(<Tabelldetaljer index={0} />)).toThrow(
      "Tabellrader og celler må brukes i DokumentTabell",
    );
    expect(() =>
      renderToStaticMarkup(
        <ThemeProvider styling="V2">
          <DokumentTabell kolonner={kolonner}>
            <Tabellcelle>Verdi</Tabellcelle>
          </DokumentTabell>
        </ThemeProvider>,
      ),
    ).toThrow("Tabellcelle må brukes i Tabellrad");
  });

  it.each<Styling>(["V1", "V2"])(
    "skal bevare HTML for %s med detaljer på en stripet rad",
    (styling) => {
      const html = renderToStaticMarkup(
        <ThemeProvider styling={styling}>
          <DokumentTabell kolonner={kolonner} bredde="400px">
            {rader.map((rad, index) => (
              <TabellradMedDetaljer key={rad.periode} rad={rad} index={index} />
            ))}
          </DokumentTabell>
        </ThemeProvider>,
      );
      expect(html).toMatchSnapshot(styling);
    },
  );

  it("skal støtte periode, span, justering og radvarianter", () => {
    const html = renderToStaticMarkup(
      <ThemeProvider styling="V2">
        <DokumentTabell
          kolonner={[
            { tittel: "Beskrivelse" },
            { tittel: "Verdi", tekstjustering: "right" },
          ]}
          bredde="500px"
          layoutAuto
        >
          <Tabellrad index={1} periode>
            <Tabellcelle fullSpan>Mai 2025</Tabellcelle>
          </Tabellrad>
          <Tabellrad index={0} skipBorderBottom harPeriode borderBottom>
            <Tabellcelle colSpanNegative={1} labelBold textAlign="center">
              Midlertidig
            </Tabellcelle>
            <Tabellcelle textAlign="right">100</Tabellcelle>
          </Tabellrad>
          <Tabellrad
            index={1}
            skipPadding
            skipBorderBottom
            zebraStripe={false}
            className="pt-2"
          >
            <Tabellcelle colSpan={2}>Forklaring</Tabellcelle>
          </Tabellrad>
        </DokumentTabell>
      </ThemeProvider>,
    );

    expect(html).toMatchSnapshot();
  });
});

function TabellradMedDetaljer({
  rad,
  index,
}: {
  rad: (typeof rader)[number];
  index: number;
}) {
  return (
    <>
      <Tabellrad index={index}>
        <Tabellcelle>{rad.periode}</Tabellcelle>
        <Tabellcelle>{rad.status}</Tabellcelle>
      </Tabellrad>
      {rad.detaljer && (
        <Tabelldetaljer index={index}>
          <strong>{rad.detaljer}</strong>
        </Tabelldetaljer>
      )}
    </>
  );
}
