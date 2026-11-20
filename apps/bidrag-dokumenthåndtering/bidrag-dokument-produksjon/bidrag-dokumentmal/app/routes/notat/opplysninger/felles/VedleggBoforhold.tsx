import {
  dateOrNull,
  DateToDDMMYYYYString,
  formatPeriode,
} from "~/utils/date-utils";
import {
  Bostatuskode,
  NotatMalType,
  OpplysningerFraFolkeregisteretMedDetaljerBostatuskodeUnit,
  OpplysningerFraFolkeregisteretMedDetaljerSivilstandskodePDLUnit,
  NotatAndreVoksneIHusstandenDetaljerDto,
  OpplysningerFraFolkeregisteretMedDetaljerBostatuskodeNotatAndreVoksneIHusstandenDetaljerDto,
} from "~/types/Api";
import { Fragment } from "react";
import { groupBy } from "~/utils/array-utils";
import DataDescription from "~/components/DataDescription";
import Person from "~/components/Person";
import elementIds from "~/utils/elementIds";
import {
  DokumentTabell,
  Tabellcelle,
  Tabelldetaljer,
  Tabellrad,
} from "~/components/DokumentTabell";
import {
  useNotatFelles,
  useVedleggNummer,
} from "~/components/notat_felles/NotatContext";
import { tekst } from "~/utils/tekst";
export default function VedleggBoforhold() {
  const vedleggNummer = useVedleggNummer("boforhold");
  const { erAvslag, type } = useNotatFelles();
  if (erAvslag) return null;
  return (
    <div className={`${vedleggNummer == 1 ? "break-before-page" : ""}`}>
      <h2 id={elementIds.vedleggBoforhold}>
        {tekst(
          "notat.opplysninger.felles.boforhold.tittel",
          "Vedlegg nr. {nummer}: Boforhold - {kilde}",
        )
          .replace("{nummer}", vedleggNummer.toString())
          .replace(
            "{kilde}",
            tekst(
              "felles.fraOffentligeRegistre",
              "Hentet fra offentlige registre",
            ),
          )}
      </h2>
      <BarnFraFolkeregisteret />
      {type === NotatMalType.FORSKUDD ? (
        <SivilstandFraFolkeregisteret />
      ) : (
        <>
          <AndreVoksneFraFolkeregisteret />
          <BidragsmottakersBoforhold />
        </>
      )}
    </div>
  );
}
function BarnFraFolkeregisteret() {
  const { data } = useNotatFelles();
  const barnMedOpplysninger = data.boforhold.barn
    .filter((barn) => barn.opplysningerFraFolkeregisteret.length > 0)
    .sort((barn) => (barn.medIBehandling ? -1 : 1));
  return groupBy(
    barnMedOpplysninger,
    (barn) => barn.gjelder.ident ?? barn.gjelder.fødselsdato!,
  ).map(([key, barn]) => {
    const opplysninger = barn[0];
    const person = opplysninger.gjelder;
    return (
      <div key={key} className="table_container mb-medium">
        <DataDescription
          style={{ marginBottom: "0px" }}
          label={
            opplysninger.medIBehandling
              ? tekst(
                  "notat.opplysninger.felles.boforhold.søknadsbarn",
                  "Søknadsbarn",
                )
              : tekst(
                  "notat.opplysninger.felles.boforhold.egetBarnIHusstanden",
                  "Eget barn i husstanden",
                )
          }
          value={
            <Person
              fødselsdato={person.fødselsdato!}
              navn={person.navn!}
              erBeskyttet={person.erBeskyttet}
            />
          }
        />
        <BoforholdTable data={opplysninger.opplysningerFraFolkeregisteret} />
      </div>
    );
  });
}
function AndreVoksneFraFolkeregisteret() {
  const { data } = useNotatFelles();
  const opplysninger =
    data.boforhold.andreVoksneIHusstanden?.opplysningerFraFolkeregisteret;
  if (!opplysninger) return null;
  return (
    <div>
      <h3>
        {tekst(
          "notat.opplysninger.felles.boforhold.andreVoksneIHusstanden",
          "Andre voksne i husstanden",
        )}
      </h3>
      <div className="table_container">
        <BoforholdTable data={opplysninger} />
      </div>
    </div>
  );
}
function BidragsmottakersBoforhold() {
  const { data } = useNotatFelles();
  const boforhold = data.boforhold.boforholdBMSøknadsbarn;
  if (!boforhold?.length) return null;
  return (
    <div>
      <h3>
        {tekst(
          "notat.opplysninger.felles.boforhold.boforholdBidragsmottaker",
          "Boforhold bidragsmottaker",
        )}
      </h3>
      {groupBy(
        boforhold.sort((a, b) =>
          a.gjelderBarn.ident!.localeCompare(b.gjelderBarn.ident!),
        ),
        (barn) => barn.gjelderBarn.ident ?? barn.gjelderBarn.fødselsdato!,
      ).map(([key, barn]) => {
        const { gjelderBarn, perioder } = barn[0];
        return (
          <div key={key} className="table_container mb-medium">
            <DataDescription
              style={{ marginBottom: "0px" }}
              label={tekst(
                "notat.opplysninger.felles.boforhold.søknadsbarn",
                "Søknadsbarn",
              )}
              value={
                <Person
                  fødselsdato={gjelderBarn.fødselsdato!}
                  navn={gjelderBarn.navn!}
                  erBeskyttet={gjelderBarn.erBeskyttet}
                />
              }
            />
            <BoforholdTable data={perioder} />
          </div>
        );
      })}
    </div>
  );
}
function SivilstandFraFolkeregisteret() {
  const { data } = useNotatFelles();
  return (
    <div>
      <h3>
        {tekst("notat.opplysninger.felles.boforhold.sivilstand", "Sivilstand")}
      </h3>
      <div className="table_container">
        <BoforholdTable
          data={data.boforhold.sivilstand.opplysningerFraFolkeregisteret}
        />
      </div>
    </div>
  );
}
export function BoforholdTable({
  data,
}: {
  data:
    | OpplysningerFraFolkeregisteretMedDetaljerBostatuskodeUnit[]
    | OpplysningerFraFolkeregisteretMedDetaljerBostatuskodeNotatAndreVoksneIHusstandenDetaljerDto[]
    | OpplysningerFraFolkeregisteretMedDetaljerSivilstandskodePDLUnit[];
}) {
  if (data.length === 0) return null;
  const periodeoverskrift =
    data[0].periode !== undefined
      ? tekst("felles.tabell.felles.fraDato", "Fra dato")
      : tekst("ord.periode", "Periode");
  return (
    <DokumentTabell
      bredde="400px"
      kolonner={[
        {
          tittel: periodeoverskrift,
          bredde: "140px",
        },
        { tittel: tekst("felles.tabell.felles.status", "Status") },
      ]}
    >
      {data.map((opplysning, index) => {
        const borMedAndreVoksne =
          isAndreVoksneIHusstanden(opplysning) &&
          opplysning.status === Bostatuskode.BOR_MED_ANDRE_VOKSNE;
        return (
          <Fragment key={index}>
            <Tabellrad index={index}>
              <Tabellcelle>
                {formatPeriode(opplysning.periode.fom, opplysning.periode.til)}
              </Tabellcelle>
              <Tabellcelle>
                {borMedAndreVoksne
                  ? `${opplysning.statusVisningsnavn} (${opplysning.detaljer?.totalAntallHusstandsmedlemmer})`
                  : opplysning.statusVisningsnavn}
              </Tabellcelle>
            </Tabellrad>
            {borMedAndreVoksne && (
              <Tabelldetaljer index={index}>
                <AndreVoksneiHusstandenDetaljer
                  andreVoksneIHusstandenDetaljer={opplysning.detaljer!}
                />
              </Tabelldetaljer>
            )}
          </Fragment>
        );
      })}
    </DokumentTabell>
  );
}
function AndreVoksneiHusstandenDetaljer({
  andreVoksneIHusstandenDetaljer,
}: {
  andreVoksneIHusstandenDetaljer: NotatAndreVoksneIHusstandenDetaljerDto;
}) {
  return (
    <>
      <h4 style={{ width: "200px" }}>
        {tekst(
          "notat.opplysninger.felles.boforhold.hvemBorPåAdresse",
          "Hvem bor på adresse?",
        )}
      </h4>
      <ul
        style={{
          listStyleType: "decimal",
          width: "300px",
          marginTop: "-2px",
          marginLeft: "-10px",
        }}
      >
        {andreVoksneIHusstandenDetaljer.husstandsmedlemmer.map(
          (husstandsmedlem, index) => {
            return (
              <li key={husstandsmedlem.navn + "-" + index}>
                {husstandsmedlem.erBeskyttet
                  ? husstandsmedlem.navn
                  : DateToDDMMYYYYString(
                      dateOrNull(husstandsmedlem.fødselsdato)!,
                    )}
                {husstandsmedlem.harRelasjonTilBp &&
                  tekst(
                    "notat.opplysninger.felles.boforhold.relasjonTilBidragspliktig",
                    " (Relasjon til Bidragspliktig)",
                  )}
              </li>
            );
          },
        )}
      </ul>
    </>
  );
}
function isAndreVoksneIHusstanden(
  data:
    | OpplysningerFraFolkeregisteretMedDetaljerBostatuskodeUnit
    | OpplysningerFraFolkeregisteretMedDetaljerBostatuskodeNotatAndreVoksneIHusstandenDetaljerDto
    | OpplysningerFraFolkeregisteretMedDetaljerSivilstandskodePDLUnit,
): data is OpplysningerFraFolkeregisteretMedDetaljerBostatuskodeNotatAndreVoksneIHusstandenDetaljerDto {
  return (
    data.status === Bostatuskode.BOR_IKKE_MED_ANDRE_VOKSNE ||
    data.status === Bostatuskode.BOR_MED_ANDRE_VOKSNE
  );
}
