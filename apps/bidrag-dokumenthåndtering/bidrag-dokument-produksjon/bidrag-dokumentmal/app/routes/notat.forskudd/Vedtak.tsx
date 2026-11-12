import TableGjelderBarn from "~/components/TableGjelderBarn";
import { KolonneTabell, Tabelloppsett } from "~/components/KolonneTabell";
import { useNotatFelles } from "~/components/notat_felles/NotatContext";
import { VedtakFattetDetaljer } from "~/components/notat_felles/components/VedtakFattetDetaljer";
import { NotatResultatForskuddBeregningBarnDto } from "~/types/Api";
import ord from "~/tekster/ord.yaml";
import forskuddTekster from "~/tekster/notat/forskudd/saksbehandler.yaml";

const t = forskuddTekster.vedtak;

const tabellAvslag: Tabelloppsett = {
  bredde: "500px",
  kolonner: [
    {
      felt: "periode",
      tittel: ord.periode,
      bredde: "170px",
      format: "periodeTilOgMedTilMåned",
    },
    { felt: "resultat", tittel: ord.resultat, bredde: "150px" },
    {
      felt: "resultatKodeVisningsnavn",
      tittel: ord.årsak,
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
      tittel: ord.inntekt,
      bredde: "80px",
      format: "beløp",
    },
    {
      felt: "sivilstandVisningsnavn",
      tittel: t.tabellInnvilget.sivilstand,
      bredde: "100px",
    },
    {
      felt: "antallBarnIHusstanden",
      tittel: t.tabellInnvilget.antallBarnIHusstand,
      bredde: "80px",
    },
    {
      felt: "beløp",
      tittel: t.tabellInnvilget.forskudd,
      bredde: "70px",
      format: "beløp",
    },
    {
      felt: "resultatKodeVisningsnavn",
      tittel: ord.resultat,
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
      <h2>{t.tittel}</h2>
      {resultat.length == 0 ? (
        <div>{ord.manglerResultat}</div>
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
      <VedtakFattetDetaljer data={data.vedtak} />
    </div>
  );
}

function VedtakAvslagForBarn({
  barn,
}: {
  barn: NotatResultatForskuddBeregningBarnDto;
}) {
  const { erOpphør } = useNotatFelles();
  const resultat = erOpphør || barn.erOpphørAvStønad ? ord.opphør : ord.avslag;
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
