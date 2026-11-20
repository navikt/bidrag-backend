import { Rolletype, NotatMalType, InntekterPerRolle } from "~/types/Api";
import { tekst } from "~/utils/tekst";
export enum InntektTableType {
  SKATTEPLIKTIG = "SKATTEPLIKTIG",
  UTVIDET_BARNETRYGD = "UTVIDET_BARNETRYGD",
  SMÅBARNSTILLEGG = "SMÅBARNSTILLEGG",
  KONTANTSTØTTE = "KONTANTSTØTTE",
  BARNETILLEGG = "BARNETILLEGG",
  BEREGNET_INNTEKTER = "BEREGNET_INNTEKTER",
  TOTAL_INNTEKTER = "TOTAL_INNTEKTER",
}
export type BehandlingRolletype = Rolletype.BP | Rolletype.BM | Rolletype.BA;
export const inntekterTablesViewRules: {
  [key in NotatMalType]: {
    [key in BehandlingRolletype]: InntektTableType[];
  };
} = {
  [NotatMalType.SAeRBIDRAG]: {
    [Rolletype.BM]: [
      InntektTableType.SKATTEPLIKTIG,
      InntektTableType.BARNETILLEGG,
      InntektTableType.UTVIDET_BARNETRYGD,
      InntektTableType.SMÅBARNSTILLEGG,
      InntektTableType.KONTANTSTØTTE,
      InntektTableType.BEREGNET_INNTEKTER,
    ],
    [Rolletype.BP]: [
      InntektTableType.SKATTEPLIKTIG,
      InntektTableType.BARNETILLEGG,
      InntektTableType.BEREGNET_INNTEKTER,
    ],
    [Rolletype.BA]: [
      InntektTableType.SKATTEPLIKTIG,
      InntektTableType.BEREGNET_INNTEKTER,
    ],
  },
  [NotatMalType.BIDRAG]: {
    [Rolletype.BM]: [
      InntektTableType.SKATTEPLIKTIG,
      InntektTableType.BARNETILLEGG,
      InntektTableType.UTVIDET_BARNETRYGD,
      InntektTableType.SMÅBARNSTILLEGG,
      InntektTableType.KONTANTSTØTTE,
      InntektTableType.BEREGNET_INNTEKTER,
    ],
    [Rolletype.BP]: [
      InntektTableType.SKATTEPLIKTIG,
      InntektTableType.BARNETILLEGG,
      InntektTableType.BEREGNET_INNTEKTER,
    ],
    [Rolletype.BA]: [
      InntektTableType.SKATTEPLIKTIG,
      InntektTableType.BEREGNET_INNTEKTER,
    ],
  },
  [NotatMalType.FORSKUDD]: {
    [Rolletype.BM]: [
      InntektTableType.SKATTEPLIKTIG,
      InntektTableType.BARNETILLEGG,
      InntektTableType.UTVIDET_BARNETRYGD,
      InntektTableType.SMÅBARNSTILLEGG,
      InntektTableType.KONTANTSTØTTE,
      InntektTableType.BEREGNET_INNTEKTER,
    ],
    [Rolletype.BP]: [],
    [Rolletype.BA]: [InntektTableType.SKATTEPLIKTIG],
  },
};
export const beregnetInntekterColumnWidth: {
  [key in NotatMalType]: {
    [key in BehandlingRolletype]: {
      [key in string]?: string;
    };
  };
} = {
  [NotatMalType.BIDRAG]: {
    [Rolletype.BM]: {},
    [Rolletype.BP]: {},
    [Rolletype.BA]: {},
  },
  [NotatMalType.SAeRBIDRAG]: {
    [Rolletype.BM]: {
      [tekst("felles.tabell.felles.fraTilOgMed", "Fra og med - Til og med")]:
        "130px",
      [tekst(
        "felles.tabell.beregnet.skattepliktigInntektSplittet",
        "Skattepliktige- inntekter",
      )]: "85px",
      [tekst("felles.tabell.beregnet.barnetilleggSplittet", "Barnetillegg")]:
        "75px",
      [tekst("felles.tabell.beregnet.utvidetBarnetrygd", "Utvidet barnetrygd")]:
        "70px",
      [tekst("felles.tabell.beregnet.småbarnstillegg", "Småbarnstillegg")]:
        "70px",
      [tekst("felles.tabell.beregnet.kontantstøtte", "Kontantstøtte")]: "80px",
      [tekst("ord.totalt", "Totalt")]: "60px",
    },
    [Rolletype.BP]: {
      [tekst("felles.tabell.felles.fraTilOgMed", "Fra og med - Til og med")]:
        "120px",
      [tekst(
        "felles.tabell.beregnet.skattepliktigInntekt",
        "Skattepliktige inntekter",
      )]: "125px",
      [tekst("felles.tabell.beregnet.barnetillegg", "Barnetillegg")]: "75px",
      [tekst("ord.totalt", "Totalt")]: "150px",
    },
    [Rolletype.BA]: {
      [tekst("felles.tabell.felles.fraTilOgMed", "Fra og med - Til og med")]:
        "152px",
      [tekst(
        "felles.tabell.beregnet.skattepliktigInntekt",
        "Skattepliktige inntekter",
      )]: "155px",
      [tekst("ord.totalt", "Totalt")]: "300px",
    },
  },
  [NotatMalType.FORSKUDD]: {
    [Rolletype.BM]: {
      [tekst("felles.tabell.felles.fraTilOgMed", "Fra og med - Til og med")]:
        "130px",
      [tekst(
        "felles.tabell.beregnet.skattepliktigInntektSplittet",
        "Skattepliktige- inntekter",
      )]: "85px",
      [tekst("felles.tabell.beregnet.barnetilleggSplittet", "Barnetillegg")]:
        "75px",
      [tekst("felles.tabell.beregnet.utvidetBarnetrygd", "Utvidet barnetrygd")]:
        "70px",
      [tekst("felles.tabell.beregnet.småbarnstillegg", "Småbarnstillegg")]:
        "70px",
      [tekst("felles.tabell.beregnet.kontantstøtte", "Kontantstøtte")]: "80px",
      [tekst("ord.totalt", "Totalt")]: "60px",
    },
    [Rolletype.BP]: {},
    [Rolletype.BA]: {},
  },
};
export const beregnetInntekterColumnNames: {
  [key in NotatMalType]: {
    [key in BehandlingRolletype]: {
      [key in InntektTableType]?: string;
    };
  };
} = {
  [NotatMalType.BIDRAG]: {
    [Rolletype.BM]: {
      [InntektTableType.SKATTEPLIKTIG]: tekst(
        "felles.tabell.beregnet.skattepliktigInntektSplittet",
        "Skattepliktige- inntekter",
      ),
      [InntektTableType.BARNETILLEGG]: tekst(
        "felles.tabell.beregnet.barnetilleggSplittet",
        "Barnetillegg",
      ),
      [InntektTableType.UTVIDET_BARNETRYGD]: tekst(
        "felles.tabell.beregnet.utvidetBarnetrygdSplittet",
        "Utvidet- barnetrygd",
      ),
      [InntektTableType.SMÅBARNSTILLEGG]: tekst(
        "felles.tabell.beregnet.småbarnstilleggSplittet",
        "Småbarns- tillegg",
      ),
      [InntektTableType.KONTANTSTØTTE]: tekst(
        "felles.tabell.beregnet.kontantstøtte",
        "Kontantstøtte",
      ),
      [InntektTableType.TOTAL_INNTEKTER]: tekst("ord.totalt", "Totalt"),
    },
    [Rolletype.BP]: {
      [InntektTableType.SKATTEPLIKTIG]: tekst(
        "felles.tabell.beregnet.skattepliktigInntekt",
        "Skattepliktige inntekter",
      ),
      [InntektTableType.BARNETILLEGG]: tekst(
        "felles.tabell.beregnet.barnetillegg",
        "Barnetillegg",
      ),
      [InntektTableType.TOTAL_INNTEKTER]: tekst("ord.totalt", "Totalt"),
    },
    [Rolletype.BA]: {
      [InntektTableType.SKATTEPLIKTIG]: tekst(
        "felles.tabell.beregnet.skattepliktigInntekt",
        "Skattepliktige inntekter",
      ),
      [InntektTableType.TOTAL_INNTEKTER]: tekst("ord.totalt", "Totalt"),
    },
  },
  [NotatMalType.SAeRBIDRAG]: {
    [Rolletype.BM]: {
      [InntektTableType.SKATTEPLIKTIG]: tekst(
        "felles.tabell.beregnet.skattepliktigInntektSplittet",
        "Skattepliktige- inntekter",
      ),
      [InntektTableType.BARNETILLEGG]: tekst(
        "felles.tabell.beregnet.barnetilleggSplittet",
        "Barnetillegg",
      ),
      [InntektTableType.UTVIDET_BARNETRYGD]: tekst(
        "felles.tabell.beregnet.utvidetBarnetrygdSplittet",
        "Utvidet- barnetrygd",
      ),
      [InntektTableType.SMÅBARNSTILLEGG]: tekst(
        "felles.tabell.beregnet.småbarnstilleggSplittet",
        "Småbarns- tillegg",
      ),
      [InntektTableType.KONTANTSTØTTE]: tekst(
        "felles.tabell.beregnet.kontantstøtte",
        "Kontantstøtte",
      ),
      [InntektTableType.TOTAL_INNTEKTER]: tekst("ord.totalt", "Totalt"),
    },
    [Rolletype.BP]: {
      [InntektTableType.SKATTEPLIKTIG]: tekst(
        "felles.tabell.beregnet.skattepliktigInntekt",
        "Skattepliktige inntekter",
      ),
      [InntektTableType.BARNETILLEGG]: tekst(
        "felles.tabell.beregnet.barnetillegg",
        "Barnetillegg",
      ),
      [InntektTableType.TOTAL_INNTEKTER]: tekst("ord.totalt", "Totalt"),
    },
    [Rolletype.BA]: {
      [InntektTableType.SKATTEPLIKTIG]: tekst(
        "felles.tabell.beregnet.skattepliktigInntekt",
        "Skattepliktige inntekter",
      ),
      [InntektTableType.TOTAL_INNTEKTER]: tekst("ord.totalt", "Totalt"),
    },
  },
  [NotatMalType.FORSKUDD]: {
    [Rolletype.BM]: {
      [InntektTableType.SKATTEPLIKTIG]: tekst(
        "felles.tabell.beregnet.skattepliktigInntektSplittet",
        "Skattepliktige- inntekter",
      ),
      [InntektTableType.BARNETILLEGG]: tekst(
        "felles.tabell.beregnet.barnetilleggSplittet",
        "Barnetillegg",
      ),
      [InntektTableType.UTVIDET_BARNETRYGD]: tekst(
        "felles.tabell.beregnet.utvidetBarnetrygdSplittet",
        "Utvidet- barnetrygd",
      ),
      [InntektTableType.SMÅBARNSTILLEGG]: tekst(
        "felles.tabell.beregnet.småbarnstilleggSplittet",
        "Småbarns- tillegg",
      ),
      [InntektTableType.KONTANTSTØTTE]: tekst(
        "felles.tabell.beregnet.kontantstøtte",
        "Kontantstøtte",
      ),
      [InntektTableType.TOTAL_INNTEKTER]: tekst("ord.totalt", "Totalt"),
    },
    [Rolletype.BP]: {},
    [Rolletype.BA]: {},
  },
};
export const beregnetInntekterColumnNamesV2: {
  [key in NotatMalType]: {
    [key in BehandlingRolletype]: {
      [key in InntektTableType]?: string;
    };
  };
} = {
  [NotatMalType.BIDRAG]: {
    [Rolletype.BM]: {
      [InntektTableType.SKATTEPLIKTIG]: tekst(
        "felles.tabell.beregnet.skattepliktigInntekt",
        "Skattepliktige inntekter",
      ),
      [InntektTableType.BARNETILLEGG]: tekst(
        "felles.tabell.beregnet.barnetillegg",
        "Barnetillegg",
      ),
      [InntektTableType.UTVIDET_BARNETRYGD]: tekst(
        "felles.tabell.beregnet.utvidetBarnetrygd",
        "Utvidet barnetrygd",
      ),
      [InntektTableType.SMÅBARNSTILLEGG]: tekst(
        "felles.tabell.beregnet.småbarnstillegg",
        "Småbarnstillegg",
      ),
      [InntektTableType.KONTANTSTØTTE]: tekst(
        "felles.tabell.beregnet.kontantstøtte",
        "Kontantstøtte",
      ),
      [InntektTableType.TOTAL_INNTEKTER]: tekst("ord.totalt", "Totalt"),
    },
    [Rolletype.BP]: {
      [InntektTableType.SKATTEPLIKTIG]: tekst(
        "felles.tabell.beregnet.skattepliktigInntekt",
        "Skattepliktige inntekter",
      ),
      [InntektTableType.BARNETILLEGG]: tekst(
        "felles.tabell.beregnet.barnetillegg",
        "Barnetillegg",
      ),
      [InntektTableType.TOTAL_INNTEKTER]: tekst("ord.totalt", "Totalt"),
    },
    [Rolletype.BA]: {
      [InntektTableType.SKATTEPLIKTIG]: tekst(
        "felles.tabell.beregnet.skattepliktigInntekt",
        "Skattepliktige inntekter",
      ),
      [InntektTableType.TOTAL_INNTEKTER]: tekst("ord.totalt", "Totalt"),
    },
  },
  [NotatMalType.SAeRBIDRAG]: {
    [Rolletype.BM]: {
      [InntektTableType.SKATTEPLIKTIG]: tekst(
        "felles.tabell.beregnet.skattepliktigInntektSplittet",
        "Skattepliktige- inntekter",
      ),
      [InntektTableType.BARNETILLEGG]: tekst(
        "felles.tabell.beregnet.barnetilleggSplittet",
        "Barnetillegg",
      ),
      [InntektTableType.UTVIDET_BARNETRYGD]: tekst(
        "felles.tabell.beregnet.utvidetBarnetrygd",
        "Utvidet barnetrygd",
      ),
      [InntektTableType.SMÅBARNSTILLEGG]: tekst(
        "felles.tabell.beregnet.småbarnstillegg",
        "Småbarnstillegg",
      ),
      [InntektTableType.KONTANTSTØTTE]: tekst(
        "felles.tabell.beregnet.kontantstøtte",
        "Kontantstøtte",
      ),
      [InntektTableType.TOTAL_INNTEKTER]: tekst("ord.totalt", "Totalt"),
    },
    [Rolletype.BP]: {
      [InntektTableType.SKATTEPLIKTIG]: tekst(
        "felles.tabell.beregnet.skattepliktigInntekt",
        "Skattepliktige inntekter",
      ),
      [InntektTableType.BARNETILLEGG]: tekst(
        "felles.tabell.beregnet.barnetillegg",
        "Barnetillegg",
      ),
      [InntektTableType.TOTAL_INNTEKTER]: tekst("ord.totalt", "Totalt"),
    },
    [Rolletype.BA]: {
      [InntektTableType.SKATTEPLIKTIG]: tekst(
        "felles.tabell.beregnet.skattepliktigInntekt",
        "Skattepliktige inntekter",
      ),
      [InntektTableType.TOTAL_INNTEKTER]: tekst("ord.totalt", "Totalt"),
    },
  },
  [NotatMalType.FORSKUDD]: {
    [Rolletype.BM]: {
      [InntektTableType.SKATTEPLIKTIG]: tekst(
        "felles.tabell.beregnet.skattepliktigInntektSplittet",
        "Skattepliktige- inntekter",
      ),
      [InntektTableType.BARNETILLEGG]: tekst(
        "felles.tabell.beregnet.barnetilleggSplittet",
        "Barnetillegg",
      ),
      [InntektTableType.UTVIDET_BARNETRYGD]: tekst(
        "felles.tabell.beregnet.utvidetBarnetrygd",
        "Utvidet barnetrygd",
      ),
      [InntektTableType.SMÅBARNSTILLEGG]: tekst(
        "felles.tabell.beregnet.småbarnstillegg",
        "Småbarnstillegg",
      ),
      [InntektTableType.KONTANTSTØTTE]: tekst(
        "felles.tabell.beregnet.kontantstøtte",
        "Kontantstøtte",
      ),
      [InntektTableType.TOTAL_INNTEKTER]: tekst("ord.totalt", "Totalt"),
    },
    [Rolletype.BP]: {},
    [Rolletype.BA]: {},
  },
};
export function isHarInntekter(inntekter: InntekterPerRolle): boolean {
  return (
    inntekter.årsinntekter.length > 0 ||
    inntekter.barnetillegg.length > 0 ||
    inntekter.utvidetBarnetrygd.length > 0 ||
    inntekter.småbarnstillegg.length > 0 ||
    inntekter.kontantstøtte.length > 0
  );
}
