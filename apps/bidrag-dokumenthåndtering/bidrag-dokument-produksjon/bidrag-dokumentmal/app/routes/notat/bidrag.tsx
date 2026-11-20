import NotatSide, {
  notatAction,
  notatMeta,
} from "~/routes/notat/felles/NotatSide";
import tekster from "~/tekster/notat/begrunnelse/bidrag.yaml";
import { BegrunnelseBidrag } from "~/routes/notat/begrunnelse/bidrag/route";
import {
  OpplysningerBidrag,
  vedleggBidrag,
} from "~/routes/notat/opplysninger/bidrag/route";

// /notat/bidrag: hele bidragsnotatet, først begrunnelsen og så vedleggene med opplysninger.
export const action = notatAction;
export const meta = notatMeta(tekster.tittel);

export default function NotatBidrag() {
  return (
    <NotatSide
      id="bidrag_notat"
      tittel={tekster.tittel}
      vedlegg={vedleggBidrag}
      kanGjeldeFlereSaker
    >
      <BegrunnelseBidrag />
      <OpplysningerBidrag />
    </NotatSide>
  );
}
