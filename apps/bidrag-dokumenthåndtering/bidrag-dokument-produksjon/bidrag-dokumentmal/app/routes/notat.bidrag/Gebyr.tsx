import { useNotatFelles } from "~/components/notat_felles/NotatContext";
import { DataViewTable, DataViewTableData } from "~/components/DataViewTable";
import { CommonTable } from "~/components/CommonTable";
import { konverterRolletype } from "~/utils/converter-utils";
import { Rolletype } from "~/types/Api";
import { rolleTilVisningsnavnV2 } from "~/utils/visningsnavn";

export default function Gebyr() {
  const { data, erAvslag } = useNotatFelles();
  const gebyr = data.gebyr;
  if (!gebyr || gebyr.length == 0) return null;
  return (
    <div>
      <div className={"elements_inline section-title"}>
        <h2>Gebyr</h2>
      </div>
      <>
        {gebyr
          .sort((d) =>
            konverterRolletype(d.rolle.rolle) == Rolletype.BP ? 1 : -1,
          )
          .map((gebyRolle, i) => (
            <div
              key={gebyRolle.rolle.ident! + i}
              className={gebyr.length - 1 == i ? "" : "mb-2"}
            >
              <DataViewTable
                className={"mb-1 mt-2"}
                data={
                  [
                    {
                      label: rolleTilVisningsnavnV2(gebyRolle.rolle),
                      labelBold: true,
                      value: gebyRolle.rolle.navn,
                    },
                  ].filter((d) => d != null) as DataViewTableData[]
                }
              />
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
                          content: gebyRolle.inntekt.skattepliktigInntekt,
                        },
                      ],
                    },
                    !erAvslag && {
                      columns: [
                        {
                          content: "Høyeste barnetillegg",
                        },
                        {
                          content: gebyRolle.inntekt.maksBarnetillegg ?? 0,
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
                          content: gebyRolle.inntekt.totalInntekt,
                        },
                      ],
                    },
                  ].filter((d) => d != false),
                }}
              />

              <DataViewTable
                className={"mt-4"}
                data={
                  [
                    {
                      label: "Gebyr",
                      value: gebyRolle.gebyrResultatVisningsnavn,
                    },
                    gebyRolle.erManueltOverstyrt && {
                      label: "Begrunnelse",
                      value: gebyRolle.begrunnelse,
                    },
                  ].filter((d) => d) as DataViewTableData[]
                }
              />
              {gebyRolle.beregnetIlagtGebyr}
            </div>
          ))}
      </>
    </div>
  );
}
