import { Rolletype } from "~/types/Api";
import { useBeregningDetaljer } from "~/routes/notat/opplysninger/bidrag/VedleggBeregningsDetaljer";
import ord from "~/tekster/ord.yaml";
import bidragOpplysningerTekster from "~/tekster/notat/opplysninger/bidrag.yaml";
import {
  formatterBeløpForBeregning,
  formatterProsent,
  rolleTilVisningsnavn,
} from "~/utils/visningsnavn";
import { FeltTabell, Feltrad } from "~/components/FeltTabell";
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
      <div>
        <FeltTabell>
          <Feltrad label={ord.inntekt}>
            {formatterBeløpForBeregning(barnetilleggSkattesats.sumInntekt)}
          </Feltrad>
          <Feltrad
            label={t.skattesats}
          >{`${formatterBeløpForBeregning(barnetilleggSkattesats.sumSkatt, true)} / ${formatterBeløpForBeregning(barnetilleggSkattesats.sumInntekt)} = ${formatterProsent(barnetilleggSkattesats.skattFaktor)}`}</Feltrad>
        </FeltTabell>
      </div>
    </div>
  );
};
