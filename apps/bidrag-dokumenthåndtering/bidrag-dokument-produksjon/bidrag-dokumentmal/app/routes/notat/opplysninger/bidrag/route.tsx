import NotatSide, {
  notatAction,
  notatMeta,
} from "~/routes/notat/felles/NotatSide";
import {
  useNotatFelles,
  Vedlegg,
} from "~/components/notat_felles/NotatContext";
import { VedtakNotatDto, Vedtakstype } from "~/types/Api";
import VedleggUnderholdskostnader from "~/routes/notat/opplysninger/bidrag/VedleggUnderholdskostnader";
import VedleggBoforhold from "~/routes/notat/opplysninger/felles/VedleggBoforhold";
import VedleggInntekter from "~/routes/notat/opplysninger/felles/VedleggInntekter";
import VedleggSamvær from "~/routes/notat/opplysninger/bidrag/VedleggSamvær";
import VedleggBeregningsDetaljer from "~/routes/notat/opplysninger/bidrag/VedleggBeregningsDetaljer";
import { tekst } from "~/utils/tekst";
// /notat/opplysninger/bidrag: bare vedleggene i bidragsnotatet.
export const action = notatAction;
export const meta = notatMeta(
  tekst(
    "notat.opplysninger.bidrag.tittel",
    "Bidrag, vedlegg til saksbehandlingsnotat",
  ),
);
/**
 * Vedleggene i bidragsnotatet, i den rekkefølgen de står. Vedleggsnummeret følger av rekkefølgen.
 * Ved innkreving er det bare beregningsdetaljene.
 */
export function vedleggBidrag(data: VedtakNotatDto): Vedlegg[] {
  if (data.behandling.vedtakstype === Vedtakstype.INNKREVING) {
    return ["beregningsdetaljer"];
  }
  return [
    "underholdskostnader",
    "boforhold",
    "inntekter",
    "samvær",
    "beregningsdetaljer",
  ];
}
export default function NotatOpplysningerBidrag() {
  return (
    <NotatSide
      id="bidrag_notat"
      del="opplysninger"
      tittel={tekst(
        "notat.opplysninger.bidrag.tittel",
        "Bidrag, vedlegg til saksbehandlingsnotat",
      )}
      vedlegg={vedleggBidrag}
      kanGjeldeFlereSaker
    >
      <OpplysningerBidrag />
    </NotatSide>
  );
}
export function OpplysningerBidrag() {
  const { erInnkrevingsgrunnlag } = useNotatFelles();
  return (
    <>
      {!erInnkrevingsgrunnlag && (
        <>
          <VedleggUnderholdskostnader />
          <VedleggBoforhold />
          <VedleggInntekter />
          <VedleggSamvær />
        </>
      )}
      <VedleggBeregningsDetaljer />
    </>
  );
}
