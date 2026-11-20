import { Fragment, ReactNode } from "react";
import {
  DokumentmalResultatBidragsberegningBarnDto,
  ResultatBarnebidragsberegningPeriodeDto,
  Samvaersklasse,
} from "~/types/Api";
import {
  useNotatFelles,
  useVedleggNummer,
} from "~/components/notat_felles/NotatContext";
import { VedtakFattetDetaljer } from "~/routes/notat/begrunnelse/felles/VedtakFattetDetaljer";
import { sortByAge } from "~/utils/date-utils";
import elementIds from "~/utils/elementIds";
import { groupBy } from "~/utils/array-utils";
import {
  DokumentTabell,
  type Kolonne,
  Tabellcelle,
  Tabellrad,
} from "~/components/DokumentTabell";
import TableGjelderBarn from "~/components/TableGjelderBarn";
import { FeltTabell, Feltrad, Feltrader } from "~/components/FeltTabell";
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
import { tekst } from "~/utils/tekst";
const tabellInnkreving: Tabelloppsett = {
  kolonner: [
    {
      felt: "periode",
      tittel: tekst("ord.periode", "Periode"),
      bredde: "170px",
      format: "periodeTilOgMedTilMåned",
    },
    {
      felt: "faktiskBidrag",
      tittel: tekst("ord.beløp", "Beløp"),
      bredde: "150px",
      format: "beløpAvrundet",
    },
    {
      felt: "resultatkodeVisningsnavn",
      tittel: tekst("ord.resultat", "Resultat"),
      bredde: "250px",
    },
  ],
};
const tabellAvslag: Tabelloppsett = {
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
      felt: "resultatkodeVisningsnavn",
      tittel: tekst("ord.årsak", "Årsak"),
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
      tittel: tekst(
        "notat.begrunnelse.bidrag.vedtak.tabellBeregning.underholdskostnad",
        "U",
      ),
      bredde: "50px",
      format: "beløpAvrundet",
    },
    {
      felt: "bpsAndel",
      tittel: tekst(
        "notat.begrunnelse.bidrag.vedtak.tabellBeregning.bpsAndel",
        "BPs andel U",
      ),
      bredde: "120px",
    },
    {
      felt: "samvær",
      tittel: tekst(
        "notat.begrunnelse.bidrag.vedtak.tabellBeregning.samvær",
        "Samvær",
      ),
      bredde: "100px",
    },
    {
      felt: "evne",
      tittel: tekst(
        "notat.begrunnelse.bidrag.vedtak.tabellBeregning.evne",
        "Evne / 25%",
      ),
      bredde: "100px",
    },
    {
      felt: "beregnetBidrag",
      tittel: tekst(
        "notat.begrunnelse.bidrag.vedtak.tabellBeregning.beregnetBidrag",
        "Beregnet bidrag",
      ),
      bredde: "80px",
      format: "beløpAvrundet",
    },
    {
      felt: "faktiskBidrag",
      tittel: tekst(
        "notat.begrunnelse.bidrag.vedtak.tabellBeregning.endeligBidrag",
        "Endelig bidrag",
      ),
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
    if (resultat.length == 0)
      return <div>{tekst("ord.manglerResultat", "Mangler resultat")}</div>;
    if (erInnkrevingsgrunnlag) return <VedtakInnkreving resultat={resultat} />;
    if (erAvslag) return <VedtakAvslag resultat={resultat} />;
    return <VedtakBeregning resultat={resultat} />;
  }
  return (
    <>
      <div className={"elements_inline section-title break-before-page"}>
        <h2 className={"section-title"}>
          {data.erOrkestrertVedtak
            ? tekst(
                "notat.begrunnelse.bidrag.vedtak.tittelKlagevedtak",
                "Klagevedtak",
              )
            : tekst("notat.begrunnelse.bidrag.vedtak.tittel", "Vedtak")}
        </h2>
        {!erAvslag && !erInnkrevingsgrunnlag && (
          <a href={`#${elementIds.vedleggBeregningsdetaljer}`}>
            {tekst(
              "ord.seVedleggForBeregningsdetaljer",
              "se vedlegg nr. {} for beregningsdetaljer",
            ).replace("{}", `${vedleggNummer}`)}
          </a>
        )}
      </div>
      {renderTabell()}
      {data.erOrkestrertVedtak && (
        <>
          <div className={"elements_inline section-title break-before-page"}>
            <h2 className={"section-title"}>
              {tekst(
                "notat.begrunnelse.bidrag.vedtak.tittelEndeligVedtak",
                "Endelig vedtak",
              )}
            </h2>
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
      ? tekst("ord.opphør", "Opphør")
      : tekst("ord.avslag", "Avslag");
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
    return (
      <div>
        {tekst(
          "notat.begrunnelse.bidrag.vedtak.ingenPerioderSlåttUtTilFF",
          "Ingen perioder har slått ut til FF. Det vil derfor ikke fattes noe vedtak for revurderingsbarnet.",
        )}
      </div>
    );
  }
  if (resultat.erAvvisning) {
    return (
      <div>
        {tekst(
          "notat.begrunnelse.bidrag.vedtak.avvisning",
          "Vedtak er avslag på behandling og har derfor ingen perioder",
        )}
      </div>
    );
  }
  return <>{children}</>;
}
function OverstyrFatteVedtakRevurderingsbarnInformasjon() {
  const { data } = useNotatFelles();
  const { vedtak } = data;
  if (!vedtak.kanFatteVedtakForRevurderingsbarn) return null;
  return (
    <div>
      <h2 className={"section-title"}>
        {tekst(
          "notat.begrunnelse.bidrag.revurderingsbarn.tittel",
          "Vedtak for revurderingsbarn",
        )}
      </h2>
      {vedtak.skalFatteVedtakForRevurderingsbarn
        ? tekst(
            "notat.begrunnelse.bidrag.revurderingsbarn.foreslåttFatteVedtak",
            "Basert på informasjonen som ble lagt inn foreslo beregningen å fatte vedtak for revurderingsbarn.",
          )
        : tekst(
            "notat.begrunnelse.bidrag.revurderingsbarn.foreslåttIkkeFatteVedtak",
            "Basert på informasjonen som ble lagt inn foreslo beregningen å ikke fatte vedtak for revurderingsbarn.",
          )}

      {!!vedtak.manueltOverstyrtFatteVedtakRevurderingsbarnBegrunnelse && (
        <div className={"mt-2"}>
          {vedtak.skalFatteVedtakForRevurderingsbarn
            ? tekst(
                "notat.begrunnelse.bidrag.revurderingsbarn.overstyrtTilIkkeFatteVedtak",
                "Det ble manuelt overstyrt til å ikke fatte vedtak for revurderingsbarn",
              )
            : tekst(
                "notat.begrunnelse.bidrag.revurderingsbarn.overstyrtTilFatteVedtak",
                "Det ble manuelt overstyrt til å fatte vedtak for revurderingsbarn",
              )}
          <div>
            <h5 style={{ margin: 0 }}>
              {tekst(
                "notat.begrunnelse.bidrag.revurderingsbarn.begrunnelse",
                "Begrunnelse:",
              )}
            </h5>
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
    ? tekst(
        "notat.begrunnelse.bidrag.vedtak.tabellBeregning.samværsklasseDeltBosted",
        "D",
      )
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
function ForklaringRad({ index }: { index: number }) {
  return (
    <Tabellrad
      index={index}
      skipBorderBottom
      zebraStripe={false}
      skipPadding
      className="pt-2"
    >
      <Tabellcelle fullSpan>
        {tekst(
          "notat.begrunnelse.bidrag.vedtak.tabellBeregning.forklaringForkortelser",
          "U = Underholdskostnad, BP = Bidragspliktig, BM = Bidragsmottaker",
        )}
      </Tabellcelle>
    </Tabellrad>
  );
}
function Beregningsrader({
  perioder,
  avslagstekst,
}: {
  perioder: Periode[];
  avslagstekst: string;
}) {
  const antallKolonnerFørResultat = tabellBeregning.kolonner.length - 1;
  return (
    <>
      {perioder.map((periode, periodeIndex) => {
        const periodeverdi = periodelinje(tabellBeregning, periode);
        const radIndex = periodeIndex * 2;
        return (
          <Fragment key={periodeIndex}>
            {periodeverdi && (
              <Tabellrad index={1} periode skipBorderBottom>
                <Tabellcelle fullSpan>{periodeverdi}</Tabellcelle>
              </Tabellrad>
            )}
            <Tabellrad
              index={radIndex}
              skipBorderBottom
              harPeriode={!!periodeverdi}
            >
              {periode.erAvslagUtenBeløp ? (
                <>
                  <Tabellcelle colSpan={antallKolonnerFørResultat} />
                  <Tabellcelle>{avslagstekst}</Tabellcelle>
                </>
              ) : (
                kolonneceller(tabellBeregning, periode, beregningsceller).map(
                  (verdi, celleIndex) => (
                    <Tabellcelle key={celleIndex}>{verdi}</Tabellcelle>
                  ),
                )
              )}
            </Tabellrad>
            <Tabellrad index={radIndex + 1} zebraStripe={false} skipPadding>
              <Tabellcelle colSpan={8}>
                <div className={"pl-0 ml-0"}>
                  <FeltTabell>
                    <Feltrad
                      label={tekst("ord.resultat", "Resultat")}
                      labelBold
                    >
                      {periode.resultatkodeVisningsnavn}
                    </Feltrad>
                  </FeltTabell>
                </div>
              </Tabellcelle>
            </Tabellrad>
          </Fragment>
        );
      })}
      <ForklaringRad index={perioder.length * 2} />
    </>
  );
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
        <div className={"mb-1 mt-2"}>
          <FeltTabell>
            <Feltrader
              rows={[
                visIndeksår
                  ? {
                      label: tekst(
                        "notat.begrunnelse.bidrag.vedtak.nesteIndeksår",
                        "Neste indeksår",
                      ),
                      value: resultat.indeksår,
                    }
                  : {},
              ]}
            />
          </FeltTabell>
        </div>
      )}
      {gjelderBarn.innbetaltBeløp && (
        <div className="mt-2 mb-2">
          <FeltTabell>
            <Feltrad
              label={tekst(
                "notat.begrunnelse.bidrag.vedtak.innbetaltBeløp",
                "Innbetalt beløp",
              )}
              labelBold
            >
              {gjelderBarn.innbetaltBeløp}
            </Feltrad>
          </FeltTabell>
        </div>
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
              <DokumentTabell kolonner={kolonneoverskrifter(tabellBeregning)}>
                <Beregningsrader
                  perioder={r.perioder}
                  avslagstekst={avslagstekst(r)}
                />
              </DokumentTabell>
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
): Kolonne[] {
  const bredde = (saksbehandler: string, brev: string) =>
    visningForSaksbehandler ? saksbehandler : brev;
  return [
    ...(vurderUgyldighet
      ? [
          {
            tittel: tekst(
              "notat.begrunnelse.bidrag.vedtak.tabellEndelig.vurderUgyldighet",
              "Vurder ugyldighet",
            ),
            bredde: "20px",
          },
        ]
      : []),
    { tittel: tekst("ord.periode", "Periode"), bredde: bredde("120px", "30%") },
    {
      tittel: visningForSaksbehandler
        ? tekst("ord.type", "Type")
        : tekst("ord.årsak", "Årsak"),
      bredde: bredde("100px", "40%"),
    },
    { tittel: tekst("ord.beløp", "Beløp"), bredde: bredde("120px", "30%") },
    visningForSaksbehandler
      ? { tittel: tekst("ord.resultat", "Resultat"), bredde: "130px" }
      : { tittel: TOM_KOLONNE.name },
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
        return (
          <>
            <BarnOgIndeksår resultat={r} visIndeksår />
            <DokumentTabell
              kolonner={overskrifterEndelig(
                vurderUgyldighet,
                visningForSaksbehandler,
              )}
            >
              {perioder.map((periode, index) => (
                <Tabellrad key={index} index={index} skipBorderBottom>
                  {vurderUgyldighet && (
                    <Tabellcelle>
                      {periode.klageOmgjøringDetaljer?.kanOpprette35c
                        ? periode.klageOmgjøringDetaljer?.skalOpprette35c
                          ? tekst("ord.ja", "Ja")
                          : tekst("ord.nei", "Nei")
                        : ""}
                    </Tabellcelle>
                  )}
                  <Tabellcelle>
                    {formater(periode.periode, "periode")}
                  </Tabellcelle>
                  <Tabellcelle>
                    {periode.delvedtakstypeVisningsnavn}
                  </Tabellcelle>
                  <Tabellcelle>
                    {periode.erOpphør
                      ? visningForSaksbehandler
                        ? tekst(
                            "notat.begrunnelse.bidrag.vedtak.tabellEndelig.opphørForSaksbehandler",
                            "-",
                          )
                        : tekst("ord.opphør", "Opphør")
                      : formater(periode.faktiskBidrag, "beløpAvrundet")}
                  </Tabellcelle>
                  <Tabellcelle>
                    {visningForSaksbehandler
                      ? periode.resultatkodeVisningsnavn
                      : TOM_KOLONNE.content}
                  </Tabellcelle>
                </Tabellrad>
              ))}
              {visningForSaksbehandler && (
                <ForklaringRad index={perioder.length} />
              )}
            </DokumentTabell>
          </>
        );
      })}
    </>
  );
}
