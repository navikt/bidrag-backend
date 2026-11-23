import NotatSide, {
  notatAction,
  notatMeta,
} from "~/routes/notat/felles/NotatSide";
import { BegrunnelseBidrag } from "~/routes/notat/begrunnelse/bidrag/route";
import {
  OpplysningerBidrag,
  vedleggBidrag,
} from "~/routes/notat/opplysninger/bidrag/route";
import { tekst } from "~/utils/tekst";
// /notat/bidrag: hele bidragsnotatet, først begrunnelsen og så vedleggene med opplysninger.
export const action = notatAction;
export const meta = notatMeta(
  tekst("notat.begrunnelse.bidrag.tittel", "Bidrag, saksbehandlingsnotat"),
);
export default function NotatBidrag() {
  return (
    <NotatSide
      id="bidrag_notat"
      del="komplett"
      tittel={tekst(
        "notat.begrunnelse.bidrag.tittel",
        "Bidrag, saksbehandlingsnotat",
      )}
      vedlegg={vedleggBidrag}
      kanGjeldeFlereSaker
    >
      <BegrunnelseBidrag />
      <OpplysningerBidrag />
    </NotatSide>
  );
}
