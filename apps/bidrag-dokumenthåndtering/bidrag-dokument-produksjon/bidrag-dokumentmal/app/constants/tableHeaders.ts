import type { Kolonne } from "~/components/DokumentTabell";
import { Styling } from "~/components/notat_felles/ThemeContext";
import { tekst } from "~/utils/tekst";
export const getInntektTableHeaders = (
  erBarnetillegg: boolean = false,
  erBidrag: boolean = false,
  inkluderKilde: boolean = true,
  styling: Styling,
) =>
  [
    !erBarnetillegg || styling == "V1"
      ? {
          tittel: tekst(
            "felles.tabell.felles.fraTilOgMed",
            "Fra og med - Til og med",
          ),
          bredde: inkluderKilde ? "170px" : erBarnetillegg ? "150px" : "345px",
        }
      : undefined,
    inkluderKilde
      ? {
          tittel: tekst("felles.tabell.felles.kilde", "Kilde"),
          bredde: erBarnetillegg ? "70px" : "230px",
        }
      : undefined,
    erBarnetillegg && erBidrag
      ? [
          { tittel: tekst("ord.type", "Type"), bredde: "160px" },
          { tittel: tekst("ord.beløp", "Beløp"), bredde: "80px" },
          {
            tittel: tekst("felles.tabell.inntekt.beløpstype", "Beløpstype"),
            bredde: "80px",
          },
          {
            tittel: tekst("felles.tabell.inntekt.skattesats", "Skattesats"),
            bredde: "80px",
          },
          {
            tittel: tekst("felles.tabell.inntekt.beløp12Mnd", "Beløp (12mnd)"),
            bredde: "120px",
          },
        ]
      : erBarnetillegg
        ? [
            { tittel: tekst("ord.type", "Type"), bredde: "160px" },
            { tittel: tekst("ord.beløp", "Beløp"), bredde: "80px" },
            {
              tittel: tekst("felles.tabell.inntekt.beløpstype", "Beløpstype"),
              bredde: "80px",
            },
            {
              tittel: tekst(
                "felles.tabell.inntekt.beløp12Mnd",
                "Beløp (12mnd)",
              ),
              bredde: "120px",
            },
          ]
        : {
            tittel: tekst("ord.beløp", "Beløp"),
            bredde: erBarnetillegg ? "140px" : undefined,
          },
  ]
    .filter((d) => d !== undefined)
    .flatMap((d) => d) as Kolonne[];
