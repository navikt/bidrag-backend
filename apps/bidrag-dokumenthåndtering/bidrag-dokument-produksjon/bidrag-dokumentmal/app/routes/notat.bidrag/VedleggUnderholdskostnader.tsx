import { useNotatFelles } from "~/components/notat_felles/NotatContext";
import elementIds from "~/utils/elementIds";
import {
  DatoperiodeDto,
  NotatOffentligeOpplysningerUnderholdBarn,
  DokumentmalPersonDto,
  NotatUnderholdskostnadPeriodeBeregningsdetaljer,
  Rolletype,
  InntektBelopstype,
} from "~/types/Api";
import { DataViewTable, DataViewTableData } from "~/components/DataViewTable";
import GjelderPerson from "~/components/GjelderPerson";
import { CommonTable } from "~/components/CommonTable";
import tekster from "~/tekster";
import { formatPeriode } from "~/utils/date-utils";
import {
  formatterBeløpForBeregning,
  formatterProsent,
} from "~/utils/visningsnavn";
import CalculationTable from "~/components/notat_felles/CalculationTable";
import { VedleggProps } from "~/types/commonTypes";
import { erRolle } from "~/utils/converter-utils";

export default function VedleggUnderholdskostnader({
  vedleggNummer,
}: VedleggProps) {
  const { data, erAvslag } = useNotatFelles();
  if (erAvslag) return null;
  return (
    <div className={`${vedleggNummer == 1 ? "break-before-page" : ""}`}>
      <h2 id={elementIds.vedleggUnderholdskostnader}>
        Vedlegg nr. {vedleggNummer}: Underholdskostnader
      </h2>
      {data.underholdskostnader?.offentligeOpplysningerV2.offentligeOpplysningerBarn.map(
        (offentligOpplysningBarn, index) => (
          <InnhentetBarnetilsynTable
            opplysninger={offentligOpplysningBarn}
            key={index}
            vedleggNummer={vedleggNummer!}
          />
        ),
      )}
      <InnhentetTilleggsstønad
        vedleggNummer={vedleggNummer!}
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
        vedleggNummer={vedleggNummer!}
      />

      <>
        <h4 className={"mt-2"}>
          {vedleggNummer}.4 Beregningsdetaljer tilsynsutgifter
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
  vedleggNummer,
}: {
  opplysninger: DokumentmalPersonDto[];
  vedleggNummer: number;
}) {
  return (
    <>
      <h4 className={"mt-3"}>
        {vedleggNummer}.2 Andre barn til bidragsmottaker - hentet fra offentlige
        registre
      </h4>
      {opplysninger.length == 0 ? (
        <p>Bidragsmottaker har ingen andre barn</p>
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
  vedleggNummer,
}: {
  opplysninger: NotatOffentligeOpplysningerUnderholdBarn;
  vedleggNummer: number;
}) {
  return (
    <>
      <h4>{vedleggNummer}.1 Barnetilsyn</h4>
      <GjelderPerson rolle={opplysninger.gjelderBarn!} />
      {opplysninger.barnetilsyn.length == 0 ? (
        <p>Ingen offentlig barnetilsyn</p>
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
  vedleggNummer,
}: {
  bidragsmottakerHarInnvilgetTilleggsstønad: boolean;
  vedleggNummer: number;
}) {
  const { data } = useNotatFelles();

  const bm = data.roller.find((r) => erRolle(Rolletype.BM)(r));
  return (
    <>
      <h4 className={"mt-3"}>{vedleggNummer}.2 Tilleggsstønad</h4>
      <GjelderPerson rolle={bm!} />
      <DataViewTable
        className={"pt-2"}
        data={
          [
            {
              label:
                "Har fått innvilget tilleggsstønad for ett eller flere barn",
              value: bidragsmottakerHarInnvilgetTilleggsstønad ? "Ja" : "Nei",
            },
          ].filter((d) => d != null) as DataViewTableData[]
        }
      />
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
      <DataViewTable
        className={"mb-2"}
        data={[
          {
            label: "Periode",
            value: formatPeriode(periode.fom, periode.tom),
          },
        ]}
      />
      <div className={"flex flex-col gap-3"}>
        <UnderholdskostnadBeregningsdetaljer detaljer={detaljer} />
      </div>
    </>
  );
}

function UnderholdskostnadBeregningsdetaljer({
  detaljer,
}: {
  detaljer?: NotatUnderholdskostnadPeriodeBeregningsdetaljer;
}) {
  if (!detaljer) return <div>Ingen beregningsdetaljer</div>;
  const tilleggstønad =
    detaljer.tilsynsutgifterBarn?.filter(
      (b) => b.tilleggsstønadBeløp !== undefined,
    ) ?? [];

  return (
    <>
      <DataViewTable
        data={[
          {
            label: "Antall barn under 12 år",
            textRight: false,
            value: `${formatterBeløpForBeregning(detaljer.antallBarnBMUnderTolvÅr)}`,
          },
          detaljer.antallBarnBMBeregnet !==
            detaljer.antallBarnMedTilsynsutgifter && {
            label: "Antall barn med tilsynsutgifter",
            textRight: false,
            value: `${formatterBeløpForBeregning(detaljer.antallBarnMedTilsynsutgifter)}`,
          },
          detaljer.antallBarnBMBeregnet !==
            detaljer.antallBarnBMUnderTolvÅr && {
            label: "Antall barn over 12 år med tilsynsutgifter",
            textRight: false,
            value: `${formatterBeløpForBeregning(detaljer.antallBarnBMBeregnet - detaljer.antallBarnBMUnderTolvÅr)}`,
          },
          detaljer.antallBarnBMBeregnet !==
            detaljer.antallBarnBMUnderTolvÅr && {
            label: "Antall barn i beregningen",
            textRight: false,
            value: `${formatterBeløpForBeregning(detaljer.antallBarnBMBeregnet)}`,
          },
        ].filter((d) => typeof d !== "boolean")}
      />

      {tilleggstønad.length > 0 && (
        <CalculationTable
          title="Faktisk Tilsynsutgifter"
          data={
            detaljer.tilsynsutgifterBarn
              ?.flatMap((b) => [
                {
                  label: b.gjelderBarn.navn,
                  calculation: `(${formatterBeløpForBeregning(b.totalTilsynsutgift)} - ${formatterBeløpForBeregning(b.kostpenger)}) x 11/12`,
                  result: formatterBeløpForBeregning(b.faktiskUtgiftBeregnet),
                },
              ])
              .filter((d) => d) ?? []
          }
        />
      )}
      {tilleggstønad.length > 0 && (
        <CalculationTable
          title="Tilleggsstønad"
          data={detaljer.tilsynsutgifterBarn
            ?.filter((b) => b.tilleggsstønadBeløp !== undefined)
            .map((b) => ({
              label: b.gjelderBarn.navn,
              calculation:
                b.beløpstype === InntektBelopstype.DAGSATS
                  ? `(${formatterBeløpForBeregning(b.tilleggsstønadBeløp)} x 260) / 12 x 11 / 12`
                  : `${formatterBeløpForBeregning(b.tilleggsstønadBeløp)} x 11 / 12`,
              result: formatterBeløpForBeregning(b.tilleggsstønad),
            }))
            .filter((d) => d)}
        />
      )}
      <CalculationTable
        title="Tilsynsutgifter"
        data={[
          ...(detaljer.tilsynsutgifterBarn?.flatMap((b) => [
            {
              label: b.gjelderBarn.navn,
              calculation: b.tilleggsstønad
                ? `${formatterBeløpForBeregning(b.faktiskUtgiftBeregnet)} - ${formatterBeløpForBeregning(b.tilleggsstønad)}`
                : undefined,
              result: formatterBeløpForBeregning(b.beløp),
            },
          ]) ?? []),
          {
            label: "Total",
            labelBold: true,
            result: `${formatterBeløpForBeregning(detaljer.sumTilsynsutgifter)}`,
          },
        ].filter((d) => d)}
      />

      {detaljer.erBegrensetAvMaksTilsyn && (
        <DataViewTable
          data={[
            {
              label: "Totalbeløp begrenset av maks tilsynsutgift",
              textRight: false,
              value: `${formatterBeløpForBeregning(detaljer.totalTilsynsutgift)}`,
            },
            {
              label: "Andel søknadsbarn",
              textRight: false,
              value: `${formatterBeløpForBeregning(detaljer.bruttoTilsynsutgift)} / ${formatterBeløpForBeregning(detaljer.sumTilsynsutgifter)} = ${formatterProsent(detaljer.fordelingFaktor)}`,
            },
          ].filter((d) => d)}
        />
      )}
      <CalculationTable
        title="Skattefradrag"
        data={[
          {
            label: "Maks fradrag",
            calculation: `${formatterBeløpForBeregning(detaljer.sjablonMaksFradrag)} x ${formatterProsent(detaljer.skattesatsFaktor)}`,
            result: formatterBeløpForBeregning(
              detaljer.skattefradragMaksFradrag,
            ),
          },
          {
            label: "Total tilsynsutgift",
            calculation: `${formatterBeløpForBeregning(detaljer.totalTilsynsutgift)} x ${formatterProsent(detaljer.skattesatsFaktor)}`,
            result: formatterBeløpForBeregning(
              detaljer.skattefradragTotalTilsynsutgift,
            ),
          },
          {
            label: "Skattefradrag (laveste verdi)",
            labelBold: true,
            result: `${formatterBeløpForBeregning(detaljer.skattefradrag)}`,
          },
        ].filter((d) => d)}
      />
      <CalculationTable
        title="Beregnet tilsynsutgifter"
        data={[
          {
            label: "Brutto beløp",
            calculation: detaljer.erBegrensetAvMaksTilsyn
              ? `${formatterBeløpForBeregning(detaljer.totalTilsynsutgift)} x ${formatterProsent(detaljer.fordelingFaktor)}`
              : undefined,
            result: `${formatterBeløpForBeregning(detaljer.justertBruttoTilsynsutgift)}`,
          },
          {
            label: "Skattefradrag (per barn)",
            calculation: `${formatterBeløpForBeregning(detaljer.skattefradrag)} / ${formatterBeløpForBeregning(detaljer.antallBarnMedTilsynsutgifter)}`,
            result: `- ${formatterBeløpForBeregning(detaljer.skattefradragPerBarn)}`,
          },
          {
            label: "Resultat",
            labelBold: true,
            result: `${formatterBeløpForBeregning(detaljer.nettoTilsynsutgift)}`,
          },
        ].filter((d) => d)}
      />
    </>
  );
}
