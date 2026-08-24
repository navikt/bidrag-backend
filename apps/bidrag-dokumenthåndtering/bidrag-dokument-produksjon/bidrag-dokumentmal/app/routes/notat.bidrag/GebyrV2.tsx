import { useNotatFelles } from "~/components/notat_felles/NotatContext";
import { rolleTilVisningsnavn } from "~/utils/visningsnavn";
import { DataViewTable, DataViewTableData } from "~/components/DataViewTable";
import { CommonTable } from "~/components/CommonTable";
import { konverterRolletype } from "~/utils/converter-utils";
import { Rolletype } from "~/types/Api";
import { PersonV2 } from "~/components/Person";
import HorizontalLine from "~/components/HorizontalLine";
import { dateToDDMMYYYY } from "~/utils/date-utils";

export default function GebyrV2() {
  const { data, erAvslag } = useNotatFelles();
  const gebyr = data.gebyrV2;
  if (!gebyr || gebyr.gebyrRoller.length == 0) return null;
  return (
    <div>
      <div className={"elements_inline section-title"}>
        <h2>Gebyr</h2>
      </div>
      <>
        {gebyr.gebyrRoller
          .sort((d) =>
            konverterRolletype(d.rolle.rolle) == Rolletype.BP ? 1 : -1,
          )
          .map((gebyRolle, i) => (
            <div
              key={gebyRolle.rolle.ident! + i}
              className={gebyr.gebyrRoller.length - 1 == i ? "" : "mb-2"}
            >
              <DataViewTable
                className={"mb-1 mt-2"}
                data={
                  [
                    {
                      label: rolleTilVisningsnavn(gebyRolle.rolle.rolle!),
                      labelBold: true,
                      value: <PersonV2 {...gebyRolle.rolle} />,
                    },
                  ].filter((d) => d != null) as DataViewTableData[]
                }
              />
              {Array.from(
                gebyRolle.gebyrDetaljer
                  .reduce((grouped, gebyrDetaljer) => {
                    const saksnummer =
                      gebyrDetaljer.søknad?.saksnummer || "Unknown";
                    if (!grouped.has(saksnummer)) {
                      grouped.set(saksnummer, []);
                    }
                    grouped.get(saksnummer)!.push(gebyrDetaljer);
                    return grouped;
                  }, new Map<string, typeof gebyRolle.gebyrDetaljer>())
                  .entries(),
              ).map(([saksnummer, gebyrDetaljerListe], index) => (
                <div
                  key={`${index} ${gebyRolle.rolle.ident}`}
                  className={index > 0 ? "mt-2" : ""}
                >
                  {gebyrDetaljerListe[0].søknad &&
                    gebyRolle.gebyrDetaljer.length > 1 && (
                      <DataViewTable
                        gap={"5px"}
                        data={
                          [
                            {
                              label: "Sak",
                              textRight: false,
                              value: `${gebyrDetaljerListe[0].søknad.saksnummer}`,
                            },
                          ].filter((d) => d != null) as DataViewTableData[]
                        }
                      />
                    )}
                  <CommonTable
                    layoutAuto
                    data={{
                      headers: [
                        {
                          name: "Beskrivelse",
                        },
                        {
                          name: "Beløp",
                        },
                      ],
                      rows: [
                        {
                          columns: [
                            {
                              content: erAvslag
                                ? "Inntekt siste 12 måneder"
                                : "Skattepliktig inntekt",
                            },
                            {
                              content:
                                gebyrDetaljerListe[0].inntekt
                                  .skattepliktigInntekt,
                            },
                          ],
                        },
                        !erAvslag && {
                          columns: [
                            {
                              content: "Høyeste barnetillegg",
                            },
                            {
                              content:
                                gebyrDetaljerListe[0].inntekt
                                  .maksBarnetillegg ?? 0,
                            },
                          ],
                        },
                        !erAvslag && {
                          columns: [
                            {
                              labelBold: true,
                              content: "Total",
                            },
                            {
                              content:
                                gebyrDetaljerListe[0].inntekt.totalInntekt,
                            },
                          ],
                        },
                      ].filter((d) => d != false),
                    }}
                  />
                  {gebyrDetaljerListe.map((gebyrDetaljer) => (
                    <>
                      {gebyrDetaljer.søknad &&
                      gebyRolle.gebyrDetaljer.length > 1 ? (
                        <DataViewTable
                          gap={"5px"}
                          className={"mt-4"}
                          data={
                            [
                              {
                                label: "Søkt fra dato",
                                textRight: false,
                                value: `${dateToDDMMYYYY(gebyrDetaljer.søknad.søknadFomDato)}`,
                              },
                              {
                                label: "Mottatt dato",
                                textRight: false,
                                value: `${dateToDDMMYYYY(gebyrDetaljer.søknad.mottattDato)}`,
                              },
                              {
                                label: "Søknad fra",
                                textRight: false,
                                value: `${gebyrDetaljer.søknad.søktAvTypeVisningsnavn}`,
                              },
                              {
                                label: "Gebyr",
                                value: gebyrDetaljer.gebyrResultatVisningsnavn,
                              },
                              gebyrDetaljer.erManueltOverstyrt && {
                                label: "Begrunnelse",
                                value: gebyrDetaljer.begrunnelse,
                              },
                            ].filter((d) => d != null) as DataViewTableData[]
                          }
                        />
                      ) : (
                        <DataViewTable
                          className={"mt-4"}
                          data={
                            [
                              {
                                label: "Gebyr",
                                value: gebyrDetaljer.gebyrResultatVisningsnavn,
                              },
                              gebyrDetaljer.erManueltOverstyrt && {
                                label: "Begrunnelse",
                                value: gebyrDetaljer.begrunnelse,
                              },
                            ].filter((d) => d) as DataViewTableData[]
                          }
                        />
                      )}
                    </>
                  ))}
                </div>
              ))}
              <HorizontalLine />
            </div>
          ))}
      </>
    </div>
  );
}
