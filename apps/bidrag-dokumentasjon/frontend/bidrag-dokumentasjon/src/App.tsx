import "@cubone/react-file-manager/dist/style.css";
import "highlight.js/styles/default.css";

import {ChevronLeftIcon, ChevronRightLastIcon} from "@navikt/aksel-icons";
import {TreeItem} from "@mui/x-tree-view";
import {SimpleTreeView} from "@mui/x-tree-view/SimpleTreeView";
import {Alert, BodyShort, Button, Heading, Loader, VStack} from "@navikt/ds-react";
import {useQuery} from "@tanstack/react-query";
import mermaid from "mermaid";
import {createContext, Dispatch, SetStateAction, SyntheticEvent, useContext, useEffect, useRef, useState} from "react";
import Markdown from "react-markdown";
import remarkRaw from "rehype-raw";
import remarkGfm from "remark-gfm";
import svgPanZoom from "svg-pan-zoom";
import {MarkdownComponents} from "./components/CustomMarkdown.tsx";
import {KafkaTopicsFolderItem} from "./components/KafkaTopicsSection.tsx";
import {AutomatiskeJobberFolderItem} from "./components/AutomatiskeJobberSection.tsx";
import {TilgangskontrollFolderItem} from "./components/TilgangskontrollSection.tsx";

mermaid.initialize({
  startOnLoad: true,
  flowchart: {useMaxWidth: true, htmlLabels: true, curve: "basis"},
  securityLevel: "loose",
  look: "handDrawn",
  theme: "base",
});

const GITHUB_OWNER = "navikt";
const GITHUB_REPO = "bidrag-dokumentasjon";
const GITHUB_ROOT_PATH = "dokumentasjon";
const LOCAL_INDEX_PATH = `${GITHUB_ROOT_PATH}/index.json`;
const APPLICATION_DOCS_PATH = `${GITHUB_ROOT_PATH}/applikasjoner`;
const SYSTEM_DOCS_PATH = `${GITHUB_ROOT_PATH}/systemdokumentasjon`;
let applicationDocumentIndexCache: Record<string, string> | null = null;
let applicationDocumentIndexPromise: Promise<Record<string, string>> | null = null;
let diagramDocumentIndexCache: Record<string, string> | null = null;
let diagramDocumentIndexPromise: Promise<Record<string, string>> | null = null;
let localContentIndexCache: Record<string, GithubContent[]> | null = null;
let localContentIndexPromise: Promise<Record<string, GithubContent[]>> | null = null;
type GithubRequestError = {
  status?: number;
  message?: string;
  response?: {
    data?: {
      message?: string;
    };
    headers?: Record<string, string>;
  };
};

type GithubErrorDetails = {
  title: string;
  message: string;
};

export interface GithubContent {
  name: string;
  path: string;
  sha: string;
  size: number;
  url: string;
  html_url: string;
  git_url: string;
  download_url: string;
  type: string;
  _links: Links;
}

export interface Links {
  self: string;
  git: string;
  html: string;
}

type FileType = "mermaid" | "markdown";
type Content = {
  type: FileType;
  content: string;
};

interface AppContextType {
  showContent?: Content;
  expandedFolders: string[];
  selectedItem: string | null;
  setExpandedFolders: Dispatch<SetStateAction<string[]>>;
  setSelectedItem: Dispatch<SetStateAction<string | null>>;
  setShowContent: (content: Content) => void;
  resetContent: () => void;
}

function formatRateLimitReset(resetAt?: string): string | null {
  if (!resetAt) return null;

  const resetTimestamp = Number(resetAt);
  if (!Number.isFinite(resetTimestamp)) return null;

  return new Date(resetTimestamp * 1000).toLocaleString("nb-NO", {
    hour: "2-digit",
    minute: "2-digit",
    day: "2-digit",
    month: "2-digit",
  });
}

