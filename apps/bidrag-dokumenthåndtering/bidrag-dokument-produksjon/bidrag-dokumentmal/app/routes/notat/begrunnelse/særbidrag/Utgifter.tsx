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
import særbidragTekster from "~/tekster/notat/begrunnelse/særbidrag.yaml";
import { FeltTabell, JustertFeltrad } from "~/components/FeltTabell";

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
        <div className={"two_column_view_v2"}>
          <FeltTabell labelColWidth={"200px"}>
            <JustertFeltrad label={ord.kravbeløp}>
              {formatterBeløp(data.utgift?.beregning?.totalKravbeløp)}
            </JustertFeltrad>
            <JustertFeltrad label={ord.godkjentBeløp}>
              {formatterBeløp(data.utgift?.beregning?.totalGodkjentBeløp)}
            </JustertFeltrad>
            <JustertFeltrad label={t.totalt.direkteBetaltAvBidragspliktig}>
              {formatterBeløp(data.utgift?.beregning?.beløpDirekteBetaltAvBp)}
            </JustertFeltrad>
          </FeltTabell>
        </div>
      </div>
      {data.utgift?.maksGodkjentBeløp?.taMed && (
        <div>
          <div className={"two_column_view_v2"}>
            <FeltTabell labelColWidth={"200px"}>
              <JustertFeltrad label={t.maksGodkjentBeløp.maksGodkjentBeløp}>
                {formatterBeløp(data.utgift?.maksGodkjentBeløp?.beløp)}
              </JustertFeltrad>
              <JustertFeltrad label={t.maksGodkjentBeløp.begrunnelse}>
                {data.utgift?.maksGodkjentBeløp?.begrunnelse}
              </JustertFeltrad>
            </FeltTabell>
          </div>
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
        <div className={"two_column_view"} style={{ width: "45%" }}>
          <FeltTabell labelColWidth={"80px"}>
            <JustertFeltrad label={ord.søknadstype}>
              {capitalizeFirstLetter(behandling.søknadstype)}
            </JustertFeltrad>
            <JustertFeltrad label={t.søknadsdetaljer.kategori}>
              {behandling.kategoriVisningsnavn}
            </JustertFeltrad>
            <JustertFeltrad label={t.søknadsdetaljer.søktFra}>
              {søktAvTilVisningsnavn(behandling.søktAv)}
            </JustertFeltrad>
            <JustertFeltrad label={t.søknadsdetaljer.innkreving}>
              {behandling.innkreving ? ord.ja : ord.nei}
            </JustertFeltrad>
          </FeltTabell>
        </div>
        <div className={"two_column_view"} style={{ width: "40%" }}>
          <FeltTabell
            labelColWidth={behandling.klageMottattDato ? "110px" : "80px"}
          >
            <JustertFeltrad label={ord.mottattDato}>
              {dateToDDMMYYYY(behandling.mottattDato as string)}
            </JustertFeltrad>
            {behandling.klageMottattDato ? (
              <JustertFeltrad label={t.søknadsdetaljer.klageMottattDato}>
                {dateToDDMMYYYY(behandling.klageMottattDato)}
              </JustertFeltrad>
            ) : behandling.klageMottattDato === "" ? (
              <JustertFeltrad />
            ) : null}
            {behandling.avslag ? (
              <JustertFeltrad label={ord.avslag}>
                {`${behandling.avslagVisningsnavnUtenPrefiks}`}
              </JustertFeltrad>
            ) : typeof behandling.avslag === "string" ? (
              <JustertFeltrad />
            ) : null}
          </FeltTabell>
        </div>
      </div>
    </>
  );
}
