import { Rolletype, NotatMalType, InntekterPerRolle } from "~/types/Api";
import tekster from "~/tekster";

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
      [tekster.tabell.felles.fraTilOgMed]: "130px",
      [tekster.tabell.beregnet.skattepliktigInntektSplittet]: "85px",
      [tekster.tabell.beregnet.barnetilleggSplittet]: "75px",
      [tekster.tabell.beregnet.utvidetBarnetrygd]: "70px",
      [tekster.tabell.beregnet.småbarnstillegg]: "70px",
      [tekster.tabell.beregnet.kontantstøtte]: "80px",
      [tekster.tabell.beregnet.total]: "60px",
    },
    [Rolletype.BP]: {
      [tekster.tabell.felles.fraTilOgMed]: "120px",
      [tekster.tabell.beregnet.skattepliktigInntekt]: "125px",
      [tekster.tabell.beregnet.barnetillegg]: "75px",
      [tekster.tabell.beregnet.total]: "150px",
    },
    [Rolletype.BA]: {
      [tekster.tabell.felles.fraTilOgMed]: "152px",
      [tekster.tabell.beregnet.skattepliktigInntekt]: "155px",
      [tekster.tabell.beregnet.total]: "300px",
    },
  },
  [NotatMalType.FORSKUDD]: {
    [Rolletype.BM]: {
      [tekster.tabell.felles.fraTilOgMed]: "130px",
      [tekster.tabell.beregnet.skattepliktigInntektSplittet]: "85px",
      [tekster.tabell.beregnet.barnetilleggSplittet]: "75px",
      [tekster.tabell.beregnet.utvidetBarnetrygd]: "70px",
      [tekster.tabell.beregnet.småbarnstillegg]: "70px",
      [tekster.tabell.beregnet.kontantstøtte]: "80px",
      [tekster.tabell.beregnet.total]: "60px",
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
      [InntektTableType.SKATTEPLIKTIG]:
        tekster.tabell.beregnet.skattepliktigInntektSplittet,
      [InntektTableType.BARNETILLEGG]:
        tekster.tabell.beregnet.barnetilleggSplittet,
      [InntektTableType.UTVIDET_BARNETRYGD]:
        tekster.tabell.beregnet.utvidetBarnetrygdSplittet,
      [InntektTableType.SMÅBARNSTILLEGG]:
        tekster.tabell.beregnet.småbarnstilleggSplittet,
      [InntektTableType.KONTANTSTØTTE]: tekster.tabell.beregnet.kontantstøtte,
      [InntektTableType.TOTAL_INNTEKTER]: tekster.tabell.beregnet.total,
    },
    [Rolletype.BP]: {
      [InntektTableType.SKATTEPLIKTIG]:
        tekster.tabell.beregnet.skattepliktigInntekt,
      [InntektTableType.BARNETILLEGG]: tekster.tabell.beregnet.barnetillegg,
      [InntektTableType.TOTAL_INNTEKTER]: tekster.tabell.beregnet.total,
    },
    [Rolletype.BA]: {
      [InntektTableType.SKATTEPLIKTIG]:
        tekster.tabell.beregnet.skattepliktigInntekt,
      [InntektTableType.TOTAL_INNTEKTER]: tekster.tabell.beregnet.total,
    },
  },
  [NotatMalType.SAeRBIDRAG]: {
    [Rolletype.BM]: {
      [InntektTableType.SKATTEPLIKTIG]:
        tekster.tabell.beregnet.skattepliktigInntektSplittet,
      [InntektTableType.BARNETILLEGG]:
        tekster.tabell.beregnet.barnetilleggSplittet,
      [InntektTableType.UTVIDET_BARNETRYGD]:
        tekster.tabell.beregnet.utvidetBarnetrygdSplittet,
      [InntektTableType.SMÅBARNSTILLEGG]:
        tekster.tabell.beregnet.småbarnstilleggSplittet,
      [InntektTableType.KONTANTSTØTTE]: tekster.tabell.beregnet.kontantstøtte,
      [InntektTableType.TOTAL_INNTEKTER]: tekster.tabell.beregnet.total,
    },
    [Rolletype.BP]: {
      [InntektTableType.SKATTEPLIKTIG]:
        tekster.tabell.beregnet.skattepliktigInntekt,
      [InntektTableType.BARNETILLEGG]: tekster.tabell.beregnet.barnetillegg,
      [InntektTableType.TOTAL_INNTEKTER]: tekster.tabell.beregnet.total,
    },
    [Rolletype.BA]: {
      [InntektTableType.SKATTEPLIKTIG]:
        tekster.tabell.beregnet.skattepliktigInntekt,
      [InntektTableType.TOTAL_INNTEKTER]: tekster.tabell.beregnet.total,
    },
  },
  [NotatMalType.FORSKUDD]: {
    [Rolletype.BM]: {
      [InntektTableType.SKATTEPLIKTIG]:
        tekster.tabell.beregnet.skattepliktigInntektSplittet,
      [InntektTableType.BARNETILLEGG]:
        tekster.tabell.beregnet.barnetilleggSplittet,
      [InntektTableType.UTVIDET_BARNETRYGD]:
        tekster.tabell.beregnet.utvidetBarnetrygdSplittet,
      [InntektTableType.SMÅBARNSTILLEGG]:
        tekster.tabell.beregnet.småbarnstilleggSplittet,
      [InntektTableType.KONTANTSTØTTE]: tekster.tabell.beregnet.kontantstøtte,
      [InntektTableType.TOTAL_INNTEKTER]: tekster.tabell.beregnet.total,
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
      [InntektTableType.SKATTEPLIKTIG]:
        tekster.tabell.beregnet.skattepliktigInntekt,
      [InntektTableType.BARNETILLEGG]: tekster.tabell.beregnet.barnetillegg,
      [InntektTableType.UTVIDET_BARNETRYGD]:
        tekster.tabell.beregnet.utvidetBarnetrygd,
      [InntektTableType.SMÅBARNSTILLEGG]:
        tekster.tabell.beregnet.småbarnstillegg,
      [InntektTableType.KONTANTSTØTTE]: tekster.tabell.beregnet.kontantstøtte,
      [InntektTableType.TOTAL_INNTEKTER]: tekster.tabell.beregnet.total,
    },
    [Rolletype.BP]: {
      [InntektTableType.SKATTEPLIKTIG]:
        tekster.tabell.beregnet.skattepliktigInntekt,
      [InntektTableType.BARNETILLEGG]: tekster.tabell.beregnet.barnetillegg,
      [InntektTableType.TOTAL_INNTEKTER]: tekster.tabell.beregnet.total,
    },
    [Rolletype.BA]: {
      [InntektTableType.SKATTEPLIKTIG]:
        tekster.tabell.beregnet.skattepliktigInntekt,
      [InntektTableType.TOTAL_INNTEKTER]: tekster.tabell.beregnet.total,
    },
  },
  [NotatMalType.SAeRBIDRAG]: {
    [Rolletype.BM]: {
      [InntektTableType.SKATTEPLIKTIG]:
        tekster.tabell.beregnet.skattepliktigInntektSplittet,
      [InntektTableType.BARNETILLEGG]:
        tekster.tabell.beregnet.barnetilleggSplittet,
      [InntektTableType.UTVIDET_BARNETRYGD]:
        tekster.tabell.beregnet.utvidetBarnetrygd,
      [InntektTableType.SMÅBARNSTILLEGG]:
        tekster.tabell.beregnet.småbarnstillegg,
      [InntektTableType.KONTANTSTØTTE]: tekster.tabell.beregnet.kontantstøtte,
      [InntektTableType.TOTAL_INNTEKTER]: tekster.tabell.beregnet.total,
    },
    [Rolletype.BP]: {
      [InntektTableType.SKATTEPLIKTIG]:
        tekster.tabell.beregnet.skattepliktigInntekt,
      [InntektTableType.BARNETILLEGG]: tekster.tabell.beregnet.barnetillegg,
      [InntektTableType.TOTAL_INNTEKTER]: tekster.tabell.beregnet.total,
    },
    [Rolletype.BA]: {
      [InntektTableType.SKATTEPLIKTIG]:
        tekster.tabell.beregnet.skattepliktigInntekt,
      [InntektTableType.TOTAL_INNTEKTER]: tekster.tabell.beregnet.total,
    },
  },
  [NotatMalType.FORSKUDD]: {
    [Rolletype.BM]: {
      [InntektTableType.SKATTEPLIKTIG]:
        tekster.tabell.beregnet.skattepliktigInntektSplittet,
      [InntektTableType.BARNETILLEGG]:
        tekster.tabell.beregnet.barnetilleggSplittet,
      [InntektTableType.UTVIDET_BARNETRYGD]:
        tekster.tabell.beregnet.utvidetBarnetrygd,
      [InntektTableType.SMÅBARNSTILLEGG]:
        tekster.tabell.beregnet.småbarnstillegg,
      [InntektTableType.KONTANTSTØTTE]: tekster.tabell.beregnet.kontantstøtte,
      [InntektTableType.TOTAL_INNTEKTER]: tekster.tabell.beregnet.total,
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
