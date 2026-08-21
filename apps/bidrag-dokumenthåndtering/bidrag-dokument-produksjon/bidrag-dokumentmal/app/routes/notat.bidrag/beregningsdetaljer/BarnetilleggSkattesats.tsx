import { Rolletype } from "~/types/Api";
import { useBeregningDetaljer } from "~/routes/notat.bidrag/VedleggBeregningsDetaljer";
import {
  formatterBeløpForBeregning,
  formatterProsent,
  rolleTilVisningsnavn,
} from "~/utils/visningsnavn";
import { DataViewTable } from "~/components/DataViewTable";
import CalculationTable from "~/components/notat_felles/CalculationTable";
import { barnetilleggTiltakspengerVisningsnavn } from "~/constants/beregning";

export const BarnetilleggSkattesats = ({ rolle }: { rolle: Rolletype }) => {
  const { barnetilleggBP, barnetilleggBM } = useBeregningDetaljer();

  const barnetillegg = rolle == Rolletype.BP ? barnetilleggBP : barnetilleggBM;
  const barnetilleggSkattesats = barnetillegg.delberegningSkattesats;

  if (barnetilleggSkattesats == null) return null;
  if (
    barnetillegg.barnetillegg.every(
      (bt) => bt.visningsnavn === barnetilleggTiltakspengerVisningsnavn,
    )
  )
    return null;

  return (
    <div className={"mb-2"}>
      <h4>{`Beregning av skatteprosent på barnetillegg (${rolleTilVisningsnavn(rolle)})`}</h4>
      <CalculationTable
        data={[
          {
            label: "Skatt",
            result: formatterBeløpForBeregning(
              barnetilleggSkattesats.skattAlminneligInntekt,
              true,
            ),
          },
          {
            label: "Trygdeavgift",
            result: formatterBeløpForBeregning(
              barnetilleggSkattesats.trygdeavgift,
              true,
            ),
          },
          {
            label: "Trinnskatt",
            result: formatterBeløpForBeregning(
              barnetilleggSkattesats.trinnskatt,
              true,
            ),
          },
          {
            label: "Sum skatt",
            labelBold: true,
            result: formatterBeløpForBeregning(
              barnetilleggSkattesats.sumSkatt,
              true,
            ),
          },
        ]}
      />
      <DataViewTable
        data={[
          {
            label: "Inntekt",
            textRight: false,
            value: formatterBeløpForBeregning(
              barnetilleggSkattesats.sumInntekt,
            ),
          },
          {
            label: "Skattesats",
            textRight: false,
            value: `${formatterBeløpForBeregning(barnetilleggSkattesats.sumSkatt, true)} / ${formatterBeløpForBeregning(barnetilleggSkattesats.sumInntekt)} = ${formatterProsent(barnetilleggSkattesats.skattFaktor)}`,
          },
        ].filter((d) => d)}
      />
    </div>
  );
};
