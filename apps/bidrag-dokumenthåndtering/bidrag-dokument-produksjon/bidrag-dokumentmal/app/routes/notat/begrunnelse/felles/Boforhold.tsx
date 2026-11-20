import {
  BoforholdBarn,
  NotatAndreVoksneIHusstanden,
  NotatMalType,
} from "~/types/Api";
import Person from "~/components/Person";
import { SimpleTable } from "~/components/SimpleTable";
import NotatBegrunnelse from "~/components/NotatBegrunnelse";
import elementIds from "~/utils/elementIds";
import ord from "~/tekster/ord.yaml";
import tekster from "~/tekster";
import notatTekster from "~/tekster/notat/begrunnelse/felles.yaml";
import {
  useNotatFelles,
  useVedleggNummer,
} from "~/components/notat_felles/NotatContext";
import Sivilstand from "~/routes/notat/begrunnelse/felles/Sivilstand";
import {
  DokumentTabell,
  Tabellcelle,
  Tabellrad,
} from "~/components/DokumentTabell";
import { formatPeriode, sortByAge } from "~/utils/date-utils";
import { FeltTabell, Feltrad } from "~/components/FeltTabell";

const t = notatTekster.boforhold;

export default function Boforhold() {
  const vedleggNummer = useVedleggNummer("boforhold");
  const { erAvslag, data } = useNotatFelles();
  if (erAvslag) return null;
  return (
    <>
      <div className={"elements_inline section-title"}>
        <h2>{t.tittel}</h2>
        <a href={`#${elementIds.vedleggBoforhold}`}>
          {ord.seVedleggForOffentligeRegistre.replace(
            "{}",
            vedleggNummer?.toString(),
          )}
        </a>
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
      <h3 id={"linktilmeg"}>{t.beregnetBoforhold.tittel}</h3>

      <DokumentTabell
        kolonner={[
          { tittel: tekster.tabell.felles.fraTilOgMed },
          { tittel: t.beregnetBoforhold.kolonner.antallBarnIHusstanden },
          { tittel: t.beregnetBoforhold.kolonner.borMedAndreVoksne },
        ]}
      >
        {beregnetBoforhold.map((boforhold, index) => (
          <Tabellrad key={index} index={index}>
            <Tabellcelle>
              {formatPeriode(boforhold.periode.fom, boforhold.periode.til)}
            </Tabellcelle>
            <Tabellcelle>{boforhold.antallBarn}</Tabellcelle>
            <Tabellcelle>
              {boforhold.borMedAndreVoksne ? ord.ja : ord.nei}
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
      <h3 id={"linktilmeg"}>{t.andreVoksneIHusstanden.tittel}</h3>
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
                  ? t.revurderingsbarn
                  : t.søknadsbarn
                : t.egetBarnIHusstanden
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
