import { describe, expect, it } from "vitest";
import {
  DokumentmalPersonDto,
  InntekterPerRolle,
  NotatBegrunnelseDto,
  NotatInntekterDto,
  NotatUnderholdBarnDto,
  Rolletype,
} from "~/types/Api";
import {
  delUnderholdskostnader,
  finnInntekterFor,
  velgRoller,
} from "~/utils/notatUtvalg";

function person(
  rolle: Rolletype | null,
  ident: string,
  fødselsdato?: string,
): DokumentmalPersonDto {
  return { rolle, ident, fødselsdato, erBeskyttet: false, revurdering: false };
}

const bm = person(Rolletype.BM, "bm");
const bm2 = person(Rolletype.BM, "bm2");
const bp = person(Rolletype.BP, "bp");
const yngsteBarn = person(Rolletype.BA, "yngst", "2020-01-01");
const eldsteBarn = person(Rolletype.BA, "eldst", "2010-01-01");

const identer = (personer: DokumentmalPersonDto[]) =>
  personer.map((p) => p.ident);

describe("velgRoller", () => {
  const roller = [yngsteBarn, bp, bm, eldsteBarn, bm2];

  it("skal returnere rollene i samme rekkefølge som typene", () => {
    expect(
      identer(velgRoller(roller, [Rolletype.BM, Rolletype.BP, Rolletype.BA])),
    ).toEqual(["bm", "bm2", "bp", "eldst", "yngst"]);
  });

  it("skal bare returnere rollene av de gitte typene", () => {
    expect(identer(velgRoller(roller, [Rolletype.BM]))).toEqual(["bm", "bm2"]);
  });

  it("skal sortere barn fra eldst til yngst uten å endre listen som sendes inn", () => {
    const før = [...roller];
    expect(identer(velgRoller(roller, [Rolletype.BA]))).toEqual([
      "eldst",
      "yngst",
    ]);
    expect(roller).toEqual(før);
  });

  it("skal godta rolletyper med fullt navn fra backend", () => {
    const fraBackend = person("BIDRAGSPLIKTIG" as Rolletype, "bp");
    expect(identer(velgRoller([fraBackend], [Rolletype.BP]))).toEqual(["bp"]);
  });

  it("skal returnere tom liste når ingen roller har typen", () => {
    expect(velgRoller([bm], [Rolletype.BP])).toEqual([]);
  });
});

describe("finnInntekterFor", () => {
  const inntekterFor = (gjelder: DokumentmalPersonDto) =>
    ({ gjelder }) as InntekterPerRolle;
  const begrunnelseFor = (gjelder: DokumentmalPersonDto, innhold: string) =>
    ({ gjelder, innhold }) as NotatBegrunnelseDto;

  const data = {
    inntekterPerRolle: [inntekterFor(bm), inntekterFor(bp)],
    notatPerRolle: [begrunnelseFor(bm, "om bm"), begrunnelseFor(bp, "om bp")],
  } as NotatInntekterDto;

  it("skal finne inntektene og begrunnelsen til rollen", () => {
    const resultat = finnInntekterFor(data, bp);
    expect(resultat.inntekter?.gjelder.ident).toBe("bp");
    expect(resultat.begrunnelse?.innhold).toBe("om bp");
  });

  it("skal ikke finne inntekter når identen er lik men rollen er en annen", () => {
    const samePersonSomBarn = person(Rolletype.BA, "bm");
    expect(finnInntekterFor(data, samePersonSomBarn).inntekter).toBeUndefined();
  });

  it("skal returnere undefined når rollen mangler i dataene", () => {
    const resultat = finnInntekterFor(data, eldsteBarn);
    expect(resultat.inntekter).toBeUndefined();
    expect(resultat.begrunnelse).toBeUndefined();
  });
});

describe("delUnderholdskostnader", () => {
  const underhold = (gjelderBarn: DokumentmalPersonDto) =>
    ({ gjelderBarn }) as NotatUnderholdBarnDto;
  const annetBarn = person(null, "annet");

  it("skal dele i søknadsbarn sortert etter alder og andre barn uten rolle", () => {
    const { søknadsbarn, andreBarn } = delUnderholdskostnader([
      underhold(yngsteBarn),
      underhold(annetBarn),
      underhold(eldsteBarn),
    ]);
    expect(søknadsbarn.map((b) => b.gjelderBarn.ident)).toEqual([
      "eldst",
      "yngst",
    ]);
    expect(andreBarn.map((b) => b.gjelderBarn.ident)).toEqual(["annet"]);
  });

  it("skal gi tomme lister når det ikke finnes underholdskostnader", () => {
    expect(delUnderholdskostnader([])).toEqual({
      søknadsbarn: [],
      andreBarn: [],
    });
  });
});
