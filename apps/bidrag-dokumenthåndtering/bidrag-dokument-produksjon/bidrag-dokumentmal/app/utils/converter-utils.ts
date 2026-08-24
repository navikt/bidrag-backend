import { Rolletype, DokumentmalPersonDto } from "~/types/Api";

export const konverterRolletype = (
  rolle?: Rolletype,
): Rolletype | undefined => {
  switch (rolle as string) {
    case "BIDRAGSMOTTAKER":
      return Rolletype.BM;
    case "BIDRAGSPLIKTIG":
      return Rolletype.BP;
    case "BARN":
      return Rolletype.BA;
    case "REEL_MOTTAKER":
      return Rolletype.RM;
    case "FEILREGISTRERT":
      return Rolletype.FR;
    default:
      return rolle;
  }
};
export const erRolle =
  (rolletype: Rolletype) => (person: DokumentmalPersonDto) =>
    konverterRolletype(person.rolle) == rolletype;
