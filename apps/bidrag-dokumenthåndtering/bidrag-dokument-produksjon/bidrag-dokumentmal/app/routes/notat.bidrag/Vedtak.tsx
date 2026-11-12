import { ReactNode } from "react";
import {
  DokumentmalResultatBidragsberegningBarnDto,
  ResultatBarnebidragsberegningPeriodeDto,
  Samvaersklasse,
} from "~/types/Api";
import {
  useNotatFelles,
  useVedleggNummer,
} from "~/components/notat_felles/NotatContext";
import { VedtakFattetDetaljer } from "~/components/notat_felles/components/VedtakFattetDetaljer";
import { sortByAge } from "~/utils/date-utils";
import elementIds from "~/utils/elementIds";
import { groupBy } from "~/utils/array-utils";
import { CommonTable, TableHeader, TableRow } from "~/components/CommonTable";
import TableGjelderBarn from "~/components/TableGjelderBarn";
import { DataViewTable } from "~/components/DataViewTable";
import {
  useDokumentFelles,
  TypeInnhold,
} from "~/components/vedtak_felles/FellesContext";
import {
  KolonneTabell,
  kolonneceller,
  kolonneoverskrifter,
  periodelinje,
  Tabelloppsett,
} from "~/components/KolonneTabell";
import { formater } from "~/utils/visning";
import ord from "~/tekster/ord.json";
import bidragTekster from "~/tekster/notat/bidrag/saksbehandler.json";

const t = bidragTekster.vedtak;
const tr = bidragTekster.revurderingsbarn;

const te = t.tabellEndelig;
const tb = t.tabellBeregning;

const tabellInnkreving: Tabelloppsett = {
  kolonner: [
    {
      felt: "periode",
      tittel: ord.periode,
      bredde: "170px",
      format: "periodeTilOgMedTilMåned",
    },
    {
      felt: "faktiskBidrag",
      tittel: ord.beløp,
      bredde: "150px",
      format: "beløpAvrundet",
    },
    {
      felt: "resultatkodeVisningsnavn",
      tittel: ord.resultat,
      bredde: "250px",
    },
  ],
};

const tabellAvslag: Tabelloppsett = {
  kolonner: [
    {
      felt: "periode",
      tittel: ord.periode,
      bredde: "170px",
      format: "periodeTilOgMedTilMåned",
    },
    { felt: "resultat", tittel: ord.resultat, bredde: "150px" },
    {
      felt: "resultatkodeVisningsnavn",
      tittel: ord.årsak,
      bredde: "250px",
    },
  ],
};

// bpsAndel, samvær og evne viser to verdier i samme celle, se `beregningsceller`
const tabellBeregning: Tabelloppsett = {
  periodelinje: { felt: "periode", format: "periode" },
  kolonner: [
    {
      felt: "underholdskostnad",
      tittel: tb.underholdskostnad,
      bredde: "50px",
      format: "beløpAvrundet",
    },
    { felt: "bpsAndel", tittel: tb.bpsAndel, bredde: "120px" },
    { felt: "samvær", tittel: tb.samvær, bredde: "100px" },
    { felt: "evne", tittel: tb.evne, bredde: "100px" },
    {
      felt: "beregnetBidrag",
      tittel: tb.beregnetBidrag,
      bredde: "80px",
      format: "beløpAvrundet",
    },
    {
      felt: "faktiskBidrag",
      tittel: tb.endeligBidrag,
      bredde: "80px",
      format: "beløpAvrundet",
    },
  ],
};

type Resultat = DokumentmalResultatBidragsberegningBarnDto;
type Periode = ResultatBarnebidragsberegningPeriodeDto;

