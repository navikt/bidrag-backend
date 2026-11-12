import {
  formatterBeløpForBeregning,
  formatterProsent,
} from "~/utils/visningsnavn";
import { DataViewTableData } from "~/components/DataViewTable";
import { useBeregningDetaljer } from "~/routes/notat.bidrag/VedleggBeregningsDetaljer";
import bidragOpplysningerTekster from "~/tekster/notat/bidrag/opplysninger.yaml";
import { DataViewTableV2 } from "~/components/DataViewTableV2";

const t = bidragOpplysningerTekster.beregningsdetaljer.indeksregulering;

export const IndeksreguleringDetaljer = () => {
  const { indeksreguleringDetaljer } = useBeregningDetaljer();
  return (
    <div style={{ width: "600px" }}>
      <h4>{t.tittel}</h4>
      <div>
        <DataViewTableV2
          gap={"5px"}
          data={
            [
              {
                label: t.indeksregulertBeløp,
                result: `${formatterBeløpForBeregning(indeksreguleringDetaljer.sluttberegning?.originaltBeløp?.verdi, true)}`,
              },
              {
                label: t.indeksprosent,
                result: `${formatterProsent(indeksreguleringDetaljer.faktor)}`,
              },
              {
                label: t.beregning,
                value: `${formatterBeløpForBeregning(indeksreguleringDetaljer.sluttberegning?.originaltBeløp?.verdi, true)} x ${1 + indeksreguleringDetaljer.faktor / 100}`,
                result: formatterBeløpForBeregning(
                  indeksreguleringDetaljer.sluttberegning?.beløp?.verdi,
                ),
              },
            ].filter((d) => d != null) as DataViewTableData[]
          }
        />
      </div>
    </div>
  );
};
