import { Rolletype } from "~/types/Api";
import { useBeregningDetaljer } from "~/routes/notat.bidrag/VedleggBeregningsDetaljer";
import ord from "~/tekster/ord.yaml";
import bidragOpplysningerTekster from "~/tekster/notat/bidrag/opplysninger.yaml";
import {
  formatterBeløpForBeregning,
  formatterProsent,
  rolleTilVisningsnavn,
} from "~/utils/visningsnavn";
import { DataViewTable } from "~/components/DataViewTable";
import CalculationTable from "~/components/notat_felles/CalculationTable";
import { barnetilleggTiltakspengerVisningsnavn } from "~/constants/beregning";

const t = bidragOpplysningerTekster.beregningsdetaljer.barnetilleggSkattesats;

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
      <h4>{t.tittel.replace("{rolle}", rolleTilVisningsnavn(rolle))}</h4>
      <CalculationTable
        data={[
          {
            label: t.skatt,
            result: formatterBeløpForBeregning(
              barnetilleggSkattesats.skattAlminneligInntekt,
              true,
            ),
          },
          {
            label: t.trygdeavgift,
            result: formatterBeløpForBeregning(
              barnetilleggSkattesats.trygdeavgift,
              true,
            ),
          },
          {
            label: t.trinnskatt,
            result: formatterBeløpForBeregning(
              barnetilleggSkattesats.trinnskatt,
              true,
            ),
          },
          {
            label: t.sumSkatt,
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
            label: ord.inntekt,
            textRight: false,
            value: formatterBeløpForBeregning(
              barnetilleggSkattesats.sumInntekt,
            ),
          },
          {
            label: t.skattesats,
            textRight: false,
            value: `${formatterBeløpForBeregning(barnetilleggSkattesats.sumSkatt, true)} / ${formatterBeløpForBeregning(barnetilleggSkattesats.sumInntekt)} = ${formatterProsent(barnetilleggSkattesats.skattFaktor)}`,
          },
        ].filter((d) => d)}
      />
    </div>
  );
};
