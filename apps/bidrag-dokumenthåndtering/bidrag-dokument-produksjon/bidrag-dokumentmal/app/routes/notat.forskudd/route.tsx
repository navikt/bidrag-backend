import { useActionData, ActionFunctionArgs } from "react-router";
import "../../style/style.css";
import Vedtak from "~/routes/notat.forskudd/Vedtak";
import tekster from "~/tekster";
import {
  NotatProvider,
  RenderMode,
} from "~/components/notat_felles/NotatContext";
import Soknaddetaljer from "~/components/notat_felles/components/Soknaddetaljer";
import Boforhold from "~/components/notat_felles/components/Boforhold";
import Inntekter from "~/components/notat_felles/components/Inntekter";
import VedleggBoforhold from "~/components/notat_felles/components/VedleggBoforhold";
import VedleggInntekter from "~/components/notat_felles/components/VedleggInntekter";
import NotatTittel from "~/components/NotatTittel";
import HeaderFooter from "~/components/notat_felles/HeaderFooter";
import { parseRequestAction, NotatRequest } from "~/routes/common";
import Virkningstidspunkt from "~/components/notat_felles/components/Virkningstidspunkt";

export async function action(args: ActionFunctionArgs) {
  return await parseRequestAction(args);
}

export function meta() {
  return [
    { title: tekster.titler.forskudd },
    { name: "description", content: tekster.titler.forskudd },
    { property: "author", content: "bidrag-dokument-produksjon" },
    { property: "subject", content: tekster.titler.forskudd },
  ];
}

export default function NotatForskudd() {
  const response = useActionData<NotatRequest>();
  if (response === undefined) {
    return <div>Oops</div>;
  }

  const data = response.data;
  return (
    <div id="forskudd_notat">
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
          <NotatTittel title={tekster.titler.forskudd} />
          <Virkningstidspunkt />
          <Boforhold vedleggNummer={1} />
          <Inntekter vedleggNummer={2} />
          <Vedtak />
          <VedleggBoforhold vedleggNummer={1} />
          <VedleggInntekter vedleggNummer={2} />
        </div>
      </NotatProvider>
    </div>
  );
}
