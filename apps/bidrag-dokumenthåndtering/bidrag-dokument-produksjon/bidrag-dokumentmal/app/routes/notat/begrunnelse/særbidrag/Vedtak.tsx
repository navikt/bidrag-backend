import {
  NotatResultatSaerbidragsberegningDto,
  Resultatkode,
} from "~/types/Api";
import {
  useNotatFelles,
  useVedleggNummer,
} from "~/components/notat_felles/NotatContext";
import { VedtakFattetDetaljer } from "~/routes/notat/begrunnelse/felles/VedtakFattetDetaljer";
import elementIds from "~/utils/elementIds";
import { erResutlatMedBeregning } from "~/routes/notat/felles/SærbidragHelpers";
import { KolonneTabell, Tabelloppsett } from "~/components/KolonneTabell";
import { Feltliste, Feltlisteoppsett } from "~/components/Feltliste";
import type { RowContent } from "~/components/DokumentTabell";
import { Tabellcelle, Tabellrad } from "~/components/DokumentTabell";
import { formater } from "~/utils/visning";
import { tekst } from "~/utils/tekst";
const avslagMedUtgifter: Feltlisteoppsett = {
  ledetekstBredde: "90px",
  felter: [
    { felt: "resultatVisningsnavn", tittel: tekst("ord.årsak", "Årsak") },
    {
      felt: "forskuddssats",
      tittel: tekst(
        "notat.begrunnelse.særbidrag.vedtak.avslagMedUtgifter.forskuddssats",
        "Forskuddssaats",
      ),
      format: "beløp",
    },
    {
      felt: "beregning.totalKravbeløp",
      tittel: tekst("ord.kravbeløp", "Kravbeløp"),
      format: "beløp",
    },
    {
      felt: "beregning.totalGodkjentBeløp",
      tittel: tekst("ord.godkjentBeløp", "Godkjent beløp"),
      format: "beløp",
    },
  ],
};
const inntekter: Feltlisteoppsett = {
  tittel: tekst(
    "notat.begrunnelse.særbidrag.vedtak.inntekter.tittel",
    "Inntekter",
  ),
  ledetekstBredde: "140px",
  felter: [
    {
      felt: "inntekter.inntektBM",
      tittel: tekst(
        "notat.begrunnelse.særbidrag.vedtak.inntekter.inntektBidragsmottaker",
        "Inntekt Bidragsmottaker",
      ),
      format: "beløp",
    },
    {
      felt: "inntekter.inntektBP",
      tittel: tekst(
        "notat.begrunnelse.særbidrag.vedtak.inntekter.inntektBidragspliktig",
        "Inntekt Bidragspliktig",
      ),
      format: "beløp",
    },
    {
      felt: "inntekter.inntektBarn",
      tittel: tekst(
        "notat.begrunnelse.særbidrag.vedtak.inntekter.inntektBarn",
        "Inntekt Barn",
      ),
      format: "beløp",
    },
  ],
};
const boforhold: Feltlisteoppsett = {
  tittel: tekst(
    "notat.begrunnelse.særbidrag.vedtak.boforhold.tittel",
    "Boforhold",
  ),
  ledetekstBredde: "145px",
  felter: [
    {
      felt: "antallBarnIHusstanden",
      tittel: tekst(
        "notat.begrunnelse.særbidrag.vedtak.boforhold.antallBarnIHusstanden",
        "Antall barn i husstanden",
      ),
    },
    {
      felt: "voksenIHusstanden",
      tittel: tekst(
        "notat.begrunnelse.særbidrag.vedtak.boforhold.voksneIHusstanden",
        "Voksne i husstanden",
      ),
    },
  ],
};
function beregning(medInnkreving: boolean): Feltlisteoppsett {
  return {
    tittel: tekst(
      "notat.begrunnelse.særbidrag.vedtak.beregning.tittel",
      "Beregning",
    ),
    ledetekstBredde: "130px",
    felter: [
      {
        felt: "beregning.totalKravbeløp",
        tittel: tekst("ord.kravbeløp", "Kravbeløp"),
        format: "beløp",
      },
      {
        felt: "beregning.totalGodkjentBeløp",
        tittel: tekst("ord.godkjentBeløp", "Godkjent beløp"),
        format: "beløp",
      },
      {
        felt: "maksGodkjentBeløp",
        tittel: tekst(
          "notat.begrunnelse.særbidrag.vedtak.beregning.maksGodkjentBeløp",
          "Maks godkjent beløp",
        ),
        format: "beløp",
      },
      {
        felt: "bpsAndel.endeligAndelFaktor",
        tittel: tekst(
          "notat.begrunnelse.særbidrag.vedtak.beregning.bidragspliktigesAndel",
          "Bidragspliktiges andel",
        ),
        format: "prosent",
      },
      {
        felt: "bpHarEvne",
        tittel: tekst(
          "notat.begrunnelse.særbidrag.vedtak.beregning.bidragspliktigHarEvne",
          "Bidragspliktig har evne",
        ),
      },
      {
        felt: "resultat",
        tittel: tekst("ord.resultat", "Resultat"),
        format: "beløp",
      },
      {
        felt: "beregning.totalBeløpBetaltAvBp",
        tittel: tekst(
          "notat.begrunnelse.særbidrag.vedtak.beregning.betaltAvBidragspliktig",
          "Betalt av Bidragspliktig",
        ),
        format: "beløp",
      },
      {
        felt: "beløpSomInnkreves",
        tittel: medInnkreving
          ? tekst(
              "notat.begrunnelse.særbidrag.vedtak.beregning.beløpSomInnkreves",
              "Beløp som innkreves",
            )
          : tekst(
              "notat.begrunnelse.særbidrag.vedtak.beregning.beløpSomInnkrevesUtenInnkreving",
              "Fastsatt beløp å betale",
            ),
        format: "beløp",
      },
    ],
  };
}
const utgifter: Tabelloppsett = {
  bredde: "630px",
  kolonner: [
    {
      felt: "betaltAvBp",
      tittel: tekst(
        "notat.begrunnelse.særbidrag.vedtak.utgifter.betaltAvBidragspliktig",
        "Betalt av Bidragspliktig",
      ),
      bredde: "100px",
    },
    {
      felt: "dato",
      tittel: tekst(
        "notat.begrunnelse.særbidrag.vedtak.utgifter.forfallsdato",
        "Forfallsdato",
      ),
      bredde: "70px",
      format: "dato",
    },
    {
      felt: "utgiftstypeVisningsnavn",
      tittel: tekst(
        "notat.begrunnelse.særbidrag.vedtak.utgifter.utgift",
        "Utgift",
      ),
      bredde: "120px",
    },
    {
      felt: "kravbeløp",
      tittel: tekst("ord.kravbeløp", "Kravbeløp"),
      bredde: "80px",
      format: "beløp",
    },
    {
      felt: "godkjentBeløp",
      tittel: tekst("ord.godkjentBeløp", "Godkjent beløp"),
      bredde: "110px",
      format: "beløp",
    },
  ],
};
export default function Vedtak() {
  const vedleggNummer = useVedleggNummer("beregningsdetaljer");
  const { erAvslag, data } = useNotatFelles();
  const resultat =
    (data.vedtak?.resultat as NotatResultatSaerbidragsberegningDto[]) ?? [];
  return (
    <>
      <div
        style={{
          pageBreakBefore: erAvslag ? "auto" : "always",
          display: "inline-block",
        }}
      >
        <div className={"elements_inline section-title mb-2"}>
          <h2>
            {tekst("notat.begrunnelse.særbidrag.vedtak.tittel", "Vedtak")}
          </h2>
          {erResutlatMedBeregning(resultat) && (
            <a href={`#${elementIds.vedleggBeregningsdetaljer}`}>
              {tekst(
                "ord.seVedleggForBeregningsdetaljer",
                "se vedlegg nr. {} for beregningsdetaljer",
              ).replace("{}", vedleggNummer.toString())}
            </a>
          )}
        </div>
        {resultat.length == 0 ? (
          <div>{tekst("ord.manglerResultat", "Mangler resultat")}</div>
        ) : (
          <VedtakResultat resultat={resultat[0]} />
        )}
      </div>
      <VedtakFattetDetaljer data={data.vedtak} />
    </>
  );
}
function VedtakResultat({
  resultat,
}: {
  resultat: NotatResultatSaerbidragsberegningDto;
}) {
  const {
    data: { medInnkreving },
  } = useNotatFelles();
  if (resultat.erDirekteAvslag) {
    return (
      <div>
        <h3 style={{ marginTop: 0 }}>{tekst("ord.avslag", "Avslag")}</h3>
        <p>
          <dl className="bd_datadisplay">
            <dt>
              {tekst("notat.begrunnelse.særbidrag.vedtak.årsak", "Årsak: ")}
            </dt>
            <dd>{resultat.resultatVisningsnavn}</dd>
          </dl>
        </p>
      </div>
    );
  }
  if (resultat.erAvslagSomInneholderUtgifter) {
    const visForskuddssats =
      resultat.resultatKode ==
      Resultatkode.GODKJENTBELOPERLAVEREENNFORSKUDDSSATS;
    return (
      <div>
        <h3 style={{ marginTop: 0 }}>{tekst("ord.avslag", "Avslag")}</h3>
        <UtgiftsposterTabell />
        <div>
          <Feltliste
            oppsett={avslagMedUtgifter}
            kilde={resultat}
            skjul={visForskuddssats ? [] : ["forskuddssats"]}
          />
        </div>
      </div>
    );
  }
  const avslagEllerBeløp = (_: unknown, beløp: RowContent) =>
    resultat.erBeregningAvslag ? tekst("ord.avslag", "Avslag") : beløp;
  return (
    <div>
      <h3 style={{ marginTop: 0 }}>
        {resultat.erBeregningAvslag
          ? tekst(
              "notat.begrunnelse.særbidrag.vedtak.avslagMedÅrsak",
              "Avslag: {}",
            ).replace("{}", resultat.resultatVisningsnavn)
          : tekst(
              "notat.begrunnelse.særbidrag.vedtak.innvilget",
              "Særbidrag innvilget",
            )}
      </h3>
      <div>
        <div className="two_column_view_v2" style={{ width: "35%" }}>
          <Feltliste oppsett={inntekter} kilde={resultat} />
        </div>
        <div className="two_column_view_v2" style={{ width: "45%" }}>
          <Feltliste
            oppsett={boforhold}
            kilde={resultat}
            verdier={{
              voksenIHusstanden: (r) =>
                !r.voksenIHusstanden
                  ? tekst("ord.nei", "Nei")
                  : r.enesteVoksenIHusstandenErEgetBarn
                    ? tekst(
                        "notat.begrunnelse.særbidrag.vedtak.boforhold.svarVoksneIHusstanden.jaEgetBarn",
                        "Ja (barn over 18 år)",
                      )
                    : tekst("ord.ja", "Ja"),
            }}
          />
        </div>
        <div className="two_column_view_v2 mt-medium" style={{ width: "100%" }}>
          <Feltliste
            oppsett={beregning(medInnkreving)}
            kilde={resultat}
            verdier={{
              resultat: avslagEllerBeløp,
              beløpSomInnkreves: avslagEllerBeløp,
            }}
          />
        </div>
      </div>
      <UtgiftsposterTabell />
    </div>
  );
}
function UtgiftsposterTabell() {
  const { data } = useNotatFelles();
  const beregning = data.utgift?.beregning;
  return (
    <div className={"mt-4"}>
      <h4>
        {tekst(
          "notat.begrunnelse.særbidrag.vedtak.utgifter.tittel",
          "Utgiftene lagt til grunn",
        )}
      </h4>
      <KolonneTabell
        layoutAuto
        oppsett={utgifter}
        rader={data.utgift?.utgifter ?? []}
        ekstraRader={
          <Tabellrad index={(data.utgift?.utgifter ?? []).length}>
            <Tabellcelle colSpan={3} labelBold>
              {tekst("ord.sum", "Sum")}
            </Tabellcelle>
            <Tabellcelle>
              {formater(beregning?.totalKravbeløp, "beløpAvrundet")}
            </Tabellcelle>
            <Tabellcelle>
              {formater(beregning?.totalGodkjentBeløp, "beløpAvrundet")}
            </Tabellcelle>
          </Tabellrad>
        }
      />
    </div>
  );
}
