import { FeltTabell, Feltrad } from "~/components/FeltTabell";
import { formatterBeløpForBeregning } from "~/utils/visningsnavn";
import { useBeregningDetaljer } from "~/routes/notat/opplysninger/bidrag/VedleggBeregningsDetaljer";
import { tekst } from "~/utils/tekst";
export const Samværsfradrag = () => {
  const { samværsfradrag } = useBeregningDetaljer();
  return (
    <div style={{ width: "600px" }}>
      <h4>
        {tekst(
          "notat.opplysninger.bidrag.beregningsdetaljer.samværsfradrag.tittel",
          "Samværsfradrag",
        )}
      </h4>
      <div>
        <FeltTabell>
          <Feltrad
            label={tekst(
              "notat.opplysninger.bidrag.beregningsdetaljer.samværsfradrag.samværsklasse",
              "Samværsklasse",
            )}
          >
            {tekst(
              "notat.opplysninger.bidrag.beregningsdetaljer.samværsfradrag.samværsklasseVerdi",
              "Samværsklasse {samværsklasse} (samvær per måned: {samværPerMåned})",
            )
              .replace(
                "{samværsklasse}",
                `${samværsfradrag.samværsklasseVisningsnavn}`,
              )
              .replace(
                "{samværPerMåned}",
                `${samværsfradrag.gjennomsnittligSamværPerMåned}`,
              )}
          </Feltrad>
          <Feltrad
            label={tekst(
              "notat.opplysninger.bidrag.beregningsdetaljer.samværsfradrag.samværsfradrag",
              "Samværsfradrag",
            )}
          >
            {formatterBeløpForBeregning(samværsfradrag.samværsfradrag)}
          </Feltrad>
        </FeltTabell>
      </div>
    </div>
  );
};
