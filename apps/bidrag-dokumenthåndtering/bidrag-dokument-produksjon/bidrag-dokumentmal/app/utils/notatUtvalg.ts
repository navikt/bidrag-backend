import {
  DokumentmalPersonDto,
  NotatInntekterDto,
  NotatUnderholdBarnDto,
  Rolletype,
} from "~/types/Api";
import { erRolle } from "~/utils/converter-utils";
import { sortByAge } from "~/utils/date-utils";
import { sammenlignRoller } from "~/utils/visningsnavn";

/** Henter rollene av de gitte typene, i samme rekkefølge som `typer`. Barn sorteres fra eldst til yngst. */
export function velgRoller(
  roller: DokumentmalPersonDto[],
  typer: Rolletype[],
): DokumentmalPersonDto[] {
  return typer.flatMap((type) => {
    const rollerAvType = roller.filter(erRolle(type));
    return type === Rolletype.BA
      ? [...rollerAvType].sort(sortByAge)
      : rollerAvType;
  });
}

/** Henter inntektene og begrunnelsen som gjelder én rolle. */
export function finnInntekterFor(
  inntekter: NotatInntekterDto,
  rolle: DokumentmalPersonDto,
) {
  return {
    inntekter: inntekter.inntekterPerRolle.find(
      (d) =>
        sammenlignRoller(
          d.gjelder.rolle ?? undefined,
          rolle.rolle ?? undefined,
        ) && d.gjelder.ident == rolle.ident,
    ),
    begrunnelse: inntekter.notatPerRolle.find(
      (d) => d.gjelder?.ident == rolle.ident,
    ),
  };
}

/**
 * Deler underholdskostnadene i søknadsbarn (sortert fra eldst til yngst)
 * og andre barn av bidragsmottaker (barn uten rolle i saken).
 */
export function delUnderholdskostnader(barn: NotatUnderholdBarnDto[]) {
  return {
    søknadsbarn: barn
      .filter((b) => b.gjelderBarn.rolle != null)
      .sort((a, b) => sortByAge(a.gjelderBarn, b.gjelderBarn)),
    andreBarn: barn.filter((b) => b.gjelderBarn.rolle == null),
  };
}
