import { FeltTabell, Feltrad } from "~/components/FeltTabell";
import { formatterBeløpForBeregning } from "~/utils/visningsnavn";
import { useBeregningDetaljer } from "~/routes/notat/opplysninger/bidrag/VedleggBeregningsDetaljer";
import bidragOpplysningerTekster from "~/tekster/notat/opplysninger/bidrag.yaml";

const t = bidragOpplysningerTekster.beregningsdetaljer.samværsfradrag;

export const Samværsfradrag = () => {
  const { samværsfradrag } = useBeregningDetaljer();
  return (
    <div style={{ width: "600px" }}>
      <h4>{t.tittel}</h4>
      <div>
        <FeltTabell>
          <Feltrad label={t.samværsklasse}>
            {t.samværsklasseVerdi
              .replace(
                "{samværsklasse}",
                `${samværsfradrag.samværsklasseVisningsnavn}`,
              )
              .replace(
                "{samværPerMåned}",
                `${samværsfradrag.gjennomsnittligSamværPerMåned}`,
              )}
          </Feltrad>
          <Feltrad label={t.samværsfradrag}>
            {formatterBeløpForBeregning(samværsfradrag.samværsfradrag)}
          </Feltrad>
        </FeltTabell>
      </div>
    </div>
  );
};
