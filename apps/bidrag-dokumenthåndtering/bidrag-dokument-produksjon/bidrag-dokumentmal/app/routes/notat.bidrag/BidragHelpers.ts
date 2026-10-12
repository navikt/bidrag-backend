import {
  DokumentmalResultatBidragsberegningBarnDto,
  DokumentmalSluttberegningBarnebidragDetaljer,
  ResultatBarnebidragsberegningPeriodeDto,
  Resultatkode,
} from "~/types/Api";

// Beregningen gir avslag/opphør uten beløp for disse resultatene, på samme måte som når barnet er selvforsørget
const beregnetAvslagUtenBeløp = [
  Resultatkode.BIDRAGIKKEBEREGNETDELTBOSTED,
  Resultatkode.FULLTUNDERHOLDTAVOFFENTLIG,
];

export function erBeregnetAvslagUtenBeløp(
  sluttberegning?: DokumentmalSluttberegningBarnebidragDetaljer | null,
): boolean {
  return (
    sluttberegning?.barnetErSelvforsørget == true ||
    (!!sluttberegning?.resultat &&
      beregnetAvslagUtenBeløp.includes(sluttberegning.resultat))
  );
}

export function erResutlatMedBeregning(
  data: DokumentmalResultatBidragsberegningBarnDto,
): boolean {
  return data?.perioder.length === 1 && data.perioder[0].erDirekteAvslag;
}

export function erResultatMedIPeriode(
  data: ResultatBarnebidragsberegningPeriodeDto,
): boolean {
  return (
    data.erDirekteAvslag ||
    erBeregnetAvslagUtenBeløp(data.beregningsdetaljer?.sluttberegning)
  );
}
