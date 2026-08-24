import elementIds from "~/utils/elementIds";
import { formatterBeløp } from "~/utils/visningsnavn";
import {
  Resultatkode,
  NotatResultatSaerbidragsberegningDto,
} from "~/types/Api";
import { useNotatFelles } from "~/components/notat_felles/NotatContext";
import { erResutlatMedBeregning } from "~/routes/notat.særbidrag/SærbidragHelpers";
import tekster from "~/tekster";
import { BPsEvneTable } from "~/components/vedtak/BPsEvneTable";
import { CommonTable, TableHeader } from "~/components/CommonTable";
import { BPsAndel } from "~/routes/notat.særbidrag/BPAndelTable";
import { BPsBeregnedeTotalbidrag } from "~/routes/notat.særbidrag/BPsBeregnedeTotalbidrag";
import { VedleggProps } from "~/types/commonTypes";

export default function VedleggBeregningsDetaljer({
  vedleggNummer = 2,
}: VedleggProps) {
  const { data } = useNotatFelles();
  if (
    !erResutlatMedBeregning(
      (data.vedtak?.resultat as NotatResultatSaerbidragsberegningDto[]) ?? [],
    )
  )
    return null;
  return (
    <div className={`${vedleggNummer == 1 ? "break-before-page" : ""}`}>
      <h2 id={elementIds.vedleggBeregningsdetaljer} className={"pb-2"}>
        Vedlegg nr. {vedleggNummer}: Beregningsdetaljer
      </h2>
      <VedleggBeregningsDetaljerInnhold />
    </div>
  );
}

function VedleggBeregningsDetaljerInnhold() {
  const { data } = useNotatFelles();
  const resultatPerioder = data.vedtak?.resultat;
  if (resultatPerioder.length == 0) return <div>Mangler resultat</div>;

  const resultat = data.vedtak
    .resultat[0]! as NotatResultatSaerbidragsberegningDto;

  const erBeregningeAvslag =
    resultat?.resultatKode !== Resultatkode.SAeRBIDRAGINNVILGET;

  const beregnetSærbidrag = data.vedtak!
    .resultat[0] as NotatResultatSaerbidragsberegningDto;

  return (
    <>
      <BPsAndel />
      <div className={"mb-medium"} />
      <BPsEvneTable
        inntekter={beregnetSærbidrag.inntekter!}
        delberegningBidragsevne={beregnetSærbidrag.delberegningBidragsevne!}
      />
      <div className={"mb-medium"} />
      <BPsBeregnedeTotalbidrag
        delberegning={
          beregnetSærbidrag.delberegningBidragspliktigesBeregnedeTotalbidrag!
        }
      />
      <div className={"mb-medium"} />

      <CommonTable
        data={{
          headers: [
            {
              name: tekster.tabell.felles.beskrivelse,
            },
            {
              name: "",
              textAlign: "right",
            },
          ].filter((d) => typeof d != "boolean") as TableHeader[],
          rows: [
            {
              columns: [
                {
                  content: "BP har evne",
                },
                {
                  textAlign: "right",
                  content: !resultat.bpHarEvne
                    ? "Nei, bidragsevnen er lavere enn beregnet totalbidrag"
                    : "Ja, bidragsevnen er høyere enn beregnet totalbidrag",
                },
              ],
            },
            {
              columns: [
                {
                  content: "Resultat",
                },
                {
                  textAlign: "right",
                  content: erBeregningeAvslag
                    ? "Avslag"
                    : formatterBeløp(resultat.resultat, true),
                },
              ],
            },
            {
              columns: [
                {
                  content: `Betalt av ${tekster.begreper.bidragspliktig}`,
                },
                {
                  textAlign: "right",
                  content: formatterBeløp(
                    resultat.beregning?.totalBeløpBetaltAvBp,
                    true,
                  ),
                },
              ],
            },
            {
              columns: [
                {
                  content: data.medInnkreving
                    ? "Beløp som innkreves"
                    : "Fastsatt beløp å betale",
                },
                {
                  textAlign: "right",
                  content: erBeregningeAvslag
                    ? "Avslag"
                    : formatterBeløp(resultat.beløpSomInnkreves, true),
                },
              ],
            },
          ],
        }}
      />
    </>
  );
}
