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
import { dateToDDMMYYYY, formatPeriode } from "~/utils/date-utils";
import { FeltTabell, Feltrad } from "~/components/FeltTabell";
import NotatBegrunnelse from "~/components/NotatBegrunnelse";
import {
  formatterBeløp,
  formatterProsent,
  rolleTilVisningsnavnV2,
} from "~/utils/visningsnavn";
import { tekst } from "~/utils/tekst";
export default function PrivatAvtale() {
  const { data, erAvslag } = useNotatFelles();
  const privatAvtale = data.privatavtale;
  if (privatAvtale.length == 0 || erAvslag) return null;
  return (
    <div>
      <div className={"elements_inline section-title"}>
        <h2>
          {tekst(
            "notat.begrunnelse.bidrag.privatAvtale.tittel",
            "Privat avtale",
          )}
        </h2>
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
          <Feltrad
            label={tekst(
              "notat.begrunnelse.bidrag.privatAvtale.avtale.avtalemåned",
              "Avtalemåned",
            )}
          >
            {dateToDDMMYYYY(data.avtaleDato)}
          </Feltrad>
          <Feltrad
            label={tekst(
              "notat.begrunnelse.bidrag.privatAvtale.avtale.avtaletype",
              "Avtaletype",
            )}
          >
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
      <h4>
        {tekst(
          "notat.begrunnelse.bidrag.privatAvtale.vedtakFraNav.tittel",
          "Vedtak fra Nav",
        )}
      </h4>
      <DokumentTabell
        layoutAuto
        bredde="350px"
        kolonner={[
          {
            tittel: tekst(
              "notat.begrunnelse.bidrag.privatAvtale.vedtakFraNav.kolonner.valgt",
              "Valgt",
            ),
          },
          {
            tittel: tekst(
              "notat.begrunnelse.bidrag.privatAvtale.vedtakFraNav.kolonner.virkningsdato",
              "Virkningsdato",
            ),
          },
          {
            tittel: tekst(
              "notat.begrunnelse.bidrag.privatAvtale.vedtakFraNav.kolonner.vedtaksdato",
              "Vedtaksdato",
            ),
          },
          { tittel: tekst("ord.søknadstype", "Søknadstype") },
          {
            tittel: tekst(
              "notat.begrunnelse.bidrag.privatAvtale.vedtakFraNav.kolonner.resultatSistePeriode",
              "Resultat siste periode",
            ),
          },
        ]}
      >
        {data.map((vedtak, index) => (
          <Tabellrad key={index} index={index}>
            <Tabellcelle>
              {vedtak.valgt ? tekst("ord.ja", "Ja") : tekst("ord.nei", "Nei")}
            </Tabellcelle>
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
        {
          tittel: tekst(
            "felles.tabell.felles.fraTilOgMed",
            "Fra og med - Til og med",
          ),
        },
        { tittel: tekst("ord.beløp", "Beløp") },
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
      <h4 className={"mb-1 mt-4"}>
        {tekst(
          "notat.begrunnelse.bidrag.privatAvtale.beregnetPrivatAvtale.tittel",
          "Beregnet privat avtale",
        )}
      </h4>
      <DokumentTabell
        layoutAuto
        bredde="350px"
        kolonner={[
          {
            tittel: tekst(
              "felles.tabell.felles.fraTilOgMed",
              "Fra og med - Til og med",
            ),
          },
          {
            tittel: tekst(
              "notat.begrunnelse.bidrag.privatAvtale.beregnetPrivatAvtale.indeksprosent",
              "Indeksprosent",
            ),
          },
          { tittel: tekst("ord.beløp", "Beløp") },
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
