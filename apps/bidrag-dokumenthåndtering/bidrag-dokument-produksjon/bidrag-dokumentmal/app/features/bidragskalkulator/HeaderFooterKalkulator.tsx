import { hentTekst, SpråkType } from "~/utils/oversettelser";

const contentWithCustomStyles = `
      body {
        font-family: "Source Sans 3";
        font-size: 8px;
        line-height: 16px;
      }
`;

interface HeaderFooterProps {
  språk?: SpråkType;
}
export default function HeaderFooter({ språk }: HeaderFooterProps) {
  const valgteSpråk = språk || "nb";
  const tekster = hentTekst(valgteSpråk, tekst);

  const renderHeaderFooterV2 = (isHeader: boolean) => (
    <>
      <div id={isHeader ? "header" : "footer"}>
        <style
          dangerouslySetInnerHTML={{
            __html: contentWithCustomStyles,
          }}
        ></style>
        <span
          style={{
            position: "absolute",
            right: "80px",
            top: isHeader ? "40px" : "auto",
            bottom: isHeader ? "auto" : "40px",
          }}
        >
          <span className="pageNumber"></span>
          <span style={{ marginRight: "5px", marginLeft: "5px" }}>
            {tekster.av}
          </span>
          <span className="totalPages"></span>
        </span>
      </div>
    </>
  );
  return (
    <>
      {renderHeaderFooterV2(true)}
      {renderHeaderFooterV2(false)}
    </>
  );
}

const tekst = {
  av: {
    nb: "av",
    nn: "av",
    en: "of",
  },
};
