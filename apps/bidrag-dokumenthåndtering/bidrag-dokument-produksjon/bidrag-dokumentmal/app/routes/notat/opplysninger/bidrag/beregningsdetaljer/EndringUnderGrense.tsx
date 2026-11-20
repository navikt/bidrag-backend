import { FeltTabell, Feltrad } from "~/components/FeltTabell";
import {
  formatterBeløpForBeregning,
  formatterProsent,
} from "~/utils/visningsnavn";
import { useBeregningDetaljer } from "~/routes/notat/opplysninger/bidrag/VedleggBeregningsDetaljer";
import { tekst } from "~/utils/tekst";
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
      <h4>
        {tekst(
          "notat.opplysninger.bidrag.beregningsdetaljer.endringUnderGrense.tittel",
          "Endring under grense",
        )}
      </h4>
      <div>
        <FeltTabell>
          <Feltrad
            label={tekst(
              "notat.opplysninger.bidrag.beregningsdetaljer.endringUnderGrense.løpendeBidragsbeløp",
              "Løpende bidragsbeløp",
            )}
          >{`${formatterBeløpForBeregning(endeligBeløp)}`}</Feltrad>
          <Feltrad
            label={tekst(
              "notat.opplysninger.bidrag.beregningsdetaljer.endringUnderGrense.endringIProsent",
              "Endring i prosent",
            )}
          >{`(${formatterBeløpForBeregning(endeligBeløp)} - ${formatterBeløpForBeregning(sluttberegning.beregnetBeløp)}) / ${formatterBeløpForBeregning(endeligBeløp)} = ${formatterProsent(endringUnderGrense.faktiskEndringFaktor)}`}</Feltrad>
        </FeltTabell>
      </div>
    </div>
  );
};
