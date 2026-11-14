import { Fragment } from "react";
import { useNotatFelles } from "~/components/notat_felles/NotatContext";
import { rolleTilVisningsnavn } from "~/utils/visningsnavn";
import { DataViewTable, DataViewTableData } from "~/components/DataViewTable";
import { CommonTable, TableRow } from "~/components/CommonTable";
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
import bidragTekster from "~/tekster/notat/bidrag/saksbehandler.yaml";

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
        <DataViewTable
          gap={"5px"}
          className={"mt-4"}
          data={[
            { label: t.søknad.sak, labelBold: true, value: sak.saksnummer },
          ]}
        />
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

  const inntektsrader: TableRow[] = [
    {
      columns: [
        {
          content: erAvslag
            ? t.inntekt.inntektSiste12Måneder
            : t.inntekt.skattepliktigInntekt,
        },
        { content: formater(inntekt.skattepliktigInntekt, "beløp") },
      ],
    },
  ];
  if (!erAvslag) {
    inntektsrader.push(
      {
        columns: [
          { content: t.inntekt.høyesteBarnetillegg },
          { content: formater(inntekt.maksBarnetillegg, "beløp") },
        ],
      },
      {
        columns: [
          { labelBold: true, content: ord.total },
          { content: formater(inntekt.totalInntekt, "beløp") },
        ],
      },
    );
  }

  return (
    <>
      <DataViewTable
        className={"mb-1 mt-2"}
        data={[
          {
            label: rolleTilVisningsnavn(rolle.rolle!),
            labelBold: true,
            value: <PersonV2 {...rolle} />,
          },
        ]}
      />
      <div>
        <CommonTable
          layoutAuto
          data={{
            headers: [{ name: ord.beskrivelse }, { name: ord.beløp }],
            rows: inntektsrader,
          }}
        />
        {søknader.map((gebyr, i) => (
          <Fragment key={gebyr.søknad?.søknadsid ?? i}>
            <DataViewTable
              gap={flereSøknader ? "5px" : undefined}
              className={"mt-4"}
              data={
                [
                  ...(flereSøknader && gebyr.søknad
                    ? [
                        {
                          label: ord.søktFraDato,
                          textRight: false,
                          value: formater(gebyr.søknad.søknadFomDato, "dato"),
                        },
                        {
                          label: ord.mottattDato,
                          textRight: false,
                          value: formater(gebyr.søknad.mottattDato, "dato"),
                        },
                        {
                          label: ord.søknadFra,
                          textRight: false,
                          value: gebyr.søknad.søktAvTypeVisningsnavn,
                        },
                      ]
                    : []),
                  {
                    label: t.resultat.gebyr,
                    value: gebyr.gebyrResultatVisningsnavn,
                  },
                  gebyr.erManueltOverstyrt && {
                    label: ord.begrunnelse,
                    value: gebyr.begrunnelse,
                  },
                ].filter((d) => d) as DataViewTableData[]
              }
            />
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
