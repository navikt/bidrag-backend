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
import { barnetilleggTiltakspengerVisningsnavn } from "~/constants/beregning";
import { tekst } from "~/utils/tekst";
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
      <h4>
        {tekst(
          "notat.opplysninger.bidrag.beregningsdetaljer.nettoBarnetillegg.tittel",
          "Netto barnetillegg ({rolle})",
        ).replace("{rolle}", rolleTilVisningsnavn(rolle))}
      </h4>

      <DokumentTabell
        layoutAuto
        kolonner={[
          {
            tittel: tekst(
              "notat.opplysninger.bidrag.beregningsdetaljer.nettoBarnetillegg.tabell.typeBarnetillegg",
              "Type barnetillegg",
            ),
            bredde: "100px",
          },
          ...(!harBareTiltakspenger
            ? [
                {
                  tittel: tekst(
                    "notat.opplysninger.bidrag.beregningsdetaljer.nettoBarnetillegg.tabell.brutto",
                    "Brutto",
                  ),
                  bredde: "50px",
                },
              ]
            : []),
          {
            tittel: tekst(
              "notat.opplysninger.bidrag.beregningsdetaljer.nettoBarnetillegg.tabell.skattesats",
              "Skattesats",
            ),
            bredde: "50px",
          },
          {
            tittel: tekst(
              "notat.opplysninger.bidrag.beregningsdetaljer.nettoBarnetillegg.tabell.netto",
              "Netto",
            ),
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
                  ? tekst("ord.ikkeRelevant", "Ikke relevant")
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
          <Tabellcelle>{tekst("ord.resultat", "Resultat")}</Tabellcelle>
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
      return tekst(
        "notat.opplysninger.bidrag.beregningsdetaljer.nettoBarnetillegg.justerBPsBarnetillegg.justertOppTilBPsNettoBarnetillegg",
        " (justert opp til BPs netto barnetillegg)",
      );
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
          label={tekst(
            "notat.opplysninger.bidrag.beregningsdetaljer.nettoBarnetillegg.justerBPsBarnetillegg.foreløpigBidrag",
            "Foreløpig bidrag",
          )}
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
      return tekst(
        "notat.opplysninger.bidrag.beregningsdetaljer.nettoBarnetillegg.justerBMsBarnetillegg.justertTilUMinusBMsNettoBarnetillegg",
        " (justert til U - BMs netto barnetillegg + samværsfradrag)",
      );
    }
    return "";
  }
  return (
    <div>
      <FeltTabell>
        <Feltrad
          label={tekst(
            "notat.opplysninger.bidrag.beregningsdetaljer.nettoBarnetillegg.justerBMsBarnetillegg.uMinusBMsNettoBarnetillegg",
            "U - BMs netto barnetillegg",
          )}
        >{`${formatterBeløpForBeregning(underholdskostnad.underholdskostnad)} - ${formatterBeløpForBeregning(barnetilleggBM.sumNettoBeløp)} = ${formatterBeløpForBeregning(sluttberegning.uminusNettoBarnetilleggBM)}`}</Feltrad>
        <Feltrad
          label={tekst(
            "notat.opplysninger.bidrag.beregningsdetaljer.nettoBarnetillegg.justerBMsBarnetillegg.foreløpigBidrag",
            "Foreløpig bidrag",
          )}
        >{`${formatterBeløpForBeregning(sluttberegning.bruttoBidragEtterBarnetilleggBM)}${renderResult()}`}</Feltrad>
      </FeltTabell>
    </div>
  );
};
