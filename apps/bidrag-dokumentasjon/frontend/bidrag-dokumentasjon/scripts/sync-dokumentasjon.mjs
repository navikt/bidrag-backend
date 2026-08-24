import {cp, mkdir, readdir, rm, writeFile} from "node:fs/promises";
import path from "node:path";
import {fileURLToPath} from "node:url";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const frontendRoot = path.resolve(__dirname, "..");
const repoRoot = path.resolve(frontendRoot, "..", "..");
const sourceDocsDir = path.join(repoRoot, "dokumentasjon");
const publicDir = path.join(frontendRoot, "public");
const targetDocsDir = path.join(publicDir, "dokumentasjon");
const supportedFileExtensions = new Set([".md", ".markdown", ".mermaid"]);

function createContentItem(relativePath, type) {
  const name = path.posix.basename(relativePath);

  return {
    name,
    path: relativePath,
    sha: "",
    size: 0,
    url: "",
    html_url: "",
    git_url: "",
    download_url: relativePath,
    type,
    _links: {
      self: "",
      git: "",
      html: "",
    },
  };
}

async function buildFolderIndex(absoluteDirPath, relativeDirPath, index) {
  const entries = await readdir(absoluteDirPath, {withFileTypes: true});
  const dirs = entries.filter((entry) => entry.isDirectory()).sort((a, b) => a.name.localeCompare(b.name, "nb"));
  const files = entries
  .filter((entry) => entry.isFile())
  .filter((entry) => supportedFileExtensions.has(path.extname(entry.name).toLowerCase()))
  .sort((a, b) => a.name.localeCompare(b.name, "nb"));

  index[relativeDirPath] = [
    ...dirs.map((entry) => createContentItem(path.posix.join(relativeDirPath, entry.name), "dir")),
    ...files.map((entry) => createContentItem(path.posix.join(relativeDirPath, entry.name), "file")),
  ];

  await Promise.all(
    dirs.map((entry) =>
      buildFolderIndex(path.join(absoluteDirPath, entry.name), path.posix.join(relativeDirPath, entry.name), index),
    ),
  );
}

async function syncDocumentation() {
  await mkdir(publicDir, {recursive: true});
  await rm(targetDocsDir, {recursive: true, force: true});
  await cp(sourceDocsDir, targetDocsDir, {recursive: true});

  const index = {};
  await buildFolderIndex(targetDocsDir, "dokumentasjon", index);

  await writeFile(path.join(targetDocsDir, "index.json"), `${JSON.stringify(index, null, 2)}\n`, "utf8");
}

await syncDocumentation();

