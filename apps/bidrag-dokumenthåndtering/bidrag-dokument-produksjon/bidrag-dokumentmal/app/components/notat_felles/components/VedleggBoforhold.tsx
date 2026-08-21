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
import { groupBy } from "~/utils/array-utils";
import DataDescription from "~/components/DataDescription";
import Person from "~/components/Person";
import elementIds from "~/utils/elementIds";
import tekster from "~/tekster";
import { CommonTable } from "~/components/CommonTable";
import { useNotatFelles } from "~/components/notat_felles/NotatContext";
import { VedleggProps } from "~/types/commonTypes";

export default function VedleggBoforhold({ vedleggNummer = 1 }: VedleggProps) {
  const { erAvslag, data, type } = useNotatFelles();
  if (erAvslag) return null;
  return (
    <div className={`${vedleggNummer == 1 ? "break-before-page" : ""}`}>
      <h2 id={elementIds.vedleggBoforhold}>
        Vedlegg nr. {vedleggNummer}: Boforhold - {tekster.fraOffentligeRegistre}
      </h2>
      {groupBy(
        data.boforhold.barn
          .filter((d) => d.opplysningerFraFolkeregisteret.length > 0)
          .sort((d) => (d.medIBehandling ? -1 : 1)),
        (d) => d.gjelder.ident ?? d.gjelder.fødselsdato!,
      ).map(([key, value]) => {
        const gjelderBarn = value[0].gjelder!;
        const barn = value[0];
        return (
          <div key={key} className="table_container mb-medium">
            <DataDescription
              style={{ marginBottom: "0px" }}
              label={
                barn.medIBehandling ? "Søknadsbarn" : "Eget barn i husstanden"
              }
              value={
                <Person
                  fødselsdato={gjelderBarn.fødselsdato!}
                  navn={gjelderBarn.navn!}
                  erBeskyttet={gjelderBarn.erBeskyttet}
                />
              }
            />
            <BoforholdTable data={barn.opplysningerFraFolkeregisteret} />
          </div>
        );
      })}
      {type !== NotatMalType.FORSKUDD &&
        data.boforhold.andreVoksneIHusstanden
          ?.opplysningerFraFolkeregisteret && (
          <div>
            <h3>Andre voksne i husstanden</h3>
            <div className="table_container">
              <BoforholdTable
                data={
                  data.boforhold.andreVoksneIHusstanden
                    .opplysningerFraFolkeregisteret
                }
              />
            </div>
          </div>
        )}
      {type !== NotatMalType.FORSKUDD &&
        data.boforhold.boforholdBMSøknadsbarn &&
        data.boforhold.boforholdBMSøknadsbarn.length > 0 && (
          <div>
            <h3>Boforhold bidragsmottaker</h3>
            {groupBy(
              data.boforhold.boforholdBMSøknadsbarn.sort((a, b) =>
                a.gjelderBarn.ident!.localeCompare(b.gjelderBarn.ident!),
              ),
              (d) => d.gjelderBarn.ident ?? d.gjelderBarn.fødselsdato!,
            ).map(([key, value]) => {
              const gjelderBarn = value[0].gjelderBarn!;
              const perioder = value[0].perioder;
              return (
                <div key={key} className="table_container mb-medium">
                  <DataDescription
                    style={{ marginBottom: "0px" }}
                    label={"Søknadsbarn"}
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
        )}
      {type == NotatMalType.FORSKUDD && (
        <div>
          <h3>Sivilstand</h3>
          <div className="table_container">
            <BoforholdTable
              data={data.boforhold.sivilstand.opplysningerFraFolkeregisteret}
            />
          </div>
        </div>
      )}
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

  const erSivilstand = isSivilstand(data);
  return (
    <CommonTable
      width={"400px"}
      data={{
        headers: [
          {
            name: erSivilstand
              ? tekster.tabell.felles.fraDato
              : tekster.tabell.felles.periode,
            width: "140px",
          },
          { name: tekster.tabell.felles.status },
        ],
        rows: data.map((d) => ({
          columns: [
            { content: formatPeriode(d.periode.fom, d.periode.til) },
            {
              content:
                isAndreVoksneIHusstanden(d) &&
                d.status == Bostatuskode.BOR_MED_ANDRE_VOKSNE
                  ? `${d.statusVisningsnavn} (${d.detaljer?.totalAntallHusstandsmedlemmer})`
                  : d.statusVisningsnavn,
            },
          ],
          expandableContent:
            isAndreVoksneIHusstanden(d) &&
            d.status == Bostatuskode.BOR_MED_ANDRE_VOKSNE
              ? [
                  {
                    content: (
                      <AndreVoksneiHusstandenDetaljer
                        andreVoksneIHusstandenDetaljer={d.detaljer!}
                      />
                    ),
                  },
                ]
              : undefined,
        })),
      }}
    />
  );
}

function AndreVoksneiHusstandenDetaljer({
  andreVoksneIHusstandenDetaljer,
}: {
  andreVoksneIHusstandenDetaljer: NotatAndreVoksneIHusstandenDetaljerDto;
}) {
  return (
    <>
      <h4 style={{ width: "200px" }}>Hvem bor på adresse?</h4>
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
                  " (Relasjon til Bidragspliktig)"}
              </li>
            );
          },
        )}
      </ul>
    </>
  );
}
function isSivilstand(
  data:
    | OpplysningerFraFolkeregisteretMedDetaljerBostatuskodeUnit[]
    | OpplysningerFraFolkeregisteretMedDetaljerBostatuskodeNotatAndreVoksneIHusstandenDetaljerDto[]
    | OpplysningerFraFolkeregisteretMedDetaljerSivilstandskodePDLUnit[],
): data is OpplysningerFraFolkeregisteretMedDetaljerSivilstandskodePDLUnit[] {
  return data && data.length > 0 && data[0].periode !== undefined;
}
function isAndreVoksneIHusstandenListe(
  data: (
    | OpplysningerFraFolkeregisteretMedDetaljerBostatuskodeUnit
    | OpplysningerFraFolkeregisteretMedDetaljerBostatuskodeNotatAndreVoksneIHusstandenDetaljerDto
    | OpplysningerFraFolkeregisteretMedDetaljerSivilstandskodePDLUnit
  )[],
): data is OpplysningerFraFolkeregisteretMedDetaljerBostatuskodeNotatAndreVoksneIHusstandenDetaljerDto[] {
  return (
    data &&
    data.length > 0 &&
    [
      Bostatuskode.BOR_IKKE_MED_ANDRE_VOKSNE,
      Bostatuskode.BOR_MED_ANDRE_VOKSNE,
    ].includes(data[0].status as Bostatuskode)
  );
}

function isAndreVoksneIHusstanden(
  data:
    | OpplysningerFraFolkeregisteretMedDetaljerBostatuskodeUnit
    | OpplysningerFraFolkeregisteretMedDetaljerBostatuskodeNotatAndreVoksneIHusstandenDetaljerDto
    | OpplysningerFraFolkeregisteretMedDetaljerSivilstandskodePDLUnit,
): data is OpplysningerFraFolkeregisteretMedDetaljerBostatuskodeNotatAndreVoksneIHusstandenDetaljerDto {
  return isAndreVoksneIHusstandenListe([data]);
}
