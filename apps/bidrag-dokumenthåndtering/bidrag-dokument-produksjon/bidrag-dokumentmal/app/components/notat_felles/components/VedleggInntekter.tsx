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
import tekster from "~/tekster";
import {
  CommonTable,
  TableColumn,
  TableHeader,
} from "~/components/CommonTable";
import { getInntektTableHeaders } from "~/constants/tableHeaders";
import { useNotatFelles } from "~/components/notat_felles/NotatContext";
import Inntektsposter from "~/components/notat_felles/components/Inntektsposter";
import { isHarInntekter } from "~/components/inntektTableHelpers";
import InntektTableTitle from "~/components/inntekt/InntektTableTitle";
import TableGjelderBarn from "~/components/TableGjelderBarn";
import { useTheme } from "~/components/notat_felles/ThemeContext";
import GjelderPerson from "~/components/GjelderPerson";
import { VedleggProps } from "~/types/commonTypes";

export default function VedleggInntekter({ vedleggNummer = 2 }: VedleggProps) {
  const { erAvslag, bidragsmottaker, bidragspliktig, søknadsbarn, type } =
    useNotatFelles();
  if (erAvslag) return null;

  return (
    <div>
      <h2
        id={elementIds.vedleggInntekter}
        className={`${vedleggNummer == 1 ? "break-before-page" : ""}`}
      >
        Vedlegg nr. {vedleggNummer}: Inntekt - {tekster.fraOffentligeRegistre}
      </h2>
      <OpplysningerForRolle
        rolle={bidragsmottaker}
        showRole={type !== NotatMalType.FORSKUDD}
        vedleggNummer={vedleggNummer}
      />
      {type !== NotatMalType.FORSKUDD && bidragspliktig && (
        <div className={"mt-medium"}>
          <OpplysningerForRolle
            rolle={bidragspliktig}
            vedleggNummer={vedleggNummer}
          />
        </div>
      )}
      {type !== NotatMalType.FORSKUDD &&
        søknadsbarn.map((barn) => (
          <div key={barn.ident} className={"mt-medium"}>
            <OpplysningerForRolle rolle={barn} vedleggNummer={vedleggNummer} />
          </div>
        ))}
    </div>
  );
}

function OpplysningerForRolle({
  rolle,
  showRole = true,
  vedleggNummer,
}: {
  rolle: DokumentmalPersonDto;
  showRole?: boolean;
  vedleggNummer?: number;
}) {
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
          <h4>{vedleggNummer}.1 Arbeidsforhold</h4>
          <ArbeidsforholdTable data={inntekter.arbeidsforhold} />
        </div>
      )}
      {!isHarInntekter(inntekter) ? (
        <p>Ingen offentlige inntekter</p>
      ) : (
        <>
          <OffentligeInntekter
            data={inntekter.årsinntekter}
            tittel={`${vedleggNummer}.2 Skattepliktige og pensjonsgivende inntekter`}
            medInntektsposter
          />
          <OffentligeInntekter
            data={inntekter.barnetillegg}
            tittel={`${vedleggNummer}.3 Barnetillegg`}
            medBarn
          />
          <OffentligeInntekter
            data={inntekter.utvidetBarnetrygd}
            tittel={`${vedleggNummer}.4 Utvidet barnetrygd`}
          />
          <OffentligeInntekter
            data={inntekter.småbarnstillegg}
            tittel={`${vedleggNummer}.5 Småbarnstillegg`}
          />
          <OffentligeInntekter
            data={inntekter.kontantstøtte}
            tittel={`${vedleggNummer}.6 Kontantstøtte`}
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
              name: tekster.tabell.felles.beskrivelse,
              width: "175px",
            },
            { name: tekster.tabell.inntekt.beløp },
          ].filter((d) => typeof d != "boolean") as TableHeader[],
          rows: data.map((d, i) => {
            const periode = d.opprinneligPeriode;
            return {
              columns: [
                { content: formatPeriode(periode!.fom, periode!.til) },
                inkluderBeskrivelse && {
                  content: `${d.visningsnavn}${d.historisk ? " (historisk)" : ""}`,
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
          { name: tekster.tabell.felles.periode, width: "100px" },
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
