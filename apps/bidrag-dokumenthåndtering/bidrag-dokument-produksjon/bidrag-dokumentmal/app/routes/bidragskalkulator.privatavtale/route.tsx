/* eslint-disable prettier/prettier */
import "../../style/style.css";

import { parseRequestAction } from "~/routes/common";
import { ActionFunctionArgs, useActionData } from "react-router";
import HeaderFooter from "~/features/bidragskalkulator/HeaderFooterKalkulator";
import NavLogo from "~/components/NavLogo";
import {
  kodeOfNavSkjemaIdKey,
  GenererPrivatAvtalePdfRequest,
} from "~/types/bidragskalkulator";
import { hentTekst, språkkodeTilSpråkType } from "~/utils/oversettelser";
import PrivatAvtaleInnhold from "~/features/bidragskalkulator/PrivatAvtaleInnhold";

// Mock data for development
// eslint-disable-next-line unused-imports/no-unused-vars, @typescript-eslint/no-unused-vars
const mockUnder18Request: GenererPrivatAvtalePdfRequest = {
  privatAvtalePdf: {
    språk: "NB",
    bidragsmottaker: {
      fornavn: "Kristian",
      etternavn: "Etternavnesen",
      ident: "12345678901",
    },
    bidragspliktig: {
      fornavn: "Kristine",
      etternavn: "Etternavnesen",
      ident: "12345678901",
    },
    barn: [
      {
        fornavn: "Ola",
        etternavn: "Etternavnesen",
        ident: "12345678901",
        sumBidrag: 5000,
        fraDato: "01.01.2025",
      },
      {
        fornavn: "Ola2",
        etternavn: "Etternavnesen2",
        ident: "12345678901",
        sumBidrag: 5000,
        fraDato: "01.01.2025",
      },
    ],
    oppgjør: {
      nyAvtale: true,
      oppgjørsformØnsket: "PRIVAT",
      oppgjørsformIdag: "PRIVAT",
    },
    andreBestemmelser: {
      harAndreBestemmelser: true,
      beskrivelse: "Dette er en beskrivelse av andre bestemmelser.",
    },
    vedlegg: "INGEN_EKSTRA_DOKUMENTASJON",
  },
  navSkjemaId: "AVTALE_OM_BARNEBIDRAG_UNDER_18",
};
const mockOver18Request: GenererPrivatAvtalePdfRequest = {
  privatAvtalePdf: {
    språk: "NB",
    bidragsmottaker: {
      fornavn: "Kristian",
      etternavn: "Etternavnesen",
      ident: "12345678901",
    },
    bidragspliktig: {
      fornavn: "Kristine",
      etternavn: "Etternavnesen",
      ident: "12345678901",
    },
    bidrag: [
      {
        bidragPerMåned: 2000,
        fraDato: "2025-01",
        tilDato: "2025-02",
      },
      {
        bidragPerMåned: 3000,
        fraDato: "2025-03",
        tilDato: "2025-06",
      },
    ],
    oppgjør: {
      nyAvtale: true,
      oppgjørsformØnsket: "PRIVAT",
      oppgjørsformIdag: "PRIVAT",
    },
    andreBestemmelser: {
      harAndreBestemmelser: true,
      beskrivelse: "Dette er en beskrivelse av andre bestemmelser.",
    },
    vedlegg: "INGEN_EKSTRA_DOKUMENTASJON",
  },
  navSkjemaId: "AVTALE_OM_BARNEBIDRAG_OVER_18",
};

export function meta() {
  return [
    { title: "Privat avtale bidragskalkulator" },
    { name: "description", content: "Privat avtale bidragskalkulator" },
    { property: "author", content: "bidrag-dokument-produksjon" },
    { property: "subject", content: "Privat avtale bidragskalkulator" },
  ];
}

export async function action(args: ActionFunctionArgs) {
  return await parseRequestAction(args);
}

export default function PrivatAvtaleBidragskalkulator() {
  const actionData = useActionData<{ data: GenererPrivatAvtalePdfRequest }>();
  const response =
    process.env.NODE_ENV === "development"
      ? { data: mockOver18Request }
      : actionData;

  if (response === undefined) {
    return <div>Oops</div>;
  }

  const { privatAvtalePdf, navSkjemaId } = response.data;

  const erPrivatAvtaleBarnOver18 =
    navSkjemaId === "AVTALE_OM_BARNEBIDRAG_OVER_18";
  const språk = språkkodeTilSpråkType(privatAvtalePdf.språk) ?? "nb";
  const tekster = hentTekst(språk, tekst);

  return (
    <div id="privat_avtale" lang={språk}>
      <HeaderFooter språk={språk} />
      <div className="bidragskalkulatorContainer">
        <NavLogo />
        <h1>
          {erPrivatAvtaleBarnOver18
            ? tekster.barnOver18.tittel
            : tekster.barnUnder18.tittel}
        </h1>
        <p>{kodeOfNavSkjemaIdKey(navSkjemaId)}</p>
        <PrivatAvtaleInnhold språk={språk} data={response.data} />
      </div>
    </div>
  );
}
const tekst = {
  barnUnder18: {
    nb: {
      tittel: "Privat avtale om barnebidrag",
    },
    nn: {
      tittel: "Privat avtale om barnebidrag",
    },
    en: {
      tittel: "Private agreement on child support",
    },
  },
  barnOver18: {
    nb: {
      tittel: "Privat avtale om barnebidrag for barn over 18 år",
    },
    nn: {
      tittel: "Privat avtale om barnebidrag for barn over 18 år",
    },
    en: {
      tittel: "Private agreement on child support for children over 18 years",
    },
  },
};
