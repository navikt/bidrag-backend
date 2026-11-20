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
import ord from "~/tekster/ord.yaml";
import tekster from "~/tekster";
import bidragTekster from "~/tekster/notat/begrunnelse/bidrag.yaml";
import {
  formatterBeløpForBeregning,
  rolleTilVisningsnavnV2,
  beløpstypeVisningsnavn,
} from "~/utils/visningsnavn";
import KildeIcon from "~/components/KildeIcon";

const t = bidragTekster.underholdskostnad;

export default function Underholdskostnad() {
  const { data, erAvslag } = useNotatFelles();
  if (erAvslag || data.underholdskostnader == null) return null;
  const { søknadsbarn, andreBarn } = delUnderholdskostnader(
    data.underholdskostnader.underholdskostnaderBarn,
  );
  return (
    <Seksjon
      tittel={t.tittel}
      vedlegg={"underholdskostnader"}
      lenketekst={t.lenkeTilVedlegg}
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
                label={t.andreBarnTilBidragsmottaker}
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
          <Feltrad label={t.harTilsynsutgift}>
            {data.harTilsynsordning ? ord.ja : ord.nei}
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
    <Underseksjon tittel={t.tilleggsstønad}>
      <DokumentTabell
        layoutAuto
        bredde="320px"
        kolonner={[
          { tittel: tekster.tabell.felles.fraTilOgMed },
          { tittel: ord.beløp },
          {
            tittel:
              tekster.tabell.underholdskostnader.tilleggsstønad.beløpstype,
          },
          {
            tittel:
              tekster.tabell.underholdskostnader.faktiskeTilsynsutgifter.totalt,
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
    <Underseksjon tittel={t.forpleining}>
      <DokumentTabell
        layoutAuto
        bredde="320px"
        kolonner={[
          { tittel: tekster.tabell.felles.fraTilOgMed },
          { tittel: ord.beløp },
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
    <Underseksjon tittel={t.faktiskeTilsynsutgifter} avstandOver={false}>
      <DokumentTabell
        kolonner={[
          {
            tittel:
              tekster.tabell.underholdskostnader.faktiskeTilsynsutgifter
                .totalTilsynsutgift,
            bredde: "100px",
          },
          {
            tittel:
              tekster.tabell.underholdskostnader.faktiskeTilsynsutgifter
                .fratrekk,
          },
          {
            tittel:
              tekster.tabell.underholdskostnader.faktiskeTilsynsutgifter.totalt,
          },
          {
            tittel:
              tekster.tabell.underholdskostnader.faktiskeTilsynsutgifter
                .kommentar,
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
    <Underseksjon tittel={t.stønadTilBarnetilsyn}>
      <DokumentTabell
        layoutAuto
        bredde="400px"
        kolonner={[
          { tittel: tekster.tabell.felles.fraTilOgMed },
          {
            tittel:
              tekster.tabell.underholdskostnader.stønadTilBarnetilsyn
                .stønadTilBarnetilsyn,
          },
          {
            tittel:
              tekster.tabell.underholdskostnader.stønadTilBarnetilsyn.omfang,
          },
          { tittel: tekster.tabell.felles.kilde },
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
    <Underseksjon tittel={t.underholdskostnader}>
      <DokumentTabell
        kolonner={[
          {
            tittel: tekster.tabell.underholdskostnader.beregning.forbruk,
            bredde: "80px",
          },
          {
            tittel: tekster.tabell.underholdskostnader.beregning.boutgifter,
            bredde: "80px",
          },
          {
            tittel:
              tekster.tabell.underholdskostnader.beregning.stønadTilBarnetilsyn,
            bredde: "100px",
          },
          {
            tittel: tekster.tabell.underholdskostnader.beregning.beregnetTilsyn,
            bredde: "100px",
          },
          {
            tittel: tekster.tabell.underholdskostnader.beregning.barnetrygd,
            bredde: "80px",
          },
          {
            tittel: tekster.tabell.underholdskostnader.beregning.forpleining,
            bredde: "80px",
          },
          {
            tittel:
              tekster.tabell.underholdskostnader.beregning.underholdskostnad,
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
