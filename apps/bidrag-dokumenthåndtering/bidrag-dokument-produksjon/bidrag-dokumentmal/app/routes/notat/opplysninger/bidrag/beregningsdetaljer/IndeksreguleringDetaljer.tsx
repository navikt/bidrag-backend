import {
  formatterBeløpForBeregning,
  formatterProsent,
} from "~/utils/visningsnavn";
import { FeltTabell, JusterteFeltrader } from "~/components/FeltTabell";
import { useBeregningDetaljer } from "~/routes/notat/opplysninger/bidrag/VedleggBeregningsDetaljer";
import bidragOpplysningerTekster from "~/tekster/notat/opplysninger/bidrag.yaml";

const t = bidragOpplysningerTekster.beregningsdetaljer.indeksregulering;

export const IndeksreguleringDetaljer = () => {
  const { indeksreguleringDetaljer } = useBeregningDetaljer();
  return (
    <div style={{ width: "600px" }}>
      <h4>{t.tittel}</h4>
      <div>
        <div>
          <FeltTabell>
            <JusterteFeltrader
              rows={[
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
              ]}
            />
          </FeltTabell>
        </div>
      </div>
    </div>
  );
};
