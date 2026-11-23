import { createInstance, type Resource } from "i18next";

export type Tekstverdier = Record<string, unknown>;

export function opprettTekstoversetter(ressurser: Resource = {}) {
  const i18n = createInstance();
  void i18n.init({
    lng: "nb",
    fallbackLng: "no",
    resources: ressurser,
    initAsync: false,
    interpolation: {
      escapeValue: false,
    },
  });

  return {
    i18n,
    tekst(nokkel: string, norsk: string, verdier?: Tekstverdier): string {
      return i18n.t(nokkel, {
        ...verdier,
        defaultValue: norsk,
      });
    },
  };
}

export const tekstoversetter = opprettTekstoversetter();
export const tekst = tekstoversetter.tekst;
