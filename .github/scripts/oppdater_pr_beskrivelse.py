#!/usr/bin/env python3
"""Fletter en generert endringsbeskrivelse inn i en pull request-beskrivelse.

Teksten legges i en markert blokk slik at kjøringen er idempotent og aldri
overskriver det utvikleren selv har skrevet i beskrivelsen.
"""

from __future__ import annotations

import argparse
import sys

MARKOR_START = "<!-- pr-beskrivelse:start -->"
MARKOR_SLUTT = "<!-- pr-beskrivelse:slutt -->"
OVERSKRIFT = "## Automatisk endringsoversikt"
FOTNOTE = (
    "_Generert automatisk. Rediger gjerne teksten utenfor blokken - "
    "innholdet i blokken blir overskrevet ved neste push._"
)


def bygg_blokk(sammendrag: str, tabell: str) -> str:
    """Setter sammen den markerte blokken av sammendrag og modultabell."""
    deler = [
        MARKOR_START,
        OVERSKRIFT,
        sammendrag.strip(),
        "",
        "### Berørte moduler",
        tabell.strip(),
        "",
        FOTNOTE,
        MARKOR_SLUTT,
    ]
    return "\n".join(deler)


def flett_inn(beskrivelse: str, blokk: str) -> str:
    """Erstatter en eksisterende markert blokk, eller legger blokken til slutt.

    Beholder posisjonen til en eksisterende blokk slik at beskrivelsen ikke
    stokkes om ved hver push. Leter etter den siste startmarkøren som har en
    tilhørende sluttmarkør, slik at en løs markør i utviklerens egen tekst
    ikke fører til at tekst mellom markørene blir spist opp.
    """
    beskrivelse = beskrivelse or ""
    start = beskrivelse.rfind(MARKOR_START)
    slutt = beskrivelse.find(MARKOR_SLUTT, start) if start != -1 else -1

    if start != -1 and slutt != -1:
        foran = beskrivelse[:start]
        bak = beskrivelse[slutt + len(MARKOR_SLUTT):]
        return f"{foran}{blokk}{bak}"

    if not beskrivelse.strip():
        return blokk

    return f"{beskrivelse.rstrip()}\n\n{blokk}"


def _les(sti: str) -> str:
    if sti == "-":
        return sys.stdin.read()
    with open(sti, encoding="utf-8") as fil:
        return fil.read()


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--beskrivelse-fil", required=True,
                        help="Fil med nåværende PR-beskrivelse")
    parser.add_argument("--sammendrag-fil", required=True,
                        help="Fil med generert prosa-sammendrag")
    parser.add_argument("--tabell-fil", required=True,
                        help="Fil med markdown-tabell over berørte moduler")
    args = parser.parse_args(argv)

    blokk = bygg_blokk(_les(args.sammendrag_fil), _les(args.tabell_fil))
    sys.stdout.write(flett_inn(_les(args.beskrivelse_fil), blokk))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
