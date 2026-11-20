import {
  useNotatFelles,
  useVedleggNummer,
} from "~/components/notat_felles/NotatContext";
import elementIds from "~/utils/elementIds";
import {
  DatoperiodeDto,
  NotatOffentligeOpplysningerUnderholdBarn,
  DokumentmalPersonDto,
  NotatUnderholdskostnadPeriodeBeregningsdetaljer,
  Rolletype,
  InntektBelopstype,
} from "~/types/Api";
import { FeltTabell, Feltrad } from "~/components/FeltTabell";
import GjelderPerson from "~/components/GjelderPerson";
import DokumentStabel from "~/components/DokumentStabel";
import { CommonTable } from "~/components/CommonTable";
import ord from "~/tekster/ord.yaml";
import tekster from "~/tekster";
import { formatPeriode } from "~/utils/date-utils";
import {
  formatterBeløpForBeregning,
  formatterProsent,
} from "~/utils/visningsnavn";
import CalculationTable from "~/components/notat_felles/CalculationTable";
import { erRolle } from "~/utils/converter-utils";
import bidragOpplysningerTekster from "~/tekster/notat/opplysninger/bidrag.yaml";

const t = bidragOpplysningerTekster.underholdskostnader;

export default function VedleggUnderholdskostnader() {
  const vedleggNummer = useVedleggNummer("underholdskostnader");
  const { data, erAvslag } = useNotatFelles();
  if (erAvslag) return null;
  return (
    <div className={`${vedleggNummer == 1 ? "break-before-page" : ""}`}>
      <h2 id={elementIds.vedleggUnderholdskostnader}>
        {t.tittel.replace("{}", `${vedleggNummer}`)}
      </h2>
      {data.underholdskostnader?.offentligeOpplysningerV2.offentligeOpplysningerBarn.map(
        (offentligOpplysningBarn, index) => (
          <InnhentetBarnetilsynTable
            opplysninger={offentligOpplysningBarn}
            key={index}
          />
        ),
      )}
      <InnhentetTilleggsstønad
        bidragsmottakerHarInnvilgetTilleggsstønad={
          data.underholdskostnader?.offentligeOpplysningerV2
            ?.bidragsmottakerHarInnvilgetTilleggsstønad ?? false
        }
      />
      <InnhentetAndreBarnTilBidragsmottaker
        opplysninger={
          data.underholdskostnader?.offentligeOpplysningerV2
            ?.andreBarnTilBidragsmottaker ?? []
        }
      />

      <>
        <h4 className={"mt-2"}>
          {t.beregningsdetaljer.tittel.replace("{}", `${vedleggNummer}`)}
        </h4>
        {data.underholdskostnader?.underholdskostnaderBarn?.flatMap((uBarn) =>
          uBarn.underholdskostnad.map((underholdskostnader, index) => (
            <div className={index > 0 ? "mt-4" : ""} key={index}>
              <UnderholdskostnaderBeregningsdetaljerTabell
                detaljer={underholdskostnader.beregningsdetaljer}
                periode={underholdskostnader.periode}
              />
            </div>
          )),
        )}
      </>
    </div>
  );
}
function InnhentetAndreBarnTilBidragsmottaker({
  opplysninger,
}: {
  opplysninger: DokumentmalPersonDto[];
}) {
  const vedleggNummer = useVedleggNummer("underholdskostnader");
  return (
    <>
      <h4 className={"mt-3"}>
        {t.andreBarnTilBidragsmottaker.tittel.replace("{}", `${vedleggNummer}`)}
      </h4>
      {opplysninger.length == 0 ? (
        <p>{t.andreBarnTilBidragsmottaker.ingenAndreBarn}</p>
      ) : (
        opplysninger.map((barn, index) => (
          <GjelderPerson rolle={barn} key={index} visFødselsdato />
        ))
      )}
    </>
  );
}
function InnhentetBarnetilsynTable({
  opplysninger,
}: {
  opplysninger: NotatOffentligeOpplysningerUnderholdBarn;
}) {
  const vedleggNummer = useVedleggNummer("underholdskostnader");
  return (
    <>
      <h4>{t.barnetilsyn.tittel.replace("{}", `${vedleggNummer}`)}</h4>
      <GjelderPerson rolle={opplysninger.gjelderBarn!} />
      {opplysninger.barnetilsyn.length == 0 ? (
        <p>{t.barnetilsyn.ingenOffentligBarnetilsyn}</p>
      ) : (
        <CommonTable
          layoutAuto
          data={{
            headers: [
              {
                name: tekster.tabell.felles.fraTilOgMed,
                width: "150px",
              },
            ],
            rows: opplysninger.barnetilsyn.map((periode) => ({
              columns: [
                {
                  content: formatPeriode(
                    periode.periode.fom,
                    periode.periode.til,
                  ),
                },
              ],
            })),
          }}
        />
      )}
    </>
  );
}
function InnhentetTilleggsstønad({
  bidragsmottakerHarInnvilgetTilleggsstønad,
}: {
  bidragsmottakerHarInnvilgetTilleggsstønad: boolean;
}) {
  const vedleggNummer = useVedleggNummer("underholdskostnader");
  const { data } = useNotatFelles();

  const bm = data.roller.find((r) => erRolle(Rolletype.BM)(r));
  return (
    <>
      <h4 className={"mt-3"}>
        {t.tilleggsstønad.tittel.replace("{}", `${vedleggNummer}`)}
      </h4>
      <GjelderPerson rolle={bm!} />
      <div className={"pt-2"}>
        <FeltTabell>
          <Feltrad label={t.tilleggsstønad.harFåttInnvilget}>
            {bidragsmottakerHarInnvilgetTilleggsstønad ? ord.ja : ord.nei}
          </Feltrad>
        </FeltTabell>
      </div>
    </>
  );
}

