import { createContext, PropsWithChildren, useContext } from "react";
import {
  Rolletype,
  DokumentBestilling,
  DokumentmalPersonDto,
  VedtakNotatDto,
} from "~/types/Api";
import { konverterRolletype, erRolle } from "~/utils/converter-utils";
import { ThemeProvider, Styling } from "~/components/notat_felles/ThemeContext";

interface IDokumentFellesContext {
  bidragsmottaker: DokumentmalPersonDto;
  bidragspliktig?: DokumentmalPersonDto;
  søknadsbarn: DokumentmalPersonDto[];
  roller: DokumentmalPersonDto[];
  saksnummer: string;
  typeInnhold: TypeInnhold;
}

export enum TypeInnhold {
  NOTAT,
  VEDTAK,
}
export const DokumentFellesContext =
  createContext<IDokumentFellesContext | null>(null);
export function useDokumentFelles(): IDokumentFellesContext {
  return useContext(DokumentFellesContext) as IDokumentFellesContext;
}
export type FellesDataProps = {
  styling: Styling;
  notat?: VedtakNotatDto;
  dokumentbestilling?: DokumentBestilling;
};

export function DokumentFellesProvider({
  styling,
  children,
  notat,
  dokumentbestilling,
}: PropsWithChildren<FellesDataProps>) {
  const rollerFraInput = notat?.roller ?? dokumentbestilling?.rollerV2 ?? [];
  const roller = rollerFraInput.map((d) => ({
    ...d,
    rolle: konverterRolletype(d.rolle),
  }));
  return (
    <ThemeProvider styling={styling}>
      <DokumentFellesContext.Provider
        value={{
          typeInnhold: notat ? TypeInnhold.NOTAT : TypeInnhold.VEDTAK,
          roller: roller,
          saksnummer: notat?.saksnummer ?? dokumentbestilling?.saksnummer ?? "",
          bidragsmottaker: roller.find(erRolle(Rolletype.BM))!,
          bidragspliktig: roller.find(erRolle(Rolletype.BP)),
          søknadsbarn: roller.filter(erRolle(Rolletype.BA)),
        }}
      >
        {children}
      </DokumentFellesContext.Provider>
    </ThemeProvider>
  );
}
