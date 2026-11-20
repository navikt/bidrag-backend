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
  CommonTable,
  TableHeader,
  TableColumn,
} from "~/components/CommonTable";
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

      <CommonTable
        layoutAuto
        data={{
          headers: [
            {
              name: t.tabell.typeBarnetillegg,
              width: "100px",
            },
            !harBareTiltakspenger && {
              name: t.tabell.brutto,
              width: "50px",
            },
            {
              name: t.tabell.skattesats,
              width: "50px",
            },
            {
              name: t.tabell.netto,
              width: "50px",
            },
          ].filter((h) => h != null) as TableHeader[],
          rows: barnetillegg.barnetillegg
            .map((bt) => ({
              columns: [
                {
                  content: bt.visningsnavn,
                  colSpan: 1,
                },
                !harBareTiltakspenger && {
                  content:
                    bt.visningsnavn === barnetilleggTiltakspengerVisningsnavn
                      ? ord.ikkeRelevant
                      : formatterBeløpForBeregning(bt.bruttoBeløp, true),
                },
                {
                  content: formatterProsent(bt.skattefaktor),
                },
                {
                  content: formatterBeløpForBeregning(bt.nettoBeløp, true),
                },
              ].filter((d) => d != null) as TableColumn[],
            }))
            .concat([
              {
                columns: [
                  {
                    content: ord.resultat,
                  },
                  !harBareTiltakspenger && {
                    content: formatterBeløpForBeregning(
                      barnetillegg!.sumBruttoBeløp,
                    ),
                  },
                  {
                    content: "",
                  },
                  {
                    content: formatterBeløpForBeregning(
                      barnetillegg!.sumNettoBeløp,
                    ),
                  },
                ] as TableColumn[],
              },
            ]),
        }}
      />
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
