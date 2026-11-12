import { DataViewTable, DataViewTableData } from "~/components/DataViewTable";
import {
  formatterBeløpForBeregning,
  formatterProsent,
} from "~/utils/visningsnavn";
import { useBeregningDetaljer } from "~/routes/notat.bidrag/VedleggBeregningsDetaljer";
import bidragOpplysningerTekster from "~/tekster/notat/bidrag/opplysninger.yaml";

const t = bidragOpplysningerTekster.beregningsdetaljer.endringUnderGrense;

export const EndringUnderGrense = () => {
  const {
    endringUnderGrense,
    sluttberegning,
    erEndringUnderGrense,
    endeligBeløp,
  } = useBeregningDetaljer();

  if (!erEndringUnderGrense) return null;

  return (
    <div className={"pt-2"}>
      <h4>{t.tittel}</h4>
      <DataViewTable
        gap={"5px"}
        data={
          [
            {
              label: t.løpendeBidragsbeløp,
              textRight: false,
              value: `${formatterBeløpForBeregning(endeligBeløp)}`,
            },
            {
              label: t.endringIProsent,
              textRight: false,
              value: `(${formatterBeløpForBeregning(endeligBeløp)} - ${formatterBeløpForBeregning(sluttberegning.beregnetBeløp)}) / ${formatterBeløpForBeregning(endeligBeløp)} = ${formatterProsent(endringUnderGrense.faktiskEndringFaktor)}`,
            },
          ].filter((d) => d != null) as DataViewTableData[]
        }
      />
    </div>
  );
};
