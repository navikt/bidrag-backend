import NotatSide, {
  notatAction,
  notatMeta,
} from "~/routes/notat/felles/NotatSide";
import tekster from "~/tekster/notat/begrunnelse/særbidrag.yaml";
import { vedleggSærbidrag } from "~/routes/notat/opplysninger/særbidrag/route";
import Utgifter from "~/routes/notat/begrunnelse/særbidrag/Utgifter";
import Inntekter from "~/routes/notat/begrunnelse/felles/Inntekter";
import Boforhold from "~/routes/notat/begrunnelse/felles/Boforhold";
import Vedtak from "~/routes/notat/begrunnelse/særbidrag/Vedtak";

// /notat/begrunnelse/særbidrag: bare begrunnelsen av særbidragsnotatet.
export const action = notatAction;
export const meta = notatMeta(tekster.tittel);

export default function NotatBegrunnelseSærbidrag() {
  return (
    <NotatSide
      id="særbidrag_notat"
      tittel={tekster.tittel}
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
