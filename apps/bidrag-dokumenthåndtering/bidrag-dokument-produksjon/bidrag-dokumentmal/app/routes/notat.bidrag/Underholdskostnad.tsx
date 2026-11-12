import {
  useNotatFelles,
  useVedleggNummer,
} from "~/components/notat_felles/NotatContext";
import NotatBegrunnelse from "~/components/NotatBegrunnelse";
import { NotatUnderholdBarnDto } from "~/types/Api";
import { DataViewTable } from "~/components/DataViewTable";
import { CommonTable } from "~/components/CommonTable";
import { formatPeriode, dateToDDMMYYYY, sortByAge } from "~/utils/date-utils";
import ord from "~/tekster/ord.yaml";
import tekster from "~/tekster";
import bidragTekster from "~/tekster/notat/bidrag/saksbehandler.yaml";
import {
  formatterBeløpForBeregning,
  rolleTilVisningsnavnV2,
  beløpstypeVisningsnavn,
} from "~/utils/visningsnavn";
import KildeIcon from "~/components/KildeIcon";
import elementIds from "~/utils/elementIds";

const t = bidragTekster.underholdskostnad;

export default function Underholdskostnad() {
  const vedleggNummer = useVedleggNummer("underholdskostnader");
  const { data, erAvslag } = useNotatFelles();
  if (erAvslag) return null;

  const underholdskostnader = data.underholdskostnader;
  if (underholdskostnader == null) return null;
  return (
    <>
      <div className={"section-title elements_inline"}>
        <h2>{t.tittel}</h2>
        <a href={`#${elementIds.vedleggUnderholdskostnader}`}>
          {t.lenkeTilVedlegg.replace("{}", `${vedleggNummer}`)}
        </a>
      </div>
      <>
        {underholdskostnader.underholdskostnaderBarn
          .filter((barn) => barn.gjelderBarn.rolle != null)
          .sort((a, b) => sortByAge(a.gjelderBarn, b.gjelderBarn))
          .map((barn, i) => (
            <UnderholdskostnaderSøknadsbarn
              data={barn}
              key={i + barn.gjelderBarn.ident!}
            />
          ))}
        <UnderholdskostnaderAndreBarn
          data={underholdskostnader.underholdskostnaderBarn.filter(
            (barn) => barn.gjelderBarn.rolle == null,
          )}
        />
      </>
    </>
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
          <DataViewTable
            className={"pt-2 pb-0 mb-2 mt-2"}
            data={[
              {
                label: t.andreBarnTilBidragsmottaker,
                labelBold: true,
                value: `${barn.gjelderBarn.navn} / ${dateToDDMMYYYY(barn.gjelderBarn.fødselsdato)}`,
              },
            ]}
          />
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
      <DataViewTable
        className={"mb-2 mt-2"}
        data={[
          {
            label: rolleTilVisningsnavnV2(data.gjelderBarn),
            labelBold: true,
            value: data.gjelderBarn.navn,
          },
        ]}
      />
      <DataViewTable
        data={[
          {
            label: t.harTilsynsutgift,
            value: data.harTilsynsordning ? ord.ja : ord.nei,
          },
        ]}
      />
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
    <div className={"mb-4 mt-4"}>
      <h4>{t.tilleggsstønad}</h4>
      <CommonTable
        layoutAuto
        width={"320px"}
        data={{
          headers: [
            {
              name: tekster.tabell.felles.fraTilOgMed,
            },
            {
              name: ord.beløp,
            },
            {
              name: tekster.tabell.underholdskostnader.tilleggsstønad
                .beløpstype,
            },
            {
              name: tekster.tabell.underholdskostnader.faktiskeTilsynsutgifter
                .totalt,
            },
          ],
          rows: data.tilleggsstønad.map((d) => ({
            columns: [
              { content: formatPeriode(d.periode.fom, d.periode.tom) },
              { content: formatterBeløpForBeregning(d.beløp) },
              {
                content: beløpstypeVisningsnavn(d.beløpstype),
              },
              { content: formatterBeløpForBeregning(d.total) },
            ],
          })),
        }}
      />
    </div>
  );
}

function ForpleiningTabell({ data }: { data: NotatUnderholdBarnDto }) {
  if (!data.forpleining?.length) return null;

  return (
    <div className={"mb-4 mt-4"}>
      <h4>{t.forpleining}</h4>
      <CommonTable
        layoutAuto
        width={"320px"}
        data={{
          headers: [
            {
              name: tekster.tabell.felles.fraTilOgMed,
            },
            {
              name: ord.beløp,
            },
          ],
          rows: data.forpleining.map((d) => ({
            columns: [
              { content: formatPeriode(d.periode.fom, d.periode.tom) },
              { content: formatterBeløpForBeregning(d.beløp) },
            ],
          })),
        }}
      />
    </div>
  );
}

