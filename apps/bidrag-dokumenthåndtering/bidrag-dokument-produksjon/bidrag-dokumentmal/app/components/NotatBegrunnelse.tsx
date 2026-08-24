import xss from "xss";
import { NotatBegrunnelseDto } from "~/types/Api";
import { DataViewTable, DataViewTableData } from "~/components/DataViewTable";

type NotatProps = {
  data?: NotatBegrunnelseDto;
  label?: string;
};
export default function NotatBegrunnelse({
  data,
  label = "Begrunnelse",
}: NotatProps) {
  return (
    <div
      className={"mt-2"}
      style={{
        maxWidth: "35rem",
        overflowWrap: "anywhere",
      }}
    >
      <DataViewTable
        data={
          [
            {
              label,
              textRight: false,
              labelBold: true,
            },
            {
              label: undefined,
              textRight: false,
              labelBold: true,
              value: (
                <PurifiedHtml text={data?.innhold?.replaceAll("&nbsp;", " ")} />
              ),
            },
          ].filter((d) => d != null) as DataViewTableData[]
        }
      />
      {data?.innholdFraOmgjortVedtak && (
        <DataViewTable
          className={"pt-1"}
          data={
            [
              {
                label: label
                  ? `${label} (opprinnelig vedtak)`
                  : "Begrunnelse (opprinnelig vedtak)",
                textRight: false,
                labelBold: true,
              },
              {
                label: undefined,
                textRight: false,
                labelBold: true,
                value: (
                  <PurifiedHtml
                    text={data?.innholdFraOmgjortVedtak?.replaceAll(
                      "&nbsp;",
                      " ",
                    )}
                  />
                ),
              },
            ].filter((d) => d != null) as DataViewTableData[]
          }
        />
      )}
    </div>
  );
}

function PurifiedHtml({ text }: { text?: string }) {
  if (text == null || text === "") return null;
  const cleanText = xss(
    text
      .replace(/(?:\r\n|\r|\n)/g, "<br />")
      .replaceAll("<p><br/></p>", "<br/>"),
    {
      whiteList: {
        ...getDefaultWhiteList(),
        em: ["style"],
        p: ["style"],
      },
    },
  );

  return (
    <span
      className={"begrunnelse"}
      dangerouslySetInnerHTML={{ __html: cleanText }}
    />
  );
}
function getDefaultWhiteList() {
  return {
    a: ["target", "href", "title"],
    abbr: ["title"],
    address: [],
    area: ["shape", "coords", "href", "alt"],
    article: [],
    aside: [],
    audio: [
      "autoplay",
      "controls",
      "crossorigin",
      "loop",
      "muted",
      "preload",
      "src",
    ],
    b: [],
    bdi: ["dir"],
    bdo: ["dir"],
    big: [],
    blockquote: ["cite"],
    br: [],
    caption: [],
    center: [],
    cite: [],
    code: [],
    col: ["align", "valign", "span", "width"],
    colgroup: ["align", "valign", "span", "width"],
    dd: [],
    del: ["datetime"],
    details: ["open"],
    div: [],
    dl: [],
    dt: [],
    em: [],
    figcaption: [],
    figure: [],
    font: ["color", "size", "face"],
    footer: [],
    h1: [],
    h2: [],
    h3: [],
    h4: [],
    h5: [],
    h6: [],
    header: [],
    hr: [],
    i: [],
    img: ["src", "alt", "title", "width", "height", "loading"],
    ins: ["datetime"],
    kbd: [],
    li: [],
    mark: [],
    nav: [],
    ol: [],
    p: [],
    pre: [],
    s: [],
    section: [],
    small: [],
    span: [],
    sub: [],
    summary: [],
    sup: [],
    strong: [],
    strike: [],
    table: ["width", "border", "align", "valign"],
    tbody: ["align", "valign"],
    td: ["width", "rowspan", "colspan", "align", "valign"],
    tfoot: ["align", "valign"],
    th: ["width", "rowspan", "colspan", "align", "valign"],
    thead: ["align", "valign"],
    tr: ["rowspan", "align", "valign"],
    tt: [],
    u: [],
    ul: [],
    video: [
      "autoplay",
      "controls",
      "crossorigin",
      "loop",
      "muted",
      "playsinline",
      "poster",
      "preload",
      "src",
      "height",
      "width",
    ],
  };
}
