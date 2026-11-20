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
import ord from "~/tekster/ord.yaml";
import særbidragOpplysningerTekster from "~/tekster/notat/opplysninger/særbidrag.yaml";
import { BPsEvneTable } from "~/components/vedtak/BPsEvneTable";
import {
  DokumentTabell,
  Tabellcelle,
  Tabellrad,
} from "~/components/DokumentTabell";
import { BPsAndel } from "~/routes/notat/opplysninger/særbidrag/BPAndelTable";
import { BPsBeregnedeTotalbidrag } from "~/routes/notat/opplysninger/særbidrag/BPsBeregnedeTotalbidrag";

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

      <DokumentTabell
        kolonner={[
          { tittel: ord.beskrivelse },
          { tittel: "", tekstjustering: "right" },
        ]}
      >
        <Tabellrad index={0}>
          <Tabellcelle>{t.resultat.bpHarEvne}</Tabellcelle>
          <Tabellcelle textAlign="right">
            {!resultat.bpHarEvne
              ? t.resultat.bpHarIkkeEvneVerdi
              : t.resultat.bpHarEvneVerdi}
          </Tabellcelle>
        </Tabellrad>
        <Tabellrad index={1}>
          <Tabellcelle>{ord.resultat}</Tabellcelle>
          <Tabellcelle textAlign="right">
            {erBeregningeAvslag
              ? ord.avslag
              : formatterBeløp(resultat.resultat, true)}
          </Tabellcelle>
        </Tabellrad>
        <Tabellrad index={2}>
          <Tabellcelle>{t.resultat.betaltAvBidragspliktig}</Tabellcelle>
          <Tabellcelle textAlign="right">
            {formatterBeløp(resultat.beregning?.totalBeløpBetaltAvBp, true)}
          </Tabellcelle>
        </Tabellrad>
        <Tabellrad index={3}>
          <Tabellcelle>
            {data.medInnkreving
              ? t.resultat.beløpSomInnkreves
              : t.resultat.beløpSomInnkrevesUtenInnkreving}
          </Tabellcelle>
          <Tabellcelle textAlign="right">
            {erBeregningeAvslag
              ? ord.avslag
              : formatterBeløp(resultat.beløpSomInnkreves, true)}
          </Tabellcelle>
        </Tabellrad>
      </DokumentTabell>
    </>
  );
}
