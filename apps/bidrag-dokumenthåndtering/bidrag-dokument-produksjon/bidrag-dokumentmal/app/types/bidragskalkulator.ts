import { SpråkkodeIdKey, SpråkType } from "~/utils/oversettelser";

export type Bidragstype = "MOTTAKER" | "PLIKTIG";

export const bidragstypeTekster: Record<
  Bidragstype,
  Record<SpråkType, string>
> = {
  PLIKTIG: {
    nb: "Bidragspliktig",
    nn: "Bidragspliktig",
    en: "Support debtor",
  },
  MOTTAKER: {
    nb: "Bidragsmottaker",
    nn: "Bidragsmottakar",
    en: "Support recipient",
  },
};

export type Oppgjørsform = "PRIVAT" | "INNKREVING";

export const oppgjørsformTekster: Record<
  Oppgjørsform,
  Record<SpråkType, string>
> = {
  PRIVAT: {
    nb: "Vi ønsker å gjøre opp bidraget oss i mellom (privat).",
    nn: "Vi ønskjer å gjere opp bidraget oss i mellom (privat).",
    en: "We want to settle the support between us (privately).",
  },
  INNKREVING: {
    nb: "Vi ønsker at bidraget skal betales gjennom Skatteetaten v/ NAV Innkreving.",
    nn: "Vi ønskjer at bidraget skal betalast gjennom Skatteetaten v/ NAV Innkreving.",
    en: "We want the support to be paid through the Tax Administration / NAV Collection.",
  },
};

export interface IPerson {
  fornavn: string;
  etternavn: string;
  ident: string;
}

export interface IAndreBestemmelser {
  harAndreBestemmelser: boolean;
  beskrivelse?: string;
}

export type Vedleggskrav = "SENDES_MED_SKJEMA" | "INGEN_EKSTRA_DOKUMENTASJON";

export const vedleggskravTekster: Record<
  Vedleggskrav,
  Record<SpråkType, string>
> = {
  SENDES_MED_SKJEMA: {
    nb: "Jeg legger det ved dette skjemaet",
    nn: "Eg legg det ved dette skjemaet",
    en: "I'll attach it to this form",
  },
  INGEN_EKSTRA_DOKUMENTASJON: {
    nb: "Nei, jeg har ingen ekstra dokumentasjon jeg vil legge ved",
    nn: "Nei, eg har ingen ekstra dokumentasjon eg vil leggje ved",
    en: "No, I have no additional documentation to attach",
  },
};

export interface IBarnOgBidrag extends IPerson {
  sumBidrag: number; // Beløp i kroner
  fraDato: string;
}

export interface IBidrag {
  bidragPerMåned: number;
  fraDato: string;
  tilDato: string;
}

export interface IOppgjør {
  nyAvtale: boolean;
  oppgjørsformØnsket: Oppgjørsform;
  oppgjørsformIdag: Oppgjørsform;
}

export interface PrivatAvtaleBarnUnder18Dto {
  språk: SpråkkodeIdKey;
  bidragsmottaker: IPerson;
  bidragspliktig: IPerson;
  barn: IBarnOgBidrag[];
  oppgjør: IOppgjør;
  andreBestemmelser: IAndreBestemmelser;
  vedlegg: Vedleggskrav;
}

export interface PrivatAvtaleBarnOver18Dto
  extends Omit<PrivatAvtaleBarnUnder18Dto, "barn"> {
  bidrag: IBidrag[];
}

export type GenererPrivatAvtalePdfRequest =
  | {
      navSkjemaId: "AVTALE_OM_BARNEBIDRAG_UNDER_18";
      privatAvtalePdf: PrivatAvtaleBarnUnder18Dto;
    }
  | {
      navSkjemaId: "AVTALE_OM_BARNEBIDRAG_OVER_18";
      privatAvtalePdf: PrivatAvtaleBarnOver18Dto;
    };

export enum NavSkjemaId {
  AVTALE_OM_BARNEBIDRAG_UNDER_18 = "NAV 55-00.63",
  AVTALE_OM_BARNEBIDRAG_OVER_18 = "NAV 55-00.50",
}

export type NavSkjemaIdKey = keyof typeof NavSkjemaId;

export function kodeOfNavSkjemaIdKey(id: NavSkjemaIdKey): string {
  return NavSkjemaId[id];
}
