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
import { erResutlatMedBeregning } from "~/routes/notat/felles/SærbidragHelpers";
import { BPsEvneTable } from "~/components/vedtak/BPsEvneTable";
import {
  DokumentTabell,
  Tabellcelle,
  Tabellrad,
} from "~/components/DokumentTabell";
import { BPsAndel } from "~/routes/notat/opplysninger/særbidrag/BPAndelTable";
import { BPsBeregnedeTotalbidrag } from "~/routes/notat/opplysninger/særbidrag/BPsBeregnedeTotalbidrag";
import { tekst } from "~/utils/tekst";
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
        {tekst(
          "notat.opplysninger.særbidrag.beregningsdetaljer.vedleggBeregningsdetaljer.tittel",
          "Vedlegg nr. {}: Beregningsdetaljer",
        ).replace("{}", vedleggNummer.toString())}
      </h2>
      <VedleggBeregningsDetaljerInnhold />
    </div>
  );
}
function VedleggBeregningsDetaljerInnhold() {
  const { data } = useNotatFelles();
  const resultatPerioder = data.vedtak?.resultat;
  if (resultatPerioder.length == 0)
    return <div>{tekst("ord.manglerResultat", "Mangler resultat")}</div>;
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

      <DokumentTabell
        kolonner={[
          { tittel: tekst("ord.beskrivelse", "Beskrivelse") },
          { tittel: "", tekstjustering: "right" },
        ]}
      >
        <Tabellrad index={0}>
          <Tabellcelle>
            {tekst(
              "notat.opplysninger.særbidrag.beregningsdetaljer.vedleggBeregningsdetaljer.resultat.bpHarEvne",
              "BP har evne",
            )}
          </Tabellcelle>
          <Tabellcelle textAlign="right">
            {!resultat.bpHarEvne
              ? tekst(
                  "notat.opplysninger.særbidrag.beregningsdetaljer.vedleggBeregningsdetaljer.resultat.bpHarIkkeEvneVerdi",
                  "Nei, bidragsevnen er lavere enn beregnet totalbidrag",
                )
              : tekst(
                  "notat.opplysninger.særbidrag.beregningsdetaljer.vedleggBeregningsdetaljer.resultat.bpHarEvneVerdi",
                  "Ja, bidragsevnen er høyere enn beregnet totalbidrag",
                )}
          </Tabellcelle>
        </Tabellrad>
        <Tabellrad index={1}>
          <Tabellcelle>{tekst("ord.resultat", "Resultat")}</Tabellcelle>
          <Tabellcelle textAlign="right">
            {erBeregningeAvslag
              ? tekst("ord.avslag", "Avslag")
              : formatterBeløp(resultat.resultat, true)}
          </Tabellcelle>
        </Tabellrad>
        <Tabellrad index={2}>
          <Tabellcelle>
            {tekst(
              "notat.opplysninger.særbidrag.beregningsdetaljer.vedleggBeregningsdetaljer.resultat.betaltAvBidragspliktig",
              "Betalt av Bidragspliktig",
            )}
          </Tabellcelle>
          <Tabellcelle textAlign="right">
            {formatterBeløp(resultat.beregning?.totalBeløpBetaltAvBp, true)}
          </Tabellcelle>
        </Tabellrad>
        <Tabellrad index={3}>
          <Tabellcelle>
            {data.medInnkreving
              ? tekst(
                  "notat.opplysninger.særbidrag.beregningsdetaljer.vedleggBeregningsdetaljer.resultat.beløpSomInnkreves",
                  "Beløp som innkreves",
                )
              : tekst(
                  "notat.opplysninger.særbidrag.beregningsdetaljer.vedleggBeregningsdetaljer.resultat.beløpSomInnkrevesUtenInnkreving",
                  "Fastsatt beløp å betale",
                )}
          </Tabellcelle>
          <Tabellcelle textAlign="right">
            {erBeregningeAvslag
              ? tekst("ord.avslag", "Avslag")
              : formatterBeløp(resultat.beløpSomInnkreves, true)}
          </Tabellcelle>
        </Tabellrad>
      </DokumentTabell>
    </>
  );
}