function isGithubRateLimitError(error: unknown): boolean {
  const githubError = error as GithubRequestError | undefined;
  const message = githubError?.response?.data?.message ?? githubError?.message ?? "";
  return githubError?.status === 403 && /rate limit/i.test(message);
}

function getGithubErrorDetails(error: unknown): GithubErrorDetails {
  const githubError = error as GithubRequestError | undefined;
  const message = githubError?.response?.data?.message ?? githubError?.message ?? "Ukjent feil ved henting fra GitHub.";
  const resetAt = formatRateLimitReset(githubError?.response?.headers?.["x-ratelimit-reset"]);

  return {
    title: "GitHub svarte med rate limit",
    message: resetAt
        ? `${message} Kvoten nullstilles omtrent ${resetAt}.`
        : `${message} Prov igjen senere eller bruk lokal fil i mellomtiden.`,
  };
}

function sortGithubContent(content: GithubContent[]): GithubContent[] {
  return [...content].sort((left, right) => {
    if (left.type !== right.type) {
      return left.type === "dir" ? -1 : 1;
    }

    return left.name.localeCompare(right.name, "nb");
  });
}

function getFileItemId(path: string): string {
  return `file_${path}`;
}

function getPathFromFileItemId(itemId: string): string {
  return itemId.replace(/^file_/, "");
}

function formatSidebarName(name: string, isFile: boolean): string {
  let formatted = isFile ? name.replace(/\.[^/.]+$/, "") : name;
  formatted = formatted.replace(/_/g, " ");

  if (formatted.length === 0) {
    return name;
  }

  return `${formatted.charAt(0).toUpperCase()}${formatted.slice(1)}`;
}

function getLocalFileUrl(path: string): string {
  const basePath = import.meta.env.BASE_URL.endsWith("/") ? import.meta.env.BASE_URL : `${import.meta.env.BASE_URL}/`;
  return `${basePath}${path}`;
}

async function fetchTextFromUrl(url: string, errorMessage: string): Promise<string> {
  const response = await fetch(url, {cache: "no-store"});

  if (!response.ok) {
    throw new Error(errorMessage);
  }

  return response.text();
}

async function fetchGithubTextByPath(path: string): Promise<string> {
  return fetchTextFromUrl(getLocalFileUrl(path), `Klarte ikke å hente ${path} lokalt.`);
}

async function getLocalContentIndex(): Promise<Record<string, GithubContent[]>> {
  if (localContentIndexCache) {
    return localContentIndexCache;
  }

  if (!localContentIndexPromise) {
    localContentIndexPromise = fetchTextFromUrl(getLocalFileUrl(LOCAL_INDEX_PATH), "Klarte ikke å hente lokal dokumentasjonsindeks.")
    .then((raw) => {
      const parsed = JSON.parse(raw) as Record<string, GithubContent[]>;
      localContentIndexCache = parsed;
      return parsed;
    })
    .finally(() => {
      localContentIndexPromise = null;
    });
  }

  return localContentIndexPromise;
}

async function getLocalFolderContent(path: string): Promise<GithubContent[]> {
  const index = await getLocalContentIndex();
  return sortGithubContent(index[path] ?? []);
}

function createMissingApplicationMarkdown(applicationId: string): string {
  return `# Fant ikke applikasjonsbeskrivelse\n\nDet finnes foreløpig ingen registrert applikasjonsside for **${applicationId}**.\n\nLegg til en markdown-fil under \`${APPLICATION_DOCS_PATH}\` hvis applikasjonen skal kunne åpnes fra diagrammet.`;
}

function createApplicationLoadErrorMarkdown(applicationId: string): string {
  return `# Klarte ikke å åpne applikasjonsbeskrivelse\n\nVi fant en kobling fra diagrammet til **${applicationId}**, men markdown-filen kunne ikke lastes akkurat nå.\n\nPrøv igjen senere, eller åpne applikasjonen manuelt fra mappen \`applikasjoner\` i sidepanelet.`;
}

