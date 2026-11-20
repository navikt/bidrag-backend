import NotatSide, {
  notatAction,
  notatMeta,
} from "~/routes/notat/felles/NotatSide";
import {
  useNotatFelles,
  Vedlegg,
} from "~/components/notat_felles/NotatContext";
import { VedtakNotatDto, Vedtakstype } from "~/types/Api";
import tekster from "~/tekster/notat/opplysninger/bidrag.yaml";
import VedleggUnderholdskostnader from "~/routes/notat/opplysninger/bidrag/VedleggUnderholdskostnader";
import VedleggBoforhold from "~/routes/notat/opplysninger/felles/VedleggBoforhold";
import VedleggInntekter from "~/routes/notat/opplysninger/felles/VedleggInntekter";
import VedleggSamvær from "~/routes/notat/opplysninger/bidrag/VedleggSamvær";
import VedleggBeregningsDetaljer from "~/routes/notat/opplysninger/bidrag/VedleggBeregningsDetaljer";

// /notat/opplysninger/bidrag: bare vedleggene i bidragsnotatet.
export const action = notatAction;
export const meta = notatMeta(tekster.tittel);

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
      tittel={tekster.tittel}
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
