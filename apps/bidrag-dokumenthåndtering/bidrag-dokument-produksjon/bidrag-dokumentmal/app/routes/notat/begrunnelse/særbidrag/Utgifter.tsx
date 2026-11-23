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
import { FeltTabell, JustertFeltrad } from "~/components/FeltTabell";
import { tekst } from "~/utils/tekst";
export default function Utgifter() {
  const { data } = useNotatFelles();
  const utgifter = data.utgift;
  return (
    <>
      <h2>{tekst("notat.begrunnelse.særbidrag.utgifter.tittel", "Utgift")}</h2>
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
      <h3>
        {tekst(
          "notat.begrunnelse.særbidrag.utgifter.oversiktOverUtgifter.tittel",
          "Oversikt over utgifter",
        )}
      </h3>
      <DokumentTabell
        layoutAuto
        bredde="630px"
        kolonner={[
          {
            tittel: tekst(
              "felles.tabell.utgifter.betaltAvBp",
              "Betalt av Bidragspliktig",
            ),
            bredde: "100px",
          },
          {
            tittel: tekst("felles.tabell.utgifter.dato", "Forfallsdato"),
            bredde: "70px",
          },
          {
            tittel: tekst("felles.tabell.utgifter.utgift", "Utgift"),
            bredde: "120px",
          },
          { tittel: tekst("ord.kravbeløp", "Kravbeløp"), bredde: "80px" },
          {
            tittel: tekst("ord.godkjentBeløp", "Godkjent beløp"),
            bredde: "110px",
          },
          { tittel: tekst("ord.begrunnelse", "Begrunnelse"), bredde: "180px" },
        ]}
      >
        {utgifter.map((utgift, index) => (
          <Tabellrad key={index} index={index}>
            <Tabellcelle>
              {utgift.betaltAvBp
                ? tekst("ord.ja", "Ja")
                : tekst("ord.nei", "Nei")}
            </Tabellcelle>
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
          <h3>
            {tekst(
              "notat.begrunnelse.særbidrag.utgifter.beregnetTotalt.tittel",
              "Beregnet totalt",
            )}
          </h3>
          <DokumentTabell
            layoutAuto
            bredde="450px"
            kolonner={[
              {
                tittel: tekst(
                  "felles.tabell.utgifter.betaltAvBp",
                  "Betalt av Bidragspliktig",
                ),
                bredde: "100px",
              },
              {
                tittel: tekst(
                  "felles.tabell.utgifter.utgiftskategori",
                  "Utgiftskategori",
                ),
                bredde: "220px",
              },
              { tittel: tekst("ord.kravbeløp", "Kravbeløp"), bredde: "80px" },
              {
                tittel: tekst("ord.godkjentBeløp", "Godkjent beløp"),
                bredde: "110px",
              },
            ]}
          >
            {beregnetUtgifter.map((utgift, index) => (
              <Tabellrad key={index} index={index}>
                <Tabellcelle>
                  {utgift.betaltAvBp
                    ? tekst("ord.ja", "Ja")
                    : tekst("ord.nei", "Nei")}
                </Tabellcelle>
                <Tabellcelle>{utgift.utgiftstypeVisningsnavn}</Tabellcelle>
                <Tabellcelle>{utgift.totalKravbeløp}</Tabellcelle>
                <Tabellcelle>{utgift.totalGodkjentBeløp}</Tabellcelle>
              </Tabellrad>
            ))}
          </DokumentTabell>
        </div>
      )}
      <h3>{tekst("ord.totalt", "Totalt")}</h3>
      <div style={{ marginTop: "8px", width: "280px" }}>
        <div className={"two_column_view_v2"}>
          <FeltTabell labelColWidth={"200px"}>
            <JustertFeltrad label={tekst("ord.kravbeløp", "Kravbeløp")}>
              {formatterBeløp(data.utgift?.beregning?.totalKravbeløp)}
            </JustertFeltrad>
            <JustertFeltrad
              label={tekst("ord.godkjentBeløp", "Godkjent beløp")}
            >
              {formatterBeløp(data.utgift?.beregning?.totalGodkjentBeløp)}
            </JustertFeltrad>
            <JustertFeltrad
              label={tekst(
                "notat.begrunnelse.særbidrag.utgifter.totalt.direkteBetaltAvBidragspliktig",
                "Direkte betalt av Bidragspliktig",
              )}
            >
              {formatterBeløp(data.utgift?.beregning?.beløpDirekteBetaltAvBp)}
            </JustertFeltrad>
          </FeltTabell>
        </div>
      </div>
      {data.utgift?.maksGodkjentBeløp?.taMed && (
        <div>
          <div className={"two_column_view_v2"}>
            <FeltTabell labelColWidth={"200px"}>
              <JustertFeltrad
                label={tekst(
                  "notat.begrunnelse.særbidrag.utgifter.maksGodkjentBeløp.maksGodkjentBeløp",
                  "Maks godkjent beløp",
                )}
              >
                {formatterBeløp(data.utgift?.maksGodkjentBeløp?.beløp)}
              </JustertFeltrad>
              <JustertFeltrad
                label={tekst(
                  "notat.begrunnelse.særbidrag.utgifter.maksGodkjentBeløp.begrunnelse",
                  "Begrunnelse for maks godkjent beløp",
                )}
              >
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
            <JustertFeltrad label={tekst("ord.søknadstype", "Søknadstype")}>
              {capitalizeFirstLetter(behandling.søknadstype)}
            </JustertFeltrad>
            <JustertFeltrad
              label={tekst(
                "notat.begrunnelse.særbidrag.utgifter.søknadsdetaljer.kategori",
                "Kategori",
              )}
            >
              {behandling.kategoriVisningsnavn}
            </JustertFeltrad>
            <JustertFeltrad
              label={tekst(
                "notat.begrunnelse.særbidrag.utgifter.søknadsdetaljer.søktFra",
                "Søkt fra",
              )}
            >
              {søktAvTilVisningsnavn(behandling.søktAv)}
            </JustertFeltrad>
            <JustertFeltrad
              label={tekst(
                "notat.begrunnelse.særbidrag.utgifter.søknadsdetaljer.innkreving",
                "Innkreving",
              )}
            >
              {behandling.innkreving
                ? tekst("ord.ja", "Ja")
                : tekst("ord.nei", "Nei")}
            </JustertFeltrad>
          </FeltTabell>
        </div>
        <div className={"two_column_view"} style={{ width: "40%" }}>
          <FeltTabell
            labelColWidth={behandling.klageMottattDato ? "110px" : "80px"}
          >
            <JustertFeltrad label={tekst("ord.mottattDato", "Mottatt dato")}>
              {dateToDDMMYYYY(behandling.mottattDato as string)}
            </JustertFeltrad>
            {behandling.klageMottattDato ? (
              <JustertFeltrad
                label={tekst(
                  "notat.begrunnelse.særbidrag.utgifter.søknadsdetaljer.klageMottattDato",
                  "Klage mottatt dato",
                )}
              >
                {dateToDDMMYYYY(behandling.klageMottattDato)}
              </JustertFeltrad>
            ) : behandling.klageMottattDato === "" ? (
              <JustertFeltrad />
            ) : null}
            {behandling.avslag ? (
              <JustertFeltrad label={tekst("ord.avslag", "Avslag")}>
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