function createFileLoadErrorMarkdown(fileName: string): string {
  return `# Klarte ikke å åpne fil\n\nFilen **${fileName}** kunne ikke hentes akkurat nå.\n\nDette kan skyldes at filen ikke finnes, at GitHub svarte med en feil, eller at innholdet ikke er tilgjengelig fra nettleseren. Prøv igjen senere.`;
}

function createMermaidRenderErrorMarkdown(): string {
  return `# Klarte ikke å vise diagram\n\nInnholdet som ble åpnet kunne ikke tolkes som et gyldig Mermaid-diagram.\n\nKontroller at filen finnes, at den ikke returnerte en feilside fra GitHub, og at Mermaid-syntaksen er gyldig.`;
}

function createVirtualFolder(path: string): GithubContent {
  const name = path.split("/").pop() ?? path;

  return {
    name,
    path,
    sha: "",
    size: 0,
    url: "",
    html_url: "",
    git_url: "",
    download_url: path,
    type: "dir",
    _links: {
      self: "",
      git: "",
      html: "",
    },
  };
}

function ensureRootFolders(content: GithubContent[]): GithubContent[] {
  const requiredRootFolders = [
    `${GITHUB_ROOT_PATH}/systemdokumentasjon`,
    APPLICATION_DOCS_PATH,
  ];

  const knownPaths = new Set(content.map((item) => item.path));
  const missingFolders = requiredRootFolders
  .filter((folderPath) => !knownPaths.has(folderPath))
  .map((folderPath) => createVirtualFolder(folderPath));

  return [...content, ...missingFolders];
}

function normalizeApplicationKey(value: string): string {
  return value.toLowerCase()
  .replace(/å/g, "a").replace(/ø/g, "o").replace(/æ/g, "ae")
  .replace(/[^a-z0-9]/g, "");
}

function toKebabCase(value: string): string {
  return value.replace(/([a-z0-9])([A-Z])/g, "$1-$2").replace(/_/g, "-").toLowerCase();
}

function buildApplicationDocumentIndex(content: GithubContent[]): Record<string, string> {
  const index: Record<string, string> = {};

  content
  .filter((item) => item.type === "file")
  .filter((item) => item.name.endsWith(".md"))
  .forEach((item) => {
    const fileNameWithoutExt = item.name.replace(/\.md$/, "");
    index[normalizeApplicationKey(fileNameWithoutExt)] = item.path;
  });

  return index;
}

async function getApplicationDocumentIndex(): Promise<Record<string, string>> {
  if (applicationDocumentIndexCache) {
    return applicationDocumentIndexCache;
  }

  if (!applicationDocumentIndexPromise) {
    applicationDocumentIndexPromise = getLocalFolderContent(APPLICATION_DOCS_PATH)
    .then((content) => {
      const index = buildApplicationDocumentIndex(content);
      applicationDocumentIndexCache = index;
      return index;
    })
    .finally(() => {
      applicationDocumentIndexPromise = null;
    });
  }

  return applicationDocumentIndexPromise;
}

async function resolveApplicationDocumentPath(applicationId: string): Promise<string | undefined> {
  const guessedPath = `${APPLICATION_DOCS_PATH}/${toKebabCase(applicationId)}.md`;

  try {
    const index = await getApplicationDocumentIndex();
    const directKey = normalizeApplicationKey(applicationId);
    const kebabKey = normalizeApplicationKey(toKebabCase(applicationId));

    return index[directKey] ?? index[kebabKey] ?? guessedPath;
  } catch {
    // Fallback when GitHub API listing fails (rate-limit/network)
    return guessedPath;
  }
}

function buildDiagramDocumentIndex(content: GithubContent[]): Record<string, string> {
  const index: Record<string, string> = {};

  content
  .filter((item) => item.type === "file")
  .filter((item) => item.name.endsWith(".mermaid"))
  .forEach((item) => {
    const fileNameWithoutExt = item.name.replace(/\.mermaid$/, "");
    index[normalizeApplicationKey(fileNameWithoutExt)] = item.path;
  });

  return index;
}