function UnderholdskostnaderBeregningsdetaljerTabell({
  detaljer,
  periode,
}: {
  detaljer?: NotatUnderholdskostnadPeriodeBeregningsdetaljer;
  periode: DatoperiodeDto;
}) {
  return (
    <>
      <div className={"mb-2"}>
        <FeltTabell>
          <Feltrad label={ord.periode}>
            {formatPeriode(periode.fom, periode.tom)}
          </Feltrad>
        </FeltTabell>
      </div>
      <UnderholdskostnadBeregningsdetaljer detaljer={detaljer} />
    </>
  );
}

function UnderholdskostnadBeregningsdetaljer({
  detaljer,
}: {
  detaljer?: NotatUnderholdskostnadPeriodeBeregningsdetaljer;
}) {
  if (!detaljer)
    return <div>{t.beregningsdetaljer.ingenBeregningsdetaljer}</div>;
  const tilleggstønad =
    detaljer.tilsynsutgifterBarn?.filter(
      (b) => b.tilleggsstønadBeløp !== undefined,
    ) ?? [];

  return (
    <DokumentStabel gap={3}>
      <div>
        <FeltTabell>
          <Feltrad label={t.beregningsdetaljer.antallBarnUnderTolvÅr}>
            {formatterBeløpForBeregning(detaljer.antallBarnBMUnderTolvÅr)}
          </Feltrad>
          {detaljer.antallBarnBMBeregnet !==
            detaljer.antallBarnMedTilsynsutgifter && (
            <Feltrad label={t.beregningsdetaljer.antallBarnMedTilsynsutgifter}>
              {formatterBeløpForBeregning(
                detaljer.antallBarnMedTilsynsutgifter,
              )}
            </Feltrad>
          )}
          {detaljer.antallBarnBMBeregnet !==
            detaljer.antallBarnBMUnderTolvÅr && (
            <>
              <Feltrad
                label={
                  t.beregningsdetaljer.antallBarnOverTolvÅrMedTilsynsutgifter
                }
              >
                {formatterBeløpForBeregning(
                  detaljer.antallBarnBMBeregnet -
                    detaljer.antallBarnBMUnderTolvÅr,
                )}
              </Feltrad>
              <Feltrad label={t.beregningsdetaljer.antallBarnIBeregningen}>
                {formatterBeløpForBeregning(detaljer.antallBarnBMBeregnet)}
              </Feltrad>
            </>
          )}
        </FeltTabell>
      </div>

      {tilleggstønad.length > 0 && (
        <CalculationTable
          title={t.faktiskTilsynsutgifter.tittel}
          data={
            detaljer.tilsynsutgifterBarn
              ?.flatMap((b) => [
                {
                  label: b.gjelderBarn.navn,
                  calculation: t.faktiskTilsynsutgifter.beregning
                    .replace(
                      "{totalTilsynsutgift}",
                      formatterBeløpForBeregning(b.totalTilsynsutgift),
                    )
                    .replace(
                      "{kostpenger}",
                      formatterBeløpForBeregning(b.kostpenger),
                    ),
                  result: formatterBeløpForBeregning(b.faktiskUtgiftBeregnet),
                },
              ])
              .filter((d) => d) ?? []
          }
        />
      )}
      {tilleggstønad.length > 0 && (
        <CalculationTable
          title={t.tilleggsstønadBeregning.tittel}
          data={detaljer.tilsynsutgifterBarn
            ?.filter((b) => b.tilleggsstønadBeløp !== undefined)
            .map((b) => ({
              label: b.gjelderBarn.navn,
              calculation:
                b.beløpstype === InntektBelopstype.DAGSATS
                  ? t.tilleggsstønadBeregning.beregningDagsats.replace(
                      "{beløp}",
                      formatterBeløpForBeregning(b.tilleggsstønadBeløp),
                    )
                  : t.tilleggsstønadBeregning.beregning.replace(
                      "{beløp}",
                      formatterBeløpForBeregning(b.tilleggsstønadBeløp),
                    ),
              result: formatterBeløpForBeregning(b.tilleggsstønad),
            }))
            .filter((d) => d)}
        />
      )}
      <CalculationTable
        title={t.tilsynsutgifter.tittel}
        data={[
          ...(detaljer.tilsynsutgifterBarn?.flatMap((b) => [
            {
              label: b.gjelderBarn.navn,
              calculation: b.tilleggsstønad
                ? t.tilsynsutgifter.beregning
                    .replace(
                      "{faktiskUtgift}",
                      formatterBeløpForBeregning(b.faktiskUtgiftBeregnet),
                    )
                    .replace(
                      "{tilleggsstønad}",
                      formatterBeløpForBeregning(b.tilleggsstønad),
                    )
                : undefined,
              result: formatterBeløpForBeregning(b.beløp),
            },
          ]) ?? []),
          {
            label: ord.total,
            labelBold: true,
            result: `${formatterBeløpForBeregning(detaljer.sumTilsynsutgifter)}`,
          },
        ].filter((d) => d)}
      />

      {detaljer.erBegrensetAvMaksTilsyn && (
        <div>
          <FeltTabell>
            <Feltrad
              label={t.begrensetAvMaksTilsyn.totalbeløp}
            >{`${formatterBeløpForBeregning(detaljer.totalTilsynsutgift)}`}</Feltrad>
            <Feltrad label={t.begrensetAvMaksTilsyn.andelSøknadsbarn}>
              {t.begrensetAvMaksTilsyn.andelSøknadsbarnBeregning
                .replace(
                  "{bruttoTilsynsutgift}",
                  formatterBeløpForBeregning(detaljer.bruttoTilsynsutgift),
                )
                .replace(
                  "{sumTilsynsutgifter}",
                  formatterBeløpForBeregning(detaljer.sumTilsynsutgifter),
                )
                .replace(
                  "{fordelingFaktor}",
                  formatterProsent(detaljer.fordelingFaktor),
                )}
            </Feltrad>
          </FeltTabell>
        </div>
      )}
      <CalculationTable
        title={t.skattefradrag.tittel}
        data={[
          {
            label: t.skattefradrag.maksFradrag,
            calculation: t.skattefradrag.maksFradragBeregning
              .replace(
                "{sjablonMaksFradrag}",
                formatterBeløpForBeregning(detaljer.sjablonMaksFradrag),
              )
              .replace(
                "{skattesats}",
                formatterProsent(detaljer.skattesatsFaktor),
              ),
            result: formatterBeløpForBeregning(
              detaljer.skattefradragMaksFradrag,
            ),
          },
          {
            label: t.skattefradrag.totalTilsynsutgift,
            calculation: t.skattefradrag.totalTilsynsutgiftBeregning
              .replace(
                "{totalTilsynsutgift}",
                formatterBeløpForBeregning(detaljer.totalTilsynsutgift),
              )
              .replace(
                "{skattesats}",
                formatterProsent(detaljer.skattesatsFaktor),
              ),
            result: formatterBeløpForBeregning(
              detaljer.skattefradragTotalTilsynsutgift,
            ),
          },
          {
            label: t.skattefradrag.lavesteVerdi,
            labelBold: true,
            result: `${formatterBeløpForBeregning(detaljer.skattefradrag)}`,
          },
        ].filter((d) => d)}
      />
      <CalculationTable
        title={t.beregnetTilsynsutgifter.tittel}
        data={[
          {
            label: t.beregnetTilsynsutgifter.bruttoBeløp,
            calculation: detaljer.erBegrensetAvMaksTilsyn
              ? t.beregnetTilsynsutgifter.bruttoBeløpBeregning
                  .replace(
                    "{totalTilsynsutgift}",
                    formatterBeløpForBeregning(detaljer.totalTilsynsutgift),
                  )
                  .replace(
                    "{fordelingFaktor}",
                    formatterProsent(detaljer.fordelingFaktor),
                  )
              : undefined,
            result: `${formatterBeløpForBeregning(detaljer.justertBruttoTilsynsutgift)}`,
          },
          {
            label: t.beregnetTilsynsutgifter.skattefradragPerBarn,
            calculation: t.beregnetTilsynsutgifter.skattefradragPerBarnBeregning
              .replace(
                "{skattefradrag}",
                formatterBeløpForBeregning(detaljer.skattefradrag),
              )
              .replace(
                "{antallBarn}",
                formatterBeløpForBeregning(
                  detaljer.antallBarnMedTilsynsutgifter,
                ),
              ),
            result:
              t.beregnetTilsynsutgifter.skattefradragPerBarnResultat.replace(
                "{}",
                formatterBeløpForBeregning(detaljer.skattefradragPerBarn),
              ),
          },
          {
            label: ord.resultat,
            labelBold: true,
            result: `${formatterBeløpForBeregning(detaljer.nettoTilsynsutgift)}`,
          },
        ].filter((d) => d)}
      />
    </DokumentStabel>
  );
}
