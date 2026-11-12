import {
  useNotatFelles,
  useVedleggNummer,
} from "~/components/notat_felles/NotatContext";
import elementIds from "~/utils/elementIds";
import { CommonTable } from "~/components/CommonTable";
import { NotatSamvaerBarnDto, NotatSamvaersperiodeDto } from "~/types/Api";
import { DataViewTable, DataViewTableData } from "~/components/DataViewTable";
import { formatPeriode } from "~/utils/date-utils";
import { rolleTilVisningsnavnV2 } from "~/utils/visningsnavn";
import ord from "~/tekster/ord.json";
import bidragOpplysningerTekster from "~/tekster/notat/bidrag/opplysninger.json";

const t = bidragOpplysningerTekster.samvær;

export default function VedleggSamvær() {
  const vedleggNummer = useVedleggNummer("samvær");
  const { data, erAvslag } = useNotatFelles();
  if (erAvslag) return null;
  return (
    <div className={`${vedleggNummer == 1 ? "break-before-page" : ""}`}>
      <h2 id={elementIds.vedleggSamvær}>
        {t.tittel.replace("{}", `${vedleggNummer}`)}
      </h2>
      {(data.samværV2?.barn ?? []).map((samværBarn, index) => (
        <div
          className={"flex flex-col gap-2"}
          key={index + "-" + samværBarn.gjelderBarn.ident}
        >
          <SamværsberegningDetaljerBarn samværBarn={samværBarn} key={index} />
        </div>
      ))}
    </div>
  );
}

function SamværsberegningDetaljerBarn({
  samværBarn,
}: {
  samværBarn: NotatSamvaerBarnDto;
}) {
  const samværsperioderMedBeregning = samværBarn.perioder.filter(
    (p) => p.beregning != null,
  );
  return (
    <>
      <DataViewTable
        data={
          [
            {
              label: rolleTilVisningsnavnV2(samværBarn.gjelderBarn),
              labelBold: true,
              value: samværBarn.gjelderBarn.navn,
            },
          ].filter((d) => d != null) as DataViewTableData[]
        }
      />
      {samværsperioderMedBeregning.length == 0 ? (
        <p>{t.ingenBeregnetSamvær}</p>
      ) : (
        samværsperioderMedBeregning.map((periode, index) => (
          <div key={index + "_" + periode.samværsklasseVisningsnavn}>
            <DataViewTable
              data={
                [
                  {
                    label: ord.periode,
                    value: formatPeriode(
                      periode.periode.fom,
                      periode.periode.tom,
                    ),
                  },
                ].filter((d) => d != null) as DataViewTableData[]
              }
            />
            <SamværKalkulatorTable
              {...periode}
              beregning={periode.beregning!}
              key={`ferie_${index}_${samværBarn.gjelderBarn.navn}`}
            />
          </div>
        ))
      )}
    </>
  );
}

function SamværKalkulatorTable({
  beregning,
  samværsklasseVisningsnavn,
  gjennomsnittligSamværPerMåned,
  ferieVisningsnavnMap,
  frekvensVisningsnavnMap,
}: NotatSamvaersperiodeDto) {
  const samværNetterBeskrivelse = () => {
    if (gjennomsnittligSamværPerMåned === 0) return "";
    return t.beregning.samværPerMåned
      .replace("{antall}", `${gjennomsnittligSamværPerMåned}`)
      .replace(
        "{netter}",
        gjennomsnittligSamværPerMåned === 1
          ? t.beregning.natt
          : t.beregning.netter,
      );
  };
  return (
    <div className={"flex flex-col gap-1 pt-2 pb-2"}>
      <DataViewTable
        title={t.regelmessigSamvær.tittel}
        data={
          [
            {
              label: t.regelmessigSamvær.antallNetter,
              value: t.regelmessigSamvær.antallNetterVerdi.replace(
                "{}",
                `${beregning!.regelmessigSamværNetter}`,
              ),
            },
          ].filter((d) => d != null) as DataViewTableData[]
        }
      />
      <div>
        <CommonTable
          layoutAuto
          data={{
            headers: [
              {
                name: t.ferieTabell.ferie,
              },
              {
                name: ord.bidragspliktig,
              },
              {
                name: ord.bidragsmottaker,
              },
              {
                name: t.ferieTabell.frekvens,
              },
            ],
            rows: beregning!.ferier.map((ferie) => ({
              borderBottom: true,
              columns: [
                {
                  content: ferieVisningsnavnMap[ferie.type],
                },
                {
                  content: zeroToEmpty(ferie.bidragspliktigNetter),
                },
                {
                  content: zeroToEmpty(ferie.bidragsmottakerNetter),
                },
                {
                  content: frekvensVisningsnavnMap[ferie.frekvens],
                },
              ],
            })),
          }}
        />
        <DataViewTable
          data={
            [
              {
                label: t.beregning.ledetekst,
                labelBold: true,
                value: t.beregning.samværsklasse
                  .replace("{samværsklasse}", `${samværsklasseVisningsnavn}`)
                  .replace("{samværPerMåned}", samværNetterBeskrivelse()),
              },
            ].filter((d) => d != null) as DataViewTableData[]
          }
        />
      </div>
    </div>
  );
}

function zeroToEmpty(value: number) {
  return value === 0 ? "" : value;
}