async function getDiagramDocumentIndex(): Promise<Record<string, string>> {
  if (diagramDocumentIndexCache) {
    return diagramDocumentIndexCache;
  }

  if (!diagramDocumentIndexPromise) {
    diagramDocumentIndexPromise = getLocalFolderContent(SYSTEM_DOCS_PATH)
    .then((content) => {
      const index = buildDiagramDocumentIndex(content);
      diagramDocumentIndexCache = index;
      return index;
    })
    .finally(() => {
      diagramDocumentIndexPromise = null;
    });
  }

  return diagramDocumentIndexPromise;
}

async function resolveDiagramPath(nodeId: string): Promise<string | undefined> {
  try {
    const index = await getDiagramDocumentIndex();
    const directKey = normalizeApplicationKey(nodeId);
    const kebabKey = normalizeApplicationKey(toKebabCase(nodeId));

    return index[directKey] ?? index[kebabKey];
  } catch {
    return undefined;
  }
}

function TreeLabel({name, badge}: { name: string; badge: string }) {
  const isFile = badge === "fil";
  const displayName = formatSidebarName(name, isFile);

  return (
      <span className="tree-label">
        <span className="tree-label__name" title={displayName}>{displayName}</span>
        <span className="tree-label__badge">{badge}</span>
      </span>
  );
}

function GithubInfo({error}: { error: unknown }) {
  if (!error || !isGithubRateLimitError(error)) {
    return null;
  }

  const errorDetails = getGithubErrorDetails(error);

  return (
      <VStack gap="space-4" className="github-info-stack">
        <Alert variant="warning" size="small">
          <Heading spacing size="xsmall" level="3">{errorDetails.title}</Heading>
          <BodyShort size="small">{errorDetails.message}</BodyShort>
          <BodyShort size="small">
            Dette skjer fordi appen henter direkte fra GitHub API-et i nettleseren uten autentisering. En bedre losning er en backend eller proxy med token pa serversiden.
          </BodyShort>
        </Alert>
      </VStack>
  );
}

const AppContext = createContext<AppContextType | undefined>(undefined);

export const useAppContext = () => {
  const context = useContext(AppContext);
  if (context === undefined) {
    throw new Error("useJournalpost must be used within a JournalpostProvider");
  }
  return context;
};

export default function DokumentasjonPage() {
  const [showContent, setShowContent] = useState<Content | undefined>();
  const [expandedFolders, setExpandedFolders] = useState<string[]>([]);
  const [selectedItem, setSelectedItem] = useState<string | null>(null);
  const [isExpanded, setIsExpanded] = useState(true);

  function resetContent() {
    setShowContent(undefined);
    setSelectedItem(null);
  }

  return (
      <AppContext.Provider value={{showContent, setShowContent, setExpandedFolders, expandedFolders, selectedItem, setSelectedItem, resetContent}}>
        <div className="app-shell h-full w-full flex flex-row [&_svg]:max-w-full!">
          {isExpanded ? (
              <aside className="sidebar-container">
                <VStack id="drawer-navigation" gap="space-4" className="sidebar-panel">
                  <div className="sidebar-header">
                    <button
                        type="button"
                        className="sidebar-header__home-link"
                        onClick={resetContent}
                    >
                      Bidrag-dokumentasjon
                    </button>
                    <Button
                        size="small"
                        variant="tertiary-neutral"
                        onClick={() => setIsExpanded(false)}
                        icon={<ChevronLeftIcon aria-hidden/>}
                    >
                      Skjul
                    </Button>
                  </div>
                  <BodyShort size="small" className="sidebar-copy">
                    Oversikt over dokumentasjon av systemene for bidragsområdet i Nav.
                  </BodyShort>
                  <GithubTreeView/>
                </VStack>
              </aside>
          ) : (
              <Button
                  size="small"
                  variant="secondary-neutral"
                  className="sidebar-open-button"
                  onClick={() => setIsExpanded(true)}
                  icon={<ChevronRightLastIcon aria-hidden/>}
              >
                Åpne
              </Button>
          )}
          <MermaidChart/>
        </div>
      </AppContext.Provider>
  );
}

