import { FeltTabell, Feltrad } from "~/components/FeltTabell";
import {
  formatterBeløpForBeregning,
  rolleTilVisningsnavn,
  formatterProsent,
} from "~/utils/visningsnavn";
import {
  DelberegningUnderholdskostnad,
  DokumentmalDelberegningBarnetilleggDto,
  Rolletype,
  DokumentmalSluttberegningBarnebidragDetaljer,
} from "~/types/Api";
import {
  DokumentTabell,
  Tabellcelle,
  Tabellrad,
} from "~/components/DokumentTabell";
import { useBeregningDetaljer } from "~/routes/notat/opplysninger/bidrag/VedleggBeregningsDetaljer";
import ord from "~/tekster/ord.yaml";
import bidragOpplysningerTekster from "~/tekster/notat/opplysninger/bidrag.yaml";
import { barnetilleggTiltakspengerVisningsnavn } from "~/constants/beregning";

const t = bidragOpplysningerTekster.beregningsdetaljer.nettoBarnetillegg;

export const NettoBarnetilleggTable = ({ rolle }: { rolle: Rolletype }) => {
  const {
    sluttberegning,
    delberegningUnderholdskostnad: underholdskostnad,
    barnetilleggBP,
    barnetilleggBM,
  } = useBeregningDetaljer();

  const barnetillegg = rolle == Rolletype.BP ? barnetilleggBP : barnetilleggBM;
  const harBareTiltakspenger = barnetillegg.barnetillegg.every(
    (bt) => bt.visningsnavn === barnetilleggTiltakspengerVisningsnavn,
  );
  return (
    <div style={{ width: "600px" }}>
      <h4>{t.tittel.replace("{rolle}", rolleTilVisningsnavn(rolle))}</h4>

      <DokumentTabell
        layoutAuto
        kolonner={[
          {
            tittel: t.tabell.typeBarnetillegg,
            bredde: "100px",
          },
          ...(!harBareTiltakspenger
            ? [
                {
                  tittel: t.tabell.brutto,
                  bredde: "50px",
                },
              ]
            : []),
          {
            tittel: t.tabell.skattesats,
            bredde: "50px",
          },
          {
            tittel: t.tabell.netto,
            bredde: "50px",
          },
        ]}
      >
        {barnetillegg.barnetillegg.map((bt, index) => (
          <Tabellrad key={`${bt.visningsnavn}-${index}`} index={index}>
            <Tabellcelle colSpan={1}>{bt.visningsnavn}</Tabellcelle>
            {!harBareTiltakspenger && (
              <Tabellcelle>
                {bt.visningsnavn === barnetilleggTiltakspengerVisningsnavn
                  ? ord.ikkeRelevant
                  : formatterBeløpForBeregning(bt.bruttoBeløp, true)}
              </Tabellcelle>
            )}
            <Tabellcelle>{formatterProsent(bt.skattefaktor)}</Tabellcelle>
            <Tabellcelle>
              {formatterBeløpForBeregning(bt.nettoBeløp, true)}
            </Tabellcelle>
          </Tabellrad>
        ))}
        <Tabellrad index={barnetillegg.barnetillegg.length}>
          <Tabellcelle>{ord.resultat}</Tabellcelle>
          <Tabellcelle>
            {!harBareTiltakspenger &&
              formatterBeløpForBeregning(barnetillegg!.sumBruttoBeløp)}
          </Tabellcelle>
          <Tabellcelle />
          <Tabellcelle>
            {formatterBeløpForBeregning(barnetillegg!.sumNettoBeløp)}
          </Tabellcelle>
        </Tabellrad>
      </DokumentTabell>
      {rolle == Rolletype.BP ? (
        <BeregningJusterBPsBarnetillegg sluttberegning={sluttberegning!} />
      ) : (
        <BeregningJusterBMsBarnetillegg
          sluttberegning={sluttberegning!}
          underholdskostnad={underholdskostnad!}
          barnetilleggBM={barnetillegg}
        />
      )}
    </div>
  );
};
export const BeregningJusterBPsBarnetillegg = ({
  sluttberegning,
}: {
  sluttberegning: DokumentmalSluttberegningBarnebidragDetaljer;
}) => {
  function renderResult() {
    if (sluttberegning.bidragJustertForNettoBarnetilleggBP) {
      return t.justerBPsBarnetillegg.justertOppTilBPsNettoBarnetillegg;
    }
    return "";
  }
  function hentForeløpigBidrag() {
    if (sluttberegning.bidragJustertForNettoBarnetilleggBP)
      return formatterBeløpForBeregning(
        sluttberegning.bruttoBidragEtterBarnetilleggBP,
      );
    return formatterBeløpForBeregning(
      sluttberegning.bruttoBidragJustertForEvneOg25Prosent,
    );
  }
  return (
    <div>
      <FeltTabell>
        <Feltrad
          label={t.justerBPsBarnetillegg.foreløpigBidrag}
        >{`${hentForeløpigBidrag()}${renderResult()}`}</Feltrad>
      </FeltTabell>
    </div>
  );
};
export const BeregningJusterBMsBarnetillegg = ({
  barnetilleggBM,
  sluttberegning,
  underholdskostnad,
}: {
  underholdskostnad: DelberegningUnderholdskostnad;
  sluttberegning: DokumentmalSluttberegningBarnebidragDetaljer;
  barnetilleggBM: DokumentmalDelberegningBarnetilleggDto;
}) => {
  function renderResult() {
    if (sluttberegning.bidragJustertForNettoBarnetilleggBM) {
      return t.justerBMsBarnetillegg.justertTilUMinusBMsNettoBarnetillegg;
    }
    return "";
  }

  return (
    <div>
      <FeltTabell>
        <Feltrad
          label={t.justerBMsBarnetillegg.uMinusBMsNettoBarnetillegg}
        >{`${formatterBeløpForBeregning(underholdskostnad.underholdskostnad)} - ${formatterBeløpForBeregning(barnetilleggBM.sumNettoBeløp)} = ${formatterBeløpForBeregning(sluttberegning.uminusNettoBarnetilleggBM)}`}</Feltrad>
        <Feltrad
          label={t.justerBMsBarnetillegg.foreløpigBidrag}
        >{`${formatterBeløpForBeregning(sluttberegning.bruttoBidragEtterBarnetilleggBM)}${renderResult()}`}</Feltrad>
      </FeltTabell>
    </div>
  );
};
