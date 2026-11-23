import NotatSide, {
  notatAction,
  notatMeta,
} from "~/routes/notat/felles/NotatSide";
import { BegrunnelseSærbidrag } from "~/routes/notat/begrunnelse/særbidrag/route";
import {
  OpplysningerSærbidrag,
  vedleggSærbidrag,
} from "~/routes/notat/opplysninger/særbidrag/route";
import { tekst } from "~/utils/tekst";
// /notat/særbidrag: hele særbidragsnotatet, først begrunnelsen og så vedleggene med opplysninger.
export const action = notatAction;
export const meta = notatMeta(
  tekst(
    "notat.begrunnelse.særbidrag.tittel",
    "Særbidrag, saksbehandlingsnotat",
  ),
);
export default function NotatSærbidrag() {
  return (
    <NotatSide
      id="særbidrag_notat"
      del="komplett"
      tittel={tekst(
        "notat.begrunnelse.særbidrag.tittel",
        "Særbidrag, saksbehandlingsnotat",
      )}
      vedlegg={vedleggSærbidrag}
    >
      <BegrunnelseSærbidrag />
      <OpplysningerSærbidrag />
    </NotatSide>
  );
}
