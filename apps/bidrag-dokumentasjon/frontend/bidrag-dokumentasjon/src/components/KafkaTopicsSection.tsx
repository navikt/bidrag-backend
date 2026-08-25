import {TreeItem} from "@mui/x-tree-view";
import {useQuery, useQueryClient} from "@tanstack/react-query";
import yaml from "js-yaml";
import {useAppContext} from "../App.tsx";

const KAFKA_TOPICS_API_URL = "https://api.github.com/repos/navikt/bidrag-backend/contents/.nais/bidrag-kafka/topics";
const KAFKA_RAW_BASE_URL = "https://raw.githubusercontent.com/navikt/bidrag-backend/main/.nais/bidrag-kafka/topics";
const ONE_DAY_MS = 1000 * 60 * 60 * 24;
export const KAFKA_FOLDER_ID = "folder_kafkatopics";

interface GithubFileEntry {
    name: string;
    download_url: string | null;
    type: string;
}

interface NaisAclEntry {
    application: string;
    team: string;
    access: string;
}

interface NaisTopic {
    metadata?: {name?: string};
    spec?: {acl?: NaisAclEntry[]};
}

function sanitizeMermaidId(value: string): string {
    return value.replace(/[^a-zA-Z0-9]/g, "_");
}

function formatTopicDisplayName(fileName: string): string {
    return fileName.replace(/-prod\.yaml$/, "");
}

function generateKafkaMermaid(topicName: string, acl: NaisAclEntry[]): string {
    const producers = acl.filter((a) => a.access === "readwrite" || a.access === "write");
    const consumers = acl.filter((a) => a.access === "read");

    const lines = [
        "graph LR",
        "    classDef producer fill:#27AE60,color:#fff,stroke:#1e8449",
        "    classDef consumer fill:#4A90D9,color:#fff,stroke:#2c6fad",
        "    classDef topic fill:#F39C12,color:#fff,stroke:#d68910",
        "",
        `    Topic["☕ ${topicName}"]:::topic`,
        "",
    ];

    const seenIds = new Set<string>();

    for (const producer of producers) {
        const id = sanitizeMermaidId(producer.application);
        if (!seenIds.has(`p_${id}`)) {
            lines.push(`    ${id}["${producer.application}"]:::producer`);
            seenIds.add(`p_${id}`);
        }
        lines.push(`    ${id} -->|producer| Topic`);
    }

    for (const consumer of consumers) {
        const id = sanitizeMermaidId(consumer.application);
        if (!seenIds.has(`c_${id}`)) {
            lines.push(`    ${id}["${consumer.application}"]:::consumer`);
            seenIds.add(`c_${id}`);
        }
        lines.push(`    Topic -->|consumer| ${id}`);
    }

    lines.push("");
    const allIds = new Set<string>();
    for (const entry of acl) {
        const id = sanitizeMermaidId(entry.application);
        if (!allIds.has(id)) {
            lines.push(`    click ${id} visApplikasjon "Åpne applikasjonsbeskrivelse"`);
            allIds.add(id);
        }
    }

    return lines.join("\n");
}

function TopicLabel({name}: {name: string}) {
    return (
        <span className="tree-label">
      <span className="tree-label__name" title={name}>{name}</span>
      <span className="tree-label__badge">topic</span>
    </span>
    );
}

function FolderLabel({name}: {name: string}) {
    return (
        <span className="tree-label">
      <span className="tree-label__name" title={name}>{name}</span>
      <span className="tree-label__badge">mappe</span>
    </span>
    );
}

/** Renders a single TreeItem (no SimpleTreeView wrapper) – must be placed inside an existing tree. */
export function KafkaTopicsFolderItem() {
    const {setShowContent, expandedFolders, setSelectedItem} = useAppContext();
    const queryClient = useQueryClient();
    const isExpanded = expandedFolders.includes(KAFKA_FOLDER_ID);

    const {data: topicFiles = [], isLoading, error} = useQuery<GithubFileEntry[]>({
        queryKey: ["kafkaTopicsList"],
        enabled: isExpanded,
        staleTime: ONE_DAY_MS,
        gcTime: ONE_DAY_MS,
        retry: false,
        queryFn: async () => {
            const response = await fetch(KAFKA_TOPICS_API_URL);
            if (!response.ok) throw new Error(`GitHub svarte med ${response.status}`);
            const data = await response.json() as GithubFileEntry[];
            return data
                .filter((f) => f.type === "file" && f.name.endsWith("-prod.yaml"))
                .sort((a, b) => a.name.localeCompare(b.name, "nb"));
        },
    });

    async function handleTopicClick(file: GithubFileEntry) {
        setSelectedItem(`kafkatopic_${file.name}`);
        try {
            const rawUrl = `${KAFKA_RAW_BASE_URL}/${encodeURIComponent(file.name)}`;
            const content = await queryClient.fetchQuery<string>({
                queryKey: ["kafkaTopicContent", file.name],
                staleTime: ONE_DAY_MS,
                gcTime: ONE_DAY_MS,
                queryFn: async () => {
                    const response = await fetch(rawUrl);
                    if (!response.ok) throw new Error(`Klarte ikke hente ${file.name}: ${response.status}`);
                    return response.text();
                },
            });

            const parsed = yaml.load(content) as NaisTopic;
            const topicName = parsed?.metadata?.name ?? formatTopicDisplayName(file.name);
            const acl = parsed?.spec?.acl ?? [];

            if (acl.length === 0) {
                setShowContent({type: "markdown", content: `# ${topicName}\n\nIngen ACL-regler funnet i \`${file.name}\`.`});
                return;
            }
            setShowContent({type: "mermaid", content: generateKafkaMermaid(topicName, acl)});
        } catch (err) {
            setShowContent({
                type: "markdown",
                content: `# Klarte ikke laste topic\n\nFilen **${file.name}** kunne ikke hentes eller tolkes.\n\n\`${err instanceof Error ? err.message : String(err)}\``,
            });
        }
    }

    return (
        <TreeItem itemId={KAFKA_FOLDER_ID} label={<FolderLabel name="Kafka Topics"/>}>
            {isExpanded && isLoading && topicFiles.length === 0 ? (
                <TreeItem
                    itemId="kafka_loading"
                    label={
                        <span className="tree-label">
                  <span className="tree-label__name">Laster topics fra GitHub...</span>
                  <span className="tree-label__badge">venter</span>
                </span>
                    }
                />
            ) : null}

            {error ? (
                <TreeItem
                    itemId="kafka_error"
                    label={
                        <span className="tree-label">
                  <span className="tree-label__name">Klarte ikke laste topics</span>
                  <span className="tree-label__badge">feil</span>
                </span>
                    }
                />
            ) : null}

            {topicFiles.map((file) => (
                <TreeItem
                    key={file.name}
                    itemId={`kafkatopic_${file.name}`}
                    label={<TopicLabel name={formatTopicDisplayName(file.name)}/>}
                    onClick={() => void handleTopicClick(file)}
                />
            ))}
        </TreeItem>
    );
}
