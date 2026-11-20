import NotatSide, {
  notatAction,
  notatMeta,
} from "~/routes/notat/felles/NotatSide";
import tekster from "~/tekster/notat/begrunnelse/særbidrag.yaml";
import { BegrunnelseSærbidrag } from "~/routes/notat/begrunnelse/særbidrag/route";
import {
  OpplysningerSærbidrag,
  vedleggSærbidrag,
} from "~/routes/notat/opplysninger/særbidrag/route";

// /notat/særbidrag: hele særbidragsnotatet, først begrunnelsen og så vedleggene med opplysninger.
export const action = notatAction;
export const meta = notatMeta(tekster.tittel);

export default function NotatSærbidrag() {
  return (
    <NotatSide
      id="særbidrag_notat"
      tittel={tekster.tittel}
      vedlegg={vedleggSærbidrag}
    >
      <BegrunnelseSærbidrag />
      <OpplysningerSærbidrag />
    </NotatSide>
  );
}
