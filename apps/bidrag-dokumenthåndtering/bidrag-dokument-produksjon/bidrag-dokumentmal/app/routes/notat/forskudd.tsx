import NotatSide, {
  notatAction,
  notatMeta,
} from "~/routes/notat/felles/NotatSide";
import { BegrunnelseForskudd } from "~/routes/notat/begrunnelse/forskudd/route";
import {
  OpplysningerForskudd,
  vedleggForskudd,
} from "~/routes/notat/opplysninger/forskudd/route";
import { tekst } from "~/utils/tekst";
// /notat/forskudd: hele forskuddsnotatet, først begrunnelsen og så vedleggene med opplysninger.
export const action = notatAction;
export const meta = notatMeta(
  tekst("notat.begrunnelse.forskudd.tittel", "Forskudd, saksbehandlingsnotat"),
);
export default function NotatForskudd() {
  return (
    <NotatSide
      id="forskudd_notat"
      del="komplett"
      tittel={tekst(
        "notat.begrunnelse.forskudd.tittel",
        "Forskudd, saksbehandlingsnotat",
      )}
      vedlegg={vedleggForskudd}
    >
      <BegrunnelseForskudd />
      <OpplysningerForskudd />
    </NotatSide>
  );
}
