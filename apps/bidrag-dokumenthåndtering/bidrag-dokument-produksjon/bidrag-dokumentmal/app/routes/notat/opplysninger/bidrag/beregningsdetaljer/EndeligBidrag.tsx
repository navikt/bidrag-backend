import { FeltTabell, Feltrader } from "~/components/FeltTabell";
import { formatterBeløpForBeregning } from "~/utils/visningsnavn";
import { useBeregningDetaljer } from "~/routes/notat/opplysninger/bidrag/VedleggBeregningsDetaljer";
import bidragOpplysningerTekster from "~/tekster/notat/opplysninger/bidrag.yaml";

const t = bidragOpplysningerTekster.beregningsdetaljer.endeligBidrag;

export const EndeligBidragTable = () => {
  const {
    deltBosted,
    sluttberegning,
    sluttberegningAldersjustering,
    samværsfradrag: beregning,
  } = useBeregningDetaljer();

  return (
    <div style={{ width: "600px" }}>
      <h4>{t.tittel}</h4>
      <div>
        <FeltTabell>
          <Feltrader
            rows={[
              !deltBosted
                ? {
                    label: t.etterSamværsfradraget,
                    value: `${formatterBeløpForBeregning(sluttberegningAldersjustering?.bpAndelBeløp ?? sluttberegning?.bruttoBidragEtterBarnetilleggBP)} - ${formatterBeløpForBeregning(beregning.samværsfradrag)} = ${formatterBeløpForBeregning(sluttberegningAldersjustering?.beregnetBeløp ?? sluttberegning?.beregnetBeløp)}`,
                  }
                : null,
              {
                label: t.avrundetBeløp,
                value: `${formatterBeløpForBeregning(sluttberegningAldersjustering?.resultatBeløp ?? sluttberegning?.resultatBeløp)}`,
              },
            ]}
          />
        </FeltTabell>
      </div>
    </div>
  );
};
