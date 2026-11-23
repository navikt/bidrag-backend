import NotatSide, {
  notatAction,
  notatMeta,
} from "~/routes/notat/felles/NotatSide";
import { Vedlegg } from "~/components/notat_felles/NotatContext";
import VedleggBoforhold from "~/routes/notat/opplysninger/felles/VedleggBoforhold";
import VedleggInntekter from "~/routes/notat/opplysninger/felles/VedleggInntekter";
import { tekst } from "~/utils/tekst";
// /notat/opplysninger/forskudd: bare vedleggene i forskuddsnotatet.
export const action = notatAction;
export const meta = notatMeta(
  tekst(
    "notat.opplysninger.forskudd.tittel",
    "Forskudd, vedlegg til saksbehandlingsnotat",
  ),
);
/** Vedleggene i forskuddsnotatet, i den rekkefølgen de står. Vedleggsnummeret følger av rekkefølgen. */
export function vedleggForskudd(): Vedlegg[] {
  return ["boforhold", "inntekter"];
}
export default function NotatOpplysningerForskudd() {
  return (
    <NotatSide
      id="forskudd_notat"
      del="opplysninger"
      tittel={tekst(
        "notat.opplysninger.forskudd.tittel",
        "Forskudd, vedlegg til saksbehandlingsnotat",
      )}
      vedlegg={vedleggForskudd}
    >
      <OpplysningerForskudd />
    </NotatSide>
  );
}
export function OpplysningerForskudd() {
  return (
    <>
      <VedleggBoforhold />
      <VedleggInntekter />
    </>
  );
}
