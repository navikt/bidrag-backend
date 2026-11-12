import elementIds from "~/utils/elementIds";
import { formatterBeløp } from "~/utils/visningsnavn";
import {
  Resultatkode,
  NotatResultatSaerbidragsberegningDto,
} from "~/types/Api";
import {
  useNotatFelles,
  useVedleggNummer,
} from "~/components/notat_felles/NotatContext";
import { erResutlatMedBeregning } from "~/routes/notat.særbidrag/SærbidragHelpers";
import ord from "~/tekster/ord.json";
import særbidragOpplysningerTekster from "~/tekster/notat/særbidrag/opplysninger.json";
import { BPsEvneTable } from "~/components/vedtak/BPsEvneTable";
import { CommonTable, TableHeader } from "~/components/CommonTable";
import { BPsAndel } from "~/routes/notat.særbidrag/BPAndelTable";
import { BPsBeregnedeTotalbidrag } from "~/routes/notat.særbidrag/BPsBeregnedeTotalbidrag";

const tekst = særbidragOpplysningerTekster.beregningsdetaljer;

const t = tekst.vedleggBeregningsdetaljer;

export default function VedleggBeregningsDetaljer() {
  const vedleggNummer = useVedleggNummer("beregningsdetaljer");
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
        {t.tittel.replace("{}", vedleggNummer.toString())}
      </h2>
      <VedleggBeregningsDetaljerInnhold />
    </div>
  );
}

function VedleggBeregningsDetaljerInnhold() {
  const { data } = useNotatFelles();
  const resultatPerioder = data.vedtak?.resultat;
  if (resultatPerioder.length == 0) return <div>{ord.manglerResultat}</div>;

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
              name: ord.beskrivelse,
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
                  content: t.resultat.bpHarEvne,
                },
                {
                  textAlign: "right",
                  content: !resultat.bpHarEvne
                    ? t.resultat.bpHarIkkeEvneVerdi
                    : t.resultat.bpHarEvneVerdi,
                },
              ],
            },
            {
              columns: [
                {
                  content: ord.resultat,
                },
                {
                  textAlign: "right",
                  content: erBeregningeAvslag
                    ? ord.avslag
                    : formatterBeløp(resultat.resultat, true),
                },
              ],
            },
            {
              columns: [
                {
                  content: t.resultat.betaltAvBidragspliktig,
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
                    ? t.resultat.beløpSomInnkreves
                    : t.resultat.beløpSomInnkrevesUtenInnkreving,
                },
                {
                  textAlign: "right",
                  content: erBeregningeAvslag
                    ? ord.avslag
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
