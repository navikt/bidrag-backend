import NotatSide, {
  notatAction,
  notatMeta,
} from "~/routes/notat/felles/NotatSide";
import { useNotatFelles } from "~/components/notat_felles/NotatContext";
import tekster from "~/tekster/notat/begrunnelse/bidrag.yaml";
import { vedleggBidrag } from "~/routes/notat/opplysninger/bidrag/route";
import Virkningstidspunkt from "~/routes/notat/begrunnelse/felles/Virkningstidspunkt";
import PrivatAvtale from "~/routes/notat/begrunnelse/bidrag/PrivatAvtale";
import Underholdskostnad from "~/routes/notat/begrunnelse/bidrag/Underholdskostnad";
import Inntekter from "~/routes/notat/begrunnelse/felles/Inntekter";
import Gebyr from "~/routes/notat/begrunnelse/bidrag/Gebyr";
import Boforhold from "~/routes/notat/begrunnelse/felles/Boforhold";
import Samvær from "~/routes/notat/begrunnelse/bidrag/Samvær";
import Vedtak from "~/routes/notat/begrunnelse/bidrag/Vedtak";

// /notat/begrunnelse/bidrag: bare begrunnelsen av bidragsnotatet.
export const action = notatAction;
export const meta = notatMeta(tekster.tittel);

export default function NotatBegrunnelseBidrag() {
  return (
    <NotatSide
      id="bidrag_notat"
      tittel={tekster.tittel}
      vedlegg={vedleggBidrag}
      kanGjeldeFlereSaker
    >
      <BegrunnelseBidrag />
    </NotatSide>
  );
}

/**
 * Delen saksbehandleren har skrevet og vurdert, i den rekkefølgen den står i notatet.
 * Ved innkreving vises bare privat avtale og vedtaket.
 */
export function BegrunnelseBidrag() {
  const { erInnkrevingsgrunnlag } = useNotatFelles();
  return (
    <>
      <Virkningstidspunkt />
      <PrivatAvtale />
      {!erInnkrevingsgrunnlag && (
        <>
          <Underholdskostnad />
          <Inntekter />
          <Gebyr />
          <Boforhold />
          <Samvær />
        </>
      )}
      <Vedtak />
    </>
  );
}
