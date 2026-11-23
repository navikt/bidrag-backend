import { readFileSync } from "node:fs";
import { join } from "node:path";
import { renderToStaticMarkup } from "react-dom/server";
import { afterAll, beforeAll, describe, expect, it, vi } from "vitest";
import {
  NotatProvider,
  RenderMode,
  RenderPDFVersion,
  Vedlegg,
} from "~/components/notat_felles/NotatContext";
import { VedtakNotatDto, Vedtakstype } from "~/types/Api";
import { BegrunnelseBidrag } from "~/routes/notat/begrunnelse/bidrag/route";
import {
  OpplysningerBidrag,
  vedleggBidrag,
} from "~/routes/notat/opplysninger/bidrag/route";
import { BegrunnelseSærbidrag } from "~/routes/notat/begrunnelse/særbidrag/route";
import {
  OpplysningerSærbidrag,
  vedleggSærbidrag,
} from "~/routes/notat/opplysninger/særbidrag/route";
import { BegrunnelseForskudd } from "~/routes/notat/begrunnelse/forskudd/route";
import {
  OpplysningerForskudd,
  vedleggForskudd,
} from "~/routes/notat/opplysninger/forskudd/route";

const EKSEMPELDATA = join(__dirname, "../../../../../data/notat");
const FAST_DATO = new Date("2026-01-15T12:00:00Z");

function lesEksempel(fil: string): VedtakNotatDto {
  return JSON.parse(readFileSync(join(EKSEMPELDATA, fil), "utf8"));
}

function renderNotat(
  data: VedtakNotatDto,
  vedlegg: Vedlegg[],
  innhold: React.ReactNode,
): string {
  return renderToStaticMarkup(
    <NotatProvider
      data={data}
      styling={"V2"}
      renderMode={RenderMode.PDF}
      renderPDFVersion={RenderPDFVersion.V2}
      vedlegg={vedlegg}
    >
      {innhold}
    </NotatProvider>,
  );
}

const notater = [
  {
    navn: "bidrag",
    fil: "bidrag.json",
    vedlegg: vedleggBidrag,
    innhold: (
      <>
        <BegrunnelseBidrag />
        <OpplysningerBidrag />
      </>
    ),
  },
  {
    navn: "særbidrag",
    fil: "saerbidrag.json",
    vedlegg: vedleggSærbidrag,
    innhold: (
      <>
        <BegrunnelseSærbidrag />
        <OpplysningerSærbidrag />
      </>
    ),
  },
  ...["forskudd_innvilget.json", "forskudd_avslag.json"].map((fil) => ({
    navn: fil.replace(".json", ""),
    fil,
    vedlegg: vedleggForskudd,
    innhold: (
      <>
        <BegrunnelseForskudd />
        <OpplysningerForskudd />
      </>
    ),
  })),
];

describe("notat-HTML", () => {
  beforeAll(() => {
    vi.useFakeTimers({ toFake: ["Date"] });
    vi.setSystemTime(FAST_DATO);
  });
  afterAll(() => vi.useRealTimers());

  it.each(notater)(
    "skal gi uendret HTML for $navn",
    async ({ fil, vedlegg, innhold }) => {
      const data = lesEksempel(fil);
      const html = renderNotat(data, vedlegg(data), innhold);
      await expect(html).toMatchFileSnapshot(
        `__snapshots__/${fil.replace(".json", ".html")}`,
      );
    },
  );

  it("beholder rekkefølgen på vedleggene for hver notattype", () => {
    const data = lesEksempel("bidrag.json");

    expect(vedleggBidrag(data)).toEqual([
      "underholdskostnader",
      "boforhold",
      "inntekter",
      "samvær",
      "beregningsdetaljer",
    ]);
    expect(vedleggSærbidrag()).toEqual([
      "inntekter",
      "boforhold",
      "beregningsdetaljer",
    ]);
    expect(vedleggForskudd()).toEqual(["boforhold", "inntekter"]);
  });

  it("beholder nummerlisten for innkreving uten å vise ordinære vedlegg", () => {
    const eksempel = lesEksempel("bidrag.json");
    const data = {
      ...eksempel,
      behandling: {
        ...eksempel.behandling,
        vedtakstype: Vedtakstype.INNKREVING,
      },
    };
    const vedlegg = vedleggBidrag(data);

    expect(vedlegg).toEqual(["beregningsdetaljer"]);
    expect(renderNotat(data, vedlegg, <OpplysningerBidrag />)).toBe("");
  });
});
