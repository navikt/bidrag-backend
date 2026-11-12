import { DataViewTable, DataViewTableData } from "~/components/DataViewTable";
import { formatterBeløpForBeregning } from "~/utils/visningsnavn";
import { useBeregningDetaljer } from "~/routes/notat.bidrag/VedleggBeregningsDetaljer";
import bidragOpplysningerTekster from "~/tekster/notat/bidrag/opplysninger.json";

const t = bidragOpplysningerTekster.beregningsdetaljer.samværsfradrag;

export const Samværsfradrag = () => {
  const { samværsfradrag } = useBeregningDetaljer();
  return (
    <div style={{ width: "600px" }}>
      <h4>{t.tittel}</h4>
      <DataViewTable
        gap={"5px"}
        data={
          [
            {
              label: t.samværsklasse,
              value: t.samværsklasseVerdi
                .replace(
                  "{samværsklasse}",
                  `${samværsfradrag.samværsklasseVisningsnavn}`,
                )
                .replace(
                  "{samværPerMåned}",
                  `${samværsfradrag.gjennomsnittligSamværPerMåned}`,
                ),
            },
            {
              label: t.samværsfradrag,
              value: formatterBeløpForBeregning(samværsfradrag.samværsfradrag),
            },
          ].filter((d) => d != null) as DataViewTableData[]
        }
      />
    </div>
  );
};
