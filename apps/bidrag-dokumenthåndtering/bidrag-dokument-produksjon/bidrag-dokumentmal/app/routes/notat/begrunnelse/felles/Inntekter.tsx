import {
  DelberegningSumInntekt,
  DokumentmalPersonDto,
  Inntektsrapportering,
  NotatBeregnetInntektDto,
  NotatInntektDto,
  NotatMalType,
  Rolletype,
} from "~/types/Api";
import { deductDays, formatPeriode, sortByAge } from "~/utils/date-utils";
import KildeIcon from "~/components/KildeIcon";
import { groupBy, hasValue } from "~/utils/array-utils";
import { formatterBeløp, formatterProsent } from "~/utils/visningsnavn";
import NotatBegrunnelse from "~/components/NotatBegrunnelse";
import {
  CommonTable,
  TableColumn,
  TableHeader,
} from "~/components/CommonTable";
import ord from "~/tekster/ord.yaml";
import tekster from "~/tekster";
import notatTekster from "~/tekster/notat/begrunnelse/felles.yaml";
import { getInntektTableHeaders } from "~/constants/tableHeaders";
import { useNotatFelles } from "~/components/notat_felles/NotatContext";
import { Seksjon } from "~/components/notat_felles/Seksjon";
import { ForHverRolle, useRolle } from "~/components/notat_felles/RolleContext";
import { finnInntekterFor } from "~/utils/notatUtvalg";
import Inntektsposter from "~/routes/notat/felles/Inntektsposter";
import {
  BehandlingRolletype,
  beregnetInntekterColumnNames,
  beregnetInntekterColumnNamesV2,
  beregnetInntekterColumnWidth,
  inntekterTablesViewRules,
  InntektTableType,
  isHarInntekter,
} from "~/components/inntektTableHelpers";
import InntektTableTitle from "~/components/inntekt/InntektTableTitle";
import TableGjelderBarn from "~/components/TableGjelderBarn";
import { useTheme } from "~/components/notat_felles/ThemeContext";
import GjelderPerson from "~/components/GjelderPerson";

const t = notatTekster.inntekter;

/** Hvilke roller som får vist inntektene sine, per notattype. */
const rollerMedInntekter: Record<NotatMalType, Rolletype[]> = {
  [NotatMalType.FORSKUDD]: [Rolletype.BM],
  [NotatMalType.SAeRBIDRAG]: [Rolletype.BM, Rolletype.BP, Rolletype.BA],
  [NotatMalType.BIDRAG]: [Rolletype.BM, Rolletype.BP, Rolletype.BA],
};

export default function Inntekter() {
  const { erAvslag, type } = useNotatFelles();
  if (erAvslag && type != NotatMalType.BIDRAG) return null;
  return (
    <Seksjon
      tittel={t.tittel}
      vedlegg={"inntekter"}
      lenketekst={ord.seVedleggForOffentligeRegistre}
    >
      <ForHverRolle typer={rollerMedInntekter[type]}>
        <InntekterForRolle />
      </ForHverRolle>
    </Seksjon>
  );
}

function InntekterForRolle() {
  const { data } = useNotatFelles();
  const rolle = useRolle();
  const { inntekter, begrunnelse } = finnInntekterFor(data.inntekter, rolle);
  if (inntekter == null) return null;
  return (
    <>
      <GjelderPerson rolle={rolle} />
      {isHarInntekter(inntekter) ? (
        <>
          <InntektTable
            data={inntekter.årsinntekter}
            title={t.tabeller.skattepliktigeOgPensjonsgivende}
            subsection={false}
          />
          <InntektPerBarnTable
            data={inntekter.barnetillegg}
            title={t.tabeller.barnetillegg}
          />
          <InntektTable
            data={inntekter.utvidetBarnetrygd}
            title={t.tabeller.utvidetBarnetrygd}
            inkluderBeskrivelse={false}
          />
          <InntektTable
            data={inntekter.småbarnstillegg}
            title={t.tabeller.småbarnstillegg}
            inkluderBeskrivelse={false}
          />
          <InntektPerBarnTable
            data={inntekter.kontantstøtte}
            title={t.tabeller.kontantstøtte}
          />
          <BeregnetInntektTable
            data={inntekter.beregnetInntekter}
            rolle={rolle}
          />
        </>
      ) : (
        <p>{t.ingenInntekter}</p>
      )}
      <NotatBegrunnelse data={begrunnelse} />
    </>
  );
}

type InntektTableProps = {
  data: NotatInntektDto[];
  title?: string;
  subtitle?: string;
  inkluderBeskrivelse?: boolean;
  bareMedIBeregning?: boolean;
  subsection?: boolean;
};

