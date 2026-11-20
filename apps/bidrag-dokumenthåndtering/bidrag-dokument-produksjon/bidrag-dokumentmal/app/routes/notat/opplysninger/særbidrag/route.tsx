import NotatSide, {
  notatAction,
  notatMeta,
} from "~/routes/notat/felles/NotatSide";
import { Vedlegg } from "~/components/notat_felles/NotatContext";
import tekster from "~/tekster/notat/opplysninger/særbidrag.yaml";
import VedleggInntekter from "~/routes/notat/opplysninger/felles/VedleggInntekter";
import VedleggBoforhold from "~/routes/notat/opplysninger/felles/VedleggBoforhold";
import VedleggBeregningsDetaljer from "~/routes/notat/opplysninger/særbidrag/VedleggBeregningsDetaljer";

// /notat/opplysninger/særbidrag: bare vedleggene i særbidragsnotatet.
export const action = notatAction;
export const meta = notatMeta(tekster.tittel);

/** Vedleggene i særbidragsnotatet, i den rekkefølgen de står. Vedleggsnummeret følger av rekkefølgen. */
export function vedleggSærbidrag(): Vedlegg[] {
  return ["inntekter", "boforhold", "beregningsdetaljer"];
}

export default function NotatOpplysningerSærbidrag() {
  return (
    <NotatSide
      id="særbidrag_notat"
      tittel={tekster.tittel}
      vedlegg={vedleggSærbidrag}
    >
      <OpplysningerSærbidrag />
    </NotatSide>
  );
}

export function OpplysningerSærbidrag() {
  return (
    <>
      <VedleggInntekter />
      <VedleggBoforhold />
      <VedleggBeregningsDetaljer />
    </>
  );
}
