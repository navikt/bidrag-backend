import { useActionData, ActionFunctionArgs } from "react-router";
import "../../style/style.css";
import bidragTekster from "~/tekster/notat/bidrag/saksbehandler.yaml";
import {
  NotatProvider,
  RenderMode,
  Vedlegg,
} from "~/components/notat_felles/NotatContext";
import Boforhold from "~/components/notat_felles/components/Boforhold";
import Soknaddetaljer from "~/components/notat_felles/components/Soknaddetaljer";
import Inntekter from "~/components/notat_felles/components/Inntekter";
import VedleggBoforhold from "~/components/notat_felles/components/VedleggBoforhold";
import VedleggInntekter from "~/components/notat_felles/components/VedleggInntekter";
import NotatTittel from "~/components/NotatTittel";
import HeaderFooter from "~/components/notat_felles/HeaderFooter";
import { NotatRequest, parseRequestAction } from "~/routes/common";
import Virkningstidspunkt from "~/components/notat_felles/components/Virkningstidspunkt";
import Underholdskostnad from "~/routes/notat.bidrag/Underholdskostnad";
import Samvær from "~/routes/notat.bidrag/Samvær";
import Vedtak from "~/routes/notat.bidrag/Vedtak";
import VedleggBeregningsDetaljer from "~/routes/notat.bidrag/VedleggBeregningsDetaljer";
import VedleggSamvær from "~/routes/notat.bidrag/VedleggSamvær";
import VedleggUnderholdskostnader from "~/routes/notat.bidrag/VedleggUnderholdskostnader";
import PrivatAvtale from "~/routes/notat.bidrag/PrivatAvtale";
import { Vedtakstype } from "~/types/Api";
import Gebyr from "~/routes/notat.bidrag/Gebyr";

export async function action(args: ActionFunctionArgs) {
  return await parseRequestAction(args);
}

export function meta() {
  return [
    { title: bidragTekster.tittel },
    { name: "description", content: bidragTekster.tittel },
    { property: "author", content: "bidrag-dokument-produksjon" },
    { property: "subject", content: bidragTekster.tittel },
  ];
}

// Vedleggene står i den rekkefølgen de kommer i notatet. Nummeret til hvert vedlegg følger av rekkefølgen.
const vedleggBidrag: Vedlegg[] = [
  "underholdskostnader",
  "boforhold",
  "inntekter",
  "samvær",
  "beregningsdetaljer",
];
const vedleggInnkreving: Vedlegg[] = ["beregningsdetaljer"];

export default function NotatBidrag() {
  const response = useActionData<NotatRequest>();
  if (response === undefined) {
    return <div>Oops</div>;
  }

  const data = response.data;
  const erInnkreving = data.behandling.vedtakstype === Vedtakstype.INNKREVING;
  const saker = new Set(
    response.data.personer
      .map((p) => p.saksnummer)
      .filter((p) => p != undefined),
  );
  const renderMode = response.renderForPdf ? RenderMode.PDF : RenderMode.HTML;
  const gjelderFlereSaker =
    new Set(
      response.data.roller
        .filter((b) => b.saksnummer !== undefined)
        .map((b) => b.saksnummer),
    ).size > 1;
  return (
    <div id="bidrag_notat">
      <NotatProvider
        data={data}
        styling={"V2"}
        renderPDFVersion={response.renderPDFVersion}
        renderMode={response.renderForPdf ? RenderMode.PDF : RenderMode.HTML}
        vedlegg={erInnkreving ? vedleggInnkreving : vedleggBidrag}
      >
        <HeaderFooter saker={Array.from(saker)} renderMode={renderMode} />
        <div className={"container page"}>
          <Soknaddetaljer gjelderFlereSaker={gjelderFlereSaker} />
          <NotatTittel title={bidragTekster.tittel} />
          <Virkningstidspunkt />
          {erInnkreving ? (
            <>
              <NotatSaksbehandlerInnkreving />
              <NotatOpplysningerInnkreving />
            </>
          ) : (
            <>
              <NotatSaksbehandlerBidrag />
              <NotatOpplysningerBidrag />
            </>
          )}
        </div>
      </NotatProvider>
    </div>
  );
}

function NotatSaksbehandlerBidrag() {
  return (
    <>
      <PrivatAvtale />
      <Underholdskostnad />
      <Inntekter />
      <Gebyr />
      <Boforhold />
      <Samvær />
      <Vedtak />
    </>
  );
}

// Opplysningene (vedleggene) holdes samlet slik at de senere kan flyttes til en egen route.
function NotatOpplysningerBidrag() {
  return (
    <>
      <VedleggUnderholdskostnader />
      <VedleggBoforhold />
      <VedleggInntekter />
      <VedleggSamvær />
      <VedleggBeregningsDetaljer />
    </>
  );
}

function NotatSaksbehandlerInnkreving() {
  return (
    <>
      <PrivatAvtale />
      <Vedtak />
    </>
  );
}

function NotatOpplysningerInnkreving() {
  return <VedleggBeregningsDetaljer />;
}