export default function Vedtak() {
  const vedleggNummer = useVedleggNummer("beregningsdetaljer");
  const { erAvslag, data, erInnkrevingsgrunnlag } = useNotatFelles();
  const resultat = data.vedtak.resultat as Resultat[];

  function renderTabell() {
    if (resultat.length == 0) return <div>{ord.manglerResultat}</div>;
    if (erInnkrevingsgrunnlag) return <VedtakInnkreving resultat={resultat} />;
    if (erAvslag) return <VedtakAvslag resultat={resultat} />;
    return <VedtakBeregning resultat={resultat} />;
  }

  return (
    <>
      <div className={"elements_inline section-title break-before-page"}>
        <h2 className={"section-title"}>
          {data.erOrkestrertVedtak ? t.tittelKlagevedtak : t.tittel}
        </h2>
        {!erAvslag && !erInnkrevingsgrunnlag && (
          <a href={`#${elementIds.vedleggBeregningsdetaljer}`}>
            {ord.seVedleggForBeregningsdetaljer.replace(
              "{}",
              `${vedleggNummer}`,
            )}
          </a>
        )}
      </div>
      {renderTabell()}
      {data.erOrkestrertVedtak && (
        <>
          <div className={"elements_inline section-title break-before-page"}>
            <h2 className={"section-title"}>{t.tittelEndeligVedtak}</h2>
          </div>
          <VedtakEndeligTable data={resultat} />
        </>
      )}
      <OverstyrFatteVedtakRevurderingsbarnInformasjon />
      <VedtakFattetDetaljer data={data.vedtak} />
    </>
  );
}

/** Ett resultat per barn. Barna sorteres etter alder når `sorter` er satt. */
function resultatPerBarn(data: Resultat[], sorter = false): Resultat[] {
  const perBarn = groupBy(data, (d) => d.barn?.ident!).map(([, v]) => v[0]);
  return sorter ? perBarn.sort((a, b) => sortByAge(a.barn, b.barn)) : perBarn;
}

function useAvslagstekst() {
  const { erOpphør } = useNotatFelles();
  return (resultat: Resultat) =>
    erOpphør || resultat.barn.harLøpendeBidrag == true
      ? ord.opphør
      : ord.avslag;
}

function VedtakInnkreving({ resultat }: { resultat: Resultat[] }) {
  return (
    <div style={{ paddingTop: "0px" }}>
      {resultatPerBarn(resultat).map((r) => (
        <div key={r.barn.ident} className="table_container">
          <TableGjelderBarn gjelderBarn={r.barn} />
          <KolonneTabell oppsett={tabellInnkreving} rader={r.perioder} />
        </div>
      ))}
    </div>
  );
}

function VedtakAvslag({ resultat }: { resultat: Resultat[] }) {
  const avslagstekst = useAvslagstekst();
  return (
    <div style={{ paddingTop: "0px" }}>
      {resultatPerBarn(resultat).map((r) => (
        <div key={r.barn.ident} className="table_container">
          <TableGjelderBarn gjelderBarn={r.barn} />
          <ResultatEllerTabell resultat={r}>
            <TabellAvslag resultat={r} avslagstekst={avslagstekst(r)} />
          </ResultatEllerTabell>
        </div>
      ))}
    </div>
  );
}

function TabellAvslag({
  resultat,
  avslagstekst,
}: {
  resultat: Resultat;
  avslagstekst: string;
}) {
  return (
    <KolonneTabell
      oppsett={tabellAvslag}
      rader={resultat.perioder}
      celler={{ resultat: () => avslagstekst }}
    />
  );
}

function ResultatEllerTabell({
  resultat,
  children,
}: {
  resultat: Resultat;
  children: ReactNode;
}) {
  if (resultat.erAvvistRevurdering) {
    return <div>{t.ingenPerioderSlåttUtTilFF}</div>;
  }
  if (resultat.erAvvisning) {
    return <div>{t.avvisning}</div>;
  }
  return <>{children}</>;
}

function OverstyrFatteVedtakRevurderingsbarnInformasjon() {
  const { data } = useNotatFelles();
  const { vedtak } = data;
  if (!vedtak.kanFatteVedtakForRevurderingsbarn) return null;
  return (
    <div>
      <h2 className={"section-title"}>{tr.tittel}</h2>
      {vedtak.skalFatteVedtakForRevurderingsbarn
        ? tr.foreslåttFatteVedtak
        : tr.foreslåttIkkeFatteVedtak}

      {!!vedtak.manueltOverstyrtFatteVedtakRevurderingsbarnBegrunnelse && (
        <div className={"mt-2"}>
          {vedtak.skalFatteVedtakForRevurderingsbarn
            ? tr.overstyrtTilIkkeFatteVedtak
            : tr.overstyrtTilFatteVedtak}
          <div>
            <h5 style={{ margin: 0 }}>{tr.begrunnelse}</h5>
            {`${vedtak.manueltOverstyrtFatteVedtakRevurderingsbarnBegrunnelse}`}
          </div>
        </div>
      )}
    </div>
  );
}

