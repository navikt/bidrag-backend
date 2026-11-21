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
} from "~/types/Api";
import { FeltTabell, Feltrad } from "~/components/FeltTabell";
import GjelderPerson from "~/components/GjelderPerson";
import DokumentStabel from "~/components/DokumentStabel";
import {
  DokumentTabell,
  Tabellcelle,
  Tabellrad,
} from "~/components/DokumentTabell";
import { formatPeriode } from "~/utils/date-utils";
import {
  formatterBeløpForBeregning,
  formatterProsent,
} from "~/utils/visningsnavn";
import { CalculationTable } from "~/components/CalculationTable";
import { erRolle } from "~/utils/converter-utils";
import { tekst } from "~/utils/tekst";
import {
  harTilleggsstønad,
  lagBeregnetTilsynsutgiftRader,
  lagFaktiskTilsynsutgiftRader,
  lagSkattefradragRader,
  lagTilleggsstønadRader,
  lagTilsynsutgiftRader,
} from "~/routes/notat/opplysninger/bidrag/underholdskostnaderViewModel";
export default function VedleggUnderholdskostnader() {
  const vedleggNummer = useVedleggNummer("underholdskostnader");
  const { data, erAvslag } = useNotatFelles();
  if (erAvslag) return null;
  return (
    <div className={`${vedleggNummer == 1 ? "break-before-page" : ""}`}>
      <h2 id={elementIds.vedleggUnderholdskostnader}>
        {tekst(
          "notat.opplysninger.bidrag.underholdskostnader.tittel",
          "Vedlegg nr. {}: Underholdskostnader",
        ).replace("{}", `${vedleggNummer}`)}
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
          {tekst(
            "notat.opplysninger.bidrag.underholdskostnader.beregningsdetaljer.tittel",
            "{}.4 Beregningsdetaljer tilsynsutgifter",
          ).replace("{}", `${vedleggNummer}`)}
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
        {tekst(
          "notat.opplysninger.bidrag.underholdskostnader.andreBarnTilBidragsmottaker.tittel",
          "{}.2 Andre barn til bidragsmottaker - hentet fra offentlige registre",
        ).replace("{}", `${vedleggNummer}`)}
      </h4>
      {opplysninger.length == 0 ? (
        <p>
          {tekst(
            "notat.opplysninger.bidrag.underholdskostnader.andreBarnTilBidragsmottaker.ingenAndreBarn",
            "Bidragsmottaker har ingen andre barn",
          )}
        </p>
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
      <h4>
        {tekst(
          "notat.opplysninger.bidrag.underholdskostnader.barnetilsyn.tittel",
          "{}.1 Barnetilsyn",
        ).replace("{}", `${vedleggNummer}`)}
      </h4>
      <GjelderPerson rolle={opplysninger.gjelderBarn!} />
      {opplysninger.barnetilsyn.length == 0 ? (
        <p>
          {tekst(
            "notat.opplysninger.bidrag.underholdskostnader.barnetilsyn.ingenOffentligBarnetilsyn",
            "Ingen offentlig barnetilsyn",
          )}
        </p>
      ) : (
        <DokumentTabell
          layoutAuto
          kolonner={[
            {
              tittel: tekst(
                "felles.tabell.felles.fraTilOgMed",
                "Fra og med - Til og med",
              ),
              bredde: "150px",
            },
          ]}
        >
          {opplysninger.barnetilsyn.map((periode, index) => (
            <Tabellrad key={index} index={index}>
              <Tabellcelle>
                {formatPeriode(periode.periode.fom, periode.periode.til)}
              </Tabellcelle>
            </Tabellrad>
          ))}
        </DokumentTabell>
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
        {tekst(
          "notat.opplysninger.bidrag.underholdskostnader.tilleggsstønad.tittel",
          "{}.2 Tilleggsstønad",
        ).replace("{}", `${vedleggNummer}`)}
      </h4>
      <GjelderPerson rolle={bm!} />
      <div className={"pt-2"}>
        <FeltTabell>
          <Feltrad
            label={tekst(
              "notat.opplysninger.bidrag.underholdskostnader.tilleggsstønad.harFåttInnvilget",
              "Har fått innvilget tilleggsstønad for ett eller flere barn",
            )}
          >
            {bidragsmottakerHarInnvilgetTilleggsstønad
              ? tekst("ord.ja", "Ja")
              : tekst("ord.nei", "Nei")}
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
          <Feltrad label={tekst("ord.periode", "Periode")}>
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
    return (
      <div>
        {tekst(
          "notat.opplysninger.bidrag.underholdskostnader.beregningsdetaljer.ingenBeregningsdetaljer",
          "Ingen beregningsdetaljer",
        )}
      </div>
    );
  const harInnvilgetTilleggsstønad = harTilleggsstønad(detaljer);
  return (
    <DokumentStabel gap={3}>
      <div>
        <FeltTabell>
          <Feltrad
            label={tekst(
              "notat.opplysninger.bidrag.underholdskostnader.beregningsdetaljer.antallBarnUnderTolvÅr",
              "Antall barn under 12 år",
            )}
          >
            {formatterBeløpForBeregning(detaljer.antallBarnBMUnderTolvÅr)}
          </Feltrad>
          {detaljer.antallBarnBMBeregnet !==
            detaljer.antallBarnMedTilsynsutgifter && (
            <Feltrad
              label={tekst(
                "notat.opplysninger.bidrag.underholdskostnader.beregningsdetaljer.antallBarnMedTilsynsutgifter",
                "Antall barn med tilsynsutgifter",
              )}
            >
              {formatterBeløpForBeregning(
                detaljer.antallBarnMedTilsynsutgifter,
              )}
            </Feltrad>
          )}
          {detaljer.antallBarnBMBeregnet !==
            detaljer.antallBarnBMUnderTolvÅr && (
            <>
              <Feltrad
                label={tekst(
                  "notat.opplysninger.bidrag.underholdskostnader.beregningsdetaljer.antallBarnOverTolvÅrMedTilsynsutgifter",
                  "Antall barn over 12 år med tilsynsutgifter",
                )}
              >
                {formatterBeløpForBeregning(
                  detaljer.antallBarnBMBeregnet -
                    detaljer.antallBarnBMUnderTolvÅr,
                )}
              </Feltrad>
              <Feltrad
                label={tekst(
                  "notat.opplysninger.bidrag.underholdskostnader.beregningsdetaljer.antallBarnIBeregningen",
                  "Antall barn i beregningen",
                )}
              >
                {formatterBeløpForBeregning(detaljer.antallBarnBMBeregnet)}
              </Feltrad>
            </>
          )}
        </FeltTabell>
      </div>

      {harInnvilgetTilleggsstønad && (
        <CalculationTable
          title={tekst(
            "notat.opplysninger.bidrag.underholdskostnader.faktiskTilsynsutgifter.tittel",
            "Faktisk Tilsynsutgifter",
          )}
          data={lagFaktiskTilsynsutgiftRader(detaljer)}
        />
      )}
      {harInnvilgetTilleggsstønad && (
        <CalculationTable
          title={tekst(
            "notat.opplysninger.bidrag.underholdskostnader.tilleggsstønadBeregning.tittel",
            "Tilleggsstønad",
          )}
          data={lagTilleggsstønadRader(detaljer)}
        />
      )}
      <CalculationTable
        title={tekst(
          "notat.opplysninger.bidrag.underholdskostnader.tilsynsutgifter.tittel",
          "Tilsynsutgifter",
        )}
        data={lagTilsynsutgiftRader(detaljer)}
      />

      {detaljer.erBegrensetAvMaksTilsyn && (
        <div>
          <FeltTabell>
            <Feltrad
              label={tekst(
                "notat.opplysninger.bidrag.underholdskostnader.begrensetAvMaksTilsyn.totalbeløp",
                "Totalbeløp begrenset av maks tilsynsutgift",
              )}
            >{`${formatterBeløpForBeregning(detaljer.totalTilsynsutgift)}`}</Feltrad>
            <Feltrad
              label={tekst(
                "notat.opplysninger.bidrag.underholdskostnader.begrensetAvMaksTilsyn.andelSøknadsbarn",
                "Andel søknadsbarn",
              )}
            >
              {tekst(
                "notat.opplysninger.bidrag.underholdskostnader.begrensetAvMaksTilsyn.andelSøknadsbarnBeregning",
                "{bruttoTilsynsutgift} / {sumTilsynsutgifter} = {fordelingFaktor}",
              )
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
        title={tekst(
          "notat.opplysninger.bidrag.underholdskostnader.skattefradrag.tittel",
          "Skattefradrag",
        )}
        data={lagSkattefradragRader(detaljer)}
      />
      <CalculationTable
        title={tekst(
          "notat.opplysninger.bidrag.underholdskostnader.beregnetTilsynsutgifter.tittel",
          "Beregnet tilsynsutgifter",
        )}
        data={lagBeregnetTilsynsutgiftRader(detaljer)}
      />
    </DokumentStabel>
  );
}
