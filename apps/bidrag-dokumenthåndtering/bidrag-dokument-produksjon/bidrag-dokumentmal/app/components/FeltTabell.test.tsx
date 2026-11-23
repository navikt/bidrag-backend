import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it } from "vitest";
import {
  FeltTabell,
  Feltrad,
  Feltrader,
  JustertFeltrad,
  JusterteFeltrader,
} from "~/components/FeltTabell";

describe("FeltTabell", () => {
  it("bruker felles ledetekstbredde og viser nullbeløp og JSX i verdicellen", () => {
    const html = renderToStaticMarkup(
      <FeltTabell labelColWidth="80px">
        <Feltrad label="Beløp" labelBold>
          <strong>{0}</strong>
        </Feltrad>
        <JustertFeltrad>Oppsummering</JustertFeltrad>
      </FeltTabell>,
    );

    expect(html).toContain(
      '<td style="vertical-align:text-top;width:80px;padding-right:5px;font-weight:bold">Beløp: </td><td><strong>0</strong></td>',
    );
    expect(html).toContain(
      '<td colSpan="3" class="text-left">Oppsummering</td>',
    );
  });

  it("beholder forskjellen mellom resultatkolonnene i de to radvariantene", () => {
    const rows = [
      { value: "Sum" },
      null,
      { value: "Regnestykke", result: "5" },
    ];
    const html = renderToStaticMarkup(
      <FeltTabell>
        <Feltrader rows={rows} />
        <JusterteFeltrader rows={rows} />
      </FeltTabell>,
    );

    expect(html).toBe(
      '<table style="border-spacing:0"><tbody>' +
        "<tr><td>Sum</td></tr><tr><td>Regnestykke</td><td>= 5</td></tr>" +
        '<tr><td colSpan="3" class="text-left">Sum</td><td> = </td></tr>' +
        '<tr><td colSpan="2" class="text-left">Regnestykke</td><td> = 5</td></tr>' +
        "</tbody></table>",
    );
  });
});
