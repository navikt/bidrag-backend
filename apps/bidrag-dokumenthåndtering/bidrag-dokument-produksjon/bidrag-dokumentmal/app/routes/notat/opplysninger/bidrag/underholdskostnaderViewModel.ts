import type { CalculationTableRow } from "~/components/CalculationTable";
import {
  InntektBelopstype,
  NotatUnderholdskostnadPeriodeBeregningsdetaljer,
} from "~/types/Api";
import {
  formatterBeløpForBeregning,
  formatterProsent,
} from "~/utils/visningsnavn";
import { tekst } from "~/utils/tekst";

type Beregningsdetaljer = NotatUnderholdskostnadPeriodeBeregningsdetaljer;

export function harTilleggsstønad(detaljer: Beregningsdetaljer): boolean {
  return detaljer.tilsynsutgifterBarn.some(
    ({ tilleggsstønadBeløp }) => tilleggsstønadBeløp !== undefined,
  );
}

export function lagFaktiskTilsynsutgiftRader(
  detaljer: Beregningsdetaljer,
): CalculationTableRow[] {
  return detaljer.tilsynsutgifterBarn.map((barn) => ({
    label: barn.gjelderBarn.navn,
    calculation: tekst(
      "notat.opplysninger.bidrag.underholdskostnader.faktiskTilsynsutgifter.beregning",
      "({totalTilsynsutgift} - {kostpenger}) x 11/12",
    )
      .replace(
        "{totalTilsynsutgift}",
        formatterBeløpForBeregning(barn.totalTilsynsutgift),
      )
      .replace(
        "{kostpenger}",
        formatterBeløpForBeregning(barn.kostpenger ?? undefined),
      ),
    result: formatterBeløpForBeregning(barn.faktiskUtgiftBeregnet),
  }));
}

export function lagTilleggsstønadRader(
  detaljer: Beregningsdetaljer,
): CalculationTableRow[] {
  return detaljer.tilsynsutgifterBarn
    .filter(({ tilleggsstønadBeløp }) => tilleggsstønadBeløp !== undefined)
    .map((barn) => ({
      label: barn.gjelderBarn.navn,
      calculation:
        barn.beløpstype === InntektBelopstype.DAGSATS
          ? tekst(
              "notat.opplysninger.bidrag.underholdskostnader.tilleggsstønadBeregning.beregningDagsats",
              "({beløp} x 260) / 12 x 11 / 12",
            ).replace(
              "{beløp}",
              formatterBeløpForBeregning(barn.tilleggsstønadBeløp ?? undefined),
            )
          : tekst(
              "notat.opplysninger.bidrag.underholdskostnader.tilleggsstønadBeregning.beregning",
              "{beløp} x 11 / 12",
            ).replace(
              "{beløp}",
              formatterBeløpForBeregning(barn.tilleggsstønadBeløp ?? undefined),
            ),
      result: formatterBeløpForBeregning(barn.tilleggsstønad ?? undefined),
    }));
}

export function lagTilsynsutgiftRader(
  detaljer: Beregningsdetaljer,
): CalculationTableRow[] {
  const barnRader = detaljer.tilsynsutgifterBarn.map((barn) => ({
    label: barn.gjelderBarn.navn,
    calculation: barn.tilleggsstønad
      ? tekst(
          "notat.opplysninger.bidrag.underholdskostnader.tilsynsutgifter.beregning",
          "{faktiskUtgift} - {tilleggsstønad}",
        )
          .replace(
            "{faktiskUtgift}",
            formatterBeløpForBeregning(barn.faktiskUtgiftBeregnet),
          )
          .replace(
            "{tilleggsstønad}",
            formatterBeløpForBeregning(barn.tilleggsstønad),
          )
      : undefined,
    result: formatterBeløpForBeregning(barn.beløp),
  }));

  return [
    ...barnRader,
    {
      label: tekst("ord.total", "Total"),
      labelBold: true,
      result: formatterBeløpForBeregning(detaljer.sumTilsynsutgifter),
    },
  ];
}

