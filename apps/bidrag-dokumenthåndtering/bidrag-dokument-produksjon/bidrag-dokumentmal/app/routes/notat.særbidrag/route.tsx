import { useActionData, ActionFunctionArgs } from "react-router";
import "../../style/style.css";
import tekster from "~/tekster";
import {
  NotatProvider,
  RenderMode,
} from "~/components/notat_felles/NotatContext";
import Boforhold from "~/components/notat_felles/components/Boforhold";
import Soknaddetaljer from "~/components/notat_felles/components/Soknaddetaljer";
import Utgifter from "~/routes/notat.særbidrag/Utgifter";
import Inntekter from "~/components/notat_felles/components/Inntekter";
import Vedtak from "~/routes/notat.særbidrag/Vedtak";
import VedleggBoforhold from "~/components/notat_felles/components/VedleggBoforhold";
import VedleggInntekter from "~/components/notat_felles/components/VedleggInntekter";
import NotatTittel from "~/components/NotatTittel";
import HeaderFooter from "~/components/notat_felles/HeaderFooter";
import VedleggBeregningsDetaljer from "~/routes/notat.særbidrag/VedleggBeregningsDetaljer";
import { parseRequestAction, NotatRequest } from "~/routes/common";

export async function action(args: ActionFunctionArgs) {
  return await parseRequestAction(args);
}

export function meta() {
  return [
    { title: tekster.titler.særbidrag },
    { name: "description", content: tekster.titler.særbidrag },
    { property: "author", content: "bidrag-dokument-produksjon" },
    { property: "subject", content: tekster.titler.særbidrag },
  ];
}

export default function NotatSærbidrag() {
  const response = useActionData<NotatRequest>();
  if (response === undefined) {
    return <div>Oops</div>;
  }

  const data = response.data;
  return (
    <div id="særbidrag_notat">
      <NotatProvider
        data={data}
        styling={"V2"}
        renderPDFVersion={response.renderPDFVersion}
        renderMode={response.renderForPdf ? RenderMode.PDF : RenderMode.HTML}
      >
        <HeaderFooter
          saksnummer={response.data.saksnummer}
          renderMode={response.renderForPdf ? RenderMode.PDF : RenderMode.HTML}
        />
        <div className={"container page"}>
          <Soknaddetaljer gjelderFlereSaker={false} />
          <NotatTittel title={tekster.titler.særbidrag} />
          <Utgifter />
          <Inntekter vedleggNummer={1} />
          <Boforhold vedleggNummer={2} />
          <Vedtak vedleggNummer={3} />
          <VedleggInntekter vedleggNummer={1} />
          <VedleggBoforhold vedleggNummer={2} />
          <VedleggBeregningsDetaljer vedleggNummer={3} />
        </div>
      </NotatProvider>
    </div>
  );
}
