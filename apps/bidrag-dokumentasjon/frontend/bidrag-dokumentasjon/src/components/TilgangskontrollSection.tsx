import {TreeItem} from "@mui/x-tree-view";
import {useQueryClient} from "@tanstack/react-query";
import yaml from "js-yaml";
import {useAppContext} from "../App.tsx";

const TILGANG_BASE_PATH = "dokumentasjon/tilgangskontroll";
const ONE_DAY_MS = 1000 * 60 * 60 * 24;
export const TILGANG_FOLDER_ID = "folder_tilgangskontroll";

const FILES = [
    {id: "informasjonstilganger", label: "Informasjonstilganger", file: "informasjonstilganger.yaml"},
    {id: "behandlingstemaer", label: "Behandlingstemaer", file: "behandlingstemaer.yaml"},
] as const;

type TilgangFile = (typeof FILES)[number];

interface TilgangEntry {
    kode: string;
    adgruppe: string;
}

function sanitizeMermaidId(value: string): string {
    return value.replace(/[^a-zA-Z0-9]/g, "_");
}

function getLocalFileUrl(path: string): string {
    const base = import.meta.env.BASE_URL.endsWith("/")
        ? import.meta.env.BASE_URL
        : `${import.meta.env.BASE_URL}/`;
    return `${base}${path}`;
}

function parseYamlEntries(content: string): TilgangEntry[] {
    const parsed = yaml.load(content) as Record<string, {adgruppe?: string}> | null;
    return Object.entries(parsed ?? {}).map(([kode, val]) => ({
        kode,
        adgruppe: val?.adgruppe ?? "-",
    }));
}

function generateTilgangMermaid(entries: TilgangEntry[]): string {
    // Group codes by AD group
    const byGroup = new Map<string, string[]>();
    for (const {kode, adgruppe} of entries) {
        (byGroup.get(adgruppe) ?? byGroup.set(adgruppe, []).get(adgruppe)!).push(kode);
    }

    const lines = [
        "graph LR",
        "    classDef code fill:#4A90D9,color:#fff,stroke:#2c6fad",
        "    classDef group fill:#8E44AD,color:#fff,stroke:#6c3483",
        "",
    ];

    for (const [group, codes] of byGroup) {
        const groupId = sanitizeMermaidId(group);
        lines.push(`    ${groupId}["${group}"]:::group`);
        for (const code of codes) {
            lines.push(`    ${sanitizeMermaidId(code)}["${code}"]:::code`);
        }
        lines.push("");
        for (const code of codes) {
            lines.push(`    ${groupId} --> ${sanitizeMermaidId(code)}`);
        }
        lines.push("");
    }

    return lines.join("\n");
}

function FolderLabel({name}: {name: string}) {
    return (
        <span className="tree-label">
            <span className="tree-label__name" title={name}>{name}</span>
            <span className="tree-label__badge">mappe</span>
        </span>
    );
}

function ItemLabel({name}: {name: string}) {
    return (
        <span className="tree-label">
            <span className="tree-label__name" title={name}>{name}</span>
            <span className="tree-label__badge">tilgang</span>
        </span>
    );
}

/** Renders a single TreeItem — must be placed inside an existing SimpleTreeView. */
export function TilgangskontrollFolderItem() {
    const {setShowContent, expandedFolders, setSelectedItem} = useAppContext();
    const queryClient = useQueryClient();
    const isExpanded = expandedFolders.includes(TILGANG_FOLDER_ID);

    async function handleItemClick(file: TilgangFile) {
        setSelectedItem(`tilgang_${file.id}`);
        try {
            const text = await queryClient.fetchQuery<string>({
                queryKey: ["tilgangskontroll", file.id],
                staleTime: ONE_DAY_MS,
                gcTime: ONE_DAY_MS,
                queryFn: async () => {
                    const res = await fetch(getLocalFileUrl(`${TILGANG_BASE_PATH}/${file.file}`), {cache: "no-store"});
                    if (!res.ok) throw new Error(`Klarte ikke hente ${file.file}: ${res.status}`);
                    return res.text();
                },
            });
            const entries = parseYamlEntries(text);
            setShowContent({type: "mermaid", content: generateTilgangMermaid(entries)});
        } catch (err) {
            setShowContent({
                type: "markdown",
                content: `# Klarte ikke laste tilgangsfil\n\n\`${err instanceof Error ? err.message : String(err)}\``,
            });
        }
    }

    return (
        <TreeItem itemId={TILGANG_FOLDER_ID} label={<FolderLabel name="Tilgangskontroll"/>}>
            {isExpanded
                ? FILES.map((file) => (
                    <TreeItem
                        key={file.id}
                        itemId={`tilgang_${file.id}`}
                        label={<ItemLabel name={file.label}/>}
                        onClick={() => void handleItemClick(file)}
                    />
                ))
                : null}
        </TreeItem>
    );
}

