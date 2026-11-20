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
import bidragOpplysningerTekster from "~/tekster/notat/opplysninger/bidrag.yaml";

const t = bidragOpplysningerTekster.beregningsdetaljer.aldersjustering;

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
      <h4>{t.tittel}</h4>
      <div>
        <div className={"flex flex-row gap-5 mb-4"}>
          <div>
            {t.underholdskostnad.tittel && (
              <h4>{t.underholdskostnad.tittel}</h4>
            )}
            <FeltTabell>
              <JustertFeltrad
                label={t.underholdskostnad.forbruksutgift}
              >{`${formatterBeløpForBeregning(delberegningUnderholdskostnad.forbruksutgift, true)}`}</JustertFeltrad>
              <JustertFeltrad
                label={t.underholdskostnad.boutgift}
              >{`${formatterBeløpForBeregning(delberegningUnderholdskostnad.boutgift, true)}`}</JustertFeltrad>
              <JustertFeltrad
                label={t.underholdskostnad.nettoTilsynsutgift}
              >{`${formatterBeløpForBeregning(delberegningUnderholdskostnad.nettoTilsynsutgift, true)}`}</JustertFeltrad>
              <JustertFeltrad
                label={t.underholdskostnad.barnetrygd}
              >{`${formatterBeløpForBeregning(delberegningUnderholdskostnad.barnetrygd, true)}`}</JustertFeltrad>
              <JustertFeltrad
                label={t.underholdskostnad.barnetilsynMedStønad}
              >{`${formatterBeløpForBeregning(delberegningUnderholdskostnad.barnetilsynMedStønad, true)}`}</JustertFeltrad>
            </FeltTabell>
          </div>
          <div>
            {t.samværsfradrag.tittel && <h4>{t.samværsfradrag.tittel}</h4>}
            <FeltTabell>
              <JustertFeltrad
                label={t.samværsfradrag.samværsklasse}
              >{`${samværsfradrag.samværsklasseVisningsnavn}`}</JustertFeltrad>
              <JustertFeltrad
                label={t.samværsfradrag.samværsfradrag}
              >{`${formatterBeløpForBeregning(samværsfradrag?.samværsfradrag, true)}`}</JustertFeltrad>
            </FeltTabell>
          </div>
        </div>
        <div>
          <FeltTabell>
            <JusterteFeltrader
              rows={[
                {
                  label: t.underholdskostnadLedetekst,
                  value: `${formatterBeløpForBeregning(delberegningUnderholdskostnad.underholdskostnad, true)}`,
                },
                {
                  label: t.andelUnderholdskostnad,
                  value: `${formatterBeløpForBeregning(delberegningUnderholdskostnad.underholdskostnad, true)} x ${formatterProsent(periode.bpsAndelU)}`,
                  result: formatterBeløpForBeregning(periode.bpsAndelBeløp),
                },
                deltBosted
                  ? {
                      label: t.etterFratrekkDeltBosted,
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
          {t.andelBegrensetTil}
          {formatterProsent(bpsAndel.endeligAndelFaktor)}
        </div>
      )}
    </div>
  );
};
