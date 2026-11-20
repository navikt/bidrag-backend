import { Fragment } from "react";
import { useNotatFelles } from "~/components/notat_felles/NotatContext";
import { Seksjon, Underseksjon } from "~/components/notat_felles/Seksjon";
import { delUnderholdskostnader } from "~/utils/notatUtvalg";
import NotatBegrunnelse from "~/components/NotatBegrunnelse";
import { NotatUnderholdBarnDto } from "~/types/Api";
import { FeltTabell, Feltrad } from "~/components/FeltTabell";
import {
  DokumentTabell,
  Tabellcelle,
  Tabellrad,
} from "~/components/DokumentTabell";
import { formatPeriode, dateToDDMMYYYY } from "~/utils/date-utils";
import {
  formatterBeløpForBeregning,
  rolleTilVisningsnavnV2,
  beløpstypeVisningsnavn,
} from "~/utils/visningsnavn";
import KildeIcon from "~/components/KildeIcon";
import { tekst } from "~/utils/tekst";
export default function Underholdskostnad() {
  const { data, erAvslag } = useNotatFelles();
  if (erAvslag || data.underholdskostnader == null) return null;
  const { søknadsbarn, andreBarn } = delUnderholdskostnader(
    data.underholdskostnader.underholdskostnaderBarn,
  );
  return (
    <Seksjon
      tittel={tekst(
        "notat.begrunnelse.bidrag.underholdskostnad.tittel",
        "Underholdskostnad",
      )}
      vedlegg={"underholdskostnader"}
      lenketekst={tekst(
        "notat.begrunnelse.bidrag.underholdskostnad.lenkeTilVedlegg",
        "se vedlegg nr. {} for underholdskostnad",
      )}
    >
      {søknadsbarn.map((barn, i) => (
        <UnderholdskostnaderSøknadsbarn
          data={barn}
          key={i + barn.gjelderBarn.ident!}
        />
      ))}
      <UnderholdskostnaderAndreBarn data={andreBarn} />
    </Seksjon>
  );
}
function UnderholdskostnaderAndreBarn({
  data,
}: {
  data: NotatUnderholdBarnDto[];
}) {
  if (data.length == 0) return null;
  return (
    <div className={"mb-4"}>
      {data.map((barn) => (
        <>
          <div className={"pt-2 pb-0 mb-2 mt-2"}>
            <FeltTabell>
              <Feltrad
                label={tekst(
                  "notat.begrunnelse.bidrag.underholdskostnad.andreBarnTilBidragsmottaker",
                  "Andre barn til bidragsmottaker",
                )}
                labelBold
              >{`${barn.gjelderBarn.navn} / ${dateToDDMMYYYY(barn.gjelderBarn.fødselsdato)}`}</Feltrad>
            </FeltTabell>
          </div>
          <FaktiskeTilsynsutgifterTabell data={barn} />
        </>
      ))}
      <NotatBegrunnelse data={data[0].begrunnelse} />
    </div>
  );
}
function UnderholdskostnaderSøknadsbarn({
  data,
}: {
  data: NotatUnderholdBarnDto;
}) {
  return (
    <>
      <div className={"mb-2 mt-2"}>
        <FeltTabell>
          <Feltrad label={rolleTilVisningsnavnV2(data.gjelderBarn)} labelBold>
            {data.gjelderBarn.navn}
          </Feltrad>
        </FeltTabell>
      </div>
      <div>
        <FeltTabell>
          <Feltrad
            label={tekst(
              "notat.begrunnelse.bidrag.underholdskostnad.harTilsynsutgift",
              "Barn har tilsynsutgift/forpleining",
            )}
          >
            {data.harTilsynsordning
              ? tekst("ord.ja", "Ja")
              : tekst("ord.nei", "Nei")}
          </Feltrad>
        </FeltTabell>
      </div>
      {data.harTilsynsordning && (
        <>
          <StønadTilBarnetilsynTabell data={data} />
          <FaktiskeTilsynsutgifterTabell data={data} />
          <TilleggstønadTabell data={data} />
          <ForpleiningTabell data={data} />
        </>
      )}
      <UnderholdskostnaderTabell data={data} />
      <NotatBegrunnelse data={data?.begrunnelse} />
    </>
  );
}
function TilleggstønadTabell({ data }: { data: NotatUnderholdBarnDto }) {
  if (data.tilleggsstønad.length == 0) return null;
  return (
    <Underseksjon
      tittel={tekst(
        "notat.begrunnelse.bidrag.underholdskostnad.tilleggsstønad",
        "Tilleggsstønad",
      )}
    >
      <DokumentTabell
        layoutAuto
        bredde="320px"
        kolonner={[
          {
            tittel: tekst(
              "felles.tabell.felles.fraTilOgMed",
              "Fra og med - Til og med",
            ),
          },
          { tittel: tekst("ord.beløp", "Beløp") },
          {
            tittel: tekst(
              "felles.tabell.underholdskostnader.tilleggsstønad.beløpstype",
              "Beløpstype",
            ),
          },
          {
            tittel: tekst(
              "felles.tabell.underholdskostnader.faktiskeTilsynsutgifter.totalt",
              "Totalt (12 mnd)",
            ),
          },
        ]}
      >
        {data.tilleggsstønad.map((tilleggsstønad, index) => (
          <Tabellrad key={index} index={index}>
            <Tabellcelle>
              {formatPeriode(
                tilleggsstønad.periode.fom,
                tilleggsstønad.periode.tom,
              )}
            </Tabellcelle>
            <Tabellcelle>
              {formatterBeløpForBeregning(tilleggsstønad.beløp)}
            </Tabellcelle>
            <Tabellcelle>
              {beløpstypeVisningsnavn(tilleggsstønad.beløpstype)}
            </Tabellcelle>
            <Tabellcelle>
              {formatterBeløpForBeregning(tilleggsstønad.total)}
            </Tabellcelle>
          </Tabellrad>
        ))}
      </DokumentTabell>
    </Underseksjon>
  );
}
function ForpleiningTabell({ data }: { data: NotatUnderholdBarnDto }) {
  if (!data.forpleining?.length) return null;
  return (
    <Underseksjon
      tittel={tekst(
        "notat.begrunnelse.bidrag.underholdskostnad.forpleining",
        "Forpleining",
      )}
    >
      <DokumentTabell
        layoutAuto
        bredde="320px"
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
        {data.forpleining.map((forpleining, index) => (
          <Tabellrad key={index} index={index}>
            <Tabellcelle>
              {formatPeriode(forpleining.periode.fom, forpleining.periode.tom)}
            </Tabellcelle>
            <Tabellcelle>
              {formatterBeløpForBeregning(forpleining.beløp)}
            </Tabellcelle>
          </Tabellrad>
        ))}
      </DokumentTabell>
    </Underseksjon>
  );
}
function FaktiskeTilsynsutgifterTabell({
  data,
}: {
  data: NotatUnderholdBarnDto;
}) {
  if (data.faktiskTilsynsutgift.length == 0) return null;
  return (
    <Underseksjon
      tittel={tekst(
        "notat.begrunnelse.bidrag.underholdskostnad.faktiskeTilsynsutgifter",
        "Faktiske tilsynsutgifter",
      )}
      avstandOver={false}
    >
      <DokumentTabell
        kolonner={[
          {
            tittel: tekst(
              "felles.tabell.underholdskostnader.faktiskeTilsynsutgifter.totalTilsynsutgift",
              "Total tilsynsutgift",
            ),
            bredde: "100px",
          },
          {
            tittel: tekst(
              "felles.tabell.underholdskostnader.faktiskeTilsynsutgifter.fratrekk",
              "Fratrekk",
            ),
          },
          {
            tittel: tekst(
              "felles.tabell.underholdskostnader.faktiskeTilsynsutgifter.totalt",
              "Totalt (12 mnd)",
            ),
          },
          {
            tittel: tekst(
              "felles.tabell.underholdskostnader.faktiskeTilsynsutgifter.kommentar",
              "Kommentar",
            ),
            bredde: "300px",
          },
        ]}
      >
        {data.faktiskTilsynsutgift.map((utgift, index) => (
          <Fragment key={index}>
            <Tabellrad index={1} periode>
              <Tabellcelle fullSpan>
                {formatPeriode(utgift.periode.fom, utgift.periode.tom)}
              </Tabellcelle>
            </Tabellrad>
            <Tabellrad index={index} harPeriode>
              <Tabellcelle>
                {formatterBeløpForBeregning(utgift.utgift)}
              </Tabellcelle>
              <Tabellcelle>
                {formatterBeløpForBeregning(utgift.kostpenger)}
              </Tabellcelle>
              <Tabellcelle>
                {formatterBeløpForBeregning(utgift.total)}
              </Tabellcelle>
              <Tabellcelle>{utgift.kommentar}</Tabellcelle>
            </Tabellrad>
          </Fragment>
        ))}
      </DokumentTabell>
    </Underseksjon>
  );
}
function StønadTilBarnetilsynTabell({ data }: { data: NotatUnderholdBarnDto }) {
  if (data.stønadTilBarnetilsyn.length == 0) return null;
  return (
    <Underseksjon
      tittel={tekst(
        "notat.begrunnelse.bidrag.underholdskostnad.stønadTilBarnetilsyn",
        "Stønad til barnetilsyn",
      )}
    >
      <DokumentTabell
        layoutAuto
        bredde="400px"
        kolonner={[
          {
            tittel: tekst(
              "felles.tabell.felles.fraTilOgMed",
              "Fra og med - Til og med",
            ),
          },
          {
            tittel: tekst(
              "felles.tabell.underholdskostnader.stønadTilBarnetilsyn.stønadTilBarnetilsyn",
              "Stønad til barnetilsyn",
            ),
          },
          {
            tittel: tekst(
              "felles.tabell.underholdskostnader.stønadTilBarnetilsyn.omfang",
              "Omfang",
            ),
          },
          { tittel: tekst("felles.tabell.felles.kilde", "Kilde") },
        ]}
      >
        {data.stønadTilBarnetilsyn.map((stønad, index) => (
          <Tabellrad key={index} index={index}>
            <Tabellcelle>
              {formatPeriode(stønad.periode.fom, stønad.periode.tom)}
            </Tabellcelle>
            <Tabellcelle>{stønad.skoleaderVisningsnavn}</Tabellcelle>
            <Tabellcelle>{stønad.tilsynstypeVisningsnavn}</Tabellcelle>
            <Tabellcelle>
              <KildeIcon kilde={stønad.kilde} />
            </Tabellcelle>
          </Tabellrad>
        ))}
      </DokumentTabell>
    </Underseksjon>
  );
}
function UnderholdskostnaderTabell({ data }: { data: NotatUnderholdBarnDto }) {
  if (data.underholdskostnad.length == 0) return null;
  return (
    <Underseksjon
      tittel={tekst(
        "notat.begrunnelse.bidrag.underholdskostnad.underholdskostnader",
        "Underholdskostnader",
      )}
    >
      <DokumentTabell
        kolonner={[
          {
            tittel: tekst(
              "felles.tabell.underholdskostnader.beregning.forbruk",
              "Forbruk",
            ),
            bredde: "80px",
          },
          {
            tittel: tekst(
              "felles.tabell.underholdskostnader.beregning.boutgifter",
              "Boutgifter",
            ),
            bredde: "80px",
          },
          {
            tittel: tekst(
              "felles.tabell.underholdskostnader.beregning.stønadTilBarnetilsyn",
              "Stønad til barnetilsyn",
            ),
            bredde: "100px",
          },
          {
            tittel: tekst(
              "felles.tabell.underholdskostnader.beregning.beregnetTilsyn",
              "Beregnet tilsynsutgift",
            ),
            bredde: "100px",
          },
          {
            tittel: tekst(
              "felles.tabell.underholdskostnader.beregning.barnetrygd",
              "Barnetrygd",
            ),
            bredde: "80px",
          },
          {
            tittel: tekst(
              "felles.tabell.underholdskostnader.beregning.forpleining",
              "Forpleining",
            ),
            bredde: "80px",
          },
          {
            tittel: tekst(
              "felles.tabell.underholdskostnader.beregning.underholdskostnad",
              "Underholdskostnad",
            ),
          },
        ]}
      >
        {data.underholdskostnad.map((kostnad, index) => (
          <Fragment key={index}>
            <Tabellrad index={1} periode>
              <Tabellcelle fullSpan>
                {formatPeriode(kostnad.periode.fom, kostnad.periode.tom)}
              </Tabellcelle>
            </Tabellrad>
            <Tabellrad index={index} harPeriode>
              <Tabellcelle>
                {formatterBeløpForBeregning(kostnad.forbruk)}
              </Tabellcelle>
              <Tabellcelle>
                {formatterBeløpForBeregning(kostnad.boutgifter)}
              </Tabellcelle>
              <Tabellcelle>
                {formatterBeløpForBeregning(kostnad.stønadTilBarnetilsyn)}
              </Tabellcelle>
              <Tabellcelle>
                {formatterBeløpForBeregning(kostnad.tilsynsutgifter)}
              </Tabellcelle>
              <Tabellcelle>
                {formatterBeløpForBeregning(kostnad.barnetrygd)}
              </Tabellcelle>
              <Tabellcelle>
                {formatterBeløpForBeregning(kostnad.forpleining ?? 0)}
              </Tabellcelle>
              <Tabellcelle>
                {formatterBeløpForBeregning(kostnad.total)}
              </Tabellcelle>
            </Tabellrad>
          </Fragment>
        ))}
      </DokumentTabell>
    </Underseksjon>
  );
}