export function lagSkattefradragRader(
  detaljer: Beregningsdetaljer,
): CalculationTableRow[] {
  return [
    {
      label: tekst(
        "notat.opplysninger.bidrag.underholdskostnader.skattefradrag.maksFradrag",
        "Maks fradrag",
      ),
      calculation: tekst(
        "notat.opplysninger.bidrag.underholdskostnader.skattefradrag.maksFradragBeregning",
        "{sjablonMaksFradrag} x {skattesats}",
      )
        .replace(
          "{sjablonMaksFradrag}",
          formatterBeløpForBeregning(detaljer.sjablonMaksFradrag),
        )
        .replace("{skattesats}", formatterProsent(detaljer.skattesatsFaktor)),
      result: formatterBeløpForBeregning(detaljer.skattefradragMaksFradrag),
    },
    {
      label: tekst(
        "notat.opplysninger.bidrag.underholdskostnader.skattefradrag.totalTilsynsutgift",
        "Total tilsynsutgift",
      ),
      calculation: tekst(
        "notat.opplysninger.bidrag.underholdskostnader.skattefradrag.totalTilsynsutgiftBeregning",
        "{totalTilsynsutgift} x {skattesats}",
      )
        .replace(
          "{totalTilsynsutgift}",
          formatterBeløpForBeregning(detaljer.totalTilsynsutgift),
        )
        .replace("{skattesats}", formatterProsent(detaljer.skattesatsFaktor)),
      result: formatterBeløpForBeregning(
        detaljer.skattefradragTotalTilsynsutgift,
      ),
    },
    {
      label: tekst(
        "notat.opplysninger.bidrag.underholdskostnader.skattefradrag.lavesteVerdi",
        "Skattefradrag (laveste verdi)",
      ),
      labelBold: true,
      result: formatterBeløpForBeregning(detaljer.skattefradrag),
    },
  ];
}

export function lagBeregnetTilsynsutgiftRader(
  detaljer: Beregningsdetaljer,
): CalculationTableRow[] {
  return [
    {
      label: tekst(
        "notat.opplysninger.bidrag.underholdskostnader.beregnetTilsynsutgifter.bruttoBeløp",
        "Brutto beløp",
      ),
      calculation: detaljer.erBegrensetAvMaksTilsyn
        ? tekst(
            "notat.opplysninger.bidrag.underholdskostnader.beregnetTilsynsutgifter.bruttoBeløpBeregning",
            "{totalTilsynsutgift} x {fordelingFaktor}",
          )
            .replace(
              "{totalTilsynsutgift}",
              formatterBeløpForBeregning(detaljer.totalTilsynsutgift),
            )
            .replace(
              "{fordelingFaktor}",
              formatterProsent(detaljer.fordelingFaktor),
            )
        : undefined,
      result: formatterBeløpForBeregning(detaljer.justertBruttoTilsynsutgift),
    },
    {
      label: tekst(
        "notat.opplysninger.bidrag.underholdskostnader.beregnetTilsynsutgifter.skattefradragPerBarn",
        "Skattefradrag (per barn)",
      ),
      calculation: tekst(
        "notat.opplysninger.bidrag.underholdskostnader.beregnetTilsynsutgifter.skattefradragPerBarnBeregning",
        "{skattefradrag} / {antallBarn}",
      )
        .replace(
          "{skattefradrag}",
          formatterBeløpForBeregning(detaljer.skattefradrag),
        )
        .replace(
          "{antallBarn}",
          formatterBeløpForBeregning(detaljer.antallBarnMedTilsynsutgifter),
        ),
      result: tekst(
        "notat.opplysninger.bidrag.underholdskostnader.beregnetTilsynsutgifter.skattefradragPerBarnResultat",
        "- {}",
      ).replace(
        "{}",
        formatterBeløpForBeregning(detaljer.skattefradragPerBarn),
      ),
    },
    {
      label: tekst("ord.resultat", "Resultat"),
      labelBold: true,
      result: formatterBeløpForBeregning(detaljer.nettoTilsynsutgift),
    },
  ];
}
