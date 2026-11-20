import { useNotatFelles } from "~/components/notat_felles/NotatContext";
import {
  DokumentmalManuellVedtak,
  NotatBeregnetPrivatAvtalePeriodeDto,
  NotatPrivatAvtaleDto,
  NotatPrivatAvtalePeriodeDto,
  PrivatAvtaleType,
  Vedtakstype,
} from "~/types/Api";
import {
  DokumentTabell,
  Tabellcelle,
  Tabellrad,
} from "~/components/DokumentTabell";
import ord from "~/tekster/ord.yaml";
import tekster from "~/tekster";
import bidragTekster from "~/tekster/notat/begrunnelse/bidrag.yaml";
import { dateToDDMMYYYY, formatPeriode } from "~/utils/date-utils";
import { FeltTabell, Feltrad } from "~/components/FeltTabell";
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
      <div>
        <FeltTabell>
          <Feltrad label={rolleTilVisningsnavnV2(data.gjelderBarn)} labelBold>
            {data.gjelderBarn.navn}
          </Feltrad>
        </FeltTabell>
      </div>
      <div className={"mt-2"}>
        <FeltTabell>
          <Feltrad label={t.avtale.avtalemåned}>
            {dateToDDMMYYYY(data.avtaleDato)}
          </Feltrad>
          <Feltrad label={t.avtale.avtaletype}>
            {data.avtaleTypeVisningsnavn}
          </Feltrad>
        </FeltTabell>
      </div>
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
      <DokumentTabell
        layoutAuto
        bredde="350px"
        kolonner={[
          { tittel: t.vedtakFraNav.kolonner.valgt },
          { tittel: t.vedtakFraNav.kolonner.virkningsdato },
          { tittel: t.vedtakFraNav.kolonner.vedtaksdato },
          { tittel: ord.søknadstype },
          { tittel: t.vedtakFraNav.kolonner.resultatSistePeriode },
        ]}
      >
        {data.map((vedtak, index) => (
          <Tabellrad key={index} index={index}>
            <Tabellcelle>{vedtak.valgt ? ord.ja : ord.nei}</Tabellcelle>
            <Tabellcelle>{dateToDDMMYYYY(vedtak.virkningsDato)}</Tabellcelle>
            <Tabellcelle>{dateToDDMMYYYY(vedtak.fattetTidspunkt)}</Tabellcelle>
            <Tabellcelle>{vedtak.søknadstype}</Tabellcelle>
            <Tabellcelle>{vedtak.resultatSistePeriode}</Tabellcelle>
          </Tabellrad>
        ))}
      </DokumentTabell>
    </div>
  );
}

function PrivatAvtaleTabell({ data }: { data: NotatPrivatAvtalePeriodeDto[] }) {
  return (
    <DokumentTabell
      layoutAuto
      bredde="350px"
      kolonner={[
        { tittel: tekster.tabell.felles.fraTilOgMed },
        { tittel: ord.beløp },
      ]}
    >
      {data.map((periode, index) => (
        <Tabellrad key={index} index={index}>
          <Tabellcelle>
            {formatPeriode(periode.periode.fom, periode.periode.tom)}
          </Tabellcelle>
          <Tabellcelle>{formatterBeløp(periode.beløp)}</Tabellcelle>
        </Tabellrad>
      ))}
    </DokumentTabell>
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
      <DokumentTabell
        layoutAuto
        bredde="350px"
        kolonner={[
          { tittel: tekster.tabell.felles.fraTilOgMed },
          { tittel: t.beregnetPrivatAvtale.indeksprosent },
          { tittel: ord.beløp },
        ]}
      >
        {perioder.map((periode, index) => (
          <Tabellrad key={index} index={index}>
            <Tabellcelle>
              {formatPeriode(periode.periode.fom, periode.periode.tom)}
            </Tabellcelle>
            <Tabellcelle>{formatterProsent(periode.indeksfaktor)}</Tabellcelle>
            <Tabellcelle>{formatterBeløp(periode.beløp)}</Tabellcelle>
          </Tabellrad>
        ))}
      </DokumentTabell>
    </div>
  );
}
