import { useActionData, ActionFunctionArgs } from "react-router";
import "../../style/style.css";
import tekster from "~/tekster";
import {
  NotatProvider,
  RenderMode,
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
import GebyrV2 from "~/routes/notat.bidrag/GebyrV2";

export async function action(args: ActionFunctionArgs) {
  return await parseRequestAction(args);
}

export function meta() {
  return [
    { title: tekster.titler.bidrag },
    { name: "description", content: tekster.titler.bidrag },
    { property: "author", content: "bidrag-dokument-produksjon" },
    { property: "subject", content: tekster.titler.bidrag },
  ];
}

export default function NotatBidrag() {
  const response = useActionData<NotatRequest>();
  if (response === undefined) {
    return <div>Oops</div>;
  }

  const data = response.data;
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
      >
        <HeaderFooter saker={Array.from(saker)} renderMode={renderMode} />
        <div className={"container page"}>
          <Soknaddetaljer gjelderFlereSaker={gjelderFlereSaker} />
          <NotatTittel title={tekster.titler.bidrag} />
          <Virkningstidspunkt />
          {data.behandling.vedtakstype === Vedtakstype.INNKREVING
            ? renderVisningInnkrevingsgrunnlag()
            : renderBidrag()}
        </div>
      </NotatProvider>
    </div>
  );
}
function renderBidrag() {
  return (
    <>
      <PrivatAvtale />
      <Underholdskostnad vedleggNummer={1} />
      <Inntekter vedleggNummer={2} />
      <GebyrV2 />
      <Boforhold vedleggNummer={3} />
      <Samvær vedleggNummer={4} />
      <Vedtak vedleggNummer={5} />
      <VedleggUnderholdskostnader vedleggNummer={1} />
      <VedleggBoforhold vedleggNummer={2} />
      <VedleggInntekter vedleggNummer={3} />
      <VedleggSamvær vedleggNummer={4} />
      <VedleggBeregningsDetaljer vedleggNummer={5} />
    </>
  );
}
function renderVisningInnkrevingsgrunnlag() {
  return (
    <>
      <PrivatAvtale />
      <Vedtak vedleggNummer={1} />
      <VedleggBeregningsDetaljer vedleggNummer={1} />
    </>
  );
}
