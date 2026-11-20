import { DokumentmalPersonDto } from "~/types/Api";
import { FeltTabell, Feltrad } from "~/components/FeltTabell";
import { PersonV2 } from "~/components/Person";
import { rolleTilVisningsnavnV2 } from "~/utils/visningsnavn";

export default function GjelderPerson({
  rolle,
  visFødselsdato,
}: {
  rolle: DokumentmalPersonDto;
  visFødselsdato?: boolean;
}) {
  return (
    <div className={"mb-2 mt-2"}>
      <FeltTabell>
        <Feltrad label={rolleTilVisningsnavnV2(rolle)} labelBold>
          <PersonV2 {...rolle} visFødselsdato={visFødselsdato} />
        </Feltrad>
      </FeltTabell>
    </div>
  );
}
