import NotatSide, {
  notatAction,
  notatMeta,
} from "~/routes/notat/felles/NotatSide";
import { Vedlegg } from "~/components/notat_felles/NotatContext";
import VedleggInntekter from "~/routes/notat/opplysninger/felles/VedleggInntekter";
import VedleggBoforhold from "~/routes/notat/opplysninger/felles/VedleggBoforhold";
import VedleggBeregningsDetaljer from "~/routes/notat/opplysninger/særbidrag/VedleggBeregningsDetaljer";
import { tekst } from "~/utils/tekst";
// /notat/opplysninger/særbidrag: bare vedleggene i særbidragsnotatet.
export const action = notatAction;
export const meta = notatMeta(
  tekst(
    "notat.opplysninger.særbidrag.tittel",
    "Særbidrag, vedlegg til saksbehandlingsnotat",
  ),
);
/** Vedleggene i særbidragsnotatet, i den rekkefølgen de står. Vedleggsnummeret følger av rekkefølgen. */
export function vedleggSærbidrag(): Vedlegg[] {
  return ["inntekter", "boforhold", "beregningsdetaljer"];
}
export default function NotatOpplysningerSærbidrag() {
  return (
    <NotatSide
      id="særbidrag_notat"
      del="opplysninger"
      tittel={tekst(
        "notat.opplysninger.særbidrag.tittel",
        "Særbidrag, vedlegg til saksbehandlingsnotat",
      )}
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
