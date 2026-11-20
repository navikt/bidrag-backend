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
import { Fragment } from "react";
import {
  DokumentTabell,
  type Kolonne,
  Tabellcelle,
  Tabelldetaljer,
  Tabellrad,
} from "~/components/DokumentTabell";
import { getInntektTableHeaders } from "~/constants/tableHeaders";
import {
  useNotatFelles,
  useVedleggNummer,
} from "~/components/notat_felles/NotatContext";
import Inntektsposter from "~/routes/notat/felles/Inntektsposter";
import { isHarInntekter } from "~/components/inntektTableHelpers";
import InntektTableTitle from "~/components/inntekt/InntektTableTitle";
import TableGjelderBarn from "~/components/TableGjelderBarn";
import { useTheme } from "~/components/notat_felles/ThemeContext";
import GjelderPerson from "~/components/GjelderPerson";
import { tekst } from "~/utils/tekst";
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
        {tekst(
          "notat.opplysninger.felles.inntekter.tittel",
          "Vedlegg nr. {}: Inntekt - ",
        ).replace("{}", `${vedleggNummer}`)}
        {tekst(
          "felles.fraOffentligeRegistre",
          "Hentet fra offentlige registre",
        )}
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
          <h4>
            {tekst(
              "notat.opplysninger.felles.inntekter.tabeller.arbeidsforhold",
              "{}.1 Arbeidsforhold",
            ).replace("{}", `${vedleggNummer}`)}
          </h4>
          <ArbeidsforholdTable data={inntekter.arbeidsforhold} />
        </div>
      )}
      {!isHarInntekter(inntekter) ? (
        <p>
          {tekst(
            "notat.opplysninger.felles.inntekter.ingenOffentligeInntekter",
            "Ingen offentlige inntekter",
          )}
        </p>
      ) : (
        <>
          <OffentligeInntekter
            data={inntekter.årsinntekter}
            tittel={tekst(
              "notat.opplysninger.felles.inntekter.tabeller.skattepliktigeOgPensjonsgivende",
              "{}.2 Skattepliktige og pensjonsgivende inntekter",
            ).replace("{}", `${vedleggNummer}`)}
            medInntektsposter
          />
          <OffentligeInntekter
            data={inntekter.barnetillegg}
            tittel={tekst(
              "notat.opplysninger.felles.inntekter.tabeller.barnetillegg",
              "{}.3 Barnetillegg",
            ).replace("{}", `${vedleggNummer}`)}
            medBarn
          />
          <OffentligeInntekter
            data={inntekter.utvidetBarnetrygd}
            tittel={tekst(
              "notat.opplysninger.felles.inntekter.tabeller.utvidetBarnetrygd",
              "{}.4 Utvidet barnetrygd",
            ).replace("{}", `${vedleggNummer}`)}
          />
          <OffentligeInntekter
            data={inntekter.småbarnstillegg}
            tittel={tekst(
              "notat.opplysninger.felles.inntekter.tabeller.småbarnstillegg",
              "{}.5 Småbarnstillegg",
            ).replace("{}", `${vedleggNummer}`)}
          />
          <OffentligeInntekter
            data={inntekter.kontantstøtte}
            tittel={tekst(
              "notat.opplysninger.felles.inntekter.tabeller.kontantstøtte",
              "{}.6 Kontantstøtte",
            ).replace("{}", `${vedleggNummer}`)}
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
      <DokumentTabell
        kolonner={
          [
            {
              tittel: tekst(
                "felles.tabell.felles.fraTilOgMed",
                "Fra og med - Til og med",
              ),
              bredde: "170px",
            },
            inkluderBeskrivelse && {
              tittel: tekst("ord.beskrivelse", "Beskrivelse"),
              bredde: "175px",
            },
            { tittel: tekst("ord.beløp", "Beløp") },
          ].filter((d) => typeof d != "boolean") as Kolonne[]
        }
      >
        {data.map((d, index) => {
          const periode = d.opprinneligPeriode;
          return (
            <Fragment key={index}>
              <Tabellrad index={index}>
                <Tabellcelle>
                  {formatPeriode(periode!.fom, periode!.til)}
                </Tabellcelle>
                {inkluderBeskrivelse && (
                  <Tabellcelle>
                    {`${d.visningsnavn}${d.historisk ? tekst("notat.opplysninger.felles.inntekter.historisk", " (historisk)") : ""}`}
                  </Tabellcelle>
                )}
                <Tabellcelle>{formatterBeløp(d.beløp)}</Tabellcelle>
              </Tabellrad>
              {medInntektsposter && d.inntektsposter.length > 0 && (
                <Tabelldetaljer index={index}>
                  <Inntektsposter
                    data={d}
                    periode={periode}
                    withHorizontalLine={data.length > index + 1}
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
function ArbeidsforholdTable({ data }: { data: Arbeidsforhold[] }) {
  if (data.length === 0) return null;
  return (
    <DokumentTabell
      bredde="450px"
      kolonner={[
        { tittel: tekst("ord.periode", "Periode"), bredde: "100px" },
        {
          tittel: tekst(
            "felles.tabell.arbeidsforhold.arbeidsgiver",
            "Arbeidsgiver",
          ),
          bredde: "140px",
        },
        {
          tittel: tekst("felles.tabell.arbeidsforhold.stilling", "Stilling"),
          bredde: "40px",
        },
        {
          tittel: tekst(
            "felles.tabell.arbeidsforhold.lønnsendring",
            "Lønnsendring",
          ),
          bredde: "80px",
        },
      ]}
    >
      {data.map((d, index) => (
        <Tabellrad key={index} index={index}>
          <Tabellcelle>
            {formatPeriode(d.periode.fom, d.periode.til)}
          </Tabellcelle>
          <Tabellcelle>{d.arbeidsgiver}</Tabellcelle>
          <Tabellcelle>
            {d.stillingProsent != undefined ? d.stillingProsent + "%" : "0%"}
          </Tabellcelle>
          <Tabellcelle>{dateToDDMMYYYY(d.lønnsendringDato)}</Tabellcelle>
        </Tabellrad>
      ))}
    </DokumentTabell>
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
            <DokumentTabell
              bredde="580px"
              kolonner={getInntektTableHeaders(
                erBarnetillegg,
                false,
                true,
                "V2",
              )}
            >
              {data
                .filter((d) => d.kilde == Kilde.OFFENTLIG)
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
                        {erBarnetillegg ? (
                          <>
                            <Tabellcelle>
                              {visningsnavnInntektstype}
                            </Tabellcelle>
                            <Tabellcelle>
                              {formatterBeløp(d.månedsbeløp ?? 0)}
                            </Tabellcelle>
                            <Tabellcelle>{formatterBeløp(d.beløp)}</Tabellcelle>
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