function GithubTreeView() {
  const {setShowContent, expandedFolders, setExpandedFolders, selectedItem, setSelectedItem} = useAppContext();

  async function updateShowedContent(_event: SyntheticEvent, itemId: string) {
    if (itemId.startsWith("folder_")) {
      setExpandedFolders((prev) => {
        if (prev.includes(itemId)) {
          return prev.filter((folder) => folder !== itemId);
        }
        return [...prev, itemId];
      });
      return;
    }

    // Kafka items handled via their own onClick – skip here
    if (itemId.startsWith("kafkatopic_") || itemId === "kafka_loading" || itemId === "kafka_error") {
      return;
    }

    // Automatiske jobber items handled via their own onClick – skip here
    if (itemId.startsWith("autojobb_") || itemId === "autojobb_loading" || itemId === "autojobb_error") {
      return;
    }

    if (!itemId.startsWith("file_")) {
      return;
    }

    const filePath = getPathFromFileItemId(itemId);

    setSelectedItem(itemId);
    try {
      const data = await fetchGithubTextByPath(filePath);
      setShowContent({content: data, type: filePath.endsWith(".mermaid") ? "mermaid" : "markdown"});
    } catch {
      const fileName = filePath.split("/").pop() ?? filePath;
      setShowContent({content: createFileLoadErrorMarkdown(fileName), type: "markdown"});
    }
  }

  const {data: content = [], error, isLoading} = useQuery<GithubContent[]>({
    queryKey: ["gitPage", GITHUB_ROOT_PATH],
    enabled: true,
    retry: false,
    queryFn: async (): Promise<GithubContent[]> => {
      return getLocalFolderContent(GITHUB_ROOT_PATH);
    },
  });

  const sortedContent = sortGithubContent(ensureRootFolders(content));
  const files = sortedContent.filter((file) => file.type === "file");
  const folders = sortedContent
  .filter((file) => file.type === "dir")
  .filter((folder) => folder.name !== ".github" && folder.name !== "frontend" && folder.name !== "tilgangskontroll");

  return (
      <VStack gap="space-4" className="github-section">
        <GithubInfo error={error}/>
        {isLoading && content.length === 0 ? (
            <div className="tree-loading-state">
              <Loader size="large" title="Laster dokumentasjon"/>
            </div>
        ) : error ? null : (
            <div className="tree-shell">
              <SimpleTreeView
                  expandedItems={expandedFolders}
                  selectedItems={selectedItem}
                  onItemClick={(event, itemId) => {
                    void updateShowedContent(event, itemId);
                  }}
              >
                {files.map((file) => (
                    <TreeItem key={file.path} itemId={getFileItemId(file.path)} label={<TreeLabel name={file.name} badge="fil"/>}/>
                ))}
                {folders.map((folder) => <GithubTree key={folder.path} folder={folder}/>)}
                <KafkaTopicsFolderItem/>
                <AutomatiskeJobberFolderItem/>
                <TilgangskontrollFolderItem/>
              </SimpleTreeView>
            </div>
        )}
      </VStack>
  );
}

