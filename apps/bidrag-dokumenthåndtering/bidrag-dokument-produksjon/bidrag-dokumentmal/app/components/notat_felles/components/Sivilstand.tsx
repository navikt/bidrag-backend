import { SimpleTable } from "~/components/SimpleTable";
import notatTekster from "~/tekster/notat/felles/saksbehandler.json";
import { NotatMalType } from "~/types/Api";
import { useNotatFelles } from "~/components/notat_felles/NotatContext";

export default function Sivilstand() {
  const { data } = useNotatFelles();
  if (data.type != NotatMalType.FORSKUDD) return;
  return (
    <div style={{ pageBreakBefore: "auto" }}>
      <h3 style={{ marginBottom: 0 }} id={"linktilmeg"}>
        {notatTekster.boforhold.sivilstand.tittel}
      </h3>
      <div>
        <SimpleTable
          data={data.boforhold.sivilstand.opplysningerBruktTilBeregning}
        />
      </div>
    </div>
  );
}