function FaktiskeTilsynsutgifterTabell({
  data,
}: {
  data: NotatUnderholdBarnDto;
}) {
  if (data.faktiskTilsynsutgift.length == 0) return null;
  return (
    <div className={"mb-4"}>
      <h4>{t.faktiskeTilsynsutgifter}</h4>
      <CommonTable
        data={{
          headers: [
            {
              name: tekster.tabell.underholdskostnader.faktiskeTilsynsutgifter
                .totalTilsynsutgift,
              width: "100px",
            },
            {
              name: tekster.tabell.underholdskostnader.faktiskeTilsynsutgifter
                .fratrekk,
            },
            {
              name: tekster.tabell.underholdskostnader.faktiskeTilsynsutgifter
                .totalt,
            },
            {
              name: tekster.tabell.underholdskostnader.faktiskeTilsynsutgifter
                .kommentar,
              width: "300px",
            },
          ],
          rows: data.faktiskTilsynsutgift.flatMap((d) => [
            {
              periodColumn: formatPeriode(d.periode.fom, d.periode.tom),
              columns: [
                { content: formatterBeløpForBeregning(d.utgift) },
                { content: formatterBeløpForBeregning(d.kostpenger) },
                { content: formatterBeløpForBeregning(d.total) },
                { content: d.kommentar },
              ],
            },
          ]),
        }}
      />
    </div>
  );
}

function StønadTilBarnetilsynTabell({ data }: { data: NotatUnderholdBarnDto }) {
  if (data.stønadTilBarnetilsyn.length == 0) return null;
  return (
    <div className={"mb-4 mt-4"}>
      <h4>{t.stønadTilBarnetilsyn}</h4>
      <CommonTable
        layoutAuto
        width={"400px"}
        data={{
          headers: [
            {
              name: tekster.tabell.felles.fraTilOgMed,
            },
            {
              name: tekster.tabell.underholdskostnader.stønadTilBarnetilsyn
                .stønadTilBarnetilsyn,
            },
            {
              name: tekster.tabell.underholdskostnader.stønadTilBarnetilsyn
                .omfang,
            },
            {
              name: tekster.tabell.felles.kilde,
            },
          ],
          rows: data.stønadTilBarnetilsyn.map((d) => ({
            columns: [
              { content: formatPeriode(d.periode.fom, d.periode.tom) },
              { content: d.skoleaderVisningsnavn },
              { content: d.tilsynstypeVisningsnavn },
              {
                content: <KildeIcon kilde={d.kilde} />,
              },
            ],
          })),
        }}
      />
    </div>
  );
}
function UnderholdskostnaderTabell({ data }: { data: NotatUnderholdBarnDto }) {
  if (data.underholdskostnad.length == 0) return null;

  return (
    <div className={"mb-4 mt-4"}>
      <h4>{t.underholdskostnader}</h4>
      <CommonTable
        data={{
          headers: [
            {
              name: tekster.tabell.underholdskostnader.beregning.forbruk,
              width: "80px",
            },
            {
              name: tekster.tabell.underholdskostnader.beregning.boutgifter,
              width: "80px",
            },
            {
              name: tekster.tabell.underholdskostnader.beregning
                .stønadTilBarnetilsyn,
              width: "100px",
            },
            {
              name: tekster.tabell.underholdskostnader.beregning.beregnetTilsyn,
              width: "100px",
            },
            {
              name: tekster.tabell.underholdskostnader.beregning.barnetrygd,
              width: "80px",
            },
            {
              name: tekster.tabell.underholdskostnader.beregning.forpleining,
              width: "80px",
            },
            {
              name: tekster.tabell.underholdskostnader.beregning
                .underholdskostnad,
            },
          ],
          rows: data.underholdskostnad.flatMap((d) => [
            {
              periodColumn: formatPeriode(d.periode.fom, d.periode.tom),
              columns: [
                { content: formatterBeløpForBeregning(d.forbruk) },
                { content: formatterBeløpForBeregning(d.boutgifter) },
                { content: formatterBeløpForBeregning(d.stønadTilBarnetilsyn) },
                { content: formatterBeløpForBeregning(d.tilsynsutgifter) },
                { content: formatterBeløpForBeregning(d.barnetrygd) },
                { content: formatterBeløpForBeregning(d.forpleining ?? 0) },
                { content: formatterBeløpForBeregning(d.total) },
              ],
            },
          ]),
        }}
      />
    </div>
  );
}