/** Viser to verdier i samme celle, f.eks. `45 % / 1 234`. */
function ToVerdier({
  venstre,
  høyre,
  venstreBredde = "w-[25px]",
}: {
  venstre: ReactNode;
  høyre: ReactNode;
  venstreBredde?: string;
}) {
  return (
    <table>
      <tbody>
        <tr>
          <td className={venstreBredde} align="right">
            {venstre}
          </td>
          <td className="w-[5px]">/</td>
          <td>{høyre}</td>
        </tr>
      </tbody>
    </table>
  );
}

// Samvær vises som 0 når perioden mangler beregningsdetaljer
const SAMVÆRSKLASSE_UTEN_BEREGNING = "0";

function samværsklasse(p: Periode) {
  const samværsfradrag = p.beregningsdetaljer?.samværsfradrag;
  if (p.beregningsdetaljer == undefined) return SAMVÆRSKLASSE_UTEN_BEREGNING;
  return samværsfradrag?.samværsklasse === Samvaersklasse.DELT_BOSTED
    ? tb.samværsklasseDeltBosted
    : samværsfradrag?.samværsklasseVisningsnavn;
}

const beregningsceller = {
  bpsAndel: (p: Periode) => (
    <ToVerdier
      venstre={formater(p.bpsAndelU, "prosent")}
      høyre={formater(p.bpsAndelBeløp, "beløpAvrundet")}
    />
  ),
  samvær: (p: Periode) => (
    <ToVerdier
      venstre={formater(p.samværsfradrag, "beløpAvrundet")}
      høyre={samværsklasse(p)}
    />
  ),
  evne: (p: Periode) => {
    const evne = p.beregningsdetaljer?.delberegningBidragsevne;
    return (
      <ToVerdier
        venstre={formater(evne?.bidragsevne, "beløpAvrundet")}
        høyre={formater(evne?.sumInntekt25Prosent, "beløpAvrundet")}
        venstreBredde="w-[35px]"
      />
    );
  },
};

function radForklaring(): TableRow {
  return {
    skipBorderBottom: true,
    zebraStripe: false,
    skipPadding: true,
    className: "pt-2",
    columns: [{ fullSpan: true, content: tb.forklaringForkortelser }],
  };
}

function raderBeregning(perioder: Periode[], avslagstekst: string): TableRow[] {
  const antallKolonnerFørResultat = tabellBeregning.kolonner.length - 1;
  return perioder
    .flatMap((p): TableRow[] => [
      {
        skipBorderBottom: true,
        periodColumn: periodelinje(tabellBeregning, p),
        columns: p.erAvslagUtenBeløp
          ? [
              { content: "", colSpan: antallKolonnerFørResultat },
              { content: avslagstekst },
            ]
          : kolonneceller(tabellBeregning, p, beregningsceller),
      },
      {
        zebraStripe: false,
        skipPadding: true,
        columns: [
          {
            colSpan: 8,
            content: (
              <DataViewTable
                className={"pl-0 ml-0"}
                data={[
                  {
                    label: ord.resultat,
                    labelBold: true,
                    value: p.resultatkodeVisningsnavn,
                  },
                ]}
              />
            ),
          },
        ],
      },
    ])
    .concat(radForklaring());
}

function BarnOgIndeksår({
  resultat,
  visIndeksår,
}: {
  resultat: Resultat;
  visIndeksår: boolean;
}) {
  const gjelderBarn = resultat.barn;
  return (
    <>
      <TableGjelderBarn gjelderBarn={gjelderBarn} />
      {resultat.indeksår && (
        <DataViewTable
          className={"mb-1 mt-2"}
          data={[
            visIndeksår
              ? { label: t.nesteIndeksår, value: resultat.indeksår }
              : {},
          ]}
        />
      )}
      {gjelderBarn.innbetaltBeløp && (
        <DataViewTable
          className="mt-2 mb-2"
          data={[
            {
              label: t.innbetaltBeløp,
              labelBold: true,
              value: gjelderBarn.innbetaltBeløp,
            },
          ]}
        />
      )}
    </>
  );
}