function InntektTable({
  data,
  title,
  subtitle,
  bareMedIBeregning = true,
  inkluderBeskrivelse = true,
  subsection = true,
}: InntektTableProps) {
  const inntekter = data.filter((d) => !bareMedIBeregning || d.medIBeregning);
  if (inntekter.length == 0) return null;
  return (
    <div className={subsection ? "subsection" : ""}>
      <InntektTableTitle title={title} subtitle={subtitle} />
      <CommonTable
        data={{
          headers: [
            { name: tekster.tabell.felles.fraTilOgMed, width: "170px" },
            inkluderBeskrivelse && {
              name: ord.beskrivelse,
              width: "230px",
            },
            {
              name: tekster.tabell.felles.kilde,
              width: inkluderBeskrivelse ? "80px" : "230px",
            },
            { name: ord.beløp },
          ].filter((d) => typeof d != "boolean") as TableHeader[],
          rows: inntekter
            .sort((a, b) =>
              a.periode?.fom && b.periode?.fom
                ? a.periode?.fom.localeCompare(b.periode?.fom)
                : 1,
            )
            .map((d, i) => {
              const periode = d.periode ?? d.opprinneligPeriode;
              return {
                columns: [
                  { content: formatPeriode(periode!.fom, periode!.til) },
                  inkluderBeskrivelse && {
                    content: `${d.visningsnavn}${d.historisk ? t.historisk : ""}`,
                  },
                  { content: <KildeIcon kilde={d.kilde} /> },
                  { content: formatterBeløp(d.beløp) },
                ].filter((d) => typeof d != "boolean") as TableColumn[],
                expandableContent:
                  d.inntektsposter.length > 0
                    ? [
                        {
                          content: (
                            <Inntektsposter
                              data={d}
                              periode={periode}
                              withHorizontalLine={inntekter.length > i + 1}
                            />
                          ),
                        },
                      ]
                    : undefined,
              };
            }),
        }}
      />
    </div>
  );
}

function InntektPerBarnTable({
  data,
  title,
  subtitle,
  bareMedIBeregning = true,
}: InntektTableProps) {
  const inntekter = data.filter((d) => !bareMedIBeregning || d.medIBeregning);
  const { styling } = useTheme();
  const { type } = useNotatFelles();
  const erBidrag = type === NotatMalType.BIDRAG;
  if (inntekter.length == 0) return null;

  const inntekterBarn = groupBy(data, (d) => d.gjelderBarn?.ident!);
  return (
    <div className={"mt-medium"}>
      <InntektTableTitle title={title} subtitle={subtitle} />
      {inntekterBarn
        .sort(([_, a], [d2, b]) =>
          sortByAge(a[0].gjelderBarn, b[0].gjelderBarn),
        )
        .map(([key, value], i) => {
          const gjelderBarn = value[0].gjelderBarn!;
          const erBarnetillegg =
            value[0].type == Inntektsrapportering.BARNETILLEGG;
          const addMargin = inntekterBarn.length > i + 1;
          return (
            <div
              key={gjelderBarn + key + i.toString()}
              className="table_container"
              style={{
                marginBottom: addMargin ? "16px" : "0px",
              }}
            >
              <TableGjelderBarn gjelderBarn={gjelderBarn} />
              <CommonTable
                width={"580px"}
                data={{
                  headers: getInntektTableHeaders(
                    erBarnetillegg,
                    erBidrag,
                    true,
                    styling,
                  ),
                  rows: value
                    .filter((d) => !bareMedIBeregning || d.medIBeregning)
                    .map((d) => {
                      const periode = d.periode ?? d.opprinneligPeriode;
                      const visningsnavnInntektstype =
                        d.inntektsposter.length > 0
                          ? d.inntektsposter[0].visningsnavn
                          : "";

                      return {
                        periodColumn:
                          styling == "V2" && erBarnetillegg
                            ? formatPeriode(periode!.fom, periode!.til)
                            : undefined,
                        columns: [
                          (styling == "V1" || !erBarnetillegg) && {
                            content: formatPeriode(periode!.fom, periode!.til),
                          },
                          { content: <KildeIcon kilde={d.kilde} /> },
                          erBarnetillegg && erBidrag
                            ? [
                                { content: visningsnavnInntektstype },
                                {
                                  content: Number(d.beløpMånedDagsats),
                                },
                                {
                                  content: d.beløpstypeVisningsnavn,
                                },
                                {
                                  content:
                                    d.skattefaktor == null
                                      ? ord.ikkeRelevant
                                      : formatterProsent(d.skattefaktor),
                                },
                                { content: formatterBeløp(d.beløp) },
                              ]
                            : erBarnetillegg
                              ? [
                                  { content: visningsnavnInntektstype },
                                  {
                                    content: Number(d.beløpMånedDagsats),
                                  },
                                  {
                                    content: d.beløpstypeVisningsnavn,
                                  },
                                  { content: formatterBeløp(d.beløp) },
                                ]
                              : [{ content: formatterBeløp(d.beløp) }],
                        ]
                          .filter((d) => typeof d != "boolean")
                          .flatMap((d) => d as TableColumn),
                      };
                    }),
                }}
              />
            </div>
          );
        })}
    </div>
  );
}

type BeregnetInntektTableProps = {
  data: NotatBeregnetInntektDto[];
  rolle: DokumentmalPersonDto;
};

