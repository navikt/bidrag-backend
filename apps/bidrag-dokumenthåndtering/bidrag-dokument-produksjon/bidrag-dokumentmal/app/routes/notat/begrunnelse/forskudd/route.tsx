import NotatSide, {
  notatAction,
  notatMeta,
} from "~/routes/notat/felles/NotatSide";
import tekster from "~/tekster/notat/begrunnelse/forskudd.yaml";
import { vedleggForskudd } from "~/routes/notat/opplysninger/forskudd/route";
import Virkningstidspunkt from "~/routes/notat/begrunnelse/felles/Virkningstidspunkt";
import Boforhold from "~/routes/notat/begrunnelse/felles/Boforhold";
import Inntekter from "~/routes/notat/begrunnelse/felles/Inntekter";
import Vedtak from "~/routes/notat/begrunnelse/forskudd/Vedtak";

// /notat/begrunnelse/forskudd: bare begrunnelsen av forskuddsnotatet.
export const action = notatAction;
export const meta = notatMeta(tekster.tittel);

export default function NotatBegrunnelseForskudd() {
  return (
    <NotatSide
      id="forskudd_notat"
      tittel={tekster.tittel}
      vedlegg={vedleggForskudd}
    >
      <BegrunnelseForskudd />
    </NotatSide>
  );
}

/** Delen saksbehandleren har skrevet og vurdert, i den rekkefølgen den står i notatet. */
export function BegrunnelseForskudd() {
  return (
    <>
      <Virkningstidspunkt />
      <Boforhold />
      <Inntekter />
      <Vedtak />
    </>
  );
}