function GithubTree({folder}: { folder: GithubContent }) {
  const {expandedFolders} = useAppContext();
  const folderItemId = `folder_${folder.path}`;
  const isExpanded = expandedFolders.includes(folderItemId);
  const {data: content = [], error, isLoading} = useQuery<GithubContent[]>({
    queryKey: ["gitPage", folder.path],
    enabled: isExpanded,
    retry: false,
    queryFn: async (): Promise<GithubContent[]> => {
      return getLocalFolderContent(folder.path);
    },
  });

  const sortedContent = sortGithubContent(content);
  const files = sortedContent.filter((file) => file.type === "file");
  const folders = sortedContent.filter((file) => file.type === "dir");
  const treeError = error ? getGithubErrorDetails(error) : null;


  return (
      <TreeItem itemId={folderItemId} label={<TreeLabel name={folder.name} badge="mappe"/>}>
      {isExpanded && isLoading && content.length === 0 ? (
            <TreeItem itemId={`loading_${folder.path}`} label={<TreeLabel name="Laster innhold" badge="venter"/>}/>
        ) : null}
      {treeError && isGithubRateLimitError(error) ? (
            <TreeItem itemId={`error_${folder.path}`} label={<TreeLabel name={treeError.title} badge="feil"/>}/>
        ) : null}
        {files.map((file) => (
            <TreeItem key={file.path} itemId={getFileItemId(file.path)} label={<TreeLabel name={file.name} badge="fil"/>}/>
        ))}
        {folders.map((childFolder) => (
            <GithubTree key={childFolder.path} folder={childFolder}/>
        ))}
      </TreeItem>
  );
}

function LandingPage() {
  return (
      <div className="landing-page">
        <div className="landing-page__inner">
          <div className="landing-page__icon" aria-hidden="true">📄</div>
          <Heading size="large" level="2" className="landing-page__title">
            Bidrag-dokumentasjon
          </Heading>
          <BodyShort size="large" className="landing-page__lead">
            Et verktøy for å utforske og lese teknisk dokumentasjon for NAVs bidragsløsning.
          </BodyShort>

          <div className="landing-page__cards">
            <div className="landing-page__card">
              <span className="landing-page__card-icon" aria-hidden="true">🗂️</span>
              <Heading size="xsmall" level="3">Bla i dokumentasjon</Heading>
              <BodyShort size="small" className="landing-page__card-text">
                Bruk sidepanelet til venstre for å vise dokumentasjon.
              </BodyShort>
            </div>
            <div className="landing-page__card">
              <span className="landing-page__card-icon" aria-hidden="true">🔷</span>
              <Heading size="xsmall" level="3">Mermaid-diagrammer</Heading>
              <BodyShort size="small" className="landing-page__card-text">
                Systemskisser og flytdiagrammer vises som interaktive Mermaid-diagrammer med pan og zoom. Klikk på elementer for å se detaljer.
              </BodyShort>
            </div>
          </div>

          <div className="landing-page__footer">
            <BodyShort size="small" className="landing-page__footer-text">
              Kildekoden og all dokumentasjon ligger åpent tilgjengelig på GitHub.
            </BodyShort>
            <a
                href={`https://github.com/${GITHUB_OWNER}/${GITHUB_REPO}`}
                target="_blank"
                rel="noopener noreferrer"
                className="landing-page__github-link"
            >
              <svg className="landing-page__github-icon" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
                <path d="M12 2C6.477 2 2 6.477 2 12c0 4.418 2.865 8.167 6.839 9.49.5.09.682-.217.682-.482 0-.237-.009-.868-.013-1.703-2.782.604-3.369-1.342-3.369-1.342-.454-1.154-1.11-1.462-1.11-1.462-.908-.62.069-.607.069-.607 1.003.07 1.531 1.03 1.531 1.03.892 1.529 2.341 1.087 2.91.832.092-.647.35-1.088.636-1.338-2.22-.253-4.555-1.11-4.555-4.943 0-1.091.39-1.984 1.029-2.683-.103-.253-.446-1.27.098-2.647 0 0 .84-.269 2.75 1.025A9.564 9.564 0 0 1 12 6.844a9.59 9.59 0 0 1 2.504.337c1.909-1.294 2.747-1.025 2.747-1.025.546 1.377.203 2.394.1 2.647.64.699 1.028 1.592 1.028 2.683 0 3.842-2.339 4.687-4.566 4.935.359.309.678.919.678 1.852 0 1.336-.012 2.415-.012 2.742 0 .267.18.577.688.48C19.138 20.163 22 16.418 22 12c0-5.523-4.477-10-10-10z"/>
              </svg>
              navikt / bidrag-dokumentasjon
            </a>
          </div>
        </div>
      </div>
  );
}

