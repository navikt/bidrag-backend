import {
  NotatResultatSaerbidragsberegningDto,
  Resultatkode,
} from "~/types/Api";

export function erResutlatMedBeregning(
  data: NotatResultatSaerbidragsberegningDto[],
): boolean {
  if (data.length == 0) return false;
  const resultat = data[0]!;
  const erDirekteAvslag = resultat?.erDirekteAvslag;
  const erAvslagSomInneholderUtgifter = [
    Resultatkode.GODKJENTBELOPERLAVEREENNFORSKUDDSSATS,
    Resultatkode.ALLE_UTGIFTER_ER_FORELDET,
  ].includes(resultat.resultatKode);
  return !erDirekteAvslag && !erAvslagSomInneholderUtgifter;
}
