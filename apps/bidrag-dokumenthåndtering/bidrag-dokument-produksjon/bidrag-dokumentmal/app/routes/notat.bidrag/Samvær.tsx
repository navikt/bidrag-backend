import { useNotatFelles } from "~/components/notat_felles/NotatContext";
import { NotatSamvaersperiodeDto, NotatSamvaerBarnDto } from "~/types/Api";
import { CommonTable } from "~/components/CommonTable";
import tekster from "~/tekster";
import { formatPeriode, sortByAge } from "~/utils/date-utils";
import { DataViewTable } from "~/components/DataViewTable";
import NotatBegrunnelse from "~/components/NotatBegrunnelse";
import elementIds from "~/utils/elementIds";
import { VedleggProps } from "~/types/commonTypes";
import { rolleTilVisningsnavnV2 } from "~/utils/visningsnavn";

export default function Samvær({ vedleggNummer }: VedleggProps) {
  const { data, erAvslag } = useNotatFelles();
  if (erAvslag) return null;
  const samvær = data.samværV2;
  if (samvær == null || samvær.barn.length === 0) return null;
  return (
    <div>
      <div className={"elements_inline section-title"}>
        <h2>Samvær</h2>
        <a href={`#${elementIds.vedleggSamvær}`}>
          se vedlegg nr. {vedleggNummer} for beregningsdetaljer
        </a>
      </div>
      {samvær.erSammeForAlle ? (
        <SamværBarnFelles data={samvær.barn} />
      ) : (
        <SamværBarn data={samvær.barn} />
      )}
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
        <div>Samme for alle søknadsbarn</div>
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
