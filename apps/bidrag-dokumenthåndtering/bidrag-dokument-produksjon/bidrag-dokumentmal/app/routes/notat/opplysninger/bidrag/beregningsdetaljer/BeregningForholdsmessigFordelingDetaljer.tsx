import { useBeregningDetaljer } from "~/routes/notat/opplysninger/bidrag/VedleggBeregningsDetaljer";
import {
  formatterBeløpForBeregning,
  formatterProsent,
  formatterBeløp,
} from "~/utils/visningsnavn";
import { FeltTabell, Feltrader } from "~/components/FeltTabell";
import {
  DokumentTabell,
  Tabellcelle,
  Tabellrad,
} from "~/components/DokumentTabell";
import Person from "~/components/Person";
import DokumentStabel from "~/components/DokumentStabel";
import { BeregningBarn } from "~/types/commonTypes";
import { BeregnetBidragBarnDto } from "~/types/Api";
import { tekst } from "~/utils/tekst";
export const BeregningForholdsmessigFordeling = () => {
  const { forholdsmessigFordelingBeregningsdetaljer: forholdsmessigFordeling } =
    useBeregningDetaljer();
  if (!forholdsmessigFordeling) return null;

  const visning = velgForholdsmessigFordelingVisning(forholdsmessigFordeling);

  if (visning === "revurdering-med-søknadsbarn") {
    return (
      <DokumentStabel gap={3} className="mt-2">
        <div className="border border-gray-200 rounded">
          <BeregningForholdsmessigFordelingRevurdering
            overskrift={
              forholdsmessigFordeling.beregningFordelingAvBidrag
                .finnesBarnMedLøpendeBidragSomIkkeErSøknadsbarn
                ? tekst(
                    "notat.opplysninger.bidrag.beregningsdetaljer.forholdsmessigFordeling.overskrifter.vurderingRevurderingsbarnOgAndreBarn",
                    "Vurdering av revurderingsbarn og andre barn mot beløpshistorikk",
                  )
                : tekst(
                    "notat.opplysninger.bidrag.beregningsdetaljer.forholdsmessigFordeling.overskrifter.vurderingRevurderingsbarn",
                    "Vurdering av revurderingsbarn mot beløpshistorikk",
                  )
            }
          />
        </div>

        <div className="border border-gray-200 rounded">
          <BeregningForholdsmessigFordelingSøknadsbarn
            overskrift={tekst(
              "notat.opplysninger.bidrag.beregningsdetaljer.forholdsmessigFordeling.overskrifter.beregningSøknadsbarnOgRevurderingsbarn",
              "Beregning for søknadsbarn og revurderingsbarn",
            )}
          />
        </div>
      </DokumentStabel>
    );
  }
  if (visning === "revurdering") {
    return <BeregningForholdsmessigFordelingRevurdering />;
  }
  return <BeregningForholdsmessigFordelingSøknadsbarn />;
};

type ForholdsmessigFordeling = NonNullable<
  ReturnType<
    typeof useBeregningDetaljer
  >["forholdsmessigFordelingBeregningsdetaljer"]
>;

