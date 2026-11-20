import { Rolletype } from "~/types/Api";
import { useBeregningDetaljer } from "~/routes/notat/opplysninger/bidrag/VedleggBeregningsDetaljer";
import {
  formatterBeløpForBeregning,
  formatterProsent,
  rolleTilVisningsnavn,
} from "~/utils/visningsnavn";
import { FeltTabell, Feltrad } from "~/components/FeltTabell";
import CalculationTable from "~/components/notat_felles/CalculationTable";
import { barnetilleggTiltakspengerVisningsnavn } from "~/constants/beregning";
import { tekst } from "~/utils/tekst";
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
      <h4>
        {tekst(
          "notat.opplysninger.bidrag.beregningsdetaljer.barnetilleggSkattesats.tittel",
          "Beregning av skatteprosent på barnetillegg ({rolle})",
        ).replace("{rolle}", rolleTilVisningsnavn(rolle))}
      </h4>
      <CalculationTable
        data={[
          {
            label: tekst(
              "notat.opplysninger.bidrag.beregningsdetaljer.barnetilleggSkattesats.skatt",
              "Skatt",
            ),
            result: formatterBeløpForBeregning(
              barnetilleggSkattesats.skattAlminneligInntekt,
              true,
            ),
          },
          {
            label: tekst(
              "notat.opplysninger.bidrag.beregningsdetaljer.barnetilleggSkattesats.trygdeavgift",
              "Trygdeavgift",
            ),
            result: formatterBeløpForBeregning(
              barnetilleggSkattesats.trygdeavgift,
              true,
            ),
          },
          {
            label: tekst(
              "notat.opplysninger.bidrag.beregningsdetaljer.barnetilleggSkattesats.trinnskatt",
              "Trinnskatt",
            ),
            result: formatterBeløpForBeregning(
              barnetilleggSkattesats.trinnskatt,
              true,
            ),
          },
          {
            label: tekst(
              "notat.opplysninger.bidrag.beregningsdetaljer.barnetilleggSkattesats.sumSkatt",
              "Sum skatt",
            ),
            labelBold: true,
            result: formatterBeløpForBeregning(
              barnetilleggSkattesats.sumSkatt,
              true,
            ),
          },
        ]}
      />
      <div>
        <FeltTabell>
          <Feltrad label={tekst("ord.inntekt", "Inntekt")}>
            {formatterBeløpForBeregning(barnetilleggSkattesats.sumInntekt)}
          </Feltrad>
          <Feltrad
            label={tekst(
              "notat.opplysninger.bidrag.beregningsdetaljer.barnetilleggSkattesats.skattesats",
              "Skattesats",
            )}
          >{`${formatterBeløpForBeregning(barnetilleggSkattesats.sumSkatt, true)} / ${formatterBeløpForBeregning(barnetilleggSkattesats.sumInntekt)} = ${formatterProsent(barnetilleggSkattesats.skattFaktor)}`}</Feltrad>
        </FeltTabell>
      </div>
    </div>
  );
};
