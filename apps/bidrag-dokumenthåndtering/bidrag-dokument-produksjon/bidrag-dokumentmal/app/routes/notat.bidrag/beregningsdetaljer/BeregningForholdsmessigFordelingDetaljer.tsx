import { useBeregningDetaljer } from "~/routes/notat.bidrag/VedleggBeregningsDetaljer";
import {
  formatterBeløpForBeregning,
  formatterProsent,
  formatterBeløp,
} from "~/utils/visningsnavn";
import { DataViewTable, DataViewTableData } from "~/components/DataViewTable";
import {
  CommonTable,
  TableHeader,
  TableColumn,
} from "~/components/CommonTable";
import Person from "~/components/Person";
import { BeregningBarn } from "~/types/commonTypes";
import { BeregnetBidragBarnDto } from "~/types/Api";

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
      <div className="flex flex-col gap-3 mt-2">
        <div className="border border-gray-200 rounded">
          <div className="flex flex-col gap-1">
            <h3>
              {forholdsmessigFordeling.beregningFordelingAvBidrag
                .finnesBarnMedLøpendeBidragSomIkkeErSøknadsbarn
                ? "Vurdering av revurderingsbarn og andre barn mot beløpshistorikk"
                : "Vurdering av revurderingsbarn mot beløpshistorikk"}
            </h3>
            <BeregningForholdsmessigFordelingRevurdering />
          </div>
        </div>

        {kanFatteVedtakForRevurderingsbarn && (
          <div className="border border-gray-200  rounded">
            <div className="flex flex-col gap-1">
              <h3>Beregning for søknadsbarn og revurderingsbarn</h3>
              <BeregningForholdsmessigFordelingSøknadsbarn />
            </div>
          </div>
        )}
      </div>
    );
  } else if (
    inneholderRevurderingsbarn &&
    forholdsmessigFordeling.beregningFordelingAvBidragSjekkEvnesprekk
  ) {
    return <BeregningForholdsmessigFordelingRevurdering />;
  }
  return <BeregningForholdsmessigFordelingSøknadsbarn />;
};

