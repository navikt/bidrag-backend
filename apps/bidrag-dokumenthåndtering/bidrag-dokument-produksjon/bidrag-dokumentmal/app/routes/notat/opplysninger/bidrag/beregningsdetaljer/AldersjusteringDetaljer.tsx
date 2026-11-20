import {
  formatterBeløpForBeregning,
  formatterProsent,
} from "~/utils/visningsnavn";
import {
  FeltTabell,
  JustertFeltrad,
  JusterteFeltrader,
} from "~/components/FeltTabell";
import { useBeregningDetaljer } from "~/routes/notat/opplysninger/bidrag/VedleggBeregningsDetaljer";
import { tekst } from "~/utils/tekst";
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
      <h4>
        {tekst(
          "notat.opplysninger.bidrag.beregningsdetaljer.aldersjustering.tittel",
          "Aldersjustering",
        )}
      </h4>
      <div>
        <div className={"flex flex-row gap-5 mb-4"}>
          <div>
            {tekst(
              "notat.opplysninger.bidrag.beregningsdetaljer.aldersjustering.underholdskostnad.tittel",
              "Underholdskostnad",
            ) && (
              <h4>
                {tekst(
                  "notat.opplysninger.bidrag.beregningsdetaljer.aldersjustering.underholdskostnad.tittel",
                  "Underholdskostnad",
                )}
              </h4>
            )}
            <FeltTabell>
              <JustertFeltrad
                label={tekst(
                  "notat.opplysninger.bidrag.beregningsdetaljer.aldersjustering.underholdskostnad.forbruksutgift",
                  "Forbruksutgift",
                )}
              >{`${formatterBeløpForBeregning(delberegningUnderholdskostnad.forbruksutgift, true)}`}</JustertFeltrad>
              <JustertFeltrad
                label={tekst(
                  "notat.opplysninger.bidrag.beregningsdetaljer.aldersjustering.underholdskostnad.boutgift",
                  "Boutgift",
                )}
              >{`${formatterBeløpForBeregning(delberegningUnderholdskostnad.boutgift, true)}`}</JustertFeltrad>
              <JustertFeltrad
                label={tekst(
                  "notat.opplysninger.bidrag.beregningsdetaljer.aldersjustering.underholdskostnad.nettoTilsynsutgift",
                  "Netto tilsynsutgift",
                )}
              >{`${formatterBeløpForBeregning(delberegningUnderholdskostnad.nettoTilsynsutgift, true)}`}</JustertFeltrad>
              <JustertFeltrad
                label={tekst(
                  "notat.opplysninger.bidrag.beregningsdetaljer.aldersjustering.underholdskostnad.barnetrygd",
                  "Barnetrygd",
                )}
              >{`${formatterBeløpForBeregning(delberegningUnderholdskostnad.barnetrygd, true)}`}</JustertFeltrad>
              <JustertFeltrad
                label={tekst(
                  "notat.opplysninger.bidrag.beregningsdetaljer.aldersjustering.underholdskostnad.barnetilsynMedStønad",
                  "Barnetilsyn med stønad",
                )}
              >{`${formatterBeløpForBeregning(delberegningUnderholdskostnad.barnetilsynMedStønad, true)}`}</JustertFeltrad>
            </FeltTabell>
          </div>
          <div>
            {tekst(
              "notat.opplysninger.bidrag.beregningsdetaljer.aldersjustering.samværsfradrag.tittel",
              "Samværsfradrag",
            ) && (
              <h4>
                {tekst(
                  "notat.opplysninger.bidrag.beregningsdetaljer.aldersjustering.samværsfradrag.tittel",
                  "Samværsfradrag",
                )}
              </h4>
            )}
            <FeltTabell>
              <JustertFeltrad
                label={tekst(
                  "notat.opplysninger.bidrag.beregningsdetaljer.aldersjustering.samværsfradrag.samværsklasse",
                  "Samværsklasse",
                )}
              >{`${samværsfradrag.samværsklasseVisningsnavn}`}</JustertFeltrad>
              <JustertFeltrad
                label={tekst(
                  "notat.opplysninger.bidrag.beregningsdetaljer.aldersjustering.samværsfradrag.samværsfradrag",
                  "Samværsfradrag",
                )}
              >{`${formatterBeløpForBeregning(samværsfradrag?.samværsfradrag, true)}`}</JustertFeltrad>
            </FeltTabell>
          </div>
        </div>
        <div>
          <FeltTabell>
            <JusterteFeltrader
              rows={[
                {
                  label: tekst(
                    "notat.opplysninger.bidrag.beregningsdetaljer.aldersjustering.underholdskostnadLedetekst",
                    "Underholdskostnad",
                  ),
                  value: `${formatterBeløpForBeregning(delberegningUnderholdskostnad.underholdskostnad, true)}`,
                },
                {
                  label: tekst(
                    "notat.opplysninger.bidrag.beregningsdetaljer.aldersjustering.andelUnderholdskostnad",
                    "Andel underholdskostnad",
                  ),
                  value: `${formatterBeløpForBeregning(delberegningUnderholdskostnad.underholdskostnad, true)} x ${formatterProsent(periode.bpsAndelU)}`,
                  result: formatterBeløpForBeregning(periode.bpsAndelBeløp),
                },
                deltBosted
                  ? {
                      label: tekst(
                        "notat.opplysninger.bidrag.beregningsdetaljer.aldersjustering.etterFratrekkDeltBosted",
                        "Etter fratrekk delt bosted",
                      ),
                      value: `${formatterBeløpForBeregning(bpsAndel.andelBeløp, true)} - ${formatterBeløpForBeregning(delberegningUnderholdskostnad.underholdskostnad, true)} x ${formatterProsent(0.5)}`,
                      result: formatterBeløpForBeregning(
                        sluttberegning.bpAndelAvUVedDeltBostedBeløp,
                        true,
                      ),
                    }
                  : {},
              ]}
            />
          </FeltTabell>
        </div>
      </div>

      {bpsAndel.endeligAndelFaktor !== bpsAndel.beregnetAndelFaktor && (
        <div className="text-red-500" style={{ width: "200px" }}>
          {tekst(
            "notat.opplysninger.bidrag.beregningsdetaljer.aldersjustering.andelBegrensetTil",
            "Andel begrenset til ",
          )}
          {formatterProsent(bpsAndel.endeligAndelFaktor)}
        </div>
      )}
    </div>
  );
};
