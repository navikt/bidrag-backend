import { dateToDDMMYYYY, formatPeriode } from "~/utils/date-utils";
import {
  Arbeidsforhold,
  Inntektsrapportering,
  Kilde,
  NotatInntektDto,
  NotatMalType,
  DokumentmalPersonDto,
} from "~/types/Api";
import { formatterBeløp, sammenlignRoller } from "~/utils/visningsnavn";
import { groupBy } from "~/utils/array-utils";
import elementIds from "~/utils/elementIds";
import ord from "~/tekster/ord.yaml";
import tekster from "~/tekster";
import opplysningerTekster from "~/tekster/notat/felles/opplysninger.yaml";
import {
  CommonTable,
  TableColumn,
  TableHeader,
} from "~/components/CommonTable";
import { getInntektTableHeaders } from "~/constants/tableHeaders";
import {
  useNotatFelles,
  useVedleggNummer,
} from "~/components/notat_felles/NotatContext";
import Inntektsposter from "~/components/notat_felles/components/Inntektsposter";
import { isHarInntekter } from "~/components/inntektTableHelpers";
import InntektTableTitle from "~/components/inntekt/InntektTableTitle";
import TableGjelderBarn from "~/components/TableGjelderBarn";
import { useTheme } from "~/components/notat_felles/ThemeContext";
import GjelderPerson from "~/components/GjelderPerson";

const t = opplysningerTekster.inntekter;

export default function VedleggInntekter() {
  const vedleggNummer = useVedleggNummer("inntekter");
  const { erAvslag, bidragsmottaker, bidragspliktig, søknadsbarn, type } =
    useNotatFelles();
  if (erAvslag && type != NotatMalType.BIDRAG) return null;

  return (
    <div>
      <h2
        id={elementIds.vedleggInntekter}
        className={`${vedleggNummer == 1 ? "break-before-page" : ""}`}
      >
        {t.tittel.replace("{}", `${vedleggNummer}`)}
        {tekster.fraOffentligeRegistre}
      </h2>
      <OpplysningerForRolle
        rolle={bidragsmottaker}
        showRole={type !== NotatMalType.FORSKUDD}
      />
      {type !== NotatMalType.FORSKUDD && bidragspliktig && (
        <div className={"mt-medium"}>
          <OpplysningerForRolle rolle={bidragspliktig} />
        </div>
      )}
      {type !== NotatMalType.FORSKUDD &&
        søknadsbarn.map((barn) => (
          <div key={barn.ident} className={"mt-medium"}>
            <OpplysningerForRolle rolle={barn} />
          </div>
        ))}
    </div>
  );
}

function OpplysningerForRolle({
  rolle,
  showRole = true,
}: {
  rolle: DokumentmalPersonDto;
  showRole?: boolean;
}) {
  const vedleggNummer = useVedleggNummer("inntekter");
  const { data } = useNotatFelles();

  const inntekter = data.inntekter.offentligeInntekterPerRolle.find(
    (d) =>
      sammenlignRoller(d.gjelder.rolle, rolle.rolle) &&
      d.gjelder.ident == rolle.ident,
  );
  if (!inntekter) return null;

  return (
    <div>
      {showRole && <GjelderPerson rolle={rolle} />}
      {inntekter.arbeidsforhold && inntekter.arbeidsforhold.length > 0 && (
        <div>
          <h4>{t.tabeller.arbeidsforhold.replace("{}", `${vedleggNummer}`)}</h4>
          <ArbeidsforholdTable data={inntekter.arbeidsforhold} />
        </div>
      )}
      {!isHarInntekter(inntekter) ? (
        <p>{t.ingenOffentligeInntekter}</p>
      ) : (
        <>
          <OffentligeInntekter
            data={inntekter.årsinntekter}
            tittel={t.tabeller.skattepliktigeOgPensjonsgivende.replace(
              "{}",
              `${vedleggNummer}`,
            )}
            medInntektsposter
          />
          <OffentligeInntekter
            data={inntekter.barnetillegg}
            tittel={t.tabeller.barnetillegg.replace("{}", `${vedleggNummer}`)}
            medBarn
          />
          <OffentligeInntekter
            data={inntekter.utvidetBarnetrygd}
            tittel={t.tabeller.utvidetBarnetrygd.replace(
              "{}",
              `${vedleggNummer}`,
            )}
          />
          <OffentligeInntekter
            data={inntekter.småbarnstillegg}
            tittel={t.tabeller.småbarnstillegg.replace(
              "{}",
              `${vedleggNummer}`,
            )}
          />
          <OffentligeInntekter
            data={inntekter.kontantstøtte}
            tittel={t.tabeller.kontantstøtte.replace("{}", `${vedleggNummer}`)}
            medBarn
          />
        </>
      )}
    </div>
  );
}

