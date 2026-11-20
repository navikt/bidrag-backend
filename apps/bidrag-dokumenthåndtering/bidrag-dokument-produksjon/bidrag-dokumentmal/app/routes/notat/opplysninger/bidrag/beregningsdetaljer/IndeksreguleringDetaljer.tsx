import {
  formatterBeløpForBeregning,
  formatterProsent,
} from "~/utils/visningsnavn";
import { FeltTabell, JusterteFeltrader } from "~/components/FeltTabell";
import { useBeregningDetaljer } from "~/routes/notat/opplysninger/bidrag/VedleggBeregningsDetaljer";
import { tekst } from "~/utils/tekst";
export const IndeksreguleringDetaljer = () => {
  const { indeksreguleringDetaljer } = useBeregningDetaljer();
  return (
    <div style={{ width: "600px" }}>
      <h4>
        {tekst(
          "notat.opplysninger.bidrag.beregningsdetaljer.indeksregulering.tittel",
          "Indeksregulering",
        )}
      </h4>
      <div>
        <div>
          <FeltTabell>
            <JusterteFeltrader
              rows={[
                {
                  label: tekst(
                    "notat.opplysninger.bidrag.beregningsdetaljer.indeksregulering.indeksregulertBeløp",
                    "Indeksregulert beløp",
                  ),
                  result: `${formatterBeløpForBeregning(indeksreguleringDetaljer.sluttberegning?.originaltBeløp?.verdi, true)}`,
                },
                {
                  label: tekst(
                    "notat.opplysninger.bidrag.beregningsdetaljer.indeksregulering.indeksprosent",
                    "Indeksprosent",
                  ),
                  result: `${formatterProsent(indeksreguleringDetaljer.faktor)}`,
                },
                {
                  label: tekst(
                    "notat.opplysninger.bidrag.beregningsdetaljer.indeksregulering.beregning",
                    "Beregning",
                  ),
                  value: `${formatterBeløpForBeregning(indeksreguleringDetaljer.sluttberegning?.originaltBeløp?.verdi, true)} x ${1 + indeksreguleringDetaljer.faktor / 100}`,
                  result: formatterBeløpForBeregning(
                    indeksreguleringDetaljer.sluttberegning?.beløp?.verdi,
                  ),
                },
              ]}
            />
          </FeltTabell>
        </div>
      </div>
    </div>
  );
};
