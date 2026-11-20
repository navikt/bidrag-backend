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
import { tekst } from "~/utils/tekst";
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
        <h2>{tekst("notat.begrunnelse.bidrag.gebyr.tittel", "Gebyr")}</h2>
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
            <Feltrad
              label={tekst("notat.begrunnelse.bidrag.gebyr.søknad.sak", "Sak")}
              labelBold
            >
              {sak.saksnummer}
            </Feltrad>
          </FeltTabell>
        </div>
      )}
      <GebyrForRoller gebyr={sak.gebyrRoller} />
      {sak.gebyr18År.length > 0 && (
        <>
          <h3>
            {tekst(
              "notat.begrunnelse.bidrag.gebyr.søknadOver18År",
              "Søknad om bidrag etter fylte 18 år",
            )}
          </h3>
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
          kolonner={[
            { tittel: tekst("ord.beskrivelse", "Beskrivelse") },
            { tittel: tekst("ord.beløp", "Beløp") },
          ]}
        >
          <Tabellrad index={0}>
            <Tabellcelle>
              {erAvslag
                ? tekst(
                    "notat.begrunnelse.bidrag.gebyr.inntekt.inntektSiste12Måneder",
                    "Inntekt siste 12 måneder",
                  )
                : tekst(
                    "notat.begrunnelse.bidrag.gebyr.inntekt.skattepliktigInntekt",
                    "Skattepliktig inntekt",
                  )}
            </Tabellcelle>
            <Tabellcelle>
              {formater(inntekt.skattepliktigInntekt, "beløp")}
            </Tabellcelle>
          </Tabellrad>
          {!erAvslag && (
            <>
              <Tabellrad index={1}>
                <Tabellcelle>
                  {tekst(
                    "notat.begrunnelse.bidrag.gebyr.inntekt.høyesteBarnetillegg",
                    "Høyeste barnetillegg",
                  )}
                </Tabellcelle>
                <Tabellcelle>
                  {formater(inntekt.maksBarnetillegg, "beløp")}
                </Tabellcelle>
              </Tabellrad>
              <Tabellrad index={2}>
                <Tabellcelle labelBold>
                  {tekst("ord.total", "Total")}
                </Tabellcelle>
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
                    <Feltrad label={tekst("ord.søktFraDato", "Søkt fra dato")}>
                      {formater(gebyr.søknad.søknadFomDato, "dato")}
                    </Feltrad>
                    <Feltrad label={tekst("ord.mottattDato", "Mottatt dato")}>
                      {formater(gebyr.søknad.mottattDato, "dato")}
                    </Feltrad>
                    <Feltrad label={tekst("ord.søknadFra", "Søknad fra")}>
                      {gebyr.søknad.søktAvTypeVisningsnavn}
                    </Feltrad>
                  </>
                )}
                <Feltrad
                  label={tekst(
                    "notat.begrunnelse.bidrag.gebyr.resultat.gebyr",
                    "Gebyr",
                  )}
                >
                  {gebyr.gebyrResultatVisningsnavn}
                </Feltrad>
                {gebyr.erManueltOverstyrt && (
                  <Feltrad label={tekst("ord.begrunnelse", "Begrunnelse")}>
                    {gebyr.begrunnelse}
                  </Feltrad>
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
