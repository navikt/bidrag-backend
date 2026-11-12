import { useNotatFelles } from "~/components/notat_felles/NotatContext";
import { NotatResultatSaerbidragsberegningDto } from "~/types/Api";
import {
  formatterBeløpForBeregning,
  formatterProsent,
} from "~/utils/visningsnavn";
import særbidragOpplysningerTekster from "~/tekster/notat/særbidrag/opplysninger.json";
import { DataViewTableData } from "~/components/DataViewTable";
import { DataViewTableV2 } from "~/components/DataViewTableV2";
import {
  MathValue,
  MathMultiplication,
  CalculationTable,
} from "~/components/vedtak/CalculationTable";

const tekst = særbidragOpplysningerTekster.beregningsdetaljer;

const t = tekst.bpsAndel;

export const BPsAndel = () => {
  const { data } = useNotatFelles();

  const beregnetSærbidrag = data.vedtak!
    .resultat[0] as NotatResultatSaerbidragsberegningDto;
  const delberegningBpsAndel = beregnetSærbidrag.bpsAndel!;
  const inntekter = beregnetSærbidrag.inntekter!;
  const utgifter = beregnetSærbidrag.delberegningUtgift!;
  return (
    <div style={{ width: "600px" }}>
      <h4>{t.tittel}</h4>
      <CalculationTable
        width={"55%"}
        className={"pb-2 "}
        data={[
          {
            label: t.bidragspliktigesInntekt,
            value: formatterBeløpForBeregning(inntekter.inntektBP, true),
          },
          {
            label: t.bidragsmottakerensInntekt,
            value: formatterBeløpForBeregning(inntekter.inntektBM, true),
          },
          {
            label: t.barnetsInntekt,
            calculation:
              inntekter.inntektBarn! > 0 ? (
                <div>
                  <MathValue value={inntekter.inntektBarn!} /> -{" "}
                  <MathMultiplication
                    left="30"
                    right={beregnetSærbidrag.forskuddssats!}
                  />
                </div>
              ) : undefined,
            value: formatterBeløpForBeregning(
              inntekter.barnEndeligInntekt,
              true,
            ),
          },
        ]}
        result={{
          label: t.totalInntekt,
          value: formatterBeløpForBeregning(
            inntekter.totalEndeligInntekt,
            true,
          ),
        }}
      />

      <DataViewTableV2
        gap={"5px"}
        className={"pt-2"}
        data={
          [
            {
              label: t.bpsAndelAvUtgifter,
              value: `${formatterBeløpForBeregning(inntekter.inntektBP, true)} X ${formatterBeløpForBeregning(inntekter.totalEndeligInntekt)}`,
              result: `${formatterProsent(delberegningBpsAndel.beregnetAndelFaktor)}`,
            },
            {
              label: t.andelUtgifter,
              value: `${formatterBeløpForBeregning(utgifter.sumGodkjent, true)} X ${formatterProsent(delberegningBpsAndel.endeligAndelFaktor)}`,
              result: `${formatterBeløpForBeregning(delberegningBpsAndel.andelBeløp, true)}`,
            },
          ].filter((d) => d != null) as DataViewTableData[]
        }
      />
      <>
        {delberegningBpsAndel.endeligAndelFaktor !==
          delberegningBpsAndel.beregnetAndelFaktor && (
          <div className="text-red-500" style={{ width: "200px" }}>
            {t.andelBegrensetTil}{" "}
            {formatterProsent(delberegningBpsAndel.endeligAndelFaktor)}
          </div>
        )}
      </>
    </div>
  );
};
