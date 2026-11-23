import { createContext, PropsWithChildren, useContext } from "react";
import { DokumentmalPersonDto, Rolletype } from "~/types/Api";
import { useNotatFelles } from "~/components/notat_felles/NotatContext";
import { velgRoller } from "~/utils/notatUtvalg";

const RolleContext = createContext<DokumentmalPersonDto | null>(null);

/** Gir rollen som vises nå. Kan bare brukes inne i `ForHverRolle`. */
export function useRolle(): DokumentmalPersonDto {
  const rolle = useContext(RolleContext);
  if (rolle == null)
    throw new Error("useRolle kan bare brukes inne i ForHverRolle");
  return rolle;
}

/**
 * Viser innholdet én gang for hver rolle av de gitte typene, i samme rekkefølge som `typer`.
 * Innholdet henter rollen med `useRolle()`.
 */
export function ForHverRolle({
  typer,
  children,
}: PropsWithChildren<{ typer: Rolletype[] }>) {
  const { data } = useNotatFelles();
  return velgRoller(data.roller, typer).map((rolle) => (
    <div key={`${rolle.rolle}-${rolle.ident}`} className={"mt-medium"}>
      <RolleContext.Provider value={rolle}>{children}</RolleContext.Provider>
    </div>
  ));
}
