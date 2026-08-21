import {
  capitalizeFirstLetter,
  formatterBeløp,
  søktAvTilVisningsnavn,
} from "~/utils/visningsnavn";
import { dateToDDMMYYYY } from "~/utils/date-utils";
import NotatBegrunnelse from "~/components/NotatBegrunnelse";
import { useNotatFelles } from "~/components/notat_felles/NotatContext";
import {
  CommonTable,
  TableHeader,
  TableColumn,
} from "~/components/CommonTable";
import tekster from "~/tekster";
import { DataViewTableData } from "~/components/DataViewTable";
import { DataViewTableV2 } from "~/components/DataViewTableV2";

export default function Utgifter() {
  const { data } = useNotatFelles();
  const utgifter = data.utgift;
  return (
    <>
      <h2>Utgift</h2>
      <>
        <SøknadsDetaljer />
        <Utgiftsposter />
        <NotatBegrunnelse data={utgifter?.begrunnelse} />
      </>
    </>
  );
}

function Utgiftsposter() {
  const { data, erAvslag } = useNotatFelles();
  if (erAvslag) return;
  const utgifter = data.utgift?.utgifter ?? [];
  const beregnetUtgifter = data.utgift?.totalBeregning ?? [];
  return (
    <>
      <h3>Oversikt over utgifter</h3>
      <CommonTable
        layoutAuto
        width={"630px"}
        data={{
          headers: [
            { name: tekster.tabell.utgifter.betaltAvBp, width: "100px" },
            { name: tekster.tabell.utgifter.dato, width: "70px" },
            { name: tekster.tabell.utgifter.utgift, width: "120px" },
            { name: tekster.tabell.utgifter.kravbeløp, width: "80px" },
            { name: tekster.tabell.utgifter.godkjentBeløp, width: "110px" },
            { name: tekster.tabell.utgifter.begrunnelse, width: "180px" },
          ].filter((d) => typeof d != "boolean") as TableHeader[],
          rows: utgifter.map((d) => {
            return {
              columns: [
                { content: d.betaltAvBp ? "Ja" : "Nei" },
                { content: dateToDDMMYYYY(d.dato) },
                { content: d.utgiftstypeVisningsnavn },
                { content: formatterBeløp(d.kravbeløp) },
                { content: formatterBeløp(d.godkjentBeløp) },
                { content: d.begrunnelse },
              ].filter((d) => typeof d != "boolean") as TableColumn[],
            };
          }),
        }}
      />

      {beregnetUtgifter.length > 0 && (
        <div className={"mt-medium mb-medium"}>
          <h3>Beregnet totalt</h3>
          <CommonTable
            layoutAuto
            width={"450px"}
            data={{
              headers: [
                { name: tekster.tabell.utgifter.betaltAvBp, width: "100px" },
                {
                  name: tekster.tabell.utgifter.utgiftskategori,
                  width: "220px",
                },
                { name: tekster.tabell.utgifter.kravbeløp, width: "80px" },
                { name: tekster.tabell.utgifter.godkjentBeløp, width: "110px" },
              ].filter((d) => typeof d != "boolean") as TableHeader[],
              rows: beregnetUtgifter.map((d) => {
                return {
                  columns: [
                    { content: d.betaltAvBp ? "Ja" : "Nei" },
                    { content: d.utgiftstypeVisningsnavn },
                    { content: d.totalKravbeløp },
                    { content: d.totalGodkjentBeløp },
                  ].filter((d) => typeof d != "boolean") as TableColumn[],
                };
              }),
            }}
          />
        </div>
      )}
      <h3>Totalt</h3>
      <div style={{ marginTop: "8px", width: "280px" }}>
        <DataViewTableV2
          className={"two_column_view_v2"}
          labelColWidth={"200px"}
          data={
            [
              {
                label: "Kravbeløp",
                value: formatterBeløp(data.utgift?.beregning?.totalKravbeløp),
              },
              {
                label: "Godkjent beløp",
                value: formatterBeløp(
                  data.utgift?.beregning?.totalGodkjentBeløp,
                ),
              },
              {
                label: "Direkte betalt av Bidragspliktig",
                value: formatterBeløp(
                  data.utgift?.beregning?.beløpDirekteBetaltAvBp,
                ),
              },
            ].filter((d) => d != null) as DataViewTableData[]
          }
        />
      </div>
      {data.utgift?.maksGodkjentBeløp?.taMed && (
        <div>
          <DataViewTableV2
            className={"two_column_view_v2"}
            labelColWidth={"200px"}
            data={
              [
                {
                  label: "Maks godkjent beløp",
                  value: formatterBeløp(data.utgift?.maksGodkjentBeløp?.beløp),
                },
                {
                  label: "Begrunnelse for maks godkjent beløp",
                  value: data.utgift?.maksGodkjentBeløp?.begrunnelse,
                },
              ].filter((d) => d != null) as DataViewTableData[]
            }
          />
        </div>
      )}
    </>
  );
}

function SøknadsDetaljer() {
  const { data } = useNotatFelles();
  const behandling = data.behandling;
  return (
    <>
      <div style={{ width: "600px", height: "80px", marginBottom: 0 }}>
        <DataViewTableV2
          className={"two_column_view"}
          width={"45%"}
          labelColWidth={"80px"}
          data={[
            {
              label: "Søknadstype",
              value: capitalizeFirstLetter(behandling.søknadstype),
            },
            {
              label: "Kategori",
              value: behandling.kategoriVisningsnavn,
            },
            {
              label: "Søkt fra",
              value: søktAvTilVisningsnavn(behandling.søktAv),
            },
            {
              label: "Innkreving",
              value: behandling.innkreving ? "Ja" : "Nei",
            },
          ]}
        />
        <DataViewTableV2
          className={"two_column_view"}
          width={"40%"}
          labelColWidth={behandling.klageMottattDato ? "110px" : "80px"}
          data={
            [
              {
                label: "Mottatt dato",
                value: dateToDDMMYYYY(behandling.mottattDato as string),
              },
              behandling.klageMottattDato && {
                label: "Klage mottatt dato",
                value: dateToDDMMYYYY(behandling.klageMottattDato as string),
              },
              behandling.avslag && {
                label: "Avslag",
                value: `${behandling.avslagVisningsnavnUtenPrefiks}`,
              },
            ].filter((d) => d != null) as DataViewTableData[]
          }
        />
      </div>
    </>
  );
}
