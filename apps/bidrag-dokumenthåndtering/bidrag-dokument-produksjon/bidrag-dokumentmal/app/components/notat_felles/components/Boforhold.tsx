import {
  BoforholdBarn,
  NotatAndreVoksneIHusstanden,
  NotatMalType,
} from "~/types/Api";
import Person from "~/components/Person";
import { SimpleTable } from "~/components/SimpleTable";
import NotatBegrunnelse from "~/components/NotatBegrunnelse";
import elementIds from "~/utils/elementIds";
import tekster from "~/tekster";
import { useNotatFelles } from "~/components/notat_felles/NotatContext";
import Sivilstand from "~/components/notat_felles/components/Sivilstand";
import { CommonTable } from "~/components/CommonTable";
import { formatPeriode, sortByAge } from "~/utils/date-utils";
import { DataViewTable, DataViewTableData } from "~/components/DataViewTable";
import { VedleggProps } from "~/types/commonTypes";

export default function Boforhold({ vedleggNummer = 1 }: VedleggProps) {
  const { erAvslag, data } = useNotatFelles();
  if (erAvslag) return null;
  return (
    <>
      <div className={"elements_inline section-title"}>
        <h2>{tekster.titler.boforhold.tittel}</h2>
        <a href={`#${elementIds.vedleggBoforhold}`}>
          {tekster.vedleggLenke.replace("{}", vedleggNummer?.toString())}
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
      <h3 id={"linktilmeg"}>
        {tekster.titler.boforhold.beregnetBoforholdTittel}
      </h3>

      <CommonTable
        data={{
          headers: [
            {
              name: tekster.tabell.felles.fraTilOgMed,
            },
            {
              name: "Antall barn i husstanden",
            },
            {
              name: "Bor med andre voksne",
            },
          ],
          rows: data.boforhold.beregnetBoforhold.map((d) => ({
            columns: [
              { content: formatPeriode(d.periode.fom, d.periode.til) },
              { content: d.antallBarn },
              { content: d.borMedAndreVoksne ? "Ja" : "Nei" },
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
      <h3 id={"linktilmeg"}>{tekster.titler.andreVoksneIHusstanden.tittel}</h3>
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
                  ? tekster.titler.boforhold.revurderingsbarn
                  : tekster.titler.boforhold.søknadsbarn
                : tekster.titler.boforhold.egetBarnIHusstanden,
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
