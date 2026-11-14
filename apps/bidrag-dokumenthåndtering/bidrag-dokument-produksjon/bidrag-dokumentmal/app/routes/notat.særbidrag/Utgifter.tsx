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
import ord from "~/tekster/ord.yaml";
import tekster from "~/tekster";
import særbidragTekster from "~/tekster/notat/særbidrag/saksbehandler.yaml";
import { DataViewTableData } from "~/components/DataViewTable";
import { DataViewTableV2 } from "~/components/DataViewTableV2";

const t = særbidragTekster.utgifter;

export default function Utgifter() {
  const { data } = useNotatFelles();
  const utgifter = data.utgift;
  return (
    <>
      <h2>{t.tittel}</h2>
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
      <h3>{t.oversiktOverUtgifter.tittel}</h3>
      <CommonTable
        layoutAuto
        width={"630px"}
        data={{
          headers: [
            { name: tekster.tabell.utgifter.betaltAvBp, width: "100px" },
            { name: tekster.tabell.utgifter.dato, width: "70px" },
            { name: tekster.tabell.utgifter.utgift, width: "120px" },
            { name: ord.kravbeløp, width: "80px" },
            { name: ord.godkjentBeløp, width: "110px" },
            { name: ord.begrunnelse, width: "180px" },
          ].filter((d) => typeof d != "boolean") as TableHeader[],
          rows: utgifter.map((d) => {
            return {
              columns: [
                { content: d.betaltAvBp ? ord.ja : ord.nei },
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
          <h3>{t.beregnetTotalt.tittel}</h3>
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
                { name: ord.kravbeløp, width: "80px" },
                { name: ord.godkjentBeløp, width: "110px" },
              ].filter((d) => typeof d != "boolean") as TableHeader[],
              rows: beregnetUtgifter.map((d) => {
                return {
                  columns: [
                    { content: d.betaltAvBp ? ord.ja : ord.nei },
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
      <h3>{ord.totalt}</h3>
      <div style={{ marginTop: "8px", width: "280px" }}>
        <DataViewTableV2
          className={"two_column_view_v2"}
          labelColWidth={"200px"}
          data={
            [
              {
                label: ord.kravbeløp,
                value: formatterBeløp(data.utgift?.beregning?.totalKravbeløp),
              },
              {
                label: ord.godkjentBeløp,
                value: formatterBeløp(
                  data.utgift?.beregning?.totalGodkjentBeløp,
                ),
              },
              {
                label: t.totalt.direkteBetaltAvBidragspliktig,
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
                  label: t.maksGodkjentBeløp.maksGodkjentBeløp,
                  value: formatterBeløp(data.utgift?.maksGodkjentBeløp?.beløp),
                },
                {
                  label: t.maksGodkjentBeløp.begrunnelse,
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
              label: ord.søknadstype,
              value: capitalizeFirstLetter(behandling.søknadstype),
            },
            {
              label: t.søknadsdetaljer.kategori,
              value: behandling.kategoriVisningsnavn,
            },
            {
              label: t.søknadsdetaljer.søktFra,
              value: søktAvTilVisningsnavn(behandling.søktAv),
            },
            {
              label: t.søknadsdetaljer.innkreving,
              value: behandling.innkreving ? ord.ja : ord.nei,
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
                label: ord.mottattDato,
                value: dateToDDMMYYYY(behandling.mottattDato as string),
              },
              behandling.klageMottattDato && {
                label: t.søknadsdetaljer.klageMottattDato,
                value: dateToDDMMYYYY(behandling.klageMottattDato as string),
              },
              behandling.avslag && {
                label: ord.avslag,
                value: `${behandling.avslagVisningsnavnUtenPrefiks}`,
              },
            ].filter((d) => d != null) as DataViewTableData[]
          }
        />
      </div>
    </>
  );
}
