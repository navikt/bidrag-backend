import {
  useNotatFelles,
  useVedleggNummer,
} from "~/components/notat_felles/NotatContext";
import elementIds from "~/utils/elementIds";
import { CommonTable } from "~/components/CommonTable";
import DokumentStabel from "~/components/DokumentStabel";
import { NotatSamvaerBarnDto, NotatSamvaersperiodeDto } from "~/types/Api";
import { FeltTabell, Feltrad } from "~/components/FeltTabell";
import { formatPeriode } from "~/utils/date-utils";
import { rolleTilVisningsnavnV2 } from "~/utils/visningsnavn";
import ord from "~/tekster/ord.yaml";
import bidragOpplysningerTekster from "~/tekster/notat/opplysninger/bidrag.yaml";

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
        <div key={index + "-" + samværBarn.gjelderBarn.ident}>
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
    <DokumentStabel gap={2}>
      <div>
        <FeltTabell>
          <Feltrad
            label={rolleTilVisningsnavnV2(samværBarn.gjelderBarn)}
            labelBold
          >
            {samværBarn.gjelderBarn.navn}
          </Feltrad>
        </FeltTabell>
      </div>
      {samværsperioderMedBeregning.length == 0 ? (
        <p>{t.ingenBeregnetSamvær}</p>
      ) : (
        samværsperioderMedBeregning.map((periode, index) => (
          <div key={index + "_" + periode.samværsklasseVisningsnavn}>
            <div>
              <FeltTabell>
                <Feltrad label={ord.periode}>
                  {formatPeriode(periode.periode.fom, periode.periode.tom)}
                </Feltrad>
              </FeltTabell>
            </div>
            <SamværKalkulatorTable
              {...periode}
              beregning={periode.beregning!}
              key={`ferie_${index}_${samværBarn.gjelderBarn.navn}`}
            />
          </div>
        ))
      )}
    </DokumentStabel>
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
    <DokumentStabel gap={1} className="pt-2 pb-2">
      <div>
        {t.regelmessigSamvær.tittel && <h4>{t.regelmessigSamvær.tittel}</h4>}
        <FeltTabell>
          <Feltrad label={t.regelmessigSamvær.antallNetter}>
            {t.regelmessigSamvær.antallNetterVerdi.replace(
              "{}",
              `${beregning!.regelmessigSamværNetter}`,
            )}
          </Feltrad>
        </FeltTabell>
      </div>
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
        <div>
          <FeltTabell>
            <Feltrad label={t.beregning.ledetekst} labelBold>
              {t.beregning.samværsklasse
                .replace("{samværsklasse}", `${samværsklasseVisningsnavn}`)
                .replace("{samværPerMåned}", samværNetterBeskrivelse())}
            </Feltrad>
          </FeltTabell>
        </div>
      </div>
    </DokumentStabel>
  );
}

function zeroToEmpty(value: number) {
  return value === 0 ? "" : value;
}
