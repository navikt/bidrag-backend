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
import ord from "~/tekster/ord.yaml";
import bidragOpplysningerTekster from "~/tekster/notat/opplysninger/bidrag.yaml";

const t = bidragOpplysningerTekster.beregningsdetaljer.forholdsmessigFordeling;

export const BeregningForholdsmessigFordeling = () => {
  const {
    sluttberegning,
    forholdsmessigFordelingBeregningsdetaljer: forholdsmessigFordeling,
  } = useBeregningDetaljer();

  if (!forholdsmessigFordeling) return null;

  const kanFatteVedtakForRevurderingsbarn =
    !forholdsmessigFordeling.harBPFullEvne;

  const inneholderRevurderingsbarn =
    forholdsmessigFordeling.beregningFordelingAvBidrag.bidragTilFordelingAlle.some(
      (b) => b.barn.revurdering,
    );

  const periodeInneholderRevurderingsbarn =
    forholdsmessigFordeling.beregningFordelingAvBidrag.bidragTilFordelingAlle.some(
      (r) => r.erSøknadsbarn && r.barn.revurdering,
    );

  const foreløpigBidragSøknadsbarn =
    forholdsmessigFordeling.bidragEtterFordeling ??
    sluttberegning.bruttoBidragJustertForEvneOg25Prosent ??
    0;

  if (periodeInneholderRevurderingsbarn && kanFatteVedtakForRevurderingsbarn) {
    return (
      <DokumentStabel gap={3} className="mt-2">
        <div className="border border-gray-200 rounded">
          <BeregningForholdsmessigFordelingRevurdering
            overskrift={
              forholdsmessigFordeling.beregningFordelingAvBidrag
                .finnesBarnMedLøpendeBidragSomIkkeErSøknadsbarn
                ? t.overskrifter.vurderingRevurderingsbarnOgAndreBarn
                : t.overskrifter.vurderingRevurderingsbarn
            }
          />
        </div>

        {kanFatteVedtakForRevurderingsbarn && (
          <div className="border border-gray-200  rounded">
            <BeregningForholdsmessigFordelingSøknadsbarn
              overskrift={t.overskrifter.beregningSøknadsbarnOgRevurderingsbarn}
            />
          </div>
        )}
      </DokumentStabel>
    );
  } else if (
    inneholderRevurderingsbarn &&
    forholdsmessigFordeling.beregningFordelingAvBidragSjekkEvnesprekk
  ) {
    return <BeregningForholdsmessigFordelingRevurdering />;
  }
  return <BeregningForholdsmessigFordelingSøknadsbarn />;
};

