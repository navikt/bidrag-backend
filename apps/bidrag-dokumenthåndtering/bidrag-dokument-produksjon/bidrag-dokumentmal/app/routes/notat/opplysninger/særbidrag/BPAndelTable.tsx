import { useNotatFelles } from "~/components/notat_felles/NotatContext";
import { NotatResultatSaerbidragsberegningDto } from "~/types/Api";
import {
  formatterBeløpForBeregning,
  formatterProsent,
} from "~/utils/visningsnavn";
import { FeltTabell, JusterteFeltrader } from "~/components/FeltTabell";
import {
  MathValue,
  MathMultiplication,
  CalculationTable,
} from "~/components/vedtak/CalculationTable";
import { tekst } from "~/utils/tekst";
export const BPsAndel = () => {
  const { data } = useNotatFelles();
  const beregnetSærbidrag = data.vedtak!
    .resultat[0] as NotatResultatSaerbidragsberegningDto;
  const delberegningBpsAndel = beregnetSærbidrag.bpsAndel!;
  const inntekter = beregnetSærbidrag.inntekter!;
  const utgifter = beregnetSærbidrag.delberegningUtgift!;
  return (
    <div style={{ width: "600px" }}>
      <h4>
        {tekst(
          "notat.opplysninger.særbidrag.beregningsdetaljer.bpsAndel.tittel",
          "Bidragspliktiges andel",
        )}
      </h4>
      <CalculationTable
        width={"55%"}
        className={"pb-2 "}
        data={[
          {
            label: tekst(
              "notat.opplysninger.særbidrag.beregningsdetaljer.bpsAndel.bidragspliktigesInntekt",
              "Bidragspliktiges inntekt",
            ),
            value: formatterBeløpForBeregning(inntekter.inntektBP, true),
          },
          {
            label: tekst(
              "notat.opplysninger.særbidrag.beregningsdetaljer.bpsAndel.bidragsmottakerensInntekt",
              "Bidragsmottakerens inntekt",
            ),
            value: formatterBeløpForBeregning(inntekter.inntektBM, true),
          },
          {
            label: tekst(
              "notat.opplysninger.særbidrag.beregningsdetaljer.bpsAndel.barnetsInntekt",
              "Barnets inntekt",
            ),
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
          label: tekst(
            "notat.opplysninger.særbidrag.beregningsdetaljer.bpsAndel.totalInntekt",
            "Total inntekt",
          ),
          value: formatterBeløpForBeregning(
            inntekter.totalEndeligInntekt,
            true,
          ),
        }}
      />

      <div className={"pt-2"}>
        <FeltTabell>
          <JusterteFeltrader
            rows={[
              {
                label: tekst(
                  "notat.opplysninger.særbidrag.beregningsdetaljer.bpsAndel.bpsAndelAvUtgifter",
                  "BP's andel av utgifter (i prosent)",
                ),
                value: `${formatterBeløpForBeregning(inntekter.inntektBP, true)} X ${formatterBeløpForBeregning(inntekter.totalEndeligInntekt)}`,
                result: `${formatterProsent(delberegningBpsAndel.beregnetAndelFaktor)}`,
              },
              {
                label: tekst(
                  "notat.opplysninger.særbidrag.beregningsdetaljer.bpsAndel.andelUtgifter",
                  "Andel utgifter",
                ),
                value: `${formatterBeløpForBeregning(utgifter.sumGodkjent, true)} X ${formatterProsent(delberegningBpsAndel.endeligAndelFaktor)}`,
                result: `${formatterBeløpForBeregning(delberegningBpsAndel.andelBeløp, true)}`,
              },
            ]}
          />
        </FeltTabell>
      </div>
      <>
        {delberegningBpsAndel.endeligAndelFaktor !==
          delberegningBpsAndel.beregnetAndelFaktor && (
          <div className="text-red-500" style={{ width: "200px" }}>
            {tekst(
              "notat.opplysninger.særbidrag.beregningsdetaljer.bpsAndel.andelBegrensetTil",
              "Andel begrenset til",
            )}{" "}
            {formatterProsent(delberegningBpsAndel.endeligAndelFaktor)}
          </div>
        )}
      </>
    </div>
  );
};
