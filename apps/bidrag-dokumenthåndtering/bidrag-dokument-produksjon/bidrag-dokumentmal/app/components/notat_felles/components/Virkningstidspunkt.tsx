import {
  rolleTilVisningsnavnV2,
  søktAvTilVisningsnavn,
} from "~/utils/visningsnavn";
import { dateToDDMMYYYY, sortByAge } from "~/utils/date-utils";
import NotatBegrunnelse from "~/components/NotatBegrunnelse";
import { useNotatFelles } from "~/components/notat_felles/NotatContext";
import { DataViewTable } from "~/components/DataViewTable";
import { NotatVirkningstidspunktBarnDto, Stonadstype } from "~/types/Api";
import ord from "~/tekster/ord.yaml";
import notatTekster from "~/tekster/notat/felles/saksbehandler.yaml";
import Beregningsperiode from "~/routes/notat.bidrag/Beregningsperiode";
import {
  TypeInnhold,
  useDokumentFelles,
} from "~/components/vedtak_felles/FellesContext";

const t = notatTekster.virkningstidspunkt;

export default function Virkningstidspunkt() {
  const { data, gjelderFlereSaker } = useNotatFelles();
  const virkningstidspunkt = data.virkningstidspunkt;
  return (
    <div className={"virkningstidspunkt"}>
      <h2>{t.tittel}</h2>
      {gjelderFlereSaker ? (
        <VirkningstidspunktPerSak />
      ) : virkningstidspunkt.erLikForAlle ? (
        <VirkningstidspunktFelles barnListe={virkningstidspunkt.barn} />
      ) : (
        <VirkningstidspunktPerBarn barnListe={virkningstidspunkt.barn} />
      )}
    </div>
  );
}

function VirkningstidspunktPerSak() {
  const { data } = useNotatFelles();
  const { typeInnhold } = useDokumentFelles();
  const virkningstidspunkt = data.virkningstidspunkt;

  const saksnummerListe = Array.from(
    new Set(virkningstidspunkt.barn.map((barn) => barn.rolle.saksnummer)),
  );

  return (
    <div className={"flex flex-col gap-4"}>
      {saksnummerListe.map((saksnummer) => {
        const barnListe = virkningstidspunkt.barn.filter(
          (barn) => barn.rolle.saksnummer == saksnummer,
        );
        const erLikForAlle = virkningstidspunkt.erLikForAlleBasertPåSak.find(
          (it) => it.saksnummer == saksnummer,
        )?.erLikForAlle;
        return (
          <div key={saksnummer}>
            <DataViewTable
              labelColWidth={"70px"}
              data={[
                {
                  label: ord.saksnummer,
                  labelBold: true,
                  value:
                    typeInnhold == TypeInnhold.NOTAT
                      ? saksnummer
                      : ord.saksnummerMedHenvisning.replace(
                          "{}",
                          `${saksnummer}`,
                        ),
                },
              ]}
            />
            {erLikForAlle ? (
              <VirkningstidspunktFelles barnListe={barnListe} />
            ) : (
              <VirkningstidspunktPerBarn barnListe={barnListe} />
            )}
          </div>
        );
      })}
    </div>
  );
}

