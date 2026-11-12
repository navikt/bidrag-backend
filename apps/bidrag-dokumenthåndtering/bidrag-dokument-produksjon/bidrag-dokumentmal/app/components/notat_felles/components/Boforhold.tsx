import {
  BoforholdBarn,
  NotatAndreVoksneIHusstanden,
  NotatMalType,
} from "~/types/Api";
import Person from "~/components/Person";
import { SimpleTable } from "~/components/SimpleTable";
import NotatBegrunnelse from "~/components/NotatBegrunnelse";
import elementIds from "~/utils/elementIds";
import ord from "~/tekster/ord.json";
import tekster from "~/tekster";
import notatTekster from "~/tekster/notat/felles/saksbehandler.json";
import {
  useNotatFelles,
  useVedleggNummer,
} from "~/components/notat_felles/NotatContext";
import Sivilstand from "~/components/notat_felles/components/Sivilstand";
import { CommonTable } from "~/components/CommonTable";
import { formatPeriode, sortByAge } from "~/utils/date-utils";
import { DataViewTable, DataViewTableData } from "~/components/DataViewTable";

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
      <>
        {data.boforhold.barn
          .sort((a, b) => sortByAge(a.gjelder, b.gjelder))
          .map((b, i) => (
            <BoforholdHusstandsmedlem key={b.gjelder + i.toString()} data={b} />
          ))}
        {data.type === NotatMalType.FORSKUDD && <Sivilstand />}
        {data.type !== NotatMalType.FORSKUDD && (
          <BoforholdAndreVoksneIHusstanden
            data={data.boforhold.andreVoksneIHusstanden}
          />
        )}
        <NotatBegrunnelse data={data.boforhold.begrunnelse} />
        <BoforholdBeregnetTabell />
      </>
    </>
  );
}

function BoforholdBeregnetTabell() {
  const { data } = useNotatFelles();
  if (data.boforhold.beregnetBoforhold.length === 0) return null;
  return (
    <>
      <h3 id={"linktilmeg"}>{t.beregnetBoforhold.tittel}</h3>

      <CommonTable
        data={{
          headers: [
            {
              name: tekster.tabell.felles.fraTilOgMed,
            },
            {
              name: t.beregnetBoforhold.kolonner.antallBarnIHusstanden,
            },
            {
              name: t.beregnetBoforhold.kolonner.borMedAndreVoksne,
            },
          ],
          rows: data.boforhold.beregnetBoforhold.map((d) => ({
            columns: [
              { content: formatPeriode(d.periode.fom, d.periode.til) },
              { content: d.antallBarn },
              {
                content: d.borMedAndreVoksne ? ord.ja : ord.nei,
              },
            ],
          })),
        }}
      />
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
      <DataViewTable
        className={"mb-2 mt-2"}
        data={
          [
            {
              label: data.medIBehandling
                ? data.gjelder.revurdering
                  ? t.revurderingsbarn
                  : t.søknadsbarn
                : t.egetBarnIHusstanden,
              labelBold: true,
              value: (
                <Person
                  fødselsdato={data.gjelder.fødselsdato!}
                  navn={data.gjelder.navn!}
                  erBeskyttet={data.gjelder.erBeskyttet}
                />
              ),
            },
          ].filter((d) => d != null) as DataViewTableData[]
        }
      />

      <SimpleTable data={data.opplysningerBruktTilBeregning} />
    </div>
  );
}
