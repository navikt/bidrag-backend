import {
  formatterBeløpForBeregning,
  formatterProsent,
} from "~/utils/visningsnavn";
import tekster from "~/tekster";
import { DataViewTableData } from "~/components/DataViewTable";
import BPsAndelInntekterTable from "~/components/vedtak/BPsAndelInntekterTable";
import { useBeregningDetaljer } from "~/routes/notat.bidrag/VedleggBeregningsDetaljer";
import { DataViewTableV2 } from "~/components/DataViewTableV2";

export const BPAndelUnderholdskostnad = () => {
  const {
    sluttberegning,
    deltBosted,
    inntekter,
    delberegningUnderholdskostnad,
    forskuddssats,
    bpsAndel,
  } = useBeregningDetaljer();
  return (
    <div style={{ width: "600px" }}>
      <h4>{`${tekster.begreper.bidragspliktiges} andel`}</h4>
      <div>
        <BPsAndelInntekterTable
          inntekter={inntekter!}
          forskuddssats={forskuddssats}
        />
        <DataViewTableV2
          gap={"5px"}
          data={
            [
              {
                label: "BP's andel av underholdskostnad (i prosent)",
                value: `${formatterBeløpForBeregning(inntekter.inntektBP)} / ${formatterBeløpForBeregning(inntekter.totalEndeligInntekt)}`,
                result: formatterProsent(bpsAndel.beregnetAndelFaktor),
              },
              {
                label: "Andel underholdskostnad",
                value: `${formatterBeløpForBeregning(delberegningUnderholdskostnad.underholdskostnad)} x ${formatterProsent(bpsAndel.endeligAndelFaktor)}`,
                result: formatterBeløpForBeregning(bpsAndel.andelBeløp),
              },
              deltBosted
                ? {
                    label: "Etter fratrekk delt bosted",
                    textRight: false,
                    value: `${formatterBeløpForBeregning(bpsAndel.andelBeløp, true)} - ${formatterBeløpForBeregning(delberegningUnderholdskostnad.underholdskostnad)} x ${formatterProsent(0.5)}`,
                    result: formatterBeløpForBeregning(
                      sluttberegning.bpAndelAvUVedDeltBostedBeløp,
                    ),
                  }
                : null,
            ].filter((d) => d != null) as DataViewTableData[]
          }
        />
      </div>
      {bpsAndel.endeligAndelFaktor !== bpsAndel.beregnetAndelFaktor && (
        <div className="text-red-500" style={{ width: "200px" }}>
          Andel begrenset til {formatterProsent(bpsAndel.endeligAndelFaktor)}
        </div>
      )}
    </div>
  );
};
