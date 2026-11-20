import {
  rolleTilVisningsnavnV2,
  søktAvTilVisningsnavn,
} from "~/utils/visningsnavn";
import { dateToDDMMYYYY, sortByAge } from "~/utils/date-utils";
import NotatBegrunnelse from "~/components/NotatBegrunnelse";
import DokumentStabel from "~/components/DokumentStabel";
import { useNotatFelles } from "~/components/notat_felles/NotatContext";
import { FeltTabell, Feltrad, Feltrader } from "~/components/FeltTabell";
import { NotatVirkningstidspunktBarnDto, Stonadstype } from "~/types/Api";
import ord from "~/tekster/ord.yaml";
import notatTekster from "~/tekster/notat/begrunnelse/felles.yaml";
import Beregningsperiode from "~/routes/notat/begrunnelse/bidrag/Beregningsperiode";
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
    <DokumentStabel gap={4}>
      {saksnummerListe.map((saksnummer) => {
        const barnListe = virkningstidspunkt.barn.filter(
          (barn) => barn.rolle.saksnummer == saksnummer,
        );
        const erLikForAlle = virkningstidspunkt.erLikForAlleBasertPåSak.find(
          (it) => it.saksnummer == saksnummer,
        )?.erLikForAlle;
        return (
          <div key={saksnummer}>
            <div>
              <FeltTabell labelColWidth={"70px"}>
                <Feltrad label={ord.saksnummer} labelBold>
                  {typeInnhold == TypeInnhold.NOTAT
                    ? saksnummer
                    : ord.saksnummerMedHenvisning.replace(
                        "{}",
                        `${saksnummer}`,
                      )}
                </Feltrad>
              </FeltTabell>
            </div>
            {erLikForAlle ? (
              <VirkningstidspunktFelles barnListe={barnListe} />
            ) : (
              <VirkningstidspunktPerBarn barnListe={barnListe} />
            )}
          </div>
        );
      })}
    </DokumentStabel>
  );
}

function VirkningstidspunktPerBarn({
  barnListe,
}: {
  barnListe: NotatVirkningstidspunktBarnDto[];
}) {
  const { data } = useNotatFelles();
  return (
    <DokumentStabel gap={2}>
      {barnListe
        .sort((a, b) => sortByAge(a.rolle, b.rolle))
        .map((barn) => {
          return (
            <div key={barn.rolle.ident}>
              <div>
                <FeltTabell>
                  <Feltrad label={rolleTilVisningsnavnV2(barn.rolle)} labelBold>
                    {barn.rolle.navn}
                  </Feltrad>
                </FeltTabell>
              </div>
              <div className={"flex flex-row justify-between w-[500px]"}>
                <div>
                  <FeltTabell labelColWidth={"70px"}>
                    <Feltrader
                      rows={[
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
                  </FeltTabell>
                </div>
                <div className={"mb-2"}>
                  <FeltTabell labelColWidth={"100px"}>
                    <Feltrad label={ord.mottattDato}>
                      {dateToDDMMYYYY(barn.mottattDato as string)}
                    </Feltrad>
                    <Feltrad label={ord.søktFraDato}>
                      {dateToDDMMYYYY(barn.søktFraDato as string)}
                    </Feltrad>
                  </FeltTabell>
                </div>
              </div>
              <div>
                <div>
                  <FeltTabell labelColWidth={"100px"}>
                    <Feltrad label={t.virkningstidspunkt}>
                      {dateToDDMMYYYY(barn.virkningstidspunkt)}
                    </Feltrad>
                  </FeltTabell>
                </div>
                {barn.opphørsdato && (
                  <div key={barn.rolle.ident}>
                    <FeltTabell labelColWidth={"100px"}>
                      <Feltrad label={t.opphørsdato}>
                        {dateToDDMMYYYY(barn.opphørsdato as string)}
                      </Feltrad>
                    </FeltTabell>
                  </div>
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
    </DokumentStabel>
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
        <div>
          <FeltTabell labelColWidth={"70px"}>
            <Feltrader
              rows={[
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
          </FeltTabell>
        </div>
        <div>
          <FeltTabell labelColWidth={"100px"}>
            <Feltrad label={ord.mottattDato}>
              {dateToDDMMYYYY(virkningstidspunktBarn.mottattDato as string)}
            </Feltrad>
            <Feltrad label={ord.søktFraDato}>
              {dateToDDMMYYYY(virkningstidspunktBarn.søktFraDato as string)}
            </Feltrad>
          </FeltTabell>
        </div>
      </div>
      <div className={"mt-4"}>
        <FeltTabell labelColWidth={"100px"}>
          <Feltrad label={t.virkningstidspunkt}>
            {dateToDDMMYYYY(virkningstidspunktBarn.virkningstidspunkt)}
          </Feltrad>
        </FeltTabell>
      </div>
      {virkningstidspunktBarn.opphørsdato && (
        <div>
          <FeltTabell labelColWidth={"100px"}>
            <Feltrad label={t.opphørsdato}>
              {dateToDDMMYYYY(virkningstidspunktBarn.opphørsdato as string)}
            </Feltrad>
          </FeltTabell>
        </div>
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
