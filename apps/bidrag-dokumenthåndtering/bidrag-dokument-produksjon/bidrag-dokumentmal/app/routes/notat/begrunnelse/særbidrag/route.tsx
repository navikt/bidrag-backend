import NotatSide, {
  notatAction,
  notatMeta,
} from "~/routes/notat/felles/NotatSide";
import { vedleggSærbidrag } from "~/routes/notat/opplysninger/særbidrag/route";
import Utgifter from "~/routes/notat/begrunnelse/særbidrag/Utgifter";
import Inntekter from "~/routes/notat/begrunnelse/felles/Inntekter";
import Boforhold from "~/routes/notat/begrunnelse/felles/Boforhold";
import Vedtak from "~/routes/notat/begrunnelse/særbidrag/Vedtak";
import { tekst } from "~/utils/tekst";
// /notat/begrunnelse/særbidrag: bare begrunnelsen av særbidragsnotatet.
export const action = notatAction;
export const meta = notatMeta(
  tekst(
    "notat.begrunnelse.særbidrag.tittel",
    "Særbidrag, saksbehandlingsnotat",
  ),
);
export default function NotatBegrunnelseSærbidrag() {
  return (
    <NotatSide
      id="særbidrag_notat"
      del="begrunnelse"
      tittel={tekst(
        "notat.begrunnelse.særbidrag.tittel",
        "Særbidrag, saksbehandlingsnotat",
      )}
      vedlegg={vedleggSærbidrag}
    >
      <BegrunnelseSærbidrag />
    </NotatSide>
  );
}
/** Delen saksbehandleren har skrevet og vurdert, i den rekkefølgen den står i notatet. */
export function BegrunnelseSærbidrag() {
  return (
    <>
      <Utgifter />
      <Inntekter />
      <Boforhold />
      <Vedtak />
    </>
  );
}
