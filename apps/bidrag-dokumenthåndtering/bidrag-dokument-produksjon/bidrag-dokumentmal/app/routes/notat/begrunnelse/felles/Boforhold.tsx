import {
  BoforholdBarn,
  NotatAndreVoksneIHusstanden,
  NotatMalType,
} from "~/types/Api";
import Person from "~/components/Person";
import { SimpleTable } from "~/components/SimpleTable";
import NotatBegrunnelse from "~/components/NotatBegrunnelse";
import { useNotatFelles } from "~/components/notat_felles/NotatContext";
import { LenkeTilVedlegg } from "~/components/notat_felles/Seksjon";
import Sivilstand from "~/routes/notat/begrunnelse/felles/Sivilstand";
import {
  DokumentTabell,
  Tabellcelle,
  Tabellrad,
} from "~/components/DokumentTabell";
import { formatPeriode, sortByAge } from "~/utils/date-utils";
import { FeltTabell, Feltrad } from "~/components/FeltTabell";
import { tekst } from "~/utils/tekst";
export default function Boforhold() {
  const { erAvslag, data } = useNotatFelles();
  if (erAvslag) return null;
  return (
    <>
      <div className={"elements_inline section-title"}>
        <h2>
          {tekst("notat.begrunnelse.felles.boforhold.tittel", "Boforhold")}
        </h2>
        <LenkeTilVedlegg
          vedlegg="boforhold"
          lenketekst={tekst(
            "ord.seVedleggForOffentligeRegistre",
            "se vedlegg nr. {} for opplysninger fra offentlige registre",
          )}
        />
      </div>
      {data.boforhold.barn
        .sort((a, b) => sortByAge(a.gjelder, b.gjelder))
        .map((barn, index) => (
          <BoforholdHusstandsmedlem
            key={barn.gjelder + index.toString()}
            data={barn}
          />
        ))}
      {data.type === NotatMalType.FORSKUDD ? (
        <Sivilstand />
      ) : (
        <BoforholdAndreVoksneIHusstanden
          data={data.boforhold.andreVoksneIHusstanden}
        />
      )}
      <NotatBegrunnelse data={data.boforhold.begrunnelse} />
      <BoforholdBeregnetTabell />
    </>
  );
}
function BoforholdBeregnetTabell() {
  const { data } = useNotatFelles();
  const beregnetBoforhold = data.boforhold.beregnetBoforhold;
  if (beregnetBoforhold.length === 0) return null;
  return (
    <>
      <h3 id={"linktilmeg"}>
        {tekst(
          "notat.begrunnelse.felles.boforhold.beregnetBoforhold.tittel",
          "Beregnet boforhold",
        )}
      </h3>

      <DokumentTabell
        kolonner={[
          {
            tittel: tekst(
              "felles.tabell.felles.fraTilOgMed",
              "Fra og med - Til og med",
            ),
          },
          {
            tittel: tekst(
              "notat.begrunnelse.felles.boforhold.beregnetBoforhold.kolonner.antallBarnIHusstanden",
              "Antall barn i husstanden",
            ),
          },
          {
            tittel: tekst(
              "notat.begrunnelse.felles.boforhold.beregnetBoforhold.kolonner.borMedAndreVoksne",
              "Bor med andre voksne",
            ),
          },
        ]}
      >
        {beregnetBoforhold.map((boforhold, index) => (
          <Tabellrad key={index} index={index}>
            <Tabellcelle>
              {formatPeriode(boforhold.periode.fom, boforhold.periode.til)}
            </Tabellcelle>
            <Tabellcelle>{boforhold.antallBarn}</Tabellcelle>
            <Tabellcelle>
              {boforhold.borMedAndreVoksne
                ? tekst("ord.ja", "Ja")
                : tekst("ord.nei", "Nei")}
            </Tabellcelle>
          </Tabellrad>
        ))}
      </DokumentTabell>
    </>
  );
}
function BoforholdAndreVoksneIHusstanden({
  data,
}: {
  data?: NotatAndreVoksneIHusstanden;
}) {
  if (!data) return;
  return (
    <div className={"mb-2"}>
      <h3 id={"linktilmeg"}>
        {tekst(
          "notat.begrunnelse.felles.boforhold.andreVoksneIHusstanden.tittel",
          "Andre voksne i husstanden",
        )}
      </h3>
      <SimpleTable data={data.opplysningerBruktTilBeregning} />
    </div>
  );
}
function BoforholdHusstandsmedlem({ data }: { data: BoforholdBarn }) {
  return (
    <div className={"mb-3"}>
      <div className={"mb-2 mt-2"}>
        <FeltTabell>
          <Feltrad
            label={
              data.medIBehandling
                ? data.gjelder.revurdering
                  ? tekst(
                      "notat.begrunnelse.felles.boforhold.revurderingsbarn",
                      "Revurderingsbarn",
                    )
                  : tekst(
                      "notat.begrunnelse.felles.boforhold.søknadsbarn",
                      "Søknadsbarn",
                    )
                : tekst(
                    "notat.begrunnelse.felles.boforhold.egetBarnIHusstanden",
                    "Eget barn i husstanden",
                  )
            }
            labelBold
          >
            <Person
              fødselsdato={data.gjelder.fødselsdato!}
              navn={data.gjelder.navn!}
              erBeskyttet={data.gjelder.erBeskyttet}
            />
          </Feltrad>
        </FeltTabell>
      </div>

      <SimpleTable data={data.opplysningerBruktTilBeregning} />
    </div>
  );
}
