import { FeltTabell, Feltrad } from "~/components/FeltTabell";
import {
  formatterBeløpForBeregning,
  formatterProsent,
} from "~/utils/visningsnavn";
import { useBeregningDetaljer } from "~/routes/notat/opplysninger/bidrag/VedleggBeregningsDetaljer";
import bidragOpplysningerTekster from "~/tekster/notat/opplysninger/bidrag.yaml";

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
      <div>
        <FeltTabell>
          <Feltrad
            label={t.løpendeBidragsbeløp}
          >{`${formatterBeløpForBeregning(endeligBeløp)}`}</Feltrad>
          <Feltrad
            label={t.endringIProsent}
          >{`(${formatterBeløpForBeregning(endeligBeløp)} - ${formatterBeløpForBeregning(sluttberegning.beregnetBeløp)}) / ${formatterBeløpForBeregning(endeligBeløp)} = ${formatterProsent(endringUnderGrense.faktiskEndringFaktor)}`}</Feltrad>
        </FeltTabell>
      </div>
    </div>
  );
};
