import {
  useNotatFelles,
  useVedleggNummer,
} from "~/components/notat_felles/NotatContext";
import elementIds from "~/utils/elementIds";
import {
  DokumentTabell,
  Tabellcelle,
  Tabellrad,
} from "~/components/DokumentTabell";
import DokumentStabel from "~/components/DokumentStabel";
import { NotatSamvaerBarnDto, NotatSamvaersperiodeDto } from "~/types/Api";
import { FeltTabell, Feltrad } from "~/components/FeltTabell";
import { formatPeriode } from "~/utils/date-utils";
import { rolleTilVisningsnavnV2 } from "~/utils/visningsnavn";
import { tekst } from "~/utils/tekst";
export default function VedleggSamvær() {
  const vedleggNummer = useVedleggNummer("samvær");
  const { data, erAvslag } = useNotatFelles();
  if (erAvslag) return null;
  return (
    <div className={`${vedleggNummer == 1 ? "break-before-page" : ""}`}>
      <h2 id={elementIds.vedleggSamvær}>
        {tekst(
          "notat.opplysninger.bidrag.samvær.tittel",
          "Vedlegg nr. {}: Samvær",
        ).replace("{}", `${vedleggNummer}`)}
      </h2>
      {(data.samværV2?.barn ?? []).map((samværBarn, index) => (
        <div key={index + "-" + samværBarn.gjelderBarn.ident}>
          <SamværsberegningDetaljerBarn samværBarn={samværBarn} key={index} />
        </div>
      ))}
    </div>
  );
}
function SamværsberegningDetaljerBarn({
  samværBarn,
}: {
  samværBarn: NotatSamvaerBarnDto;
}) {
  const samværsperioderMedBeregning = samværBarn.perioder.filter(
    (p) => p.beregning != null,
  );
  return (
    <DokumentStabel gap={2}>
      <div>
        <FeltTabell>
          <Feltrad
            label={rolleTilVisningsnavnV2(samværBarn.gjelderBarn)}
            labelBold
          >
            {samværBarn.gjelderBarn.navn}
          </Feltrad>
        </FeltTabell>
      </div>
      {samværsperioderMedBeregning.length == 0 ? (
        <p>
          {tekst(
            "notat.opplysninger.bidrag.samvær.ingenBeregnetSamvær",
            "Ingen beregnet samvær",
          )}
        </p>
      ) : (
        samværsperioderMedBeregning.map((periode, index) => (
          <div key={index + "_" + periode.samværsklasseVisningsnavn}>
            <div>
              <FeltTabell>
                <Feltrad label={tekst("ord.periode", "Periode")}>
                  {formatPeriode(periode.periode.fom, periode.periode.tom)}
                </Feltrad>
              </FeltTabell>
            </div>
            <SamværKalkulatorTable
              {...periode}
              beregning={periode.beregning!}
              key={`ferie_${index}_${samværBarn.gjelderBarn.navn}`}
            />
          </div>
        ))
      )}
    </DokumentStabel>
  );
}
function SamværKalkulatorTable({
  beregning,
  samværsklasseVisningsnavn,
  gjennomsnittligSamværPerMåned,
  ferieVisningsnavnMap,
  frekvensVisningsnavnMap,
}: NotatSamvaersperiodeDto) {
  const samværNetterBeskrivelse = () => {
    if (gjennomsnittligSamværPerMåned === 0) return "";
    return tekst(
      "notat.opplysninger.bidrag.samvær.beregning.samværPerMåned",
      " (samvær per måned: {antall} {netter})",
    )
      .replace("{antall}", `${gjennomsnittligSamværPerMåned}`)
      .replace(
        "{netter}",
        gjennomsnittligSamværPerMåned === 1
          ? tekst("notat.opplysninger.bidrag.samvær.beregning.natt", "natt")
          : tekst(
              "notat.opplysninger.bidrag.samvær.beregning.netter",
              "netter",
            ),
      );
  };
  return (
    <DokumentStabel gap={1} className="pt-2 pb-2">
      <div>
        {tekst(
          "notat.opplysninger.bidrag.samvær.regelmessigSamvær.tittel",
          "Regelmessig samvær",
        ) && (
          <h4>
            {tekst(
              "notat.opplysninger.bidrag.samvær.regelmessigSamvær.tittel",
              "Regelmessig samvær",
            )}
          </h4>
        )}
        <FeltTabell>
          <Feltrad
            label={tekst(
              "notat.opplysninger.bidrag.samvær.regelmessigSamvær.antallNetter",
              "Antall netter",
            )}
          >
            {tekst(
              "notat.opplysninger.bidrag.samvær.regelmessigSamvær.antallNetterVerdi",
              "{} / 14 dager",
            ).replace("{}", `${beregning!.regelmessigSamværNetter}`)}
          </Feltrad>
        </FeltTabell>
      </div>
      <div>
        <DokumentTabell
          layoutAuto
          kolonner={[
            {
              tittel: tekst(
                "notat.opplysninger.bidrag.samvær.ferieTabell.ferie",
                "Ferie",
              ),
            },
            { tittel: tekst("ord.bidragspliktig", "Bidragspliktig") },
            { tittel: tekst("ord.bidragsmottaker", "Bidragsmottaker") },
            {
              tittel: tekst(
                "notat.opplysninger.bidrag.samvær.ferieTabell.frekvens",
                "Frekvens",
              ),
            },
          ]}
        >
          {beregning!.ferier.map((ferie, index) => (
            <Tabellrad key={index} index={index} borderBottom>
              <Tabellcelle>{ferieVisningsnavnMap[ferie.type]}</Tabellcelle>
              <Tabellcelle>
                {zeroToEmpty(ferie.bidragspliktigNetter)}
              </Tabellcelle>
              <Tabellcelle>
                {zeroToEmpty(ferie.bidragsmottakerNetter)}
              </Tabellcelle>
              <Tabellcelle>
                {frekvensVisningsnavnMap[ferie.frekvens]}
              </Tabellcelle>
            </Tabellrad>
          ))}
        </DokumentTabell>
        <div>
          <FeltTabell>
            <Feltrad
              label={tekst(
                "notat.opplysninger.bidrag.samvær.beregning.ledetekst",
                "Beregning",
              )}
              labelBold
            >
              {tekst(
                "notat.opplysninger.bidrag.samvær.beregning.samværsklasse",
                "Samværsklasse {samværsklasse}{samværPerMåned}",
              )
                .replace("{samværsklasse}", `${samværsklasseVisningsnavn}`)
                .replace("{samværPerMåned}", samværNetterBeskrivelse())}
            </Feltrad>
          </FeltTabell>
        </div>
      </div>
    </DokumentStabel>
  );
}
function zeroToEmpty(value: number) {
  return value === 0 ? "" : value;
}