function VirkningstidspunktPerBarn({
  barnListe,
}: {
  barnListe: NotatVirkningstidspunktBarnDto[];
}) {
  const { data } = useNotatFelles();
  return (
    <div className={"flex flex-col gap-2"}>
      {barnListe
        .sort((a, b) => sortByAge(a.rolle, b.rolle))
        .map((barn) => {
          return (
            <div key={barn.rolle.ident}>
              <DataViewTable
                data={[
                  {
                    label: rolleTilVisningsnavnV2(barn.rolle),
                    labelBold: true,
                    value: barn.rolle.navn,
                  },
                ]}
              />
              <div className={"flex flex-row justify-between w-[500px]"}>
                <DataViewTable
                  labelColWidth={"70px"}
                  data={[
                    {
                      label: ord.søknadstype,
                      value: barn.behandlingstypeVisningsnavn,
                    },
                    {
                      label: ord.søknadFra,
                      value: søktAvTilVisningsnavn(barn.søktAv),
                    },
                    {
                      label: t.innkreving,
                      value: barn.innkreving ? ord.ja : ord.nei,
                    },
                    ...[
                      barn.avslag
                        ? {
                            label: ord.avslag,
                            value: barn.avslagVisningsnavn,
                          }
                        : {
                            label: ord.årsak,
                            value: barn.årsakVisningsnavn,
                          },
                    ],
                  ]}
                />
                <DataViewTable
                  labelColWidth={"100px"}
                  className={"mb-2"}
                  data={[
                    {
                      label: ord.mottattDato,
                      value: dateToDDMMYYYY(barn.mottattDato as string),
                    },
                    {
                      label: ord.søktFraDato,
                      value: dateToDDMMYYYY(barn.søktFraDato as string),
                    },
                  ]}
                />
              </div>
              <div>
                <DataViewTable
                  labelColWidth={"100px"}
                  data={[
                    {
                      label: t.virkningstidspunkt,
                      value: dateToDDMMYYYY(barn.virkningstidspunkt),
                    },
                  ]}
                />
                {barn.opphørsdato && (
                  <DataViewTable
                    key={barn.rolle.ident}
                    labelColWidth={"100px"}
                    data={[
                      {
                        label: t.opphørsdato,
                        value: dateToDDMMYYYY(barn.opphørsdato as string),
                      },
                    ]}
                  />
                )}
                <Beregningsperiode virkningstidspunkt={barn} />
                <NotatBegrunnelse data={barn.begrunnelse} />
                {data.stønadstype == Stonadstype.BIDRAG18AAR && (
                  <NotatBegrunnelse
                    label={t.vurderingAvSkolegang}
                    data={barn.begrunnelseVurderingAvSkolegang}
                  />
                )}
              </div>
            </div>
          );
        })}
    </div>
  );
}

function VirkningstidspunktFelles({
  barnListe,
}: {
  barnListe: NotatVirkningstidspunktBarnDto[];
}) {
  const { data } = useNotatFelles();
  const virkningstidspunkt = data.virkningstidspunkt;
  const virkningstidspunktBarn = barnListe[0];
  return (
    <div>
      <div className={"flex flex-row justify-between w-[500px]"}>
        <DataViewTable
          labelColWidth={"70px"}
          data={[
            {
              label: ord.søknadstype,
              value: virkningstidspunktBarn.behandlingstypeVisningsnavn,
            },
            {
              label: t.innkreving,
              value: virkningstidspunktBarn.innkreving ? ord.ja : ord.nei,
            },
            {
              label: ord.søknadFra,
              value: søktAvTilVisningsnavn(virkningstidspunktBarn.søktAv),
            },
            ...[
              virkningstidspunkt.erAvslagForAlle
                ? {
                    label: ord.avslag,
                    value: virkningstidspunktBarn.avslagVisningsnavn,
                  }
                : {
                    label: ord.årsak,
                    value: virkningstidspunktBarn.årsakVisningsnavn,
                  },
            ],
          ]}
        />
        <DataViewTable
          labelColWidth={"100px"}
          data={[
            {
              label: ord.mottattDato,
              value: dateToDDMMYYYY(
                virkningstidspunktBarn.mottattDato as string,
              ),
            },
            {
              label: ord.søktFraDato,
              value: dateToDDMMYYYY(
                virkningstidspunktBarn.søktFraDato as string,
              ),
            },
          ]}
        />
      </div>
      <DataViewTable
        labelColWidth={"100px"}
        className={"mt-4"}
        data={[
          {
            label: t.virkningstidspunkt,
            value: dateToDDMMYYYY(virkningstidspunktBarn.virkningstidspunkt),
          },
        ]}
      />
      {virkningstidspunktBarn.opphørsdato && (
        <DataViewTable
          labelColWidth={"100px"}
          data={[
            {
              label: t.opphørsdato,
              value: dateToDDMMYYYY(
                virkningstidspunktBarn.opphørsdato as string,
              ),
            },
          ]}
        />
      )}
      <Beregningsperiode virkningstidspunkt={virkningstidspunktBarn} />
      <NotatBegrunnelse data={virkningstidspunktBarn.begrunnelse} />
      {data.stønadstype == Stonadstype.BIDRAG18AAR && (
        <NotatBegrunnelse
          label={t.vurderingAvSkolegang}
          data={virkningstidspunktBarn.begrunnelseVurderingAvSkolegang}
        />
      )}
    </div>
  );
}