export const BeregningForholdsmessigFordelingRevurdering = ({
  overskrift,
}: {
  overskrift?: string;
}) => {
  const {
    sluttberegning,
    bpsAndel,
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

  function renderResult() {
    if (sluttberegning.bidragJustertNedTil25ProsentAvInntekt) {
      return t.forholdsmessigFordeling.redusertNedTil25ProsentAvInntekt;
    } else if (sluttberegning.bidragJustertNedTilEvne) {
      return t.forholdsmessigFordeling.redusertNedTilEvne;
    }
    return "";
  }

  const beregningFordelingAvBidrag =
    forholdsmessigFordeling.beregningFordelingAvBidragSjekkEvnesprekk!;

  const bpAndelAvUVedForholdsmessigFordelingFaktor =
    sluttberegning.bpAndelAvUVedForholdsmessigFordelingFaktor ??
    forholdsmessigFordeling.andelAvSumBidragTilFordelingFaktor;
  const bpEvneVedForholdsmessigFordeling =
    forholdsmessigFordeling.andelAvEvneBeløp ??
    sluttberegning.bpEvneVedForholdsmessigFordeling;

  const foreløpigBidrag =
    forholdsmessigFordeling.bidragEtterFordeling ??
    sluttberegning.bruttoBidragJustertForEvneOg25Prosent ??
    0;
  const bpsSumAndelAvU = beregningFordelingAvBidrag.sumBidragTilFordeling ?? 0;
  const andelFordeltTilBarnet =
    forholdsmessigFordeling.bidragTilFordelingForBarnet ??
    bpsAndel.andelBeløp ??
    0;

  const sumAndreBarn =
    beregningFordelingAvBidrag.sumBidragTilFordelingIkkeSøknadsbarn +
    beregningFordelingAvBidrag.sumBidragTilFordelingPrivatAvtale;

  const finnesPriorierteBidrag =
    beregningFordelingAvBidrag.sumBidragSomIkkeKanFordeles > 0;

  const bidragTilFordelingMinusUtlandsbidrag =
    beregningFordelingAvBidrag.sumBidragTilFordeling -
    beregningFordelingAvBidrag.sumBidragSomIkkeKanFordeles;

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
              {t.forholdsmessigFordeling.tittel && (
                <h4>{t.forholdsmessigFordeling.tittel}</h4>
              )}
              <FeltTabell>
                <Feltrader
                  rows={[
                    forholdsmessigFordeling.beregningFordelingAvBidrag
                      .finnesBarnMedLøpendeBidragSomIkkeErSøknadsbarn
                      ? {
                          label:
                            t.forholdsmessigFordeling
                              .bpsTotaleUnderholdskostnad,
                          labelBold: false,
                          value: `${formatterBeløpForBeregning(sumAndreBarn)} + ${formatterBeløpForBeregning(beregningFordelingAvBidrag.sumBidragTilFordelingSøknadsbarn)}`,
                          result: `${formatterBeløpForBeregning(beregningFordelingAvBidrag.sumBidragTilFordeling)}`,
                        }
                      : {},
                    finnesPriorierteBidrag
                      ? {
                          label:
                            t.forholdsmessigFordeling.bpsEvneSomKanFordeles,
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
            {t.evnevurdering.tilstrekkeligIngenVedtakForRevurderingsbarn
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
          {t.forholdsmessigFordeling.tittel && (
            <h4>{t.forholdsmessigFordeling.tittel}</h4>
          )}
          <FeltTabell>
            <Feltrader
              rows={[
                !forholdsmessigFordeling
                  ? {
                      label:
                        t.forholdsmessigFordeling.bpsTotaleUnderholdskostnad,
                      labelBold: false,
                      value: `${formatterBeløpForBeregning(bpsSumAndelAvU)}`,
                    }
                  : {},
                {
                  label: t.forholdsmessigFordeling.bpsTotaleUnderholdskostnad,
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
            ? `${t.evnevurdering.ikkeTilstrekkelig.replace("{evne}", formatterBeløpForBeregning(evne)).replace("{totalAndel}", formatterBeløpForBeregning(beregningFordelingAvBidrag.sumBidragTilFordeling))}${erSistePeriode ? t.evnevurdering.anbefalingFatteVedtak : ""}`
            : `${t.evnevurdering.tilstrekkelig.replace("{evne}", formatterBeløpForBeregning(evne)).replace("{totalAndel}", formatterBeløpForBeregning(beregningFordelingAvBidrag.sumBidragTilFordeling))}${erSistePeriode ? t.evnevurdering.anbefalingIkkeFatteVedtak : ""}`}
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
          ? t.søknadsbarn.tittelNårAndreBarnHarLøpendeBidrag
          : t.søknadsbarn.tittel}
      </h4>
      <DokumentTabell
        layoutAuto
        kolonner={[
          { tittel: ord.barn, bredde: "500px" },
          {
            tittel: t.søknadsbarn.kolonner.andelU,
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
          <Tabellcelle labelBold>{ord.sum}</Tabellcelle>
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
      return t.forholdsmessigFordeling.redusertNedTil25ProsentAvInntekt;
    } else if (sluttberegning.bidragJustertNedTilEvne) {
      return t.forholdsmessigFordeling.redusertNedTilEvne;
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
              {t.forholdsmessigFordeling.tittel && (
                <h4>{t.forholdsmessigFordeling.tittel}</h4>
              )}
              <FeltTabell>
                <Feltrader
                  rows={[
                    forholdsmessigFordeling.beregningFordelingAvBidrag
                      .finnesBarnMedLøpendeBidragSomIkkeErSøknadsbarn
                      ? {
                          label:
                            t.forholdsmessigFordeling
                              .bpsTotaleUnderholdskostnad,
                          labelBold: false,
                          value: `${formatterBeløpForBeregning(sumAndreBarn)} + ${formatterBeløpForBeregning(sumFordeling.sumBidragTilFordelingSøknadsbarn)}`,
                          result: `${formatterBeløpForBeregning(sumFordeling.sumBidragTilFordeling)}`,
                        }
                      : {},
                    finnesPriorierteBidrag
                      ? {
                          label:
                            t.forholdsmessigFordeling.bpsEvneSomKanFordeles,
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
            {t.evnevurdering.tilstrekkeligSøknadsbarn
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
        {t.forholdsmessigFordeling.tittel && (
          <h4>{t.forholdsmessigFordeling.tittel}</h4>
        )}
        <FeltTabell>
          <Feltrader
            rows={[
              !forholdsmessigFordeling
                ? {
                    label: t.forholdsmessigFordeling.bpsTotaleUnderholdskostnad,
                    labelBold: false,
                    value: `${formatterBeløpForBeregning(bpsSumAndelAvU)}`,
                  }
                : {},
              forholdsmessigFordeling.beregningFordelingAvBidrag
                .finnesBarnMedLøpendeBidragSomIkkeErSøknadsbarn
                ? {
                    label: t.forholdsmessigFordeling.bpsTotaleUnderholdskostnad,
                    labelBold: false,
                    value: `${formatterBeløpForBeregning(sumAndreBarn)} + ${formatterBeløpForBeregning(sumFordeling.sumBidragTilFordelingSøknadsbarn)}`,
                    result: `${formatterBeløpForBeregning(sumFordeling.sumBidragTilFordeling)}`,
                  }
                : {},
              {
                label:
                  t.forholdsmessigFordeling.barnetsAndelAvUnderholdskostnad,
                labelBold: false,
                value: `${formatterBeløpForBeregning(andelFordeltTilBarnet)} / ${formatterBeløpForBeregning(bidragTilFordelingMinusUtlandsbidrag)}`,
                result: `${formatterProsent(bpAndelAvUVedForholdsmessigFordelingFaktor)}`,
              },
              finnesPriorierteBidrag
                ? {
                    label: t.forholdsmessigFordeling.bpsEvneSomKanFordeles,
                    labelBold: false,
                    value: `${formatterBeløpForBeregning(delberegningBidragsevne.bidragsevne)} - ${formatterBeløpForBeregning(sumFordeling.sumPrioriterteBidragTilFordeling)}`,
                    result: `${formatterBeløpForBeregning(forholdsmessigFordeling.evneJustertForPrioriterteBidrag)}`,
                  }
                : {},
              {
                label:
                  t.forholdsmessigFordeling
                    .barnetsAndelEtterForholdsmessigFordeling,
                labelBold: false,
                value: `${formatterProsent(bpAndelAvUVedForholdsmessigFordelingFaktor)} x ${formatterBeløpForBeregning(forholdsmessigFordeling.evneJustertForPrioriterteBidrag)}`,
                result: `${formatterBeløpForBeregning(bpEvneVedForholdsmessigFordeling)}`,
              },
              {
                label: t.forholdsmessigFordeling.foreløpigBidrag,
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
        title={t.beregnedeTotalbidrag.tittelAndreBarn}
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
  title = t.beregnedeTotalbidrag.tittel,
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
          { tittel: ord.barn, bredde: "500px" },
          {
            tittel: t.beregnedeTotalbidrag.kolonner.beregnetBidrag,
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
          <Tabellcelle labelBold>{ord.sum}</Tabellcelle>
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
      <h4>{t.privatAvtaler.tittel}</h4>
      <DokumentTabell
        layoutAuto
        kolonner={[
          { tittel: ord.barn, bredde: "500px" },
          { tittel: ord.beløp, bredde: "50px" },
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
          <Tabellcelle labelBold>{ord.sum}</Tabellcelle>
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
            tittel: ord.barn,
            bredde: "500px",
          },
          ...(finnesIndeksregulering
            ? [
                {
                  tittel: t.bidragSomIkkeKanFordeles.kolonner.indeks,
                  bredde: "50px",
                },
              ]
            : []),
          ...(inneholderUtenlandsk
            ? [
                {
                  tittel: t.bidragSomIkkeKanFordeles.kolonner.avtalebeløp,
                  bredde: "50px",
                },
                {
                  tittel: t.bidragSomIkkeKanFordeles.kolonner.valutakurs,
                  bredde: "50px",
                },
                {
                  tittel: t.bidragSomIkkeKanFordeles.kolonner.avtalebeløpNok,
                  bredde: "50px",
                },
                {
                  tittel: t.bidragSomIkkeKanFordeles.kolonner.samvær,
                  bredde: "50px",
                },
              ]
            : []),
          {
            tittel: ord.beløp,
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
            {ord.sum}
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
          {t.bidragSomIkkeKanFordeles.tittel}
        </h4>

        {renderTablePrivatAvtale()}
      </div>
    </div>
  );
};
