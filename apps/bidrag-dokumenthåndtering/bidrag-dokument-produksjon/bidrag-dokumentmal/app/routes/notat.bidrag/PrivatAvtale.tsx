import { useNotatFelles } from "~/components/notat_felles/NotatContext";
import {
  DokumentmalManuellVedtak,
  NotatBeregnetPrivatAvtalePeriodeDto,
  NotatPrivatAvtaleDto,
  NotatPrivatAvtalePeriodeDto,
  PrivatAvtaleType,
  Vedtakstype,
} from "~/types/Api";
import { CommonTable } from "~/components/CommonTable";
import ord from "~/tekster/ord.json";
import tekster from "~/tekster";
import bidragTekster from "~/tekster/notat/bidrag/saksbehandler.json";
import { dateToDDMMYYYY, formatPeriode } from "~/utils/date-utils";
import { DataViewTable } from "~/components/DataViewTable";
import NotatBegrunnelse from "~/components/NotatBegrunnelse";
import {
  formatterBeløp,
  formatterProsent,
  rolleTilVisningsnavnV2,
} from "~/utils/visningsnavn";

const t = bidragTekster.privatAvtale;

export default function PrivatAvtale() {
  const { data, erAvslag } = useNotatFelles();
  const privatAvtale = data.privatavtale;
  if (privatAvtale.length == 0 || erAvslag) return null;
  return (
    <div>
      <div className={"elements_inline section-title"}>
        <h2>{t.tittel}</h2>
      </div>
      <>
        {privatAvtale.map((barn, i) => (
          <>
            <PrivatAvtaleBarn
              data={barn}
              key={i + barn.gjelderBarn.ident!}
              vedtakInnkreving={
                data.behandling.vedtakstype === Vedtakstype.INNKREVING
              }
            />
          </>
        ))}
      </>
    </div>
  );
}

function PrivatAvtaleBarn({
  data,
  vedtakInnkreving,
}: {
  data: NotatPrivatAvtaleDto;
  vedtakInnkreving: boolean;
}) {
  return (
    <div className={"mb-medium"}>
      <DataViewTable
        gap={"5px"}
        data={[
          {
            label: rolleTilVisningsnavnV2(data.gjelderBarn),
            labelBold: true,
            value: data.gjelderBarn.navn,
          },
        ]}
      />
      <DataViewTable
        className={"mt-2"}
        data={[
          {
            label: t.avtale.avtalemåned,
            labelBold: false,
            value: dateToDDMMYYYY(data.avtaleDato),
          },
          {
            label: t.avtale.avtaletype,
            labelBold: false,
            value: data.avtaleTypeVisningsnavn,
          },
        ]}
      />
      {data.avtaleType == PrivatAvtaleType.VEDTAK_FRA_NAV &&
      vedtakInnkreving ? (
        <PrivatAvtaleManuelleVedtakTabell
          data={data.vedtakslisteUtenInnkreving}
        />
      ) : (
        <PrivatAvtaleTabell data={data.perioder} />
      )}

      <PrivatAvtaleBeregnetTabell data={data.beregnetPrivatAvtalePerioder} />
      <NotatBegrunnelse data={data?.begrunnelse} />
    </div>
  );
}
function PrivatAvtaleManuelleVedtakTabell({
  data,
}: {
  data: DokumentmalManuellVedtak[];
}) {
  return (
    <div className={"pt-2"}>
      <h4>{t.vedtakFraNav.tittel}</h4>
      <CommonTable
        layoutAuto
        width={"350px"}
        data={{
          headers: [
            {
              name: t.vedtakFraNav.kolonner.valgt,
            },
            {
              name: t.vedtakFraNav.kolonner.virkningsdato,
            },
            {
              name: t.vedtakFraNav.kolonner.vedtaksdato,
            },
            {
              name: ord.søknadstype,
            },
            {
              name: t.vedtakFraNav.kolonner.resultatSistePeriode,
            },
          ],
          rows: data.map((d) => ({
            columns: [
              { content: d.valgt ? ord.ja : ord.nei },
              { content: dateToDDMMYYYY(d.virkningsDato) },
              { content: dateToDDMMYYYY(d.fattetTidspunkt) },
              { content: d.søknadstype },
              { content: d.resultatSistePeriode },
            ],
          })),
        }}
      />
    </div>
  );
}

function PrivatAvtaleTabell({ data }: { data: NotatPrivatAvtalePeriodeDto[] }) {
  return (
    <CommonTable
      layoutAuto
      width={"350px"}
      data={{
        headers: [
          {
            name: tekster.tabell.felles.fraTilOgMed,
          },
          {
            name: ord.beløp,
          },
        ],
        rows: data.map((d) => ({
          columns: [
            { content: formatPeriode(d.periode.fom, d.periode.tom) },
            { content: formatterBeløp(d.beløp) },
          ],
        })),
      }}
    />
  );
}

function PrivatAvtaleBeregnetTabell({
  data,
}: {
  data?: NotatBeregnetPrivatAvtalePeriodeDto[];
}) {
  if (!data || data.length == 0) return null;
  const perioder = data;
  return (
    <div>
      <h4 className={"mb-1 mt-4"}>{t.beregnetPrivatAvtale.tittel}</h4>
      <CommonTable
        layoutAuto
        width={"350px"}
        data={{
          headers: [
            {
              name: tekster.tabell.felles.fraTilOgMed,
            },
            {
              name: t.beregnetPrivatAvtale.indeksprosent,
            },
            {
              name: ord.beløp,
            },
          ],
          rows: perioder.map((d) => ({
            columns: [
              { content: formatPeriode(d.periode.fom, d.periode.tom) },
              { content: formatterProsent(d.indeksfaktor) },
              { content: formatterBeløp(d.beløp) },
            ],
          })),
        }}
      />
    </div>
  );
}
