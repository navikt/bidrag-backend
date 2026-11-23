import copy
from pathlib import Path
import signal
import unittest
import xml.etree.ElementTree as ET

from weasyprint import CSS, HTML
from weasyprint.formatting_structure.boxes import TableBox, TextBox


ROOT = Path(__file__).resolve().parents[1]
FIXTURE = ROOT / "app/components/__tests__/__snapshots__/DokumentStabel.html"
STYLESHEETS = [
    ROOT / "app/style/style.css",
    ROOT / "app/style/tw_output.css",
]
TOLERANCE_PX = 0.1
RENDER_TIMEOUT_SECONDS = 15
REPETITIONS = 40
PAGE_MARGIN_PX = 40
PAGE_HEIGHT_PX = 600


class DokumentStabelLayoutTest(unittest.TestCase):
    def setUp(self):
        previous_handler = signal.signal(signal.SIGALRM, self.render_timeout)
        self.addCleanup(signal.signal, signal.SIGALRM, previous_handler)
        self.addCleanup(signal.alarm, 0)
        signal.alarm(RENDER_TIMEOUT_SECONDS)

    def render_timeout(self, _signum, _frame):
        self.fail(f"Rendering exceeded {RENDER_TIMEOUT_SECONDS} seconds")

    def render(self, html, media_type="print", height=PAGE_HEIGHT_PX):
        return html.render(
            stylesheets=[
                CSS(filename=path, media_type=media_type) for path in STYLESHEETS
            ] + [
                CSS(string=f"""
                    @page {{ size: 600px {height}px; margin: {PAGE_MARGIN_PX}px; }}
                    body {{ margin: 0; }}
                """, media_type=media_type)
            ],
        )

    def text_boxes(self, document):
        return [
            box
            for page in document.pages
            for box in page._page_box.descendants()
            if isinstance(box, TextBox)
        ]

    def original_flex(self, html):
        for element in reversed(list(html.etree_element.iter())):
            if "dokument-stabel" not in element.get("class", "").split():
                continue
            element.set(
                "class",
                " ".join(
                    name for name in element.get("class").split()
                    if name != "dokument-stabel"
                ),
            )
            children = list(element)
            element[:] = []
            for wrapper in children:
                element.extend(list(wrapper))
        return html

    def test_skal_bevare_skjermegenskaper_og_additive_marger_ved_utskrift(self):
        for media_type in ("screen", "print"):
            with self.subTest(media_type=media_type):
                source = FIXTURE.read_text()
                current = self.render(
                    HTML(string=source, media_type=media_type),
                    media_type=media_type,
                    height=2000,
                )
                original = self.render(
                    self.original_flex(HTML(string=source, media_type=media_type)),
                    media_type=media_type,
                    height=2000,
                )
                current_boxes = self.text_boxes(current)
                original_boxes = self.text_boxes(original)
                self.assertEqual(len(current.pages), 1)
                self.assertEqual(
                    [box.text for box in current_boxes],
                    [box.text for box in original_boxes],
                )
                for actual, expected in zip(current_boxes, original_boxes):
                    for attribute in ("position_x", "position_y", "width", "height"):
                        self.assertAlmostEqual(
                            getattr(actual, attribute),
                            getattr(expected, attribute),
                            delta=TOLERANCE_PX,
                            msg=f"{media_type}: {attribute}",
                        )

    def test_skal_dele_nestede_stabler_uten_aa_miste_tekst_eller_tabellkolonner(self):
        html = HTML(string=FIXTURE.read_text())
        body = next(html.etree_element.iter("body"))
        template = copy.deepcopy(body[0])
        body[:] = []
        expected = []
        for index in range(REPETITIONS):
            stack = copy.deepcopy(template)
            paragraph = ET.SubElement(stack, "div", {"class": "dokument-stabel-del"})
            ET.SubElement(paragraph, "p").text = f"Sluttmerke-{index}"
            expected.extend(" ".join(stack.itertext()).split())
            body.append(stack)

        document = self.render(html)
        self.assertGreater(len(document.pages), 1)
        self.assertEqual(
            " ".join(box.text for box in self.text_boxes(document)).split(),
            expected,
        )
        columns = []
        for page in document.pages:
            previous_bottom = PAGE_MARGIN_PX
            for box in page._page_box.descendants():
                if isinstance(box, TableBox):
                    columns.append(box.column_widths)
                if isinstance(box, TextBox):
                    self.assertGreaterEqual(
                        box.position_y + TOLERANCE_PX, previous_bottom
                    )
                    self.assertLessEqual(
                        box.position_y + box.height,
                        PAGE_HEIGHT_PX - PAGE_MARGIN_PX + TOLERANCE_PX,
                    )
                    previous_bottom = box.position_y
        self.assertEqual(len(columns), REPETITIONS)
        self.assertTrue(all(widths == columns[0] for widths in columns))
        self.assertTrue(document.write_pdf().startswith(b"%PDF-"))


if __name__ == "__main__":
    unittest.main()
