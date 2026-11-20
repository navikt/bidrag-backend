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
import { Fragment } from "react";
import {
  DokumentTabell,
  type Kolonne,
  Tabellcelle,
  Tabelldetaljer,
  Tabellrad,
} from "~/components/DokumentTabell";
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
      <DokumentTabell
        kolonner={
          [
            { tittel: tekster.tabell.felles.fraTilOgMed, bredde: "170px" },
            inkluderBeskrivelse && {
              tittel: ord.beskrivelse,
              bredde: "230px",
            },
            {
              tittel: tekster.tabell.felles.kilde,
              bredde: inkluderBeskrivelse ? "80px" : "230px",
            },
            { tittel: ord.beløp },
          ].filter((d) => typeof d != "boolean") as Kolonne[]
        }
      >
        {inntekter
          .sort((a, b) =>
            a.periode?.fom && b.periode?.fom
              ? a.periode?.fom.localeCompare(b.periode?.fom)
              : 1,
          )
          .map((d, i) => {
            const periode = d.periode ?? d.opprinneligPeriode;
            return (
              <Fragment key={i}>
                <Tabellrad index={i}>
                  <Tabellcelle>
                    {formatPeriode(periode!.fom, periode!.til)}
                  </Tabellcelle>
                  {inkluderBeskrivelse && (
                    <Tabellcelle>
                      {`${d.visningsnavn}${d.historisk ? t.historisk : ""}`}
                    </Tabellcelle>
                  )}
                  <Tabellcelle>
                    <KildeIcon kilde={d.kilde} />
                  </Tabellcelle>
                  <Tabellcelle>{formatterBeløp(d.beløp)}</Tabellcelle>
                </Tabellrad>
                {d.inntektsposter.length > 0 && (
                  <Tabelldetaljer index={i}>
                    <Inntektsposter
                      data={d}
                      periode={periode}
                      withHorizontalLine={inntekter.length > i + 1}
                    />
                  </Tabelldetaljer>
                )}
              </Fragment>
            );
          })}
      </DokumentTabell>
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
              <DokumentTabell
                bredde="580px"
                kolonner={getInntektTableHeaders(
                  erBarnetillegg,
                  erBidrag,
                  true,
                  styling,
                )}
              >
                {value
                  .filter((d) => !bareMedIBeregning || d.medIBeregning)
                  .map((d, index) => {
                    const periode = d.periode ?? d.opprinneligPeriode;
                    const visningsnavnInntektstype =
                      d.inntektsposter.length > 0
                        ? d.inntektsposter[0].visningsnavn
                        : "";
                    const periodColumn =
                      styling == "V2" && erBarnetillegg
                        ? formatPeriode(periode!.fom, periode!.til)
                        : undefined;

                    return (
                      <Fragment key={index}>
                        {periodColumn && (
                          <Tabellrad index={1} periode>
                            <Tabellcelle fullSpan>{periodColumn}</Tabellcelle>
                          </Tabellrad>
                        )}
                        <Tabellrad index={index} harPeriode={!!periodColumn}>
                          {(styling == "V1" || !erBarnetillegg) && (
                            <Tabellcelle>
                              {formatPeriode(periode!.fom, periode!.til)}
                            </Tabellcelle>
                          )}
                          <Tabellcelle>
                            <KildeIcon kilde={d.kilde} />
                          </Tabellcelle>
                          {erBarnetillegg ? (
                            <>
                              <Tabellcelle>
                                {visningsnavnInntektstype}
                              </Tabellcelle>
                              <Tabellcelle>
                                {Number(d.beløpMånedDagsats)}
                              </Tabellcelle>
                              <Tabellcelle>
                                {d.beløpstypeVisningsnavn}
                              </Tabellcelle>
                              {erBidrag && (
                                <Tabellcelle>
                                  {d.skattefaktor == null
                                    ? ord.ikkeRelevant
                                    : formatterProsent(d.skattefaktor)}
                                </Tabellcelle>
                              )}
                              <Tabellcelle>
                                {formatterBeløp(d.beløp)}
                              </Tabellcelle>
                            </>
                          ) : (
                            <Tabellcelle>{formatterBeløp(d.beløp)}</Tabellcelle>
                          )}
                        </Tabellrad>
                      </Fragment>
                    );
                  })}
              </DokumentTabell>
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
        <DokumentTabell
          bredde="580px"
          kolonner={
            [
              styling == "V1" && {
                tittel: tekster.tabell.felles.fraTilOgMed,
                bredde: columnWidth[tekster.tabell.felles.fraTilOgMed],
              },
              {
                tittel: columnNames.SKATTEPLIKTIG,
                bredde: columnWidth[columnNames.SKATTEPLIKTIG as string],
              },
              hasValue(inntektTableRules, InntektTableType.BARNETILLEGG) && {
                tittel: columnNames.BARNETILLEGG,
                bredde: columnWidth[columnNames.BARNETILLEGG as string],
              },
              hasValue(
                inntektTableRules,
                InntektTableType.UTVIDET_BARNETRYGD,
              ) && {
                tittel: columnNames.UTVIDET_BARNETRYGD,
                bredde: columnWidth[columnNames.UTVIDET_BARNETRYGD as string],
              },
              hasValue(inntektTableRules, InntektTableType.SMÅBARNSTILLEGG) && {
                tittel: columnNames.SMÅBARNSTILLEGG,
                bredde: columnWidth[columnNames.SMÅBARNSTILLEGG as string],
              },
              hasValue(inntektTableRules, InntektTableType.KONTANTSTØTTE) && {
                tittel: columnNames.KONTANTSTØTTE,
                bredde: columnWidth[columnNames.KONTANTSTØTTE as string],
              },
              {
                tittel: columnNames.TOTAL_INNTEKTER,
                bredde: columnWidth[columnNames.TOTAL_INNTEKTER as string],
              },
            ].filter((d) => typeof d != "boolean") as Kolonne[]
          }
        >
          {inntekter.map((d, index) => {
            const periode = formatPeriode(
              d.periode!.fom,
              deductDays(d.periode!.til, 1),
            );
            return (
              <Fragment key={index}>
                {styling == "V2" && (
                  <Tabellrad index={1} periode>
                    <Tabellcelle fullSpan>{periode}</Tabellcelle>
                  </Tabellrad>
                )}
                <Tabellrad index={index} harPeriode={styling == "V2"}>
                  {styling == "V1" && <Tabellcelle>{periode}</Tabellcelle>}
                  <Tabellcelle>
                    {formatterBeløp(d.skattepliktigInntekt ?? 0)}
                  </Tabellcelle>
                  {hasValue(
                    inntektTableRules,
                    InntektTableType.BARNETILLEGG,
                  ) && (
                    <Tabellcelle>
                      {formatterBeløp(d.barnetillegg ?? 0)}
                    </Tabellcelle>
                  )}
                  {hasValue(
                    inntektTableRules,
                    InntektTableType.UTVIDET_BARNETRYGD,
                  ) && (
                    <Tabellcelle>
                      {formatterBeløp(d.utvidetBarnetrygd ?? 0)}
                    </Tabellcelle>
                  )}
                  {hasValue(
                    inntektTableRules,
                    InntektTableType.SMÅBARNSTILLEGG,
                  ) && (
                    <Tabellcelle>
                      {formatterBeløp(d.småbarnstillegg ?? 0)}
                    </Tabellcelle>
                  )}
                  {hasValue(
                    inntektTableRules,
                    InntektTableType.KONTANTSTØTTE,
                  ) && (
                    <Tabellcelle>
                      {formatterBeløp(d.kontantstøtte ?? 0)}
                    </Tabellcelle>
                  )}
                  <Tabellcelle>
                    {formatterBeløp(d.totalinntekt ?? 0)}
                  </Tabellcelle>
                </Tabellrad>
              </Fragment>
            );
          })}
        </DokumentTabell>
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
