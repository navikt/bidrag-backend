import ord from "~/tekster/ord.yaml";
import tekster from "~/tekster";
import type { Kolonne } from "~/components/DokumentTabell";
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
          tittel: tekster.tabell.felles.fraTilOgMed,
          bredde: inkluderKilde ? "170px" : erBarnetillegg ? "150px" : "345px",
        }
      : undefined,
    inkluderKilde
      ? {
          tittel: tekster.tabell.felles.kilde,
          bredde: erBarnetillegg ? "70px" : "230px",
        }
      : undefined,
    erBarnetillegg && erBidrag
      ? [
          { tittel: ord.type, bredde: "160px" },
          { tittel: ord.beløp, bredde: "80px" },
          { tittel: tekster.tabell.inntekt.beløpstype, bredde: "80px" },
          { tittel: tekster.tabell.inntekt.skattesats, bredde: "80px" },
          { tittel: tekster.tabell.inntekt.beløp12Mnd, bredde: "120px" },
        ]
      : erBarnetillegg
        ? [
            { tittel: ord.type, bredde: "160px" },
            { tittel: ord.beløp, bredde: "80px" },
            { tittel: tekster.tabell.inntekt.beløpstype, bredde: "80px" },
            { tittel: tekster.tabell.inntekt.beløp12Mnd, bredde: "120px" },
          ]
        : {
            tittel: ord.beløp,
            bredde: erBarnetillegg ? "140px" : undefined,
          },
  ]
    .filter((d) => d !== undefined)
    .flatMap((d) => d) as Kolonne[];
