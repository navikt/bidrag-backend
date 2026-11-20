import { DokumentmalPersonDto } from "~/types/Api";
import { dateToDDMMYYYY } from "~/utils/date-utils";
import { FeltTabell, Feltrad } from "~/components/FeltTabell";
import { useTheme } from "~/components/notat_felles/ThemeContext";
import Person from "~/components/Person";
import { rolleTilVisningsnavnV2 } from "~/utils/visningsnavn";

export default function TableGjelderBarn({
  gjelderBarn,
}: {
  gjelderBarn: DokumentmalPersonDto;
}) {
  const { styling } = useTheme();
  if (styling == "V2") {
    return (
      <div className={"mb-2 mt-2"}>
        <FeltTabell>
          <Feltrad label={rolleTilVisningsnavnV2(gjelderBarn)} labelBold>
            {
              <Person
                fødselsdato={gjelderBarn.fødselsdato!}
                navn={gjelderBarn.navn!}
                erBeskyttet={gjelderBarn.erBeskyttet}
              />
            }
          </Feltrad>
        </FeltTabell>
      </div>
    );
  }
  return (
    <dl style={{ marginBottom: 0 }}>
      <dt>{gjelderBarn.navn}</dt>
      <dd style={{ display: "inline-table" }}>
        / {dateToDDMMYYYY(gjelderBarn.fødselsdato)}
      </dd>
    </dl>
  );
}