export const BeregningForholdsmessigFordelingRevurdering = () => {
  const {
    sluttberegning,
    bpsAndel,
    periode: { erSistePeriode },
    delberegningBidragsevne,
    kanFatteVedtakForRevurderingsbarn,
    forholdsmessigFordelingBeregningsdetaljer: forholdsmessigFordeling,
  } = useBeregningDetaljer();

  if (!forholdsmessigFordeling.beregningFordelingAvBidragSjekkEvnesprekk)
    return null;

  function renderResult() {
    if (sluttberegning.bidragJustertNedTil25ProsentAvInntekt) {
      return ` (redusert ned til 25% av inntekt)`;
    } else if (sluttberegning.bidragJustertNedTilEvne) {
      return ` (redusert ned til evne)`;
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
            <DataViewTable
              className="pb-2"
              title="Forholdsmessig fordeling"
              data={
                [
                  forholdsmessigFordeling.beregningFordelingAvBidrag
                    .finnesBarnMedLøpendeBidragSomIkkeErSøknadsbarn && {
                    label: "BPs totale underholdskostnad",
                    labelBold: false,
                    value: `${formatterBeløpForBeregning(sumAndreBarn)} + ${formatterBeløpForBeregning(beregningFordelingAvBidrag.sumBidragTilFordelingSøknadsbarn)}`,
                    result: `${formatterBeløpForBeregning(beregningFordelingAvBidrag.sumBidragTilFordeling)}`,
                  },
                  finnesPriorierteBidrag && {
                    label: "BPs evne som kan fordeles",
                    labelBold: false,
                    value: `${formatterBeløpForBeregning(delberegningBidragsevne.bidragsevne)} - ${formatterBeløpForBeregning(beregningFordelingAvBidrag.sumPrioriterteBidragTilFordeling)}`,
                    result: `${formatterBeløpForBeregning(forholdsmessigFordeling.evneJustertForPrioriterteBidrag)}`,
                  },
                ].filter((d) => d != null) as DataViewTableData[]
              }
            />
          )}
          <p className="mt-2">
            Evnen på {formatterBeløpForBeregning(evne)} er tilstrekkelig for å
            dekke total andel av U på{" "}
            {formatterBeløpForBeregning(
              beregningFordelingAvBidrag.sumBidragTilFordeling,
            )}
            . Det vil derfor ikke fattes vedtak for revurderingsbarn.
          </p>
        </>
      );
    }
    return (
      <div>
        <DataViewTable
          title="Forholdsmessig fordeling"
          data={
            [
              !forholdsmessigFordeling && {
                label: "BPs totale underholdskostnad",
                labelBold: false,
                value: `${formatterBeløpForBeregning(bpsSumAndelAvU)}`,
              },
              {
                label: "BPs totale underholdskostnad",
                labelBold: false,
                value: `${formatterBeløpForBeregning(sumAndreBarn)} + ${formatterBeløpForBeregning(beregningFordelingAvBidrag.sumBidragTilFordelingSøknadsbarn)}`,
                result: `${formatterBeløpForBeregning(beregningFordelingAvBidrag.sumBidragTilFordeling)}`,
              },
            ].filter((d) => d != null) as DataViewTableData[]
          }
        />
        <p className="mt-2">
          {!erNokEvne
            ? `Evnen på ${formatterBeløpForBeregning(evne)} er ikke tilstrekkelig for å dekke total andel av U på ${formatterBeløpForBeregning(beregningFordelingAvBidrag.sumBidragTilFordeling)}.${erSistePeriode ? " Det anbefales derfor å fatte vedtak for revurderingsbarn." : ""}`
            : `Evnen på ${formatterBeløpForBeregning(evne)} er tilstrekkelig for å dekke total andel av U på ${formatterBeløpForBeregning(beregningFordelingAvBidrag.sumBidragTilFordeling)}.${erSistePeriode ? " Det anbefales derfor å ikke fatte vedtak for revurderingsbarn." : ""}`}
        </p>
      </div>
    );
  }

  return (
    <>
      <ForholdsmessigFordelingBeregningAndreBarn
        nyBeregningRevurdering={true}
      />
      <ForholdsmessigFordelingSøknadsbarn nyBeregningRevurdering={true} />
      {renderFFBeregning()}
    </>
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
          ? "BPs totale underholdskostnad for søknadsbarn"
          : "BPs totale underholdskostnad"}
      </h4>
      <CommonTable
        layoutAuto
        data={{
          headers: [
            { name: "Barn", width: "500px" },
            { name: "Andel U", width: "50px" },
          ] as TableHeader[],
          rows: søknadsbarn
            .map((b) => ({
              columns: [
                {
                  content: (
                    <Person
                      fødselsdato={b.barn.fødselsdato!}
                      navn={b.barn.navn!}
                      erBeskyttet={b.barn.erBeskyttet}
                    />
                  ),
                  colSpan: 1,
                },
                {
                  content: formatterBeløpForBeregning(
                    b.bidragTilFordeling,
                    true,
                  ),
                },
              ] as TableColumn[],
            }))
            .concat([
              {
                columns: [
                  {
                    content: "Sum" as string,
                    labelBold: true,
                  },
                  {
                    content: formatterBeløpForBeregning(
                      sumFordeling.sumBidragTilFordelingSøknadsbarn,
                      true,
                    ),
                  },
                ] as TableColumn[],
              },
            ]),
        }}
      />
    </div>
  );
};
export const BeregningForholdsmessigFordelingSøknadsbarn = () => {
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
      return ` (redusert ned til 25% av inntekt)`;
    } else if (sluttberegning.bidragJustertNedTilEvne) {
      return ` (redusert ned til evne)`;
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
            <DataViewTable
              title="Forholdsmessig fordeling"
              data={
                [
                  forholdsmessigFordeling.beregningFordelingAvBidrag
                    .finnesBarnMedLøpendeBidragSomIkkeErSøknadsbarn && {
                    label: "BPs totale underholdskostnad",
                    labelBold: false,
                    value: `${formatterBeløpForBeregning(sumAndreBarn)} + ${formatterBeløpForBeregning(sumFordeling.sumBidragTilFordelingSøknadsbarn)}`,
                    result: `${formatterBeløpForBeregning(sumFordeling.sumBidragTilFordeling)}`,
                  },
                  finnesPriorierteBidrag && {
                    label: "BPs evne som kan fordeles",
                    labelBold: false,
                    value: `${formatterBeløpForBeregning(delberegningBidragsevne.bidragsevne)} - ${formatterBeløpForBeregning(sumFordeling.sumPrioriterteBidragTilFordeling)}`,
                    result: `${formatterBeløpForBeregning(forholdsmessigFordeling.evneJustertForPrioriterteBidrag)}`,
                  },
                ].filter((d) => d != null) as DataViewTableData[]
              }
            />
          )}
          <p className="mt-2">
            Evnen på {formatterBeløpForBeregning(evne)} er tilstrekkelig for å
            dekke total andel av U på{" "}
            {formatterBeløpForBeregning(sumFordeling.sumBidragTilFordeling)}
          </p>
        </>
      );
    }

    return (
      <DataViewTable
        title="Forholdsmessig fordeling"
        data={
          [
            !forholdsmessigFordeling && {
              label: "BPs totale underholdskostnad",
              labelBold: false,
              value: `${formatterBeløpForBeregning(bpsSumAndelAvU)}`,
            },
            forholdsmessigFordeling.beregningFordelingAvBidrag
              .finnesBarnMedLøpendeBidragSomIkkeErSøknadsbarn && {
              label: "BPs totale underholdskostnad",
              labelBold: false,
              value: `${formatterBeløpForBeregning(sumAndreBarn)} + ${formatterBeløpForBeregning(sumFordeling.sumBidragTilFordelingSøknadsbarn)}`,
              result: `${formatterBeløpForBeregning(sumFordeling.sumBidragTilFordeling)}`,
            },
            {
              label: "Barnets andel av underholdskostnad",
              labelBold: false,
              value: `${formatterBeløpForBeregning(andelFordeltTilBarnet)} / ${formatterBeløpForBeregning(bidragTilFordelingMinusUtlandsbidrag)}`,
              result: `${formatterProsent(bpAndelAvUVedForholdsmessigFordelingFaktor)}`,
            },
            finnesPriorierteBidrag && {
              label: "BPs evne som kan fordeles",
              labelBold: false,
              value: `${formatterBeløpForBeregning(delberegningBidragsevne.bidragsevne)} - ${formatterBeløpForBeregning(sumFordeling.sumPrioriterteBidragTilFordeling)}`,
              result: `${formatterBeløpForBeregning(forholdsmessigFordeling.evneJustertForPrioriterteBidrag)}`,
            },
            {
              label: "Barnets andel etter forholdsmessig fordeling",
              labelBold: false,
              value: `${formatterProsent(bpAndelAvUVedForholdsmessigFordelingFaktor)} x ${formatterBeløpForBeregning(forholdsmessigFordeling.evneJustertForPrioriterteBidrag)}`,
              result: `${formatterBeløpForBeregning(bpEvneVedForholdsmessigFordeling)}`,
            },
            {
              label: "Foreløpig bidrag",
              labelBold: false,
              value: ` ${formatterBeløpForBeregning(foreløpigBidrag)}${renderResult()}`,
            },
          ].filter((d) => d != null) as DataViewTableData[]
        }
      />
    );
  }

  return (
    <>
      <ForholdsmessigFordelingBeregningAndreBarn />
      <ForholdsmessigFordelingSøknadsbarn />
      {renderFFBeregning()}
    </>
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
        title={"BP's beregnede totalbidrag for andre barn"}
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
  title = "BP's beregnede totalbidrag",
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
      <CommonTable
        layoutAuto
        data={{
          headers: [
            { name: "Barn", width: "500px" },
            { name: "Beregnet bidrag", width: "50px" },
          ] as TableHeader[],
          rows: beregning
            .filter((b) => !b.privatAvtale)
            .map((b) => ({
              columns: [
                {
                  content: (
                    <Person
                      fødselsdato={b.barn.fødselsdato!}
                      navn={b.barn.navn!}
                      erBeskyttet={b.barn.erBeskyttet}
                    />
                  ),
                  colSpan: 1,
                },
                {
                  content: formatterBeløpForBeregning(
                    b.bidragTilFordeling,
                    true,
                  ),
                },
              ] as TableColumn[],
            }))
            .concat([
              {
                columns: [
                  {
                    content: "Sum" as string,
                    labelBold: true,
                  },
                  {
                    content: formatterBeløpForBeregning(
                      bidragspliktigesBeregnedeTotalbidrag,
                    ),
                  },
                ] as TableColumn[],
              },
            ]),
        }}
      />
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
      <h4>{"BP's private avtaler"}</h4>
      <CommonTable
        layoutAuto
        data={{
          headers: [
            { name: "Barn", width: "500px" },
            { name: "Beløp", width: "50px" },
          ] as TableHeader[],
          rows: privatAvtaleBarn
            .map((b) => ({
              columns: [
                {
                  content: (
                    <Person
                      fødselsdato={b.barn.fødselsdato!}
                      navn={b.barn.navn!}
                      erBeskyttet={b.barn.erBeskyttet}
                    />
                  ),
                  colSpan: 1,
                },
                {
                  content: formatterBeløpForBeregning(
                    b.bidragTilFordeling,
                    true,
                  ),
                },
              ] as TableColumn[],
            }))
            .concat([
              {
                columns: [
                  {
                    content: "Sum" as string,
                    labelBold: true,
                  },
                  {
                    content: formatterBeløpForBeregning(sumBidragPrivatAvtale),
                  },
                ] as TableColumn[],
              },
            ]),
        }}
      />
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
      <CommonTable
        layoutAuto
        data={{
          headers: [
            {
              name: "Barn",
              width: "500px",
            },
            finnesIndeksregulering && {
              name: "Indeks",
              width: "50px",
            },
            inneholderUtenlandsk && {
              name: "Avtalebeløp",
              width: "50px",
            },
            inneholderUtenlandsk && {
              name: "Valutakurs",
              width: "50px",
            },
            inneholderUtenlandsk && {
              name: "Avtalebeløp (NOK)",
              width: "50px",
            },
            inneholderUtenlandsk && {
              name: "Samvær",
              width: "50px",
            },
            {
              name: "Beløp",
              width: "50px",
            },
          ].filter((h) => h != null) as TableHeader[],
          rows: bidragIkkeTilFordeling
            .map(({ beregnetBidragPerBarn: row, barn }, rowIndex) => ({
              columns: [
                {
                  content: (
                    <Person
                      fødselsdato={barn.fødselsdato!}
                      navn={barn.navn!}
                      erBeskyttet={barn.erBeskyttet}
                    />
                  ),
                  colSpan: 1,
                },
                finnesIndeksregulering && {
                  content: formatterProsent(row.indeksreguleringFaktor),
                },
                inneholderUtenlandsk && {
                  content:
                    row.valutakode === "NOK"
                      ? formatterBeløpForBeregning(row.løpendeBeløp)
                      : `${formatterBeløpForBeregning(row.løpendeBeløp)} (${row.valutakode})`,
                },
                løperIUtlandskValuta && {
                  content: formatterBeløp(row.valutakurs),
                },
                inneholderUtenlandsk && {
                  content: formatterBeløpForBeregning(row.beregnetBeløp),
                },
                inneholderUtenlandsk && {
                  content: formatterBeløpForBeregning(row.samværsfradrag),
                },
                {
                  content: formatterBeløpForBeregning(row.beregnetBidrag, true),
                },
              ].filter((d) => d != null) as TableColumn[],
            }))
            .concat([
              {
                columns: [
                  {
                    content: "Sum" as string,
                    labelBold: true,
                    colSpanNegative: 1,
                  },
                  {
                    content: formatterBeløpForBeregning(sumBidrag),
                  },
                ] as TableColumn[],
              },
            ]),
        }}
      />
    );
  }

  return (
    <div className={"mb-2"}>
      <div>
        <h4 className="inline-block align-middle">
          {"BP's bidrag som ikke kan fordeles"}
        </h4>

        {renderTablePrivatAvtale()}
      </div>
    </div>
  );
};