function velgForholdsmessigFordelingVisning(
  forholdsmessigFordeling: ForholdsmessigFordeling,
): "revurdering-med-søknadsbarn" | "revurdering" | "søknadsbarn" {
  const fordelinger =
    forholdsmessigFordeling.beregningFordelingAvBidrag.bidragTilFordelingAlle;
  const inneholderRevurderingsbarn = fordelinger.some(
    ({ barn }) => barn.revurdering,
  );
  const harSøknadsbarnMedRevurdering = fordelinger.some(
    ({ barn, erSøknadsbarn }) => erSøknadsbarn && barn.revurdering,
  );

  if (harSøknadsbarnMedRevurdering && !forholdsmessigFordeling.harBPFullEvne) {
    return "revurdering-med-søknadsbarn";
  }
  if (
    inneholderRevurderingsbarn &&
    forholdsmessigFordeling.beregningFordelingAvBidragSjekkEvnesprekk
  ) {
    return "revurdering";
  }
  return "søknadsbarn";
}
export const BeregningForholdsmessigFordelingRevurdering = ({
  overskrift,
}: {
  overskrift?: string;
}) => {
  const {
    periode: { erSistePeriode },
    delberegningBidragsevne,
    kanFatteVedtakForRevurderingsbarn,
    forholdsmessigFordelingBeregningsdetaljer: forholdsmessigFordeling,
  } = useBeregningDetaljer();
  if (!forholdsmessigFordeling.beregningFordelingAvBidragSjekkEvnesprekk) {
    return overskrift ? (
      <DokumentStabel gap={1}>
        <h3>{overskrift}</h3>
      </DokumentStabel>
    ) : null;
  }
  const beregningFordelingAvBidrag =
    forholdsmessigFordeling.beregningFordelingAvBidragSjekkEvnesprekk!;
  const bpsSumAndelAvU = beregningFordelingAvBidrag.sumBidragTilFordeling ?? 0;
  const sumAndreBarn =
    beregningFordelingAvBidrag.sumBidragTilFordelingIkkeSøknadsbarn +
    beregningFordelingAvBidrag.sumBidragTilFordelingPrivatAvtale;
  const finnesPriorierteBidrag =
    beregningFordelingAvBidrag.sumBidragSomIkkeKanFordeles > 0;
  const evne = Math.min(
    delberegningBidragsevne.bidragsevne,
    delberegningBidragsevne.sumInntekt25Prosent,
  );
  const erNokEvne = evne >= beregningFordelingAvBidrag.sumBidragTilFordeling;
  function renderFFBeregning() {
    if (!kanFatteVedtakForRevurderingsbarn) {
      return (
        <>
          {forholdsmessigFordeling.beregningFordelingAvBidrag
            .finnesBarnMedLøpendeBidragSomIkkeErSøknadsbarn && (
            <div className="pb-2">
              {tekst(
                "notat.opplysninger.bidrag.beregningsdetaljer.forholdsmessigFordeling.forholdsmessigFordeling.tittel",
                "Forholdsmessig fordeling",
              ) && (
                <h4>
                  {tekst(
                    "notat.opplysninger.bidrag.beregningsdetaljer.forholdsmessigFordeling.forholdsmessigFordeling.tittel",
                    "Forholdsmessig fordeling",
                  )}
                </h4>
              )}
              <FeltTabell>
                <Feltrader
                  rows={[
                    forholdsmessigFordeling.beregningFordelingAvBidrag
                      .finnesBarnMedLøpendeBidragSomIkkeErSøknadsbarn
                      ? {
                          label: tekst(
                            "notat.opplysninger.bidrag.beregningsdetaljer.forholdsmessigFordeling.forholdsmessigFordeling.bpsTotaleUnderholdskostnad",
                            "BPs totale underholdskostnad",
                          ),
                          labelBold: false,
                          value: `${formatterBeløpForBeregning(sumAndreBarn)} + ${formatterBeløpForBeregning(beregningFordelingAvBidrag.sumBidragTilFordelingSøknadsbarn)}`,
                          result: `${formatterBeløpForBeregning(beregningFordelingAvBidrag.sumBidragTilFordeling)}`,
                        }
                      : {},
                    finnesPriorierteBidrag
                      ? {
                          label: tekst(
                            "notat.opplysninger.bidrag.beregningsdetaljer.forholdsmessigFordeling.forholdsmessigFordeling.bpsEvneSomKanFordeles",
                            "BPs evne som kan fordeles",
                          ),
                          labelBold: false,
                          value: `${formatterBeløpForBeregning(delberegningBidragsevne.bidragsevne)} - ${formatterBeløpForBeregning(beregningFordelingAvBidrag.sumPrioriterteBidragTilFordeling)}`,
                          result: `${formatterBeløpForBeregning(forholdsmessigFordeling.evneJustertForPrioriterteBidrag)}`,
                        }
                      : {},
                  ]}
                />
              </FeltTabell>
            </div>
          )}
          <p className="mt-2">
            {tekst(
              "notat.opplysninger.bidrag.beregningsdetaljer.forholdsmessigFordeling.evnevurdering.tilstrekkeligIngenVedtakForRevurderingsbarn",
              "Evnen på {evne} er tilstrekkelig for å dekke total andel av U på {totalAndel}. Det vil derfor ikke fattes vedtak for revurderingsbarn.",
            )
              .replace("{evne}", formatterBeløpForBeregning(evne))
              .replace(
                "{totalAndel}",
                formatterBeløpForBeregning(
                  beregningFordelingAvBidrag.sumBidragTilFordeling,
                ),
              )}
          </p>
        </>
      );
    }
    return (
      <div>
        <div>
          {tekst(
            "notat.opplysninger.bidrag.beregningsdetaljer.forholdsmessigFordeling.forholdsmessigFordeling.tittel",
            "Forholdsmessig fordeling",
          ) && (
            <h4>
              {tekst(
                "notat.opplysninger.bidrag.beregningsdetaljer.forholdsmessigFordeling.forholdsmessigFordeling.tittel",
                "Forholdsmessig fordeling",
              )}
            </h4>
          )}
          <FeltTabell>
            <Feltrader
              rows={[
                !forholdsmessigFordeling
                  ? {
                      label: tekst(
                        "notat.opplysninger.bidrag.beregningsdetaljer.forholdsmessigFordeling.forholdsmessigFordeling.bpsTotaleUnderholdskostnad",
                        "BPs totale underholdskostnad",
                      ),
                      labelBold: false,
                      value: `${formatterBeløpForBeregning(bpsSumAndelAvU)}`,
                    }
                  : {},
                {
                  label: tekst(
                    "notat.opplysninger.bidrag.beregningsdetaljer.forholdsmessigFordeling.forholdsmessigFordeling.bpsTotaleUnderholdskostnad",
                    "BPs totale underholdskostnad",
                  ),
                  labelBold: false,
                  value: `${formatterBeløpForBeregning(sumAndreBarn)} + ${formatterBeløpForBeregning(beregningFordelingAvBidrag.sumBidragTilFordelingSøknadsbarn)}`,
                  result: `${formatterBeløpForBeregning(beregningFordelingAvBidrag.sumBidragTilFordeling)}`,
                },
              ]}
            />
          </FeltTabell>
        </div>
        <p className="mt-2">
          {!erNokEvne
            ? `${tekst("notat.opplysninger.bidrag.beregningsdetaljer.forholdsmessigFordeling.evnevurdering.ikkeTilstrekkelig", "Evnen på {evne} er ikke tilstrekkelig for å dekke total andel av U på {totalAndel}.").replace("{evne}", formatterBeløpForBeregning(evne)).replace("{totalAndel}", formatterBeløpForBeregning(beregningFordelingAvBidrag.sumBidragTilFordeling))}${erSistePeriode ? tekst("notat.opplysninger.bidrag.beregningsdetaljer.forholdsmessigFordeling.evnevurdering.anbefalingFatteVedtak", " Det anbefales derfor å fatte vedtak for revurderingsbarn.") : ""}`
            : `${tekst("notat.opplysninger.bidrag.beregningsdetaljer.forholdsmessigFordeling.evnevurdering.tilstrekkelig", "Evnen på {evne} er tilstrekkelig for å dekke total andel av U på {totalAndel}.").replace("{evne}", formatterBeløpForBeregning(evne)).replace("{totalAndel}", formatterBeløpForBeregning(beregningFordelingAvBidrag.sumBidragTilFordeling))}${erSistePeriode ? tekst("notat.opplysninger.bidrag.beregningsdetaljer.forholdsmessigFordeling.evnevurdering.anbefalingIkkeFatteVedtak", " Det anbefales derfor å ikke fatte vedtak for revurderingsbarn.") : ""}`}
        </p>
      </div>
    );
  }
  const innhold = (
    <>
      <ForholdsmessigFordelingBeregningAndreBarn
        nyBeregningRevurdering={true}
      />
      <ForholdsmessigFordelingSøknadsbarn nyBeregningRevurdering={true} />
      {renderFFBeregning()}
    </>
  );
  return overskrift ? (
    <DokumentStabel gap={1}>
      <h3>{overskrift}</h3>
      {innhold}
    </DokumentStabel>
  ) : (
    innhold
  );
};
export const ForholdsmessigFordelingSøknadsbarn = ({
  nyBeregningRevurdering = false,
}: {
  nyBeregningRevurdering?: boolean;
}) => {
  const { forholdsmessigFordelingBeregningsdetaljer: forholdsmessigFordeling } =
    useBeregningDetaljer();
  const sumFordeling = nyBeregningRevurdering
    ? forholdsmessigFordeling?.beregningFordelingAvBidragSjekkEvnesprekk
    : forholdsmessigFordeling?.beregningFordelingAvBidrag;
  if (!sumFordeling || sumFordeling.bidragTilFordelingAlle?.length === 0)
    return null;
  const søknadsbarn = sumFordeling.bidragTilFordelingAlle.filter(
    (b) => b.erSøknadsbarn,
  );
  const finnesBarnMedLøpendeBidragSomIkkeErSøknadsbarn =
    forholdsmessigFordeling.beregningFordelingAvBidrag
      .finnesBarnMedLøpendeBidragSomIkkeErSøknadsbarn;
  return (
    <div
      className={finnesBarnMedLøpendeBidragSomIkkeErSøknadsbarn ? "mt-2" : ""}
    >
      <h4>
        {finnesBarnMedLøpendeBidragSomIkkeErSøknadsbarn
          ? tekst(
              "notat.opplysninger.bidrag.beregningsdetaljer.forholdsmessigFordeling.søknadsbarn.tittelNårAndreBarnHarLøpendeBidrag",
              "BPs totale underholdskostnad for søknadsbarn",
            )
          : tekst(
              "notat.opplysninger.bidrag.beregningsdetaljer.forholdsmessigFordeling.søknadsbarn.tittel",
              "BPs totale underholdskostnad",
            )}
      </h4>
      <DokumentTabell
        layoutAuto
        kolonner={[
          { tittel: tekst("ord.barn", "Barn"), bredde: "500px" },
          {
            tittel: tekst(
              "notat.opplysninger.bidrag.beregningsdetaljer.forholdsmessigFordeling.søknadsbarn.kolonner.andelU",
              "Andel U",
            ),
            bredde: "50px",
          },
        ]}
      >
        {søknadsbarn.map((b, index) => (
          <Tabellrad key={b.barn.ident} index={index}>
            <Tabellcelle colSpan={1}>
              <Person
                fødselsdato={b.barn.fødselsdato!}
                navn={b.barn.navn!}
                erBeskyttet={b.barn.erBeskyttet}
              />
            </Tabellcelle>
            <Tabellcelle>
              {formatterBeløpForBeregning(b.bidragTilFordeling, true)}
            </Tabellcelle>
          </Tabellrad>
        ))}
        <Tabellrad index={søknadsbarn.length}>
          <Tabellcelle labelBold>{tekst("ord.sum", "Sum")}</Tabellcelle>
          <Tabellcelle>
            {formatterBeløpForBeregning(
              sumFordeling.sumBidragTilFordelingSøknadsbarn,
              true,
            )}
          </Tabellcelle>
        </Tabellrad>
      </DokumentTabell>
    </div>
  );
};
export const BeregningForholdsmessigFordelingSøknadsbarn = ({
  overskrift,
}: {
  overskrift?: string;
}) => {
  const {
    sluttberegning,
    bpsAndel,
    delberegningBidragsevne,
    forholdsmessigFordelingBeregningsdetaljer: forholdsmessigFordeling,
  } = useBeregningDetaljer();
  if (!forholdsmessigFordeling) return null;
  const erRedusertEvne =
    sluttberegning.bidragJustertNedTilEvne ||
    sluttberegning.bidragJustertNedTil25ProsentAvInntekt;
  function renderResult() {
    if (sluttberegning.bidragJustertNedTil25ProsentAvInntekt) {
      return tekst(
        "notat.opplysninger.bidrag.beregningsdetaljer.forholdsmessigFordeling.forholdsmessigFordeling.redusertNedTil25ProsentAvInntekt",
        " (redusert ned til 25% av inntekt)",
      );
    } else if (sluttberegning.bidragJustertNedTilEvne) {
      return tekst(
        "notat.opplysninger.bidrag.beregningsdetaljer.forholdsmessigFordeling.forholdsmessigFordeling.redusertNedTilEvne",
        " (redusert ned til evne)",
      );
    }
    return "";
  }
  const bpAndelAvUVedForholdsmessigFordelingFaktor =
    sluttberegning.bpAndelAvUVedForholdsmessigFordelingFaktor ??
    forholdsmessigFordeling.andelAvSumBidragTilFordelingFaktor;
  const bpEvneVedForholdsmessigFordeling =
    forholdsmessigFordeling.andelAvEvneBeløp ??
    sluttberegning.bpEvneVedForholdsmessigFordeling;
  const sumFordeling = forholdsmessigFordeling.beregningFordelingAvBidrag;
  const foreløpigBidrag =
    forholdsmessigFordeling.bidragEtterFordeling ??
    sluttberegning.bruttoBidragJustertForEvneOg25Prosent ??
    0;
  const bpsSumAndelAvU = sumFordeling.sumBidragTilFordeling ?? 0;
  const andelFordeltTilBarnet =
    forholdsmessigFordeling.bidragTilFordelingForBarnet ??
    bpsAndel.andelBeløp ??
    0;
  const sumAndreBarn =
    sumFordeling.sumBidragTilFordelingIkkeSøknadsbarn +
    sumFordeling.sumBidragTilFordelingPrivatAvtale;
  const finnesPriorierteBidrag = sumFordeling.sumBidragSomIkkeKanFordeles > 0;
  const bidragTilFordelingMinusUtlandsbidrag =
    sumFordeling.sumBidragTilFordeling -
    sumFordeling.sumBidragSomIkkeKanFordeles;
  const evne = Math.min(
    delberegningBidragsevne.bidragsevne,
    delberegningBidragsevne.sumInntekt25Prosent,
  );
  function renderFFBeregning() {
    if (!erRedusertEvne) {
      return (
        <>
          {forholdsmessigFordeling.beregningFordelingAvBidrag
            .finnesBarnMedLøpendeBidragSomIkkeErSøknadsbarn && (
            <div>
              {tekst(
                "notat.opplysninger.bidrag.beregningsdetaljer.forholdsmessigFordeling.forholdsmessigFordeling.tittel",
                "Forholdsmessig fordeling",
              ) && (
                <h4>
                  {tekst(
                    "notat.opplysninger.bidrag.beregningsdetaljer.forholdsmessigFordeling.forholdsmessigFordeling.tittel",
                    "Forholdsmessig fordeling",
                  )}
                </h4>
              )}
              <FeltTabell>
                <Feltrader
                  rows={[
                    forholdsmessigFordeling.beregningFordelingAvBidrag
                      .finnesBarnMedLøpendeBidragSomIkkeErSøknadsbarn
                      ? {
                          label: tekst(
                            "notat.opplysninger.bidrag.beregningsdetaljer.forholdsmessigFordeling.forholdsmessigFordeling.bpsTotaleUnderholdskostnad",
                            "BPs totale underholdskostnad",
                          ),
                          labelBold: false,
                          value: `${formatterBeløpForBeregning(sumAndreBarn)} + ${formatterBeløpForBeregning(sumFordeling.sumBidragTilFordelingSøknadsbarn)}`,
                          result: `${formatterBeløpForBeregning(sumFordeling.sumBidragTilFordeling)}`,
                        }
                      : {},
                    finnesPriorierteBidrag
                      ? {
                          label: tekst(
                            "notat.opplysninger.bidrag.beregningsdetaljer.forholdsmessigFordeling.forholdsmessigFordeling.bpsEvneSomKanFordeles",
                            "BPs evne som kan fordeles",
                          ),
                          labelBold: false,
                          value: `${formatterBeløpForBeregning(delberegningBidragsevne.bidragsevne)} - ${formatterBeløpForBeregning(sumFordeling.sumPrioriterteBidragTilFordeling)}`,
                          result: `${formatterBeløpForBeregning(forholdsmessigFordeling.evneJustertForPrioriterteBidrag)}`,
                        }
                      : {},
                  ]}
                />
              </FeltTabell>
            </div>
          )}
          <p className="mt-2">
            {tekst(
              "notat.opplysninger.bidrag.beregningsdetaljer.forholdsmessigFordeling.evnevurdering.tilstrekkeligSøknadsbarn",
              "Evnen på {evne} er tilstrekkelig for å dekke total andel av U på {totalAndel}",
            )
              .replace("{evne}", formatterBeløpForBeregning(evne))
              .replace(
                "{totalAndel}",
                formatterBeløpForBeregning(sumFordeling.sumBidragTilFordeling),
              )}
          </p>
        </>
      );
    }
    return (
      <div>
        {tekst(
          "notat.opplysninger.bidrag.beregningsdetaljer.forholdsmessigFordeling.forholdsmessigFordeling.tittel",
          "Forholdsmessig fordeling",
        ) && (
          <h4>
            {tekst(
              "notat.opplysninger.bidrag.beregningsdetaljer.forholdsmessigFordeling.forholdsmessigFordeling.tittel",
              "Forholdsmessig fordeling",
            )}
          </h4>
        )}
        <FeltTabell>
          <Feltrader
            rows={[
              !forholdsmessigFordeling
                ? {
                    label: tekst(
                      "notat.opplysninger.bidrag.beregningsdetaljer.forholdsmessigFordeling.forholdsmessigFordeling.bpsTotaleUnderholdskostnad",
                      "BPs totale underholdskostnad",
                    ),
                    labelBold: false,
                    value: `${formatterBeløpForBeregning(bpsSumAndelAvU)}`,
                  }
                : {},
              forholdsmessigFordeling.beregningFordelingAvBidrag
                .finnesBarnMedLøpendeBidragSomIkkeErSøknadsbarn
                ? {
                    label: tekst(
                      "notat.opplysninger.bidrag.beregningsdetaljer.forholdsmessigFordeling.forholdsmessigFordeling.bpsTotaleUnderholdskostnad",
                      "BPs totale underholdskostnad",
                    ),
                    labelBold: false,
                    value: `${formatterBeløpForBeregning(sumAndreBarn)} + ${formatterBeløpForBeregning(sumFordeling.sumBidragTilFordelingSøknadsbarn)}`,
                    result: `${formatterBeløpForBeregning(sumFordeling.sumBidragTilFordeling)}`,
                  }
                : {},
              {
                label: tekst(
                  "notat.opplysninger.bidrag.beregningsdetaljer.forholdsmessigFordeling.forholdsmessigFordeling.barnetsAndelAvUnderholdskostnad",
                  "Barnets andel av underholdskostnad",
                ),
                labelBold: false,
                value: `${formatterBeløpForBeregning(andelFordeltTilBarnet)} / ${formatterBeløpForBeregning(bidragTilFordelingMinusUtlandsbidrag)}`,
                result: `${formatterProsent(bpAndelAvUVedForholdsmessigFordelingFaktor)}`,
              },
              finnesPriorierteBidrag
                ? {
                    label: tekst(
                      "notat.opplysninger.bidrag.beregningsdetaljer.forholdsmessigFordeling.forholdsmessigFordeling.bpsEvneSomKanFordeles",
                      "BPs evne som kan fordeles",
                    ),
                    labelBold: false,
                    value: `${formatterBeløpForBeregning(delberegningBidragsevne.bidragsevne)} - ${formatterBeløpForBeregning(sumFordeling.sumPrioriterteBidragTilFordeling)}`,
                    result: `${formatterBeløpForBeregning(forholdsmessigFordeling.evneJustertForPrioriterteBidrag)}`,
                  }
                : {},
              {
                label: tekst(
                  "notat.opplysninger.bidrag.beregningsdetaljer.forholdsmessigFordeling.forholdsmessigFordeling.barnetsAndelEtterForholdsmessigFordeling",
                  "Barnets andel etter forholdsmessig fordeling",
                ),
                labelBold: false,
                value: `${formatterProsent(bpAndelAvUVedForholdsmessigFordelingFaktor)} x ${formatterBeløpForBeregning(forholdsmessigFordeling.evneJustertForPrioriterteBidrag)}`,
                result: `${formatterBeløpForBeregning(bpEvneVedForholdsmessigFordeling)}`,
              },
              {
                label: tekst(
                  "notat.opplysninger.bidrag.beregningsdetaljer.forholdsmessigFordeling.forholdsmessigFordeling.foreløpigBidrag",
                  "Foreløpig bidrag",
                ),
                labelBold: false,
                value: ` ${formatterBeløpForBeregning(foreløpigBidrag)}${renderResult()}`,
              },
            ]}
          />
        </FeltTabell>
      </div>
    );
  }
  const innhold = (
    <>
      <ForholdsmessigFordelingBeregningAndreBarn />
      <ForholdsmessigFordelingSøknadsbarn />
      {renderFFBeregning()}
    </>
  );
  return overskrift ? (
    <DokumentStabel gap={1}>
      <h3>{overskrift}</h3>
      {innhold}
    </DokumentStabel>
  ) : (
    innhold
  );
};
const ForholdsmessigFordelingBeregningAndreBarn = ({
  nyBeregningRevurdering = false,
}: {
  nyBeregningRevurdering?: boolean;
}) => {
  const { forholdsmessigFordelingBeregningsdetaljer: forholdsmessigFordeling } =
    useBeregningDetaljer();
  const sumFordeling = nyBeregningRevurdering
    ? forholdsmessigFordeling?.beregningFordelingAvBidragSjekkEvnesprekk
    : forholdsmessigFordeling?.beregningFordelingAvBidrag;
  if (!sumFordeling || sumFordeling.bidragTilFordelingAlle?.length === 0)
    return null;
  const beregningBarn: BeregningBarn[] =
    sumFordeling.bidragTilFordelingAlle.flatMap((b) => ({
      beregnetBidragPerBarn: {
        ...b.beregnetBidrag,
        gjelderBarn: b.barn.ident,
      } as BeregnetBidragBarnDto,
      barn: b.barn,
      erSøknadsbarn: b.erSøknadsbarn,
      privatAvtale: b.privatAvtale,
      erBidragIkkeTilFordeling: b.erBidragSomIkkeKanFordeles,
      oppfostringsbidrag: b.oppfostringsbidrag,
      bidragTilFordeling: b.bidragTilFordeling,
      utenlandskbidrag: b.utenlandskbidrag,
    }));
  const bpsBarnIkkeSøknadsbarn = beregningBarn.filter(
    (b) => !b.erSøknadsbarn && (nyBeregningRevurdering || !b.barn.revurdering),
  );
  return (
    <div>
      <BpsPrivatAvtalerTabellIkkeTilFordeling
        beregning={bpsBarnIkkeSøknadsbarn}
        sumBidrag={sumFordeling.sumBidragSomIkkeKanFordeles}
      />
      <BpsBeregnedeTotalbidragTabell
        beregning={bpsBarnIkkeSøknadsbarn}
        bidragspliktigesBeregnedeTotalbidrag={
          sumFordeling.sumBidragTilFordelingIkkeSøknadsbarn
        }
        title={tekst(
          "notat.opplysninger.bidrag.beregningsdetaljer.forholdsmessigFordeling.beregnedeTotalbidrag.tittelAndreBarn",
          "BP's beregnede totalbidrag for andre barn",
        )}
      />

      <BpsPrivatAvtalerTabell
        beregning={bpsBarnIkkeSøknadsbarn}
        sumBidragPrivatAvtale={sumFordeling.sumBidragTilFordelingPrivatAvtale}
      />
    </div>
  );
};
export const BpsBeregnedeTotalbidragTabell = ({
  beregning,
  bidragspliktigesBeregnedeTotalbidrag,
  title = tekst(
    "notat.opplysninger.bidrag.beregningsdetaljer.forholdsmessigFordeling.beregnedeTotalbidrag.tittel",
    "BP's beregnede totalbidrag",
  ),
}: {
  beregning: BeregningBarn[];
  bidragspliktigesBeregnedeTotalbidrag: number;
  title?: string;
}) => {
  if (beregning.length === 0) return null;
  if (beregning.every((b) => b.erSøknadsbarn || b.privatAvtale)) return null;
  return (
    <div className="mb-2">
      <h4>{title}</h4>
      <DokumentTabell
        layoutAuto
        kolonner={[
          { tittel: tekst("ord.barn", "Barn"), bredde: "500px" },
          {
            tittel: tekst(
              "notat.opplysninger.bidrag.beregningsdetaljer.forholdsmessigFordeling.beregnedeTotalbidrag.kolonner.beregnetBidrag",
              "Beregnet bidrag",
            ),
            bredde: "50px",
          },
        ]}
      >
        {beregning
          .filter((b) => !b.privatAvtale)
          .map((b, index) => (
            <Tabellrad key={b.barn.ident} index={index}>
              <Tabellcelle colSpan={1}>
                <Person
                  fødselsdato={b.barn.fødselsdato!}
                  navn={b.barn.navn!}
                  erBeskyttet={b.barn.erBeskyttet}
                />
              </Tabellcelle>
              <Tabellcelle>
                {formatterBeløpForBeregning(b.bidragTilFordeling, true)}
              </Tabellcelle>
            </Tabellrad>
          ))}
        <Tabellrad index={beregning.filter((b) => !b.privatAvtale).length}>
          <Tabellcelle labelBold>{tekst("ord.sum", "Sum")}</Tabellcelle>
          <Tabellcelle>
            {formatterBeløpForBeregning(bidragspliktigesBeregnedeTotalbidrag)}
          </Tabellcelle>
        </Tabellrad>
      </DokumentTabell>
    </div>
  );
};
export const BpsPrivatAvtalerTabell = ({
  beregning,
  sumBidragPrivatAvtale,
}: {
  beregning: BeregningBarn[];
  sumBidragPrivatAvtale: number;
}) => {
  const privatAvtaleBarn = beregning.filter((b) => b.privatAvtale);
  if (privatAvtaleBarn.length === 0) return null;
  return (
    <div className="mb-2">
      <h4>
        {tekst(
          "notat.opplysninger.bidrag.beregningsdetaljer.forholdsmessigFordeling.privatAvtaler.tittel",
          "BP's private avtaler",
        )}
      </h4>
      <DokumentTabell
        layoutAuto
        kolonner={[
          { tittel: tekst("ord.barn", "Barn"), bredde: "500px" },
          { tittel: tekst("ord.beløp", "Beløp"), bredde: "50px" },
        ]}
      >
        {privatAvtaleBarn.map((b, index) => (
          <Tabellrad key={b.barn.ident} index={index}>
            <Tabellcelle colSpan={1}>
              <Person
                fødselsdato={b.barn.fødselsdato!}
                navn={b.barn.navn!}
                erBeskyttet={b.barn.erBeskyttet}
              />
            </Tabellcelle>
            <Tabellcelle>
              {formatterBeløpForBeregning(b.bidragTilFordeling, true)}
            </Tabellcelle>
          </Tabellrad>
        ))}
        <Tabellrad index={privatAvtaleBarn.length}>
          <Tabellcelle labelBold>{tekst("ord.sum", "Sum")}</Tabellcelle>
          <Tabellcelle>
            {formatterBeløpForBeregning(sumBidragPrivatAvtale)}
          </Tabellcelle>
        </Tabellrad>
      </DokumentTabell>
    </div>
  );
};
export const BpsPrivatAvtalerTabellIkkeTilFordeling = ({
  beregning,
  sumBidrag,
}: {
  beregning: BeregningBarn[];
  sumBidrag: number;
}) => {
  if (!beregning.some((b) => b.erBidragIkkeTilFordeling)) return null;
  const bidragIkkeTilFordeling = beregning.filter(
    (b) => b.erBidragIkkeTilFordeling,
  );
  if (bidragIkkeTilFordeling.length === 0) return null;
  const løperIUtlandskValuta = bidragIkkeTilFordeling.some(
    (b) => b.beregnetBidragPerBarn.valutakode !== "NOK",
  );
  const inneholderUtenlandsk = bidragIkkeTilFordeling.some(
    (b) => b.utenlandskbidrag,
  );
  const finnesIndeksregulering = bidragIkkeTilFordeling.some(
    (b) =>
      b.beregnetBidragPerBarn.indeksreguleringFaktor &&
      b.beregnetBidragPerBarn.indeksreguleringFaktor > 0,
  );
  function renderTablePrivatAvtale() {
    return (
      <DokumentTabell
        layoutAuto
        kolonner={[
          {
            tittel: tekst("ord.barn", "Barn"),
            bredde: "500px",
          },
          ...(finnesIndeksregulering
            ? [
                {
                  tittel: tekst(
                    "notat.opplysninger.bidrag.beregningsdetaljer.forholdsmessigFordeling.bidragSomIkkeKanFordeles.kolonner.indeks",
                    "Indeks",
                  ),
                  bredde: "50px",
                },
              ]
            : []),
          ...(inneholderUtenlandsk
            ? [
                {
                  tittel: tekst(
                    "notat.opplysninger.bidrag.beregningsdetaljer.forholdsmessigFordeling.bidragSomIkkeKanFordeles.kolonner.avtalebeløp",
                    "Avtalebeløp",
                  ),
                  bredde: "50px",
                },
                {
                  tittel: tekst(
                    "notat.opplysninger.bidrag.beregningsdetaljer.forholdsmessigFordeling.bidragSomIkkeKanFordeles.kolonner.valutakurs",
                    "Valutakurs",
                  ),
                  bredde: "50px",
                },
                {
                  tittel: tekst(
                    "notat.opplysninger.bidrag.beregningsdetaljer.forholdsmessigFordeling.bidragSomIkkeKanFordeles.kolonner.avtalebeløpNok",
                    "Avtalebeløp (NOK)",
                  ),
                  bredde: "50px",
                },
                {
                  tittel: tekst(
                    "notat.opplysninger.bidrag.beregningsdetaljer.forholdsmessigFordeling.bidragSomIkkeKanFordeles.kolonner.samvær",
                    "Samvær",
                  ),
                  bredde: "50px",
                },
              ]
            : []),
          {
            tittel: tekst("ord.beløp", "Beløp"),
            bredde: "50px",
          },
        ]}
      >
        {bidragIkkeTilFordeling.map(
          ({ beregnetBidragPerBarn: row, barn }, index) => (
            <Tabellrad key={barn.ident} index={index}>
              <Tabellcelle colSpan={1}>
                <Person
                  fødselsdato={barn.fødselsdato!}
                  navn={barn.navn!}
                  erBeskyttet={barn.erBeskyttet}
                />
              </Tabellcelle>
              {finnesIndeksregulering && (
                <Tabellcelle>
                  {formatterProsent(row.indeksreguleringFaktor)}
                </Tabellcelle>
              )}
              {inneholderUtenlandsk && (
                <Tabellcelle>
                  {row.valutakode === "NOK"
                    ? formatterBeløpForBeregning(row.løpendeBeløp)
                    : `${formatterBeløpForBeregning(row.løpendeBeløp)} (${row.valutakode})`}
                </Tabellcelle>
              )}
              {løperIUtlandskValuta && (
                <Tabellcelle>{formatterBeløp(row.valutakurs)}</Tabellcelle>
              )}
              {inneholderUtenlandsk && (
                <Tabellcelle>
                  {formatterBeløpForBeregning(row.beregnetBeløp)}
                </Tabellcelle>
              )}
              {inneholderUtenlandsk && (
                <Tabellcelle>
                  {formatterBeløpForBeregning(row.samværsfradrag)}
                </Tabellcelle>
              )}
              <Tabellcelle>
                {formatterBeløpForBeregning(row.beregnetBidrag, true)}
              </Tabellcelle>
            </Tabellrad>
          ),
        )}
        <Tabellrad index={bidragIkkeTilFordeling.length}>
          <Tabellcelle labelBold colSpanNegative={1}>
            {tekst("ord.sum", "Sum")}
          </Tabellcelle>
          <Tabellcelle>{formatterBeløpForBeregning(sumBidrag)}</Tabellcelle>
        </Tabellrad>
      </DokumentTabell>
    );
  }
  return (
    <div className={"mb-2"}>
      <div>
        <h4 className="inline-block align-middle">
          {tekst(
            "notat.opplysninger.bidrag.beregningsdetaljer.forholdsmessigFordeling.bidragSomIkkeKanFordeles.tittel",
            "BP's bidrag som ikke kan fordeles",
          )}
        </h4>

        {renderTablePrivatAvtale()}
      </div>
    </div>
  );
};
