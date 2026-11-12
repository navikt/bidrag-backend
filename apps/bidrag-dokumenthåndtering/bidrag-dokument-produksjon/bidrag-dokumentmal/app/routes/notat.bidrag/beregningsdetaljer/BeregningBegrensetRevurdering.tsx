import { DataViewTable, DataViewTableData } from "~/components/DataViewTable";
import { formatterBeløpForBeregning } from "~/utils/visningsnavn";
import { useBeregningDetaljer } from "~/routes/notat.bidrag/VedleggBeregningsDetaljer";
import bidragOpplysningerTekster from "~/tekster/notat/bidrag/opplysninger.yaml";

const t = bidragOpplysningerTekster.beregningsdetaljer.begrensetRevurdering;

export const BeregningBegrensetRevurdering = () => {
  const { sluttberegning, samværsfradrag } = useBeregningDetaljer();
  if (!sluttberegning.begrensetRevurderingUtført) return null;

  return (
    <div className={"mb-2"}>
      <h4>{t.tittel}</h4>
      <DataViewTable
        gap={"5px"}
        data={
          [
            {
              label: t.løpendeForskudd,
              textRight: false,
              value: `${formatterBeløpForBeregning(sluttberegning.løpendeForskudd ?? 0)}`,
            },
            {
              label: t.løpendeBidrag,
              textRight: false,
              value: `${formatterBeløpForBeregning(sluttberegning.løpendeBidrag ?? 0)}`,
            },
            {
              label: t.foreløpigBidrag,
              textRight: false,
              value: `${formatterBeløpForBeregning(sluttberegning.løpendeForskudd ?? 0)} + ${formatterBeløpForBeregning(samværsfradrag.samværsfradrag)}`,
              result: `${formatterBeløpForBeregning(sluttberegning.bruttoBidragEtterBegrensetRevurdering)}`,
            },
          ].filter((d) => d != null) as DataViewTableData[]
        }
      />
    </div>
  );
};
