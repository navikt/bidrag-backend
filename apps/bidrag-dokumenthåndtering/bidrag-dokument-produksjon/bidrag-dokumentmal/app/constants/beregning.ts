import { Inntektstype, NotatInntektDto } from "~/types/Api";

export const barnetilleggTiltakspengerVisningsnavn = "Tiltakspenger";

// Barnetillegg som er skattefrie og derfor ikke har noen skattesats
export const erSkattefrittBarnetillegg = (inntekt: NotatInntektDto) =>
  inntekt.inntektsposter.some(
    (post) =>
      post.inntektstype === Inntektstype.BARNETILLEGG_TILTAKSPENGER ||
      post.inntektstype === Inntektstype.BARNETILLEGG_FORSVARET,
  );
