import { FeltTabell, Feltrader } from "~/components/FeltTabell";
import { formatterBeløpForBeregning } from "~/utils/visningsnavn";
import { useBeregningDetaljer } from "~/routes/notat/opplysninger/bidrag/VedleggBeregningsDetaljer";
import { tekst } from "~/utils/tekst";
export const EndeligBidragTable = () => {
  const {
    deltBosted,
    sluttberegning,
    sluttberegningAldersjustering,
    samværsfradrag: beregning,
  } = useBeregningDetaljer();
  return (
    <div style={{ width: "600px" }}>
      <h4>
        {tekst(
          "notat.opplysninger.bidrag.beregningsdetaljer.endeligBidrag.tittel",
          "Endelig bidrag",
        )}
      </h4>
      <div>
        <FeltTabell>
          <Feltrader
            rows={[
              !deltBosted
                ? {
                    label: tekst(
                      "notat.opplysninger.bidrag.beregningsdetaljer.endeligBidrag.etterSamværsfradraget",
                      "Etter samværsfradraget",
                    ),
                    value: `${formatterBeløpForBeregning(sluttberegningAldersjustering?.bpAndelBeløp ?? sluttberegning?.bruttoBidragEtterBarnetilleggBP)} - ${formatterBeløpForBeregning(beregning.samværsfradrag)} = ${formatterBeløpForBeregning(sluttberegningAldersjustering?.beregnetBeløp ?? sluttberegning?.beregnetBeløp)}`,
                  }
                : null,
              {
                label: tekst(
                  "notat.opplysninger.bidrag.beregningsdetaljer.endeligBidrag.avrundetBeløp",
                  "Avrundet beløp",
                ),
                value: `${formatterBeløpForBeregning(sluttberegningAldersjustering?.resultatBeløp ?? sluttberegning?.resultatBeløp)}`,
              },
            ]}
          />
        </FeltTabell>
      </div>
    </div>
  );
};
