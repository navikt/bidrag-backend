import NotatSide, {
  notatAction,
  notatMeta,
} from "~/routes/notat/felles/NotatSide";
import tekster from "~/tekster/notat/begrunnelse/forskudd.yaml";
import { BegrunnelseForskudd } from "~/routes/notat/begrunnelse/forskudd/route";
import {
  OpplysningerForskudd,
  vedleggForskudd,
} from "~/routes/notat/opplysninger/forskudd/route";

// /notat/forskudd: hele forskuddsnotatet, først begrunnelsen og så vedleggene med opplysninger.
export const action = notatAction;
export const meta = notatMeta(tekster.tittel);

export default function NotatForskudd() {
  return (
    <NotatSide
      id="forskudd_notat"
      tittel={tekster.tittel}
      vedlegg={vedleggForskudd}
    >
      <BegrunnelseForskudd />
      <OpplysningerForskudd />
    </NotatSide>
  );
}
