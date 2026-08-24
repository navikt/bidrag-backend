import tekster from "~/tekster";
import { TableHeader } from "~/components/CommonTable";
import { Styling } from "~/components/notat_felles/ThemeContext";

export const getInntektTableHeaders = (
  erBarnetillegg: boolean = false,
  erBidrag: boolean = false,
  inkluderKilde: boolean = true,
  styling: Styling,
) =>
  [
    !erBarnetillegg || styling == "V1"
      ? {
          name: tekster.tabell.felles.fraTilOgMed,
          width: inkluderKilde ? "170px" : erBarnetillegg ? "150px" : "345px",
        }
      : undefined,
    inkluderKilde
      ? {
          name: tekster.tabell.felles.kilde,
          width: erBarnetillegg ? "70px" : "230px",
        }
      : undefined,
    erBarnetillegg && erBidrag
      ? [
          { name: tekster.tabell.felles.type, width: "160px" },
          { name: tekster.tabell.inntekt.beløp, width: "80px" },
          { name: tekster.tabell.inntekt.beløpstype, width: "80px" },
          { name: tekster.tabell.inntekt.skattesats, width: "80px" },
          { name: tekster.tabell.inntekt.beløp12Mnd, width: "120px" },
        ]
      : erBarnetillegg
        ? [
            { name: tekster.tabell.felles.type, width: "160px" },
            { name: tekster.tabell.inntekt.beløp, width: "80px" },
            { name: tekster.tabell.inntekt.beløpstype, width: "80px" },
            { name: tekster.tabell.inntekt.beløp12Mnd, width: "120px" },
          ]
        : {
            name: tekster.tabell.inntekt.beløp,
            width: erBarnetillegg ? "140px" : undefined,
          },
  ]
    .filter((d) => d !== undefined)
    .flatMap((d) => d) as TableHeader[];
