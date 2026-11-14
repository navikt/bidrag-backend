import {
  useNotatFelles,
  useVedleggNummer,
} from "~/components/notat_felles/NotatContext";
import { NotatSamvaersperiodeDto, NotatSamvaerBarnDto } from "~/types/Api";
import { CommonTable } from "~/components/CommonTable";
import ord from "~/tekster/ord.yaml";
import tekster from "~/tekster";
import bidragTekster from "~/tekster/notat/bidrag/saksbehandler.yaml";
import { formatPeriode, sortByAge } from "~/utils/date-utils";
import { DataViewTable } from "~/components/DataViewTable";
import NotatBegrunnelse from "~/components/NotatBegrunnelse";
import elementIds from "~/utils/elementIds";
import { rolleTilVisningsnavnV2 } from "~/utils/visningsnavn";
import {
  TypeInnhold,
  useDokumentFelles,
} from "~/components/vedtak_felles/FellesContext";

const t = bidragTekster.samvær;

export default function Samvær() {
  const vedleggNummer = useVedleggNummer("samvær");
  const { data, erAvslag, gjelderFlereSaker } = useNotatFelles();
  if (erAvslag) return null;
  const samvær = data.samværV2;
  if (samvær == null || samvær.barn.length === 0) return null;
  return (
    <div>
      <div className={"elements_inline section-title"}>
        <h2>{t.tittel}</h2>
        <a href={`#${elementIds.vedleggSamvær}`}>
          {ord.seVedleggForBeregningsdetaljer.replace("{}", `${vedleggNummer}`)}
        </a>
      </div>
      {gjelderFlereSaker ? (
        <SamværPerSak data={samvær.barn} />
      ) : samvær.erSammeForAlle ? (
        <SamværBarnFelles data={samvær.barn} />
      ) : (
        <SamværBarn data={samvær.barn} />
      )}
    </div>
  );
}
function SamværPerSak({ data }: { data: NotatSamvaerBarnDto[] }) {
  const { data: notatData } = useNotatFelles();
  const { typeInnhold } = useDokumentFelles();
  const sammeSamværForAlleSaker =
    notatData.samværV2?.sammeSamværForAlleSaker ?? [];

  const saksnummerListe = Array.from(
    new Set(data.map((barn) => barn.gjelderBarn.saksnummer)),
  );

  return (
    <div className={"flex flex-col gap-4"}>
      {saksnummerListe.map((saksnummer) => {
        const barnListe = data.filter(
          (barn) => barn.gjelderBarn.saksnummer == saksnummer,
        );
        const erSammeForAlle = sammeSamværForAlleSaker.find(
          (it) => it.saksnummer == saksnummer,
        )?.erLikForAlle;
        return (
          <div key={saksnummer}>
            <DataViewTable
              gap={"5px"}
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
            {erSammeForAlle ? (
              <SamværBarnFelles data={barnListe} />
            ) : (
              <SamværBarn data={barnListe} />
            )}
          </div>
        );
      })}
    </div>
  );
}
function SamværBarnFelles({ data }: { data: NotatSamvaerBarnDto[] }) {
  const førsteSamvær = data[0];
  return (
    <div className={"mb-medium"}>
      {data.length === 1 ? (
        <DataViewTable
          gap={"5px"}
          data={[
            {
              label: rolleTilVisningsnavnV2(førsteSamvær.gjelderBarn),
              labelBold: true,
              value: førsteSamvær.gjelderBarn.navn,
            },
          ]}
        />
      ) : (
        <div>{t.sammeForAlleSøknadsbarn}</div>
      )}
      <SamværTabell data={førsteSamvær.perioder} />
      <NotatBegrunnelse data={førsteSamvær?.begrunnelse} />
    </div>
  );
}
function SamværBarn({ data }: { data: NotatSamvaerBarnDto[] }) {
  return data
    .sort((a, b) => sortByAge(a.gjelderBarn, b.gjelderBarn))
    .map((barn, i) => (
      <div className={"mb-medium"} key={barn.gjelderBarn.ident + "-" + i}>
        <DataViewTable
          gap={"5px"}
          data={[
            {
              label: rolleTilVisningsnavnV2(barn.gjelderBarn),
              labelBold: true,
              value: barn.gjelderBarn.navn,
            },
          ]}
        />
        <SamværTabell data={barn.perioder} />
        <NotatBegrunnelse data={barn?.begrunnelse} />
      </div>
    ));
}
function SamværTabell({ data }: { data: NotatSamvaersperiodeDto[] }) {
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
            name: tekster.tabell.samvær.samværsklasse,
          },
        ],
        rows: data.map((d) => ({
          columns: [
            { content: formatPeriode(d.periode.fom, d.periode.tom) },
            { content: d.samværsklasseVisningsnavn },
          ],
        })),
      }}
    />
  );
}
