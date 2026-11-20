import {
  ActionFunctionArgs,
  useActionData,
  type MetaFunction,
} from "react-router";
import { PropsWithChildren } from "react";
import "~/style/style.css";
import {
  NotatProvider,
  RenderMode,
  Vedlegg,
} from "~/components/notat_felles/NotatContext";
import HeaderFooter from "~/components/notat_felles/HeaderFooter";
import Soknaddetaljer from "~/components/notat_felles/components/Soknaddetaljer";
import NotatTittel from "~/components/NotatTittel";
import { NotatRequest, parseRequestAction } from "~/routes/common";
import { VedtakNotatDto } from "~/types/Api";

/** Felles `action` for alle notat-routes: leser notatdataene fra forespørselen. */
export async function notatAction(args: ActionFunctionArgs) {
  return await parseRequestAction(args);
}

/** Felles `meta` for alle notat-routes: tittelen på dokumentet. */
export function notatMeta(tittel: string): MetaFunction {
  return () => [
    { title: tittel },
    { name: "description", content: tittel },
    { property: "author", content: "bidrag-dokument-produksjon" },
    { property: "subject", content: tittel },
  ];
}

type NotatSideProps = {
  id: string;
  tittel: string;
  /** Vedleggene i notatet, i rekkefølge. Nummeret til et vedlegg følger av plassen i listen. */
  vedlegg: (data: VedtakNotatDto) => Vedlegg[];
  /** Bidrag kan gjelde flere saker. Da vises alle sakene i topp- og bunnteksten og i søknadsdetaljene. */
  kanGjeldeFlereSaker?: boolean;
};

/**
 * Rammen rundt alle notatene: topp- og bunntekst, søknadsdetaljer og tittel.
 * Innholdet (begrunnelsen og/eller opplysningene) legges inn som `children`.
 */
export default function NotatSide({
  id,
  tittel,
  vedlegg,
  kanGjeldeFlereSaker = false,
  children,
}: PropsWithChildren<NotatSideProps>) {
  const response = useActionData<NotatRequest>();
  if (response === undefined) {
    return <div>Oops</div>;
  }

  const data = response.data;
  const renderMode = response.renderForPdf ? RenderMode.PDF : RenderMode.HTML;
  const saker = Array.from(
    new Set(
      data.personer.map((p) => p.saksnummer).filter((s) => s != undefined),
    ),
  );
  const gjelderFlereSaker =
    kanGjeldeFlereSaker &&
    new Set(
      data.roller
        .filter((r) => r.saksnummer !== undefined)
        .map((r) => r.saksnummer),
    ).size > 1;

  return (
    <div id={id}>
      <NotatProvider
        data={data}
        styling={"V2"}
        renderPDFVersion={response.renderPDFVersion}
        renderMode={renderMode}
        vedlegg={vedlegg(data)}
      >
        {kanGjeldeFlereSaker ? (
          <HeaderFooter saker={saker} renderMode={renderMode} />
        ) : (
          <HeaderFooter saksnummer={data.saksnummer} renderMode={renderMode} />
        )}
        <div className={"container page"}>
          <Soknaddetaljer gjelderFlereSaker={gjelderFlereSaker} />
          <NotatTittel title={tittel} />
          {children}
        </div>
      </NotatProvider>
    </div>
  );
}