type OffentligeInntekterProps = {
  tittel: string;
  data: NotatInntektDto[];
  medBarn?: boolean;
  medInntektsposter?: boolean;
  subsection?: boolean;
};

function OffentligeInntekter({
  tittel,
  data,
  medBarn,
  medInntektsposter,
  subsection = true,
}: OffentligeInntekterProps) {
  if (data.length == 0) return null;
  return (
    <div className={subsection ? "subsection" : ""}>
      <InntektTableTitle title={tittel} />
      {medBarn ? (
        <InntektPerBarnTable data={data} />
      ) : (
        <InntektTable data={data} medInntektsposter={medInntektsposter} />
      )}
    </div>
  );
}

type InntektTableProps = {
  data: NotatInntektDto[];
  inkluderBeskrivelse?: boolean;
  medInntektsposter?: boolean;
};

function InntektTable({
  data,
  inkluderBeskrivelse = true,
  medInntektsposter,
}: InntektTableProps) {
  return (
    <div className={"table_container"}>
      <CommonTable
        data={{
          headers: [
            { name: tekster.tabell.felles.fraTilOgMed, width: "170px" },
            inkluderBeskrivelse && {
              name: ord.beskrivelse,
              width: "175px",
            },
            { name: ord.beløp },
          ].filter((d) => typeof d != "boolean") as TableHeader[],
          rows: data.map((d, i) => {
            const periode = d.opprinneligPeriode;
            return {
              columns: [
                { content: formatPeriode(periode!.fom, periode!.til) },
                inkluderBeskrivelse && {
                  content: `${d.visningsnavn}${d.historisk ? t.historisk : ""}`,
                },
                { content: formatterBeløp(d.beløp) },
              ].filter((d) => typeof d != "boolean") as TableColumn[],
              expandableContent:
                medInntektsposter && d.inntektsposter.length > 0
                  ? [
                      {
                        content: (
                          <Inntektsposter
                            data={d}
                            periode={periode}
                            withHorizontalLine={data.length > i + 1}
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

function ArbeidsforholdTable({ data }: { data: Arbeidsforhold[] }) {
  if (data.length === 0) return null;

  return (
    <CommonTable
      width={"450px"}
      data={{
        headers: [
          { name: ord.periode, width: "100px" },
          { name: tekster.tabell.arbeidsforhold.arbeidsgiver, width: "140px" },
          { name: tekster.tabell.arbeidsforhold.stilling, width: "40px" },
          { name: tekster.tabell.arbeidsforhold.lønnsendring, width: "80px" },
        ],
        rows: data.map((d) => ({
          columns: [
            { content: formatPeriode(d.periode.fom, d.periode.til) },
            { content: d.arbeidsgiver },
            {
              content:
                d.stillingProsent != undefined ? d.stillingProsent + "%" : "0%",
            },
            { content: dateToDDMMYYYY(d.lønnsendringDato) },
          ],
        })),
      }}
    />
  );
}
function InntektPerBarnTable({ data }: InntektTableProps) {
  const { styling } = useTheme();
  if (data.length == 0) return null;
  return (
    <div>
      {groupBy(data, (d) => d.gjelderBarn?.ident!).map(([key, value], i) => {
        const gjelderBarn = value[0].gjelderBarn!;
        const erBarnetillegg =
          value[0].type == Inntektsrapportering.BARNETILLEGG;
        const addMargin = data.length > i + 1;
        return (
          <div
            key={key + i.toString()}
            className="table_container"
            style={{
              marginBottom: addMargin ? "16px" : "0px",
            }}
          >
            <TableGjelderBarn gjelderBarn={gjelderBarn} />
            <CommonTable
              width={"580px"}
              data={{
                headers: getInntektTableHeaders(erBarnetillegg, false, styling),
                rows: data
                  .filter((d) => d.kilde == Kilde.OFFENTLIG)
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
                        erBarnetillegg
                          ? [
                              { content: visningsnavnInntektstype },
                              {
                                content: formatterBeløp(d.månedsbeløp ?? 0),
                              },
                              { content: formatterBeløp(d.beløp) },
                            ]
                          : { content: formatterBeløp(d.beløp) },
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