function BeregnetInntektTable({ data, rolle }: BeregnetInntektTableProps) {
  const { type, harFlereEnnEttSøknadsbarn } = useNotatFelles();
  const { styling } = useTheme();
  const inntektTableRules =
    inntekterTablesViewRules[type][rolle.rolle as BehandlingRolletype];

  const columnNames =
    styling == "V2"
      ? beregnetInntekterColumnNamesV2[type][rolle.rolle as BehandlingRolletype]
      : beregnetInntekterColumnNames[type][rolle.rolle as BehandlingRolletype];

  const columnWidth =
    beregnetInntekterColumnWidth[type][rolle.rolle as BehandlingRolletype];
  function renderTable(
    inntekter: DelberegningSumInntekt[],
    gjelderBarn?: DokumentmalPersonDto,
  ) {
    return (
      <>
        {harFlereEnnEttSøknadsbarn && gjelderBarn && (
          <TableGjelderBarn gjelderBarn={gjelderBarn} />
        )}
        <CommonTable
          width={"580px"}
          data={{
            headers: [
              styling == "V1" && {
                name: tekster.tabell.felles.fraTilOgMed,
                width: columnWidth[tekster.tabell.felles.fraTilOgMed],
              },
              {
                name: columnNames.SKATTEPLIKTIG,
                width: columnWidth[columnNames.SKATTEPLIKTIG as string],
              },
              hasValue(inntektTableRules, InntektTableType.BARNETILLEGG) && {
                name: columnNames.BARNETILLEGG,
                width: columnWidth[columnNames.BARNETILLEGG as string],
              },
              hasValue(
                inntektTableRules,
                InntektTableType.UTVIDET_BARNETRYGD,
              ) && {
                name: columnNames.UTVIDET_BARNETRYGD,
                width: columnWidth[columnNames.UTVIDET_BARNETRYGD as string],
              },
              hasValue(inntektTableRules, InntektTableType.SMÅBARNSTILLEGG) && {
                name: columnNames.SMÅBARNSTILLEGG,
                width: columnWidth[columnNames.SMÅBARNSTILLEGG as string],
              },
              hasValue(inntektTableRules, InntektTableType.KONTANTSTØTTE) && {
                name: columnNames.KONTANTSTØTTE,
                width: columnWidth[columnNames.KONTANTSTØTTE as string],
              },
              {
                name: columnNames.TOTAL_INNTEKTER,
                width: columnWidth[columnNames.TOTAL_INNTEKTER as string],
              },
            ].filter((d) => typeof d != "boolean") as TableHeader[],
            rows: inntekter.map((d) => ({
              periodColumn:
                styling == "V2"
                  ? formatPeriode(d.periode!.fom, deductDays(d.periode!.til, 1))
                  : undefined,
              columns: [
                styling == "V1" && {
                  content: formatPeriode(
                    d.periode!.fom,
                    deductDays(d.periode!.til, 1),
                  ),
                },
                { content: formatterBeløp(d.skattepliktigInntekt ?? 0) },
                hasValue(inntektTableRules, InntektTableType.BARNETILLEGG) && {
                  content: formatterBeløp(d.barnetillegg ?? 0),
                },
                hasValue(
                  inntektTableRules,
                  InntektTableType.UTVIDET_BARNETRYGD,
                ) && { content: formatterBeløp(d.utvidetBarnetrygd ?? 0) },
                hasValue(
                  inntektTableRules,
                  InntektTableType.SMÅBARNSTILLEGG,
                ) && { content: formatterBeløp(d.småbarnstillegg ?? 0) },
                hasValue(inntektTableRules, InntektTableType.KONTANTSTØTTE) && {
                  content: formatterBeløp(d.kontantstøtte ?? 0),
                },
                { content: formatterBeløp(d.totalinntekt ?? 0) },
              ].filter((d) => typeof d != "boolean") as TableColumn[],
            })),
          }}
        />
      </>
    );
  }

  function renderForAllChildren() {
    const inntekterBarn = groupBy(data, (d) => d.gjelderBarn?.ident!).sort(
      ([_, a], [d2, b]) => sortByAge(a[0].gjelderBarn, b[0].gjelderBarn),
    );
    return inntekterBarn.map(([key, value], i) => {
      const gjelderBarn = value[0].gjelderBarn!;
      const inntekter = value[0].summertInntektListe;
      const addMargin = i != 0;
      return (
        <div
          key={gjelderBarn + key + i.toString()}
          className="table_container"
          style={{ marginTop: addMargin ? "16px" : "0px" }}
        >
          {renderTable(inntekter, gjelderBarn)}
        </div>
      );
    });
  }

  const harInntekter = data.every((d) => d.summertInntektListe.length > 0);

  if (!harInntekter) return;
  return (
    <>
      <InntektTableTitle title={t.tabeller.beregnetTotalt} witMarginTop />
      {rolle.rolle === Rolletype.BA
        ? renderTable(
            data.find((d) => d.gjelderBarn.ident == rolle.ident)
              ?.summertInntektListe ?? [],
          )
        : renderForAllChildren()}
    </>
  );
}
