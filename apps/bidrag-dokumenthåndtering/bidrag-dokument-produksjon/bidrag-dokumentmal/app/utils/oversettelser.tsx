export enum Språkkode {
  NB = "nb",
  NN = "nn",
  EN = "en",
}

export type SpråkType = `${Språkkode}`;

export type SpråkkodeIdKey = keyof typeof Språkkode;

export const localeMap: Record<SpråkType, string> = {
  nb: "nb-NO",
  nn: "nn-NO",
  en: "en-US",
};

export function språkkodeTilSpråkType(id: SpråkkodeIdKey): SpråkType {
  return Språkkode[id];
}

// Et språkmap: { nb|nn|en: V }
type SpråkMap<V> = Record<SpråkType, V>;

// Låser et tekst-API til ett språk S. Hvis verdien er en funksjon som
// returnerer et SpråkMap<V>, får vi en funksjon som returnerer V.
// Hvis verdien er et SpråkMap<V>, får vi direkte V.
export type LåstTilSpråk<T> = {
  [K in keyof T]: T[K] extends (...args: infer A) => SpråkMap<infer R>
    ? (...args: A) => R
    : T[K] extends SpråkMap<infer R>
      ? R
      : never;
};

/**
 * Lås et tekst-objekt til ett språk.
 * - Bevarer nøkler (f.eks. opplysningerPerson, barnOgBidrag, ...)
 * - Funksjoner beholder samme signatur, men returnerer kun valgt språk
 */
export function hentTekst<T>(språk: SpråkType, tekst: T): LåstTilSpråk<T> {
  const out: Partial<Record<keyof T, unknown>> = {};

  for (const key in tekst as Record<string, unknown>) {
    const entry = (tekst as Record<string, unknown>)[key];

    if (typeof entry === "function") {
      out[key as keyof T] = (...args: unknown[]) => {
        const res = (entry as (...a: unknown[]) => SpråkMap<unknown>)(...args);
        return res[språk];
      };
    } else if (entry && typeof entry === "object") {
      out[key as keyof T] = (entry as SpråkMap<unknown>)[språk];
    }
  }

  return out as LåstTilSpråk<T>;
}

export type JaNei = "JA" | "NEI";

export const jaNeiTekster: Record<JaNei, Record<SpråkType, string>> = {
  JA: {
    nb: "Ja",
    nn: "Ja",
    en: "Yes",
  },
  NEI: {
    nb: "Nei",
    nn: "Nei",
    en: "No",
  },
};
