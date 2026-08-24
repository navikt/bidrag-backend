import { RenderMode } from "~/components/notat_felles/NotatContext";

export default function HeaderFooter({
  renderMode,
  saksnummer,
  saker,
}: {
  renderMode: RenderMode;
  saksnummer?: string;
  saker?: string[];
}) {
  const flereSaker = saker && saker.length > 1;
  const enesteSaksnummer =
    saksnummer ?? (saker && saker.length > 0 ? saker[0] : "Ukjent");
  const saksreferanse = flereSaker
    ? `Saker ${saker.join(", ")}`
    : `Saksnummer ${enesteSaksnummer}`;

  // Lagt ut som en sidebred flex-rad (se #header/#footer i style.css) i stedet
  // for absolutt-posisjonerte spans. Absolutt posisjonering fungerte kun da
  // Gotenberg rendret header/footer som egne, sidebrede dokumenter. Som CSS
  // running elements lever de nå inni @top-center/@bottom-center-marginboksen.
  const renderHeaderFooterV2 = (isHeader: boolean) => (
    <div id={isHeader ? "header" : "footer"} className={"mt-4 mb-4"}>
      <span>{saksreferanse}</span>
      <span>
        <span className="pageNumber"></span>
        <span className="pageSeparator">av</span>
        <span className="totalPages"></span>
      </span>
    </div>
  );

  return (
    <>
      {renderMode == RenderMode.PDF && (
        <>
          {renderHeaderFooterV2(true)}
          {renderHeaderFooterV2(false)}
        </>
      )}
    </>
  );
}
