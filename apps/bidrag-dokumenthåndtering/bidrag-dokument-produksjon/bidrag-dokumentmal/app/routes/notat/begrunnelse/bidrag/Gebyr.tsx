import { Fragment } from "react";
import { useNotatFelles } from "~/components/notat_felles/NotatContext";
import { rolleTilVisningsnavn } from "~/utils/visningsnavn";
import { FeltTabell, Feltrad } from "~/components/FeltTabell";
import {
  DokumentTabell,
  Tabellcelle,
  Tabellrad,
} from "~/components/DokumentTabell";
import { konverterRolletype } from "~/utils/converter-utils";
import {
  NotatGebyrDetaljerDto,
  NotatGebyrSakDto,
  Rolletype,
} from "~/types/Api";
import { PersonV2 } from "~/components/Person";
import HorizontalLine from "~/components/HorizontalLine";
import { formater } from "~/utils/visning";
import ord from "~/tekster/ord.yaml";
import bidragTekster from "~/tekster/notat/begrunnelse/bidrag.yaml";

const t = bidragTekster.gebyr;

// Rekkefølgen rollene vises i innenfor hver sak.
const rollerekkefølge = [Rolletype.BM, Rolletype.BP, Rolletype.BA];

// Gebyr per sak. Hver sak viser rollene med inntekt og resultat for hver søknad.
export default function Gebyr() {
  const { data } = useNotatFelles();
  const saker = (data.gebyrV3?.saker ?? []).filter(
    (sak) => sak.gebyrRoller.length > 0 || sak.gebyr18År.length > 0,
  );
  if (saker.length == 0) return null;
  const visSaksnummer = saker.length > 1;
  return (
    <div>
      <div className={"elements_inline section-title"}>
        <h2>{t.tittel}</h2>
      </div>
      {saker.map((sak) => (
        <GebyrForSak
          key={sak.saksnummer}
          sak={sak}
          visSaksnummer={visSaksnummer}
        />
      ))}
    </div>
  );
}

function GebyrForSak({
  sak,
  visSaksnummer,
}: {
  sak: NotatGebyrSakDto;
  visSaksnummer: boolean;
}) {
  return (
    <>
      {visSaksnummer && (
        <div className={"mt-4"}>
          <FeltTabell>
            <Feltrad label={t.søknad.sak} labelBold>
              {sak.saksnummer}
            </Feltrad>
          </FeltTabell>
        </div>
      )}
      <GebyrForRoller gebyr={sak.gebyrRoller} />
      {sak.gebyr18År.length > 0 && (
        <>
          <h3>{t.søknadOver18År}</h3>
          <GebyrForRoller gebyr={sak.gebyr18År} />
        </>
      )}
    </>
  );
}

function GebyrForRoller({ gebyr }: { gebyr: NotatGebyrDetaljerDto[] }) {
  const roller = grupperPerRolle(gebyr);
  return roller.map((søknader, i) => (
    <div
      key={søknader[0].rolle.ident! + i}
      className={roller.length - 1 == i ? "" : "mb-2"}
    >
      <GebyrForRolle søknader={søknader} />
      <HorizontalLine />
    </div>
  ));
}

// Inntekten er den samme for alle søknadene til en rolle i en sak, og vises én gang.
function GebyrForRolle({ søknader }: { søknader: NotatGebyrDetaljerDto[] }) {
  const { erAvslag } = useNotatFelles();
  const rolle = søknader[0].rolle;
  const inntekt = søknader[0].inntekt;
  const flereSøknader = søknader.length > 1;

  return (
    <>
      <div className={"mb-1 mt-2"}>
        <FeltTabell>
          <Feltrad label={rolleTilVisningsnavn(rolle.rolle!)} labelBold>
            <PersonV2 {...rolle} />
          </Feltrad>
        </FeltTabell>
      </div>
      <div>
        <DokumentTabell
          layoutAuto
          kolonner={[{ tittel: ord.beskrivelse }, { tittel: ord.beløp }]}
        >
          <Tabellrad index={0}>
            <Tabellcelle>
              {erAvslag
                ? t.inntekt.inntektSiste12Måneder
                : t.inntekt.skattepliktigInntekt}
            </Tabellcelle>
            <Tabellcelle>
              {formater(inntekt.skattepliktigInntekt, "beløp")}
            </Tabellcelle>
          </Tabellrad>
          {!erAvslag && (
            <>
              <Tabellrad index={1}>
                <Tabellcelle>{t.inntekt.høyesteBarnetillegg}</Tabellcelle>
                <Tabellcelle>
                  {formater(inntekt.maksBarnetillegg, "beløp")}
                </Tabellcelle>
              </Tabellrad>
              <Tabellrad index={2}>
                <Tabellcelle labelBold>{ord.total}</Tabellcelle>
                <Tabellcelle>
                  {formater(inntekt.totalInntekt, "beløp")}
                </Tabellcelle>
              </Tabellrad>
            </>
          )}
        </DokumentTabell>
        {søknader.map((gebyr, i) => (
          <Fragment key={gebyr.søknad?.søknadsid ?? i}>
            <div className={"mt-4"}>
              <FeltTabell>
                {flereSøknader && gebyr.søknad && (
                  <>
                    <Feltrad label={ord.søktFraDato}>
                      {formater(gebyr.søknad.søknadFomDato, "dato")}
                    </Feltrad>
                    <Feltrad label={ord.mottattDato}>
                      {formater(gebyr.søknad.mottattDato, "dato")}
                    </Feltrad>
                    <Feltrad label={ord.søknadFra}>
                      {gebyr.søknad.søktAvTypeVisningsnavn}
                    </Feltrad>
                  </>
                )}
                <Feltrad label={t.resultat.gebyr}>
                  {gebyr.gebyrResultatVisningsnavn}
                </Feltrad>
                {gebyr.erManueltOverstyrt && (
                  <Feltrad label={ord.begrunnelse}>{gebyr.begrunnelse}</Feltrad>
                )}
              </FeltTabell>
            </div>
          </Fragment>
        ))}
      </div>
    </>
  );
}

function grupperPerRolle(
  gebyr: NotatGebyrDetaljerDto[],
): NotatGebyrDetaljerDto[][] {
  const perRolle = new Map<string, NotatGebyrDetaljerDto[]>();
  gebyr.forEach((g) => {
    const nøkkel = g.rolle.ident ?? g.rolle.navn ?? "";
    perRolle.set(nøkkel, [...(perRolle.get(nøkkel) ?? []), g]);
  });
  const rekkefølge = (g: NotatGebyrDetaljerDto) =>
    rollerekkefølge.indexOf(konverterRolletype(g.rolle.rolle ?? undefined)!);
  return Array.from(perRolle.values()).sort(
    (a, b) => rekkefølge(a[0]) - rekkefølge(b[0]),
  );
}
