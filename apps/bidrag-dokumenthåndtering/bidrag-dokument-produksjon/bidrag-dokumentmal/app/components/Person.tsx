import { dateToDDMMYYYY } from "~/utils/date-utils";
import { DokumentmalPersonDto, Rolletype } from "~/types/Api";
import { useNotatFelles } from "~/components/notat_felles/NotatContext";

type PersonProps = {
  navn: string;
  fødselsdato: string;
  personident?: string;
  erBeskyttet?: boolean;
};
export default function Person({
  navn,
  fødselsdato,
  erBeskyttet,
}: PersonProps) {
  if (erBeskyttet) {
    //TODO: Visning av beskyttelse
    return <span>{navn}</span>;
  }
  return (
    <span>
      {navn} / {dateToDDMMYYYY(fødselsdato)}
    </span>
  );
}

export function PersonV2(
  props: DokumentmalPersonDto & { visFødselsdato?: boolean },
) {
  const { visFødselsdato = false, ...person } = props;
  const { gjelderFlereSaker } = useNotatFelles();
  const erBp = person.rolle === Rolletype.BP;
  const sakDetaljer =
    gjelderFlereSaker && !erBp ? ` (sak ${person.saksnummer})` : "";
  if (person.erBeskyttet) {
    //TODO: Visning av beskyttelse
    return (
      <span>
        {person.navn}
        {sakDetaljer}
      </span>
    );
  }
  if (visFødselsdato) {
    return (
      <span>
        {person.navn} / {dateToDDMMYYYY(person.fødselsdato)}
        {sakDetaljer}
      </span>
    );
  }
  return (
    <span>
      {person.navn}
      {sakDetaljer}
    </span>
  );
}
