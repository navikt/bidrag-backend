import {TreeItem} from "@mui/x-tree-view";
import {useQuery} from "@tanstack/react-query";
import {load} from "js-yaml";
import cronstrue from "cronstrue";
import {useAppContext} from "../App.tsx";

const GITHUB_API_BASE = "https://api.github.com/repos/navikt/bidrag-automatisk-jobb/contents";
const GITHUB_RAW_BASE = "https://raw.githubusercontent.com/navikt/bidrag-automatisk-jobb/main";
const BATCH_DIR = "src/main/kotlin/no/nav/bidrag/automatiskjobb/batch";
const PROD_YAML_PATH = ".nais/prod.yaml";
const EXCLUDED_DIRS = new Set(["utils", "util"]);
const ONE_DAY_MS = 1000 * 60 * 60 * 24;
export const AUTO_JOBB_FOLDER_ID = "folder_automatiskejobber";

interface GithubEntry {
    name: string;
    path: string;
    type: string;
}

interface BatchJob {
    id: string;
    name: string;
    module: string;
    envVarName: string;
    cronExpression: string | null;
    disabled: boolean;
}

function isDisabledCron(value: string | null): boolean {
    if (!value || value.trim() === "" || value.trim() === "-") return true;
    const v = value.trim().toLowerCase();
    return v === "disabled" || v === "0 0 1 1 ? 2099";
}

/**
 * Parses @Value($$"${ENV_VAR_NAME:-}") paramName: → Map<paramName, ENV_VAR_NAME>
 * Works for both Kotlin 2.0 $$"${...}" and regular "\${...}" forms.
 */
function parseValueAnnotations(code: string): Map<string, string> {
    const map = new Map<string, string>();
    // Matches: @Value($$"${ENV_VAR:-}") paramName:  or  @Value("\${env.var}") paramName:
    const pattern = /@Value\s*\(\s*[$]*"\\?\$\{([^:}]+)[^}]*}"\s*\)\s*(\w+)\s*:/g;
    for (const match of code.matchAll(pattern)) {
        map.set(match[2].trim(), match[1].trim());
    }
    return map;
}

/** Parses Batch("Name", varName) calls */
function parseBatchCalls(code: string): Array<{name: string; variable: string}> {
    const batches: Array<{name: string; variable: string}> = [];
    for (const match of code.matchAll(/Batch\s*\(\s*"([^"]+)"\s*,\s*(\w+)/g)) {
        batches.push({name: match[1], variable: match[2]});
    }
    return batches;
}

async function fetchJson<T>(url: string): Promise<T> {
    const res = await fetch(url);
    if (!res.ok) throw new Error(`GitHub svarte med ${res.status} for ${url}`);
    return res.json() as Promise<T>;
}

async function fetchText(url: string): Promise<string> {
    const res = await fetch(url);
    if (!res.ok) throw new Error(`GitHub svarte med ${res.status} for ${url}`);
    return res.text();
}

async function fetchAllBatchJobs(): Promise<BatchJob[]> {
    // 1. Subdirectories under batch/ (exclude utils)
    const batchEntries = await fetchJson<GithubEntry[]>(`${GITHUB_API_BASE}/${BATCH_DIR}`);
    const subDirs = batchEntries.filter(e => e.type === "dir" && !EXCLUDED_DIRS.has(e.name));

    // 2. Fetch prod.yaml + dir listings in parallel
    const [prodYamlText, ...subDirListings] = await Promise.all([
        fetchText(`${GITHUB_RAW_BASE}/${PROD_YAML_PATH}`),
        ...subDirs.map(d => fetchJson<GithubEntry[]>(`${GITHUB_API_BASE}/${d.path}`)),
    ]);

    // 3. Parse prod.yaml — flat map under the "env:" key
    const prodYaml = load(prodYamlText) as Record<string, unknown>;
    const envSection = (prodYaml?.env ?? {}) as Record<string, string>;
    const envMap = new Map(Object.entries(envSection));

    // 4. Locate *Configuration.kt in each subdir
    const configToFetch: Array<{module: string; path: string}> = [];
    subDirListings.forEach((files, idx) => {
        const hit = (files as GithubEntry[]).find(
            f => f.type === "file" && f.name.endsWith("Configuration.kt"),
        );
        if (hit) configToFetch.push({module: subDirs[idx].name, path: hit.path});
    });

    // 5. Fetch all config files (best-effort)
    const configResults = await Promise.allSettled(
        configToFetch.map(cf => fetchText(`${GITHUB_RAW_BASE}/${cf.path}`)),
    );

    // 6. Parse each file
    const jobs: BatchJob[] = [];
    configResults.forEach((result, idx) => {
        if (result.status !== "fulfilled") return;
        const {module} = configToFetch[idx];
        const valueMap = parseValueAnnotations(result.value);
        for (const {name, variable} of parseBatchCalls(result.value)) {
            const envVarName = valueMap.get(variable) ?? variable.toUpperCase();
            const cronExpression = envMap.get(envVarName) ?? null;
            jobs.push({
                id: `autojobb_${module}_${name.replace(/[^a-zA-Z0-9]/g, "_")}`,
                name,
                module,
                envVarName,
                cronExpression,
                disabled: isDisabledCron(cronExpression),
            });
        }
    });

    return jobs.sort((a, b) => a.name.localeCompare(b.name, "nb"));
}

