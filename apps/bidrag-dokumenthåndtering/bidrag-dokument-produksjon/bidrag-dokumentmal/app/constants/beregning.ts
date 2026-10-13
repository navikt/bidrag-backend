import { Inntektstype, NotatInntektspostDto } from "~/types/Api";

export const barnetilleggTiltakspengerVisningsnavn = "Tiltakspenger";

// Skattesatsen er ikke relevant for skattefrie barnetillegg. Beregningen ignorerer alltid skattesatsen for tiltakspenger,
// men bruker en skattesats for Forsvaret hvis den er lagt inn.
export const erSkattesatsIkkeRelevant = (post?: NotatInntektspostDto) =>
  post?.inntektstype === Inntektstype.BARNETILLEGG_TILTAKSPENGER ||
  (post?.inntektstype === Inntektstype.BARNETILLEGG_FORSVARET &&
    !post.skattefaktor);
