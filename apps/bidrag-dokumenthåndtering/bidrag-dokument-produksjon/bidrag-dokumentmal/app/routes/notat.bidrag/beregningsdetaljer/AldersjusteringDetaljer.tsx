import {
  formatterBeløpForBeregning,
  formatterProsent,
} from "~/utils/visningsnavn";
import { DataViewTableData } from "~/components/DataViewTable";
import { useBeregningDetaljer } from "~/routes/notat.bidrag/VedleggBeregningsDetaljer";
import bidragOpplysningerTekster from "~/tekster/notat/bidrag/opplysninger.json";
import { DataViewTableV2 } from "~/components/DataViewTableV2";

const t = bidragOpplysningerTekster.beregningsdetaljer.aldersjustering;

export const AldersjusteringDetaljer = () => {
  const {
    samværsfradrag,
    bpsAndel,
    delberegningUnderholdskostnad,
    deltBosted,
    sluttberegning,
    periode,
  } = useBeregningDetaljer();
  return (
    <div style={{ width: "600px" }}>
      <h4>{t.tittel}</h4>
      <div>
        <div className={"flex flex-row gap-5 mb-4"}>
          <DataViewTableV2
            gap={"5px"}
            title={t.underholdskostnad.tittel}
            data={
              [
                {
                  label: t.underholdskostnad.forbruksutgift,
                  value: `${formatterBeløpForBeregning(delberegningUnderholdskostnad.forbruksutgift, true)}`,
                },
                {
                  label: t.underholdskostnad.boutgift,
                  value: `${formatterBeløpForBeregning(delberegningUnderholdskostnad.boutgift, true)}`,
                },
                {
                  label: t.underholdskostnad.nettoTilsynsutgift,
                  value: `${formatterBeløpForBeregning(delberegningUnderholdskostnad.nettoTilsynsutgift, true)}`,
                },
                {
                  label: t.underholdskostnad.barnetrygd,
                  value: `${formatterBeløpForBeregning(delberegningUnderholdskostnad.barnetrygd, true)}`,
                },
                {
                  label: t.underholdskostnad.barnetilsynMedStønad,
                  value: `${formatterBeløpForBeregning(delberegningUnderholdskostnad.barnetilsynMedStønad, true)}`,
                },
              ].filter((d) => d != null) as DataViewTableData[]
            }
          />
          <DataViewTableV2
            gap={"5px"}
            title={t.samværsfradrag.tittel}
            data={
              [
                {
                  label: t.samværsfradrag.samværsklasse,
                  value: `${samværsfradrag.samværsklasseVisningsnavn}`,
                },
                {
                  label: t.samværsfradrag.samværsfradrag,
                  value: `${formatterBeløpForBeregning(samværsfradrag?.samværsfradrag, true)}`,
                },
              ].filter((d) => d != null) as DataViewTableData[]
            }
          />
        </div>
        <DataViewTableV2
          gap={"5px"}
          data={
            [
              {
                label: t.underholdskostnadLedetekst,
                value: `${formatterBeløpForBeregning(delberegningUnderholdskostnad.underholdskostnad, true)}`,
              },
              {
                label: t.andelUnderholdskostnad,
                value: `${formatterBeløpForBeregning(delberegningUnderholdskostnad.underholdskostnad, true)} x ${formatterProsent(periode.bpsAndelU)}`,
                result: formatterBeløpForBeregning(periode.bpsAndelBeløp),
              },
              deltBosted && {
                label: t.etterFratrekkDeltBosted,
                value: `${formatterBeløpForBeregning(bpsAndel.andelBeløp, true)} - ${formatterBeløpForBeregning(delberegningUnderholdskostnad.underholdskostnad, true)} x ${formatterProsent(0.5)}`,
                result: formatterBeløpForBeregning(
                  sluttberegning.bpAndelAvUVedDeltBostedBeløp,
                  true,
                ),
              },
            ].filter((d) => d != null) as DataViewTableData[]
          }
        />
      </div>

      {bpsAndel.endeligAndelFaktor !== bpsAndel.beregnetAndelFaktor && (
        <div className="text-red-500" style={{ width: "200px" }}>
          {t.andelBegrensetTil}
          {formatterProsent(bpsAndel.endeligAndelFaktor)}
        </div>
      )}
    </div>
  );
};
