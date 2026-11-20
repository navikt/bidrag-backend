import {
  formatterBeløpForBeregning,
  formatterProsent,
} from "~/utils/visningsnavn";
import { FeltTabell, JusterteFeltrader } from "~/components/FeltTabell";
import BPsAndelInntekterTable from "~/components/vedtak/BPsAndelInntekterTable";
import { useBeregningDetaljer } from "~/routes/notat/opplysninger/bidrag/VedleggBeregningsDetaljer";
import bidragOpplysningerTekster from "~/tekster/notat/opplysninger/bidrag.yaml";
import { Resultatkode } from "~/types/Api";

const t = bidragOpplysningerTekster.beregningsdetaljer.bpAndelUnderholdskostnad;

export const BPAndelUnderholdskostnad = () => {
  const {
    sluttberegning,
    deltBosted,
    inntekter,
    delberegningUnderholdskostnad,
    forskuddssats,
    bpsAndel,
  } = useBeregningDetaljer();
  // Ved avslag for delt bosted setter sluttberegningen beløpet etter fratrekk til 0, så det negative beløpet regnes ut her
  const erAvslagDeltBosted =
    sluttberegning?.resultat === Resultatkode.BIDRAG_IKKE_BEREGNET_DELT_BOSTED;
  return (
    <div style={{ width: "600px" }}>
      <h4>{t.tittel}</h4>
      <div>
        <BPsAndelInntekterTable
          inntekter={inntekter!}
          forskuddssats={forskuddssats}
        />
        <div>
          <FeltTabell>
            <JusterteFeltrader
              rows={[
                {
                  label: t.bpsAndelAvUnderholdskostnad,
                  value: `${formatterBeløpForBeregning(inntekter.inntektBP)} / ${formatterBeløpForBeregning(inntekter.totalEndeligInntekt)}`,
                  result: formatterProsent(bpsAndel.beregnetAndelFaktor),
                },
                {
                  label: t.andelUnderholdskostnad,
                  value: `${formatterBeløpForBeregning(delberegningUnderholdskostnad.underholdskostnad)} x ${formatterProsent(bpsAndel.endeligAndelFaktor)}`,
                  result: formatterBeløpForBeregning(bpsAndel.andelBeløp),
                },
                deltBosted
                  ? {
                      label: t.etterFratrekkDeltBosted,
                      value: `${formatterBeløpForBeregning(bpsAndel.andelBeløp, true)} - ${formatterBeløpForBeregning(delberegningUnderholdskostnad.underholdskostnad)} x ${formatterProsent(0.5)}`,
                      result: formatterBeløpForBeregning(
                        erAvslagDeltBosted
                          ? bpsAndel!.andelBeløp -
                              delberegningUnderholdskostnad!.underholdskostnad *
                                0.5
                          : sluttberegning.bpAndelAvUVedDeltBostedBeløp,
                      ),
                    }
                  : null,
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
