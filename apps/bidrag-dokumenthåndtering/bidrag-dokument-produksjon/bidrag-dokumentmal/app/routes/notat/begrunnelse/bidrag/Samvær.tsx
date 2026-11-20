import {
  useNotatFelles,
  useVedleggNummer,
} from "~/components/notat_felles/NotatContext";
import { NotatSamvaersperiodeDto, NotatSamvaerBarnDto } from "~/types/Api";
import {
  DokumentTabell,
  Tabellcelle,
  Tabellrad,
} from "~/components/DokumentTabell";
import { formatPeriode, sortByAge } from "~/utils/date-utils";
import { FeltTabell, Feltrad } from "~/components/FeltTabell";
import NotatBegrunnelse from "~/components/NotatBegrunnelse";
import DokumentStabel from "~/components/DokumentStabel";
import elementIds from "~/utils/elementIds";
import { rolleTilVisningsnavnV2 } from "~/utils/visningsnavn";
import {
  TypeInnhold,
  useDokumentFelles,
} from "~/components/vedtak_felles/FellesContext";
import { tekst } from "~/utils/tekst";
export default function Samvær() {
  const vedleggNummer = useVedleggNummer("samvær");
  const { data, erAvslag, gjelderFlereSaker } = useNotatFelles();
  if (erAvslag) return null;
  const samvær = data.samværV2;
  if (samvær == null || samvær.barn.length === 0) return null;
  return (
    <div>
      <div className={"elements_inline section-title"}>
        <h2>{tekst("notat.begrunnelse.bidrag.samvær.tittel", "Samvær")}</h2>
        <a href={`#${elementIds.vedleggSamvær}`}>
          {tekst(
            "ord.seVedleggForBeregningsdetaljer",
            "se vedlegg nr. {} for beregningsdetaljer",
          ).replace("{}", `${vedleggNummer}`)}
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
    <DokumentStabel gap={4}>
      {saksnummerListe.map((saksnummer) => {
        const barnListe = data.filter(
          (barn) => barn.gjelderBarn.saksnummer == saksnummer,
        );
        const erSammeForAlle = sammeSamværForAlleSaker.find(
          (it) => it.saksnummer == saksnummer,
        )?.erLikForAlle;
        return (
          <div key={saksnummer}>
            <div>
              <FeltTabell>
                <Feltrad
                  label={tekst("ord.saksnummer", "Saksnummer")}
                  labelBold
                >
                  {typeInnhold == TypeInnhold.NOTAT
                    ? saksnummer
                    : tekst(
                        "ord.saksnummerMedHenvisning",
                        "{} (Oppgi saksnummeret ved henvendelse til oss.)",
                      ).replace("{}", `${saksnummer}`)}
                </Feltrad>
              </FeltTabell>
            </div>
            {erSammeForAlle ? (
              <SamværBarnFelles data={barnListe} />
            ) : (
              <SamværBarn data={barnListe} />
            )}
          </div>
        );
      })}
    </DokumentStabel>
  );
}
function SamværBarnFelles({ data }: { data: NotatSamvaerBarnDto[] }) {
  const førsteSamvær = data[0];
  return (
    <div className={"mb-medium"}>
      {data.length === 1 ? (
        <div>
          <FeltTabell>
            <Feltrad
              label={rolleTilVisningsnavnV2(førsteSamvær.gjelderBarn)}
              labelBold
            >
              {førsteSamvær.gjelderBarn.navn}
            </Feltrad>
          </FeltTabell>
        </div>
      ) : (
        <div>
          {tekst(
            "notat.begrunnelse.bidrag.samvær.sammeForAlleSøknadsbarn",
            "Samme for alle søknadsbarn",
          )}
        </div>
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
        <div>
          <FeltTabell>
            <Feltrad label={rolleTilVisningsnavnV2(barn.gjelderBarn)} labelBold>
              {barn.gjelderBarn.navn}
            </Feltrad>
          </FeltTabell>
        </div>
        <SamværTabell data={barn.perioder} />
        <NotatBegrunnelse data={barn?.begrunnelse} />
      </div>
    ));
}
function SamværTabell({ data }: { data: NotatSamvaersperiodeDto[] }) {
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
        {
          tittel: tekst("felles.tabell.samvær.samværsklasse", "Samværsklasse"),
        },
      ]}
    >
      {data.map((periode, index) => (
        <Tabellrad key={index} index={index}>
          <Tabellcelle>
            {formatPeriode(periode.periode.fom, periode.periode.tom)}
          </Tabellcelle>
          <Tabellcelle>{periode.samværsklasseVisningsnavn}</Tabellcelle>
        </Tabellrad>
      ))}
    </DokumentTabell>
  );
}
