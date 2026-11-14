import { mkdirSync, readdirSync, readFileSync, writeFileSync } from "node:fs";
import { dirname, join, relative } from "node:path";
import { pathToFileURL } from "node:url";
import { load } from "js-yaml";
import type { Plugin } from "vite";

// Tekstene i app/tekster er YAML-filer. Denne filen gjør at malene kan importere dem,
// og lager typer slik at TypeScript gir feil hvis en mal bruker en tekst som ikke finnes.
const TEKSTMAPPE = "app/tekster";
const TYPEMAPPE = ".tekster-typer";

export function lesTekstfil(innhold: string): unknown {
  return load(innhold);
}

function finnTekstfiler(mappe: string): string[] {
  return readdirSync(mappe, { withFileTypes: true }).flatMap((f) => {
    const sti = join(mappe, f.name);
    if (f.isDirectory()) return finnTekstfiler(sti);
    return f.name.endsWith(".yaml") ? [sti] : [];
  });
}

function typeFor(verdi: unknown, innrykk: string): string {
  if (Array.isArray(verdi)) {
    return `Array<${verdi.length > 0 ? typeFor(verdi[0], innrykk) : "string"}>`;
  }
  if (verdi !== null && typeof verdi === "object") {
    const neste = innrykk + "  ";
    const felter = Object.entries(verdi).map(
      ([nøkkel, v]) =>
        `${neste}${JSON.stringify(nøkkel)}: ${typeFor(v, neste)};`,
    );
    return `{\n${felter.join("\n")}\n${innrykk}}`;
  }
  return typeof verdi === "number" || typeof verdi === "boolean"
    ? typeof verdi
    : "string";
}

/** Lager `.tekster-typer/app/tekster/<fil>.d.yaml.ts` for hver tekstfil. */
export function genererTeksttyper(rot = process.cwd()) {
  for (const fil of finnTekstfiler(join(rot, TEKSTMAPPE))) {
    const innhold = lesTekstfil(readFileSync(fil, "utf8"));
    const ut = join(rot, TYPEMAPPE, relative(rot, fil)).replace(
      /\.yaml$/,
      ".d.yaml.ts",
    );
    mkdirSync(dirname(ut), { recursive: true });
    writeFileSync(
      ut,
      `// Generert fra ${relative(rot, fil)} av scripts/tekster.ts. Ikke endre.\n` +
        `declare const tekster: ${typeFor(innhold, "")};\nexport default tekster;\n`,
    );
  }
}

/** Vite-plugin som gjør YAML-tekstfilene om til vanlige objekter i malene. */
export function teksterYaml(): Plugin {
  return {
    name: "tekster-yaml",
    buildStart() {
      genererTeksttyper();
    },
    transform(kode, id) {
      if (!id.endsWith(".yaml")) return null;
      return {
        code: `export default ${JSON.stringify(lesTekstfil(kode))};`,
        map: null,
      };
    },
    handleHotUpdate({ file }) {
      if (file.endsWith(".yaml")) genererTeksttyper();
    },
  };
}

if (import.meta.url === pathToFileURL(process.argv[1]).href)
  genererTeksttyper();