function humanReadableCron(cron: string): string {
    try {
        return cronstrue.toString(cron, {use24HourTimeFormat: true, throwExceptionOnParseError: true});
    } catch {
        return "";
    }
}

function generateBatchMermaid(job: BatchJob): string {
    const safeName = job.name.replace(/"/g, "'");
    const rawCron = job.cronExpression ?? "-";

    if (job.disabled) {
        return [
            "graph LR",
            "    classDef jobDisabled fill:#7F8C8D,color:#fff,stroke:#566573",
            "",
            `    J["<b>${safeName}</b><br/>⚠️ DEAKTIVERT<br/>Cron: ${rawCron}"]:::jobDisabled`,
        ].join("\n");
    }

    const readable = humanReadableCron(rawCron);

    return [
        "graph LR",
        "    classDef job fill:#27AE60,color:#fff,stroke:#1e8449",
        "",
        readable
            ? `    J["<b>${safeName}</b><br/>Cron: ${rawCron}<br/>🕐 ${readable}"]:::job`
            : `    J["<b>${safeName}</b><br/>Cron: ${rawCron}"]:::job`,
    ].join("\n");
}

function JobLabel({job}: {job: BatchJob}) {
    return (
        <span className="tree-label">
            <span className="tree-label__name" title={job.name}>
                {job.disabled ? "⚠️ " : ""}{job.name}
            </span>
            <span className="tree-label__badge">{job.disabled ? "deaktivert" : "jobb"}</span>
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

/** Renders a single TreeItem — must be placed inside an existing SimpleTreeView. */
export function AutomatiskeJobberFolderItem() {
    const {setShowContent, expandedFolders, setSelectedItem} = useAppContext();
    const isExpanded = expandedFolders.includes(AUTO_JOBB_FOLDER_ID);

    const {data: jobs = [], isLoading, error} = useQuery<BatchJob[]>({
        queryKey: ["automatiskeJobber"],
        enabled: isExpanded,
        staleTime: ONE_DAY_MS,
        gcTime: ONE_DAY_MS,
        retry: false,
        queryFn: fetchAllBatchJobs,
    });

    function handleJobClick(job: BatchJob) {
        setSelectedItem(job.id);
        setShowContent({type: "mermaid", content: generateBatchMermaid(job)});
    }

    // Group jobs by module, preserving insertion order
    const moduleGroups = jobs.reduce<Record<string, BatchJob[]>>((acc, job) => {
        (acc[job.module] ??= []).push(job);
        return acc;
    }, {});

    return (
        <TreeItem itemId={AUTO_JOBB_FOLDER_ID} label={<FolderLabel name="Automatiske Jobber"/>}>
            {isExpanded && isLoading && jobs.length === 0 ? (
                <TreeItem
                    itemId="autojobb_loading"
                    label={
                        <span className="tree-label">
                            <span className="tree-label__name">Laster jobber fra GitHub...</span>
                            <span className="tree-label__badge">venter</span>
                        </span>
                    }
                />
            ) : null}
            {error ? (
                <TreeItem
                    itemId="autojobb_error"
                    label={
                        <span className="tree-label">
                            <span className="tree-label__name">Klarte ikke laste jobber</span>
                            <span className="tree-label__badge">feil</span>
                        </span>
                    }
                />
            ) : null}
            {Object.entries(moduleGroups).map(([module, moduleJobs]) => (
                <TreeItem
                    key={module}
                    itemId={`folder_autojobb_${module}`}
                    label={<FolderLabel name={module}/>}
                >
                    {moduleJobs.map((job) => (
                        <TreeItem
                            key={job.id}
                            itemId={job.id}
                            label={<JobLabel job={job}/>}
                            onClick={() => handleJobClick(job)}
                        />
                    ))}
                </TreeItem>
            ))}
        </TreeItem>
    );
}






