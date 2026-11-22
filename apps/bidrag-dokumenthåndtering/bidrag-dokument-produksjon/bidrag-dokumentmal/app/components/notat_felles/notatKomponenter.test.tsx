import { readFileSync } from "node:fs";
import { join } from "node:path";
import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it } from "vitest";
import {
  NotatDel,
  NotatProvider,
  RenderMode,
  RenderPDFVersion,
  Vedlegg,
} from "~/components/notat_felles/NotatContext";
import { Seksjon, Underseksjon } from "~/components/notat_felles/Seksjon";
import { ForHverRolle, useRolle } from "~/components/notat_felles/RolleContext";
import { Rolletype, VedtakNotatDto } from "~/types/Api";

const bidragNotat: VedtakNotatDto = JSON.parse(
  readFileSync(join(__dirname, "../../../../data/notat/bidrag.json"), "utf8"),
);

function render(
  innhold: React.ReactNode,
  vedlegg: Vedlegg[] = [],
  del?: NotatDel,
) {
  return renderToStaticMarkup(
    <NotatProvider
      data={bidragNotat}
      styling={"V2"}
      renderMode={RenderMode.PDF}
      renderPDFVersion={RenderPDFVersion.V2}
      vedlegg={vedlegg}
      del={del}
    >
      {innhold}
    </NotatProvider>,
  );
}

function VisRolle() {
  const rolle = useRolle();
  return <span>{rolle.rolle}</span>;
}

describe("Seksjon", () => {
  it("skal vise tittel og lenke til vedlegget med riktig nummer", () => {
    const html = render(
      <Seksjon
        tittel={"Inntekter"}
        vedlegg={"inntekter"}
        lenketekst={"se vedlegg nr. {}"}
      >
        <p>innhold</p>
      </Seksjon>,
      ["boforhold", "inntekter"],
    );
    expect(html).toBe(
      '<div class="elements_inline section-title"><h2>Inntekter</h2>' +
        '<a href="#vedlegg_inntekter">se vedlegg nr. 2</a></div><p>innhold</p>',
    );
  });

  it.each(["begrunnelse", "opplysninger"] as const)(
    "skal vise tittel uten lenke til vedlegget når bare %s rendres",
    (del) => {
      const html = render(
        <Seksjon
          tittel={"Inntekter"}
          vedlegg={"inntekter"}
          lenketekst={"se vedlegg nr. {}"}
        >
          <p>innhold</p>
        </Seksjon>,
        ["boforhold", "inntekter"],
        del,
      );
      expect(html).toBe(
        '<div class="elements_inline section-title"><h2>Inntekter</h2></div><p>innhold</p>',
      );
    },
  );
});

describe("Underseksjon", () => {
  it("skal ha avstand over som standard", () => {
    expect(render(<Underseksjon tittel={"Tabell"}>x</Underseksjon>)).toBe(
      '<div class="mb-4 mt-4"><h4>Tabell</h4>x</div>',
    );
  });

  it("skal ligge tett inntil det over når avstandOver er false", () => {
    expect(
      render(
        <Underseksjon tittel={"Tabell"} avstandOver={false}>
          x
        </Underseksjon>,
      ),
    ).toBe('<div class="mb-4"><h4>Tabell</h4>x</div>');
  });
});

describe("ForHverRolle", () => {
  it("skal vise innholdet én gang per rolle i rekkefølgen typene er gitt", () => {
    const html = render(
      <ForHverRolle typer={[Rolletype.BA, Rolletype.BM]}>
        <VisRolle />
      </ForHverRolle>,
    );
    expect(html).toBe(
      '<div class="mt-medium"><span>BA</span></div>' +
        '<div class="mt-medium"><span>BM</span></div>',
    );
  });

  it("skal ikke vise noe når ingen roller har typen", () => {
    const html = render(
      <ForHverRolle typer={[Rolletype.RM]}>
        <VisRolle />
      </ForHverRolle>,
    );
    expect(html).toBe("");
  });
});

describe("useRolle", () => {
  it("skal feile med tydelig melding når den brukes utenfor ForHverRolle", () => {
    expect(() => render(<VisRolle />)).toThrow(
      "useRolle kan bare brukes inne i ForHverRolle",
    );
  });
});
