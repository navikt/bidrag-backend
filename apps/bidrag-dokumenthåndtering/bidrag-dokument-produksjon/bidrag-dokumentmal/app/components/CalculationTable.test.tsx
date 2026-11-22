import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it } from "vitest";
import {
  CalculationTable,
  MathDivision,
  MathMultiplication,
  MathValue,
} from "~/components/CalculationTable";
import { ThemeProvider } from "~/components/notat_felles/ThemeContext";

describe("CalculationTable", () => {
  it("renders a title, calculation column, and summary row", () => {
    const html = renderToStaticMarkup(
      <ThemeProvider styling="V2">
        <CalculationTable
          title="Beregning"
          data={[
            {
              label: "Inntekt",
              calculation: "100 ÷ 2",
              result: "50",
            },
          ]}
          summary={{ label: "Resultat", result: "50" }}
          alignResults
          resultColumnWidth="100px"
        />
      </ThemeProvider>,
    );

    expect(html).toContain("<div><h4>Beregning</h4>");
    expect(html).toContain("Beregning</th>");
    expect(html).toContain('colSpan="2"');
    expect(html).toContain("text-align:right");
    expect(html).toContain('style="width:100px"');
  });

  it("omits the calculation column when no row has a calculation", () => {
    const html = renderToStaticMarkup(
      <ThemeProvider styling="V2">
        <CalculationTable
          data={[{ label: "Skatt", labelBold: true, result: "10" }]}
        />
      </ThemeProvider>,
    );

    expect(html).not.toContain("Beregning</th>");
    expect(html).toContain("font-weight:bold");
    expect(html).not.toContain("<td colSpan=");
  });
});

describe("calculation expressions", () => {
  it("renders values, division, and multiplication with signs", () => {
    const html = renderToStaticMarkup(
      <>
        <MathValue negativeValue value="100" />
        <MathDivision top="120" bottom={12} />
        <MathMultiplication negativeValue left="3" right="10" />
      </>,
    );

    expect(html).toContain("<span>-</span><span>100</span>");
    expect(html).toContain("<span>120</span><span> ÷</span> <span>12</span>");
    expect(html).toContain("<span>-</span><span>3</span><span> ×</span> ");
  });
});