function MermaidChart() {
  const {showContent, setShowContent} = useAppContext();
  const {setExpandedFolders, setSelectedItem} = useAppContext();
  const [showDetailsMarkdown, setShowDetailsMarkdown] = useState<string | null>(null);
  const currentClickedNodeRef = useRef<string | null>(null);

  const isRendering = useRef(false);
  const divRef = useRef<HTMLPreElement>(null);
  const renderCount = useRef(0);
  const panZoomInstance = useRef<ReturnType<typeof svgPanZoom> | null>(null);

  useEffect(() => {
    window.callbackKotlin = (link: string) => {
      fetchTextFromUrl(link, `Klarte ikke å hente Kotlin-kilden fra ${link}.`)
      .then(async (data) => {
        setShowDetailsMarkdown(`${"```kotlin\n"}${data}\n${"```"}`);
      })
      .catch(() => {
        setShowDetailsMarkdown("# Klarte ikke å hente Kotlin-koden\n\nLenken i diagrammet returnerte ikke gyldig innhold.");
      });
    };
    window.visGrunnlag = (link: string) => {
      const normalizedPath = link.startsWith(`${GITHUB_ROOT_PATH}/`) ? link : `${GITHUB_ROOT_PATH}/${link.replace(/^\//, "")}`;

      fetchGithubTextByPath(normalizedPath)
      .then(async (data) => {
        setShowDetailsMarkdown(data);
      })
      .catch(() => {
        setShowDetailsMarkdown("# Klarte ikke å hente grunnlag\n\nMarkdown-filen som diagrammet peker til kunne ikke lastes.");
      });
    };
    window.visMarkdown = (innhold: string) => {
      console.log(innhold.replace(/[\t\n]/g, " "));
      setShowDetailsMarkdown(innhold.replace(/ +/g, " "));
    };
    window.visApplikasjon = (applicationId: string) => {
      if (currentClickedNodeRef.current === applicationId && document.getElementById("details-markdown-container")) {
        setShowDetailsMarkdown(null);
        currentClickedNodeRef.current = null;
        return;
      }
      currentClickedNodeRef.current = applicationId;

      setShowDetailsMarkdown(`# Laster applikasjonsbeskrivelse\n\nHenter **${applicationId}** ...`);

      void (async () => {
        try {
          const applicationPath = await resolveApplicationDocumentPath(applicationId);

          if (!applicationPath) {
            setShowDetailsMarkdown(createMissingApplicationMarkdown(applicationId));
            return;
          }

          const data = await fetchGithubTextByPath(applicationPath);
          setShowDetailsMarkdown(data);
        } catch {
          setShowDetailsMarkdown(createApplicationLoadErrorMarkdown(applicationId));
        }
      })();
    };
    window.visDiagram = (nodeId: string) => {
      void (async () => {
        try {
          const diagramPath = await resolveDiagramPath(nodeId);
          if (!diagramPath) {
            setShowContent({
              content: `# Diagram ikke funnet\n\nFant ingen diagramfil for **${nodeId}** i systemdokumentasjon.\n\nLegg til en \`.mermaid\`-fil med navn som matcher node-IDen.`,
              type: "markdown",
            });
            return;
          }
          const data = await fetchGithubTextByPath(diagramPath);
          // Clear active/pressed state and expand + select in the sidebar
          (document.activeElement as HTMLElement)?.blur();
          const folderPath = diagramPath.split("/").slice(0, -1).join("/");
          const folderId = `folder_${folderPath}`;
          const fileItemId = `file_${diagramPath}`;
          setSelectedItem(null); // clear first to force visual reset
          setExpandedFolders((prev) => prev.includes(folderId) ? prev : [...prev, folderId]);
          setTimeout(() => {
            setSelectedItem(fileItemId);
            document.querySelector(`[data-id="${CSS.escape(fileItemId)}"]`)
            ?.scrollIntoView({behavior: "smooth", block: "nearest"});
          }, 50);
          setShowContent({content: data, type: "mermaid"});
        } catch {
          setShowContent({
            content: `# Klarte ikke å hente diagram\n\nDiagramfilen for **${nodeId}** kunne ikke lastes.`,
            type: "markdown",
          });
        }
      })();
    };

    return () => {
      delete window.callbackKotlin;
      delete window.visGrunnlag;
      delete window.visMarkdown;
      delete window.visApplikasjon;
      delete window.visDiagram;
    };
  }, [setShowContent]);

  useEffect(() => {
    if (!showContent) return;
    if (showContent.type !== "mermaid") {
      if (panZoomInstance.current) {
        try { panZoomInstance.current.destroy(); } catch { /* ignore */ }
        panZoomInstance.current = null;
      }
      document.getElementById("mermaidSvg")?.remove();
      return;
    }

    // Destroy previous svgPanZoom instance and remove old SVG
    if (panZoomInstance.current) {
      try { panZoomInstance.current.destroy(); } catch { /* ignore */ }
      panZoomInstance.current = null;
    }
    if (divRef.current) {
      divRef.current.innerHTML = "";
    }

    // Use a unique id per render to avoid Mermaid "id already exists" errors
    const svgId = `mermaidSvg_${++renderCount.current}`;

    isRendering.current = true;
    mermaid
    .render(svgId, showContent.content, divRef.current)
    .then((res) => {
      if (!divRef.current) return;

      divRef.current.innerHTML = res.svg;
      if (res.bindFunctions) {
        res.bindFunctions(divRef.current.firstElementChild as Element);
      }
      try {
        panZoomInstance.current = svgPanZoom(`#${svgId}`);
      } catch (e) {
        console.warn("[mermaid] svgPanZoom init failed (non-fatal):", e);
      }
    })
    .catch((error) => {
      console.error("[mermaid] render failed:", error);
      setShowContent({content: createMermaidRenderErrorMarkdown(), type: "markdown"});
    });
  }, [setShowContent, showContent]);

  return (
      <div className="relative flex flex-1 h-full w-full overflow-hidden bg-white">
        {showContent?.type === "mermaid" ? (
            <pre ref={divRef} className="mermaid h-full grow w-full max-w-full [&_svg]:max-w-full!"/>
        ) : showContent ? (
            <div className="pt-8 m-auto overflow-y-auto h-full w-275 pl-8 pr-8 pb-8">
              <Markdown components={MarkdownComponents} remarkPlugins={[remarkGfm]} rehypePlugins={[remarkRaw]}>
                {showContent.content}
              </Markdown>
            </div>
        ) : (
            <LandingPage/>
        )}

        {showDetailsMarkdown !== null && (
            <div className="absolute top-0 right-0 h-full w-150 max-w-[90%] bg-white border-l shadow-2xl p-6 overflow-y-auto z-50 flex flex-col gap-4">
              <div className="flex justify-end">
                <Button
                    size="small"
                    variant="tertiary-neutral"
                    onClick={() => {
                      setShowDetailsMarkdown(null);
                      currentClickedNodeRef.current = null;
                    }}
                >
                  Lukk
                </Button>
              </div>
              <div id="details-markdown-container" className="flex-1">
                <Markdown components={MarkdownComponents} remarkPlugins={[remarkGfm]} rehypePlugins={[remarkRaw]}>
                  {showDetailsMarkdown}
                </Markdown>
              </div>
            </div>
        )}
      </div>
  );
}