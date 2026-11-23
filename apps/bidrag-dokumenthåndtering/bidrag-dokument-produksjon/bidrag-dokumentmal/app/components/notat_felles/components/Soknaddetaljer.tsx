import { rolleTilVisningsnavnV2, sammenlignRoller } from "~/utils/visningsnavn";
import { DokumentmalPersonDto, Rolletype } from "~/types/Api";
import { dateToDDMMYYYY, sortByAge } from "~/utils/date-utils";
import NavLogo from "~/components/NavLogo";
import { FeltTabell, Feltrad, Feltrader } from "~/components/FeltTabell";
import {
  TypeInnhold,
  useDokumentFelles,
} from "~/components/vedtak_felles/FellesContext";
import { isNullOrEmpty } from "~/utils/string-utils";
import { tekst } from "~/utils/tekst";
export default function Soknaddetaljer({
  gjelderFlereSaker,
}: {
  gjelderFlereSaker: boolean;
}) {
  const { roller, saksnummer, typeInnhold } = useDokumentFelles();
  const rollerIkkeBarn = roller.filter(
    (rolle) => !sammenlignRoller(rolle.rolle, Rolletype.BA),
  );
  const rollerBarn = roller.filter((rolle) =>
    sammenlignRoller(rolle.rolle, Rolletype.BA),
  );
  function tilNavnOgFødselsdato(rolle: DokumentmalPersonDto) {
    if (isNullOrEmpty(rolle.navn)) {
      return dateToDDMMYYYY(rolle.fødselsdato);
    }
    if (isNullOrEmpty(dateToDDMMYYYY(rolle.fødselsdato))) {
      return rolle.navn;
    }
    return rolle.navn + " / " + dateToDDMMYYYY(rolle.fødselsdato);
  }
  return (
    <div className={"soknad_detaljer mb-[30px]"}>
      <div>
        {typeInnhold == TypeInnhold.NOTAT && <NavLogo />}
        <div>
          <FeltTabell labelColWidth={"98px"}>
            {gjelderFlereSaker ? (
              <Feltrad />
            ) : (
              <Feltrad label={tekst("ord.saksnummer", "Saksnummer")}>
                {typeInnhold === TypeInnhold.NOTAT
                  ? saksnummer
                  : tekst(
                      "ord.saksnummerMedHenvisning",
                      "{} (Oppgi saksnummeret ved henvendelse til oss.)",
                    ).replace("{}", `${saksnummer}`)}
              </Feltrad>
            )}
            <Feltrader
              rows={[
                ...rollerIkkeBarn
                  .sort((a, b) => {
                    const rolleA = rolleTilVisningsnavnV2(a);
                    const rolleB = rolleTilVisningsnavnV2(b);
                    if (rolleA === "BM" && rolleB !== "BM") return -1;
                    if (rolleA !== "BM" && rolleB === "BM") return 1;
                    if (rolleA === "BP" && rolleB !== "BP") return -1;
                    if (rolleA !== "BP" && rolleB === "BP") return 1;
                    return rolleA.localeCompare(rolleB);
                  })
                  .map((rolle) => ({
                    label: rolleTilVisningsnavnV2(rolle)!,
                    value: `${tilNavnOgFødselsdato(rolle)}${gjelderFlereSaker && rolle.rolle != Rolletype.BP ? ` / ${rolle.saksnummer}` : ""}`,
                  })),
                {
                  label: tekst(
                    "notat.begrunnelse.felles.søknaddetaljer.søknadsbarn",
                    "Søknadsbarn",
                  ),
                  value: (
                    <div style={{ display: "inline-table" }}>
                      {rollerBarn.sort(sortByAge).map((rolle) => (
                        <span
                          key={rolle.ident}
                          style={{
                            width: "100%",
                            display: "block",
                          }}
                        >
                          {`${tilNavnOgFødselsdato(rolle)}${gjelderFlereSaker && rolle.rolle != Rolletype.BP ? ` / ${rolle.saksnummer}` : ""}`}
                        </span>
                      ))}
                    </div>
                  ),
                },
              ]}
            />
          </FeltTabell>
        </div>
      </div>
    </div>
  );
}
