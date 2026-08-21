import {
  Rolletype,
  SoktAvType,
  DokumentmalPersonDto,
  InntektBelopstype,
} from "~/types/Api";
import { konverterRolletype } from "~/utils/converter-utils";
import { localeMap, SpråkType } from "./oversettelser";

export const sammenlignRoller = (rolle?: Rolletype, erLikRolle?: Rolletype) =>
  rolle != undefined &&
  erLikRolle != undefined &&
  (rolle == erLikRolle || rolle == rolleTilType[erLikRolle]);

export function beløpstypeVisningsnavn(type: InntektBelopstype) {
  if (type === InntektBelopstype.DAGSATS) return "Dagsats";
  return "Måned";
}
export function søktAvTilVisningsnavn(søktAv?: SoktAvType) {
  switch (søktAv) {
    case SoktAvType.NAV_INTERNASJONALT:
    case SoktAvType.NAV_BIDRAG:
      return "NAV";
    case SoktAvType.BARN18AR:
      return "Barn";
    case SoktAvType.BM_I_ANNEN_SAK:
      return "Bidragsmottaker i annen sak";
    case SoktAvType.KLAGE_ANKE:
      return "Klage";
    default:
      return capitalizeFirstLetter(søktAv);
  }
}
export const rolleTilVisningsnavnV2 = (rolle: DokumentmalPersonDto): string => {
  switch (konverterRolletype(rolle.rolle)) {
    case Rolletype.BM:
      return "Bidragsmottaker";
    case Rolletype.BP:
      return "Bidragspliktig";
    case Rolletype.BA:
      if (rolle.revurdering) {
        return "Revurderingsbarn";
      } else {
        return "Søknadsbarn";
      }
    default:
      return capitalizeFirstLetter(rolle.rolle) ?? "";
  }
};

export const rolleTilVisningsnavn = (rolle: Rolletype): string => {
  switch (konverterRolletype(rolle)) {
    case Rolletype.BM:
      return "Bidragsmottaker";
    case Rolletype.BP:
      return "Bidragspliktig";
    case Rolletype.BA:
      return "Søknadsbarn";
    default:
      return capitalizeFirstLetter(rolle) ?? "";
  }
};

export const rolleTilType = {
  [Rolletype.BA]: "BARN",
  [Rolletype.BM]: "BIDRAGSMOTTAKER",
  [Rolletype.BP]: "BIDRAGSPLIKTIG",
  [Rolletype.FR]: "FEILREGISTRERT",
  [Rolletype.RM]: "REELMOTTAKER",
};

export function capitalizeFirstLetter(str?: string) {
  if (!str) return str;
  return str.charAt(0).toUpperCase() + str.slice(1).toLowerCase();
}
export const formatterBeløpForBeregning = (
  beløp: number | string | undefined,

  visSymbol = false,
): string => {
  return (beløp ?? 0).toLocaleString("nb-NO", {
    // style: visSymbol ? "currency" : undefined,
    currency: "NOK",
    currencySign: "accounting",
    minimumFractionDigits: 0,
    maximumFractionDigits: 0,
    // currencyDisplay: visSymbol ? "symbol" : undefined,
  });
};
export const formatterBeløp = (
  beløp: number | string | undefined,

  visSymbol = false,
): string => {
  return (beløp ?? 0).toLocaleString("nb-NO", {
    // style: visSymbol ? "currency" : undefined,
    currency: "NOK",
    minimumFractionDigits: 0,
    maximumFractionDigits: 2,
    // currencyDisplay: visSymbol ? "symbol" : undefined,
  });
};
export const formatterProsent = (
  value: number | string | undefined,
): string => {
  if (!value) return "0%";
  const asNumber = typeof value == "string" ? parseFloat(value) : value;
  const percentageAsFraction = asNumber > 1 ? asNumber / 100 : asNumber;
  return percentageAsFraction.toLocaleString("nb-NO", {
    style: "percent",
    minimumFractionDigits: 0,
    maximumFractionDigits: 2,
  });
};

/** Formatterer et beløp med språkstøtte.
 * @param sum - Beløpet som skal formateres.
 * @param språk - Språkkode for formatering (standard er 'nb').
 * @returns Formatert beløp som en streng.
 * @example
 * formatterBeløpMedSpråk(2000, 'nb'); // Returneres "2 000 kr"
 * formatterBeløpMedSpråk(2000, 'en'); // Returneres "2,000 NOK"
 */
export const formatterBeløpMedSpråk = (
  sum: number,
  språk: SpråkType = "nb",
): string => {
  const locale = localeMap[språk] ?? "nb-NO";

  const formatted = sum.toLocaleString(locale, {
    minimumFractionDigits: 0,
    maximumFractionDigits: 0,
  });

  switch (språk) {
    case "nb":
    case "nn":
      return `${formatted} kr`; // Norsk bokmål/nynorsk → "2 000 kr"
    case "en":
      return `${formatted} NOK`; // Engelsk → "2,000 NOK"
    default:
      return `${formatted} kr`;
  }
};
