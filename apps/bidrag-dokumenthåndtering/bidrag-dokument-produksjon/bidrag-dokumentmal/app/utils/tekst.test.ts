import { describe, expect, it } from "vitest";
import { opprettTekstoversetter } from "./tekst";

describe("tekst", () => {
  it("returnerer norsk standardtekst", () => {
    const { i18n, tekst } = opprettTekstoversetter();

    expect(i18n.language).toBe("nb");
    expect(tekst("notat.bidrag.tittel", "Bidrag")).toBe("Bidrag");
  });

  it("bruker oversettelse fra katalogen", () => {
    const { tekst } = opprettTekstoversetter({
      nb: {
        translation: {
          notat: {
            bidrag: {
              tittel: "Oversatt tittel",
            },
          },
        },
      },
    });

    expect(tekst("notat.bidrag.tittel", "Bidrag")).toBe("Oversatt tittel");
  });

  it("returnerer norsk standardtekst når oversettelsen mangler", () => {
    const { tekst } = opprettTekstoversetter({
      no: {
        translation: {
          notat: {
            annenNokkel: "Annen tekst",
          },
        },
      },
    });

    expect(tekst("notat.ukjent", "Norsk sikkerhetsnett")).toBe(
      "Norsk sikkerhetsnett",
    );
  });

  it("bruker norsk som reservekatalog", () => {
    const { tekst } = opprettTekstoversetter({
      no: {
        translation: {
          notat: {
            bidrag: {
              tittel: "Norsk reservetittel",
            },
          },
        },
      },
    });

    expect(tekst("notat.bidrag.tittel", "Bidrag")).toBe("Norsk reservetittel");
  });

  it("interpolerer navngitte verdier uten å endre {}-plassholdere", () => {
    const { tekst } = opprettTekstoversetter();

    expect(tekst("hilsen", "Hei {{navn}}", { navn: "Ada" })).toBe("Hei Ada");
    expect(tekst("vedlegg", "Vedlegg nr. {}").replace("{}", "3")).toBe(
      "Vedlegg nr. 3",
    );
  });
});
