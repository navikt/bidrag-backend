import {
  capitalizeFirstLetter,
  formatterBeløp,
  søktAvTilVisningsnavn,
} from "~/utils/visningsnavn";
import { dateToDDMMYYYY } from "~/utils/date-utils";
import NotatBegrunnelse from "~/components/NotatBegrunnelse";
import { useNotatFelles } from "~/components/notat_felles/NotatContext";
import {
  DokumentTabell,
  Tabellcelle,
  Tabellrad,
} from "~/components/DokumentTabell";
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
      <DokumentTabell
        layoutAuto
        bredde="630px"
        kolonner={[
          { tittel: tekster.tabell.utgifter.betaltAvBp, bredde: "100px" },
          { tittel: tekster.tabell.utgifter.dato, bredde: "70px" },
          { tittel: tekster.tabell.utgifter.utgift, bredde: "120px" },
          { tittel: ord.kravbeløp, bredde: "80px" },
          { tittel: ord.godkjentBeløp, bredde: "110px" },
          { tittel: ord.begrunnelse, bredde: "180px" },
        ]}
      >
        {utgifter.map((utgift, index) => (
          <Tabellrad key={index} index={index}>
            <Tabellcelle>{utgift.betaltAvBp ? ord.ja : ord.nei}</Tabellcelle>
            <Tabellcelle>{dateToDDMMYYYY(utgift.dato)}</Tabellcelle>
            <Tabellcelle>{utgift.utgiftstypeVisningsnavn}</Tabellcelle>
            <Tabellcelle>{formatterBeløp(utgift.kravbeløp)}</Tabellcelle>
            <Tabellcelle>{formatterBeløp(utgift.godkjentBeløp)}</Tabellcelle>
            <Tabellcelle>{utgift.begrunnelse}</Tabellcelle>
          </Tabellrad>
        ))}
      </DokumentTabell>

      {beregnetUtgifter.length > 0 && (
        <div className={"mt-medium mb-medium"}>
          <h3>{t.beregnetTotalt.tittel}</h3>
          <DokumentTabell
            layoutAuto
            bredde="450px"
            kolonner={[
              { tittel: tekster.tabell.utgifter.betaltAvBp, bredde: "100px" },
              {
                tittel: tekster.tabell.utgifter.utgiftskategori,
                bredde: "220px",
              },
              { tittel: ord.kravbeløp, bredde: "80px" },
              { tittel: ord.godkjentBeløp, bredde: "110px" },
            ]}
          >
            {beregnetUtgifter.map((utgift, index) => (
              <Tabellrad key={index} index={index}>
                <Tabellcelle>
                  {utgift.betaltAvBp ? ord.ja : ord.nei}
                </Tabellcelle>
                <Tabellcelle>{utgift.utgiftstypeVisningsnavn}</Tabellcelle>
                <Tabellcelle>{utgift.totalKravbeløp}</Tabellcelle>
                <Tabellcelle>{utgift.totalGodkjentBeløp}</Tabellcelle>
              </Tabellrad>
            ))}
          </DokumentTabell>
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