function VedtakBeregning({ resultat }: { resultat: Resultat[] }) {
  const { erDirekteAvslagForBarn } = useNotatFelles();
  const avslagstekst = useAvslagstekst();
  return (
    <>
      {resultatPerBarn(resultat, true).map((r) => (
        <>
          <BarnOgIndeksår
            resultat={r}
            visIndeksår={!r.erAvvistRevurdering && !r.erAvvisning}
          />
          <ResultatEllerTabell resultat={r}>
            {erDirekteAvslagForBarn(r.barn.ident!) ? (
              <TabellAvslag resultat={r} avslagstekst={avslagstekst(r)} />
            ) : (
              <CommonTable
                data={{
                  headers: kolonneoverskrifter(tabellBeregning),
                  rows: raderBeregning(r.perioder, avslagstekst(r)),
                }}
              />
            )}
          </ResultatEllerTabell>
        </>
      ))}
    </>
  );
}

// Brevet har en tom siste kolonne der notatet viser resultat. Beholdes for å ikke endre utseendet på brevet
const TOM_KOLONNE = { name: "", content: "" };

function overskrifterEndelig(
  vurderUgyldighet: boolean,
  visningForSaksbehandler: boolean,
): TableHeader[] {
  const bredde = (saksbehandler: string, brev: string) =>
    visningForSaksbehandler ? saksbehandler : brev;
  return [
    ...(vurderUgyldighet ? [{ name: te.vurderUgyldighet, width: "20px" }] : []),
    { name: ord.periode, width: bredde("120px", "30%") },
    {
      name: visningForSaksbehandler ? ord.type : ord.årsak,
      width: bredde("100px", "40%"),
    },
    { name: ord.beløp, width: bredde("120px", "30%") },
    visningForSaksbehandler
      ? { name: ord.resultat, width: "130px" }
      : { name: TOM_KOLONNE.name },
  ];
}

export function VedtakEndeligTable({ data }: { data: Resultat[] }) {
  const { typeInnhold } = useDokumentFelles();
  if (data.length == 0) return null;
  if (data.every((e) => e.orkestrertVedtak === null)) return null;
  const visningForSaksbehandler = typeInnhold == TypeInnhold.NOTAT;

  return (
    <>
      {resultatPerBarn(data, true).map((r) => {
        const perioder = r.orkestrertVedtak?.perioder ?? [];
        const vurderUgyldighet = perioder.some(
          (e) => e.klageOmgjøringDetaljer?.kanOpprette35c === true,
        );
        const rader: TableRow[] = perioder.map((p) => ({
          skipBorderBottom: true,
          columns: [
            ...(vurderUgyldighet
              ? [
                  {
                    content: p.klageOmgjøringDetaljer?.kanOpprette35c
                      ? p.klageOmgjøringDetaljer?.skalOpprette35c
                        ? ord.ja
                        : ord.nei
                      : "",
                  },
                ]
              : []),
            { content: formater(p.periode, "periode") },
            { content: p.delvedtakstypeVisningsnavn },
            {
              content: p.erOpphør
                ? visningForSaksbehandler
                  ? te.opphørForSaksbehandler
                  : ord.opphør
                : formater(p.faktiskBidrag, "beløpAvrundet"),
            },
            {
              content: visningForSaksbehandler
                ? p.resultatkodeVisningsnavn
                : TOM_KOLONNE.content,
            },
          ],
        }));
        return (
          <>
            <BarnOgIndeksår resultat={r} visIndeksår />
            <CommonTable
              data={{
                headers: overskrifterEndelig(
                  vurderUgyldighet,
                  visningForSaksbehandler,
                ),
                rows: visningForSaksbehandler
                  ? rader.concat(radForklaring())
                  : rader,
              }}
            />
          </>
        );
      })}
    </>
  );
}
