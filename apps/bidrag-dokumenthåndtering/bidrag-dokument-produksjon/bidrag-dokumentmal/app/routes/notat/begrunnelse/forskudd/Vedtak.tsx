import TableGjelderBarn from "~/components/TableGjelderBarn";
import { KolonneTabell, Tabelloppsett } from "~/components/KolonneTabell";
import { useNotatFelles } from "~/components/notat_felles/NotatContext";
import { NotatResultatForskuddBeregningBarnDto } from "~/types/Api";
import { tekst } from "~/utils/tekst";
const tabellAvslag: Tabelloppsett = {
  bredde: "500px",
  kolonner: [
    {
      felt: "periode",
      tittel: tekst("ord.periode", "Periode"),
      bredde: "170px",
      format: "periodeTilOgMedTilMåned",
    },
    {
      felt: "resultat",
      tittel: tekst("ord.resultat", "Resultat"),
      bredde: "150px",
    },
    {
      felt: "resultatKodeVisningsnavn",
      tittel: tekst("ord.årsak", "Årsak"),
      bredde: "250px",
    },
  ],
};
const tabellInnvilget: Tabelloppsett = {
  bredde: "550px",
  periodelinje: { felt: "periode", format: "periode" },
  kolonner: [
    {
      felt: "inntekt",
      tittel: tekst("ord.inntekt", "Inntekt"),
      bredde: "80px",
      format: "beløp",
    },
    {
      felt: "sivilstandVisningsnavn",
      tittel: tekst(
        "notat.begrunnelse.forskudd.vedtak.tabellInnvilget.sivilstand",
        "Sivilstand",
      ),
      bredde: "100px",
    },
    {
      felt: "antallBarnIHusstanden",
      tittel: tekst(
        "notat.begrunnelse.forskudd.vedtak.tabellInnvilget.antallBarnIHusstand",
        "Antall barn i husstand",
      ),
      bredde: "80px",
    },
    {
      felt: "beløp",
      tittel: tekst(
        "notat.begrunnelse.forskudd.vedtak.tabellInnvilget.forskudd",
        "Forskudd",
      ),
      bredde: "70px",
      format: "beløp",
    },
    {
      felt: "resultatKodeVisningsnavn",
      tittel: tekst("ord.resultat", "Resultat"),
      bredde: "160px",
    },
  ],
};
export default function Vedtak() {
  const { erAvslag, data } = useNotatFelles();
  const resultat = data.vedtak
    .resultat as NotatResultatForskuddBeregningBarnDto[];
  return (
    <div style={{ pageBreakBefore: erAvslag ? "auto" : "always" }}>
      <h2>{tekst("notat.begrunnelse.forskudd.vedtak.tittel", "Vedtak")}</h2>
      {resultat.length == 0 ? (
        <div>{tekst("ord.manglerResultat", "Mangler resultat")}</div>
      ) : erAvslag ? (
        <div style={{ paddingTop: "0px" }}>
          {resultat.map((barn) => (
            <VedtakAvslagForBarn key={barn.barn.ident} barn={barn} />
          ))}
        </div>
      ) : (
        resultat.map((barn) => (
          <VedtakForBarn key={barn.barn.ident} barn={barn} />
        ))
      )}
    </div>
  );
}
function VedtakAvslagForBarn({
  barn,
}: {
  barn: NotatResultatForskuddBeregningBarnDto;
}) {
  const { erOpphør } = useNotatFelles();
  const resultat =
    erOpphør || barn.erOpphørAvStønad
      ? tekst("ord.opphør", "Opphør")
      : tekst("ord.avslag", "Avslag");
  return (
    <div className="table_container">
      <TableGjelderBarn gjelderBarn={barn.barn} />
      <KolonneTabell
        oppsett={tabellAvslag}
        rader={barn.perioder}
        celler={{
          resultat: () => resultat,
        }}
      />
      <div
        className="horizontal-line"
        style={{ pageBreakAfter: "avoid", marginBottom: "24px" }}
      ></div>
    </div>
  );
}
function VedtakForBarn({
  barn,
}: {
  barn: NotatResultatForskuddBeregningBarnDto;
}) {
  return (
    <div className="table_container mb-medium">
      <TableGjelderBarn gjelderBarn={barn.barn} />
      <KolonneTabell oppsett={tabellInnvilget} rader={barn.perioder} />
    </div>
  );
}
