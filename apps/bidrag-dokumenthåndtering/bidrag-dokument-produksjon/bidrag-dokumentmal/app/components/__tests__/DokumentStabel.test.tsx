import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it } from "vitest";
import DokumentStabel from "../DokumentStabel";

function TomDel() {
  return null;
}

describe("DokumentStabel", () => {
  it.each([
    [1, "4px"],
    [2, "8px"],
    [3, "12px"],
    [4, "16px"],
  ] as const)(
    "skal bruke gap %s i skjerm- og utskriftsvisning",
    (gap, avstand) => {
      const html = renderToStaticMarkup(
        <DokumentStabel gap={gap} className="mt-2">
          <p>Innhold</p>
        </DokumentStabel>,
      );
      expect(html).toContain('class="dokument-stabel flex flex-col mt-2"');
      expect(html).toContain(
        `style="--dokument-stabel-gap:${avstand};gap:${avstand}"`,
      );
    },
  );

  it("skal pakke ut lister og nestede fragmenter uten tomme mellomrom", () => {
    const html = renderToStaticMarkup(
      <DokumentStabel gap={2}>
        {null}
        {false}
        <>
          <p>Første</p>
          <>{[<p key="andre">Andre</p>, <p key="tredje">Tredje</p>]}</>
        </>
        {undefined}
        {0}
      </DokumentStabel>,
    );
    expect(html.match(/class="dokument-stabel-del"/g)).toHaveLength(4);
    expect(html).toContain(
      '<div class="dokument-stabel-del"><p>Første</p></div>' +
        '<div class="dokument-stabel-del"><p>Andre</p></div>' +
        '<div class="dokument-stabel-del"><p>Tredje</p></div>' +
        '<div class="dokument-stabel-del">0</div>',
    );
  });

  it("skal gi et fast HTML-eksempel til PDF-testene", async () => {
    const html = renderToStaticMarkup(
      <DokumentStabel gap={3}>
        <h3>Overskrift</h3>
        <TomDel />
        <p>Avsnitt med egne marger</p>
        <DokumentStabel gap={1}>
          <p>Nestet avsnitt</p>
          <div className="flex flex-row" id="vannrett">
            <span>Venstre</span>
            <span>Høyre</span>
          </div>
          <table className="table" id="tabell">
            <tbody>
              <tr>
                <td>Periode</td>
                <td>Beløp</td>
              </tr>
              <tr>
                <td>Januar</td>
                <td>1234</td>
              </tr>
            </tbody>
          </table>
        </DokumentStabel>
        <p>Siste avsnitt</p>
      </DokumentStabel>,
    );
    await expect(html).toMatchFileSnapshot("__snapshots__/DokumentStabel.html");
  });
});
