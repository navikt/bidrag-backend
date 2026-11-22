import { readFileSync } from "node:fs";
import { join } from "node:path";
import { describe, expect, it } from "vitest";
import type { VedtakNotatDto } from "~/types/Api";
import {
  harTilleggsstønad,
  lagBeregnetTilsynsutgiftRader,
  lagFaktiskTilsynsutgiftRader,
  lagSkattefradragRader,
  lagTilleggsstønadRader,
  lagTilsynsutgiftRader,
} from "~/routes/notat/opplysninger/bidrag/underholdskostnaderViewModel";

const EKSEMPELDATA = join(__dirname, "../../../../../../data/notat");

function lesBeregningsdetaljer() {
  const data = JSON.parse(
    readFileSync(join(EKSEMPELDATA, "bidrag.json"), "utf8"),
  ) as VedtakNotatDto;
  const underholdskostnader = data.underholdskostnader;
  if (!underholdskostnader) {
    throw new Error("Eksempeldatasettet må inneholde underholdskostnader");
  }
  const detaljer = underholdskostnader.underholdskostnaderBarn
    .flatMap(({ underholdskostnad }) => underholdskostnad)
    .find(
      ({ beregningsdetaljer }) => beregningsdetaljer != null,
    )?.beregningsdetaljer;

  if (!detaljer) {
    throw new Error("Eksempeldatasettet må inneholde beregningsdetaljer");
  }
  return detaljer;
}

describe("underholdskostnaderViewModel", () => {
  it("creates one table row per child and a total row", () => {
    const detaljer = lesBeregningsdetaljer();

    expect(lagFaktiskTilsynsutgiftRader(detaljer)).toHaveLength(
      detaljer.tilsynsutgifterBarn.length,
    );
    expect(lagTilsynsutgiftRader(detaljer)).toHaveLength(
      detaljer.tilsynsutgifterBarn.length + 1,
    );
    expect(lagTilsynsutgiftRader(detaljer).at(-1)).toMatchObject({
      label: "Total",
      labelBold: true,
    });
  });

  it("builds stable row sets for tax deduction and net expenses", () => {
    const detaljer = lesBeregningsdetaljer();

    expect(lagSkattefradragRader(detaljer)).toHaveLength(3);
    expect(lagBeregnetTilsynsutgiftRader(detaljer)).toHaveLength(3);
    expect(lagBeregnetTilsynsutgiftRader(detaljer)[1].calculation).toContain(
      "/",
    );
  });

  it("uses the same optional-benefit filter for visibility and rows", () => {
    const detaljer = lesBeregningsdetaljer();
    const tilleggsstønadRader = lagTilleggsstønadRader(detaljer);
    const forventetAntall = detaljer.tilsynsutgifterBarn.filter(
      ({ tilleggsstønadBeløp }) => tilleggsstønadBeløp !== undefined,
    ).length;

    expect(harTilleggsstønad(detaljer)).toBe(forventetAntall > 0);
    expect(tilleggsstønadRader).toHaveLength(forventetAntall);
  });
});
