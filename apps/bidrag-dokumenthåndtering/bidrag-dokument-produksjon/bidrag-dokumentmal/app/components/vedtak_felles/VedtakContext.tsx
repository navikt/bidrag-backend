import { createContext, PropsWithChildren, useContext } from "react";
import { DokumentBestilling } from "~/types/Api";
import { konverterRolletype } from "~/utils/converter-utils";
import { ThemeProvider, Styling } from "~/components/notat_felles/ThemeContext";
import { DokumentFellesProvider } from "~/components/vedtak_felles/FellesContext";

interface IVedtakContext {
  data: DokumentBestilling;
}
export const VedtakContext = createContext<IVedtakContext | null>(null);
export function useVedtakFelles(): IVedtakContext {
  return useContext(VedtakContext) as IVedtakContext;
}
export type VedtakDataProps = {
  styling: Styling;
  data: DokumentBestilling;
};

export function VedtakProvider({
  styling,
  children,
  data,
}: PropsWithChildren<VedtakDataProps>) {
  const dataCorrected = {
    ...data,
    rollerV2: data.rollerV2.map((d) => ({
      ...d,
      rolle: konverterRolletype(d.rolle),
    })),
  };
  return (
    <ThemeProvider styling={styling}>
      <DokumentFellesProvider styling={styling} dokumentbestilling={data}>
        <VedtakContext.Provider
          value={{
            data: dataCorrected,
          }}
        >
          {children}
        </VedtakContext.Provider>
      </DokumentFellesProvider>
    </ThemeProvider>
  );
}
