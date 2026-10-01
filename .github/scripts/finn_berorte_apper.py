#!/usr/bin/env python3
"""Finn hvilke apper og biblioteker som må bygges ut fra filene som er endret."""

import json
import os
from pathlib import Path
import re
import shlex
import subprocess
import urllib.request

import yaml


def path_matches_filters(path, patterns):
    selected = False
    for pattern in patterns:
        negative = pattern.startswith("!")
        pattern = pattern[1:] if negative else pattern
        expression = ""
        index = 0
        while index < len(pattern):
            if pattern[index:index + 3] == "**/":
                expression += "(?:.*/)?"
                index += 3
            elif pattern[index:index + 2] == "**":
                expression += ".*"
                index += 2
            elif pattern[index] == "*":
                expression += "[^/]*"
                index += 1
            elif pattern[index] == "?":
                expression += "[^/]"
                index += 1
            elif pattern[index] in "[]+":
                raise ValueError(f"Glob-mønsteret er ikke støttet: {pattern}")
            else:
                expression += re.escape(pattern[index])
                index += 1
        if re.fullmatch(expression, path, re.DOTALL):
            selected = not negative
    return selected


SUPPORTED_EVENTS = {"push", "pull_request", "merge_group"}


def select_affected_apps(app_filters, event_name, branch, changed_paths):
    if event_name not in SUPPORTED_EVENTS:
        raise ValueError(f"Appvalg støtter bare push, pull_request og merge_group, ikke {event_name}")
    if event_name == "push" and branch != "main":
        return []
    return [app for app, patterns in app_filters.items()
            if any(path_matches_filters(path, patterns) for path in changed_paths)]


def find_changed_files(root, event_name, event):
    def git_output(*args):
        return subprocess.check_output(["git", "-C", str(root), *args])

    if event_name == "pull_request":
        pr = event["pull_request"]
        head = validate_sha(pr["head"]["sha"])
        base = git_output("merge-base", validate_sha(pr["base"]["sha"]), head).decode().strip()
    elif event_name == "merge_group":
        group = event["merge_group"]
        head, base = validate_sha(group["head_sha"]), validate_sha(group["base_sha"])
    elif event_name == "push":
        head, base = validate_sha(event["after"]), validate_sha(event["before"])
        if head == "0" * 40:
            return []
        if base == "0" * 40:
            base = subprocess.check_output(
                ["git", "-C", str(root), "hash-object", "-w", "-t", "tree", "--stdin"], input=b"",
            ).decode().strip()
    else:
        raise ValueError(f"Kan ikke finne endrede filer for hendelsen {event_name}")
    # --no-renames gir både gammel og ny sti ved flytting mellom appmoduler.
    return [path for path in
            git_output("diff", "--name-only", "--no-renames", "-z", base, head, "--").decode().split("\0")
            if path]


def load_app_filters(root):
    app_filters = {}
    for path in sorted((root / ".github/workflows").glob("*.yaml")):
        with open(path) as stream:
            workflow = yaml.safe_load(stream)
        env = workflow.get("env", {})
        if "APP_PATHS" not in env:
            continue
        if not re.fullmatch(r"bidrag-[a-z0-9-]+", path.stem):
            raise ValueError(f"Ugyldig appnavn: {path.stem}")
        configured = env["APP_PATHS"]
        if not isinstance(configured, str) or not configured.strip():
            raise ValueError(f"APP_PATHS må inneholde minst ett filmønster, med ett mønster per linje: {path.name}")
        app_filters[path.stem] = [line.strip() for line in configured.splitlines() if line.strip()]
    if not app_filters:
        raise ValueError("Fant ingen app-workflows med APP_PATHS")
    return app_filters


def build_config(root, app):
    """Verdiene appen sender videre til bygg_og_deploy.yaml."""
    with open(root / f".github/workflows/{app}.yaml") as stream:
        workflow = yaml.safe_load(stream)
    build_jobs = [job for job in workflow["jobs"].values()
                  if job.get("uses") == "./.github/workflows/bygg_og_deploy.yaml"]
    if len(build_jobs) != 1:
        raise ValueError(f"{app} må ha nøyaktig én jobb som bruker bygg_og_deploy.yaml")
    return build_jobs[0]["with"]


def split_on_merged_module(root, apps):
    """Del appene i dem som har en modul i treet, og dem som ennå ikke har det.

    En ny app får workflowen sin merget før selve appmodulen, slik at resten av CI-oppsettet
    er på plass når modulen kommer. Uten modulen har appjobben ingenting å bygge, og
    `mvn -pl` feiler. Den feilen felte samlejobben og dermed bygget for alle de andre appene.
    Vi hopper heller over appen til modulen er merget.
    """
    merged, unmerged = [], []
    for app in apps:
        options = shlex.split(build_config(root, app)["maven_options"])
        if "-pl" not in options:
            raise ValueError(f"{app} mangler -pl i maven_options og kan ikke knyttes til en modul")
        module = options[options.index("-pl") + 1]
        (merged if (root / module / "pom.xml").is_file() else unmerged).append(app)
    return merged, unmerged


def required_library_groups(root, apps):
    groups = set()
    for app in apps:
        config = build_config(root, app)
        configured = config.get("bibliotekgrupper", "felles")
        selected = set(configured.split(",")) if configured else set()
        if not selected <= {"felles", "beregn", "oppgave"}:
            raise ValueError(f"Ukjente bibliotekgrupper for {app}: {configured}")
        groups.update(selected)
    if "beregn" in groups:
        groups.add("felles")
    return ",".join(group for group in ("felles", "beregn", "oppgave") if group in groups)


def target_branch(event_name, event):
    """Greina endringene havner på: PR-basen, køens base, eller greina som ble pushet."""
    if event_name == "pull_request":
        reference = event["pull_request"]["base"]["ref"]
    elif event_name == "merge_group":
        reference = event["merge_group"]["base_ref"]
    else:
        reference = event["ref"]
    return reference.removeprefix("refs/heads/")


QUEUE_REF = re.compile(r"refs/heads/gh-readonly-queue/[^/]+/pr-([0-9]+)-[a-f0-9]{40}")


def github_get(path):
    request = urllib.request.Request(
        f"{os.environ.get('GITHUB_API_URL', 'https://api.github.com')}{path}",
        headers={"Authorization": f"Bearer {os.environ['GITHUB_TOKEN']}",
                 "Accept": "application/vnd.github+json", "X-GitHub-Api-Version": "2022-11-28"})
    with urllib.request.urlopen(request, timeout=30) as response:
        return json.load(response)


def tested_pr_head(root, event, workflow_file):
    """PR-hodet som allerede er bygget grønt med nøyaktig samme tre som merge-køen skal teste.

    Når PR-branchen var à jour med main, er køens commit bare PR-hodet lagt oppå main, med
    identisk filtre. PR-kjøringen har da allerede bygget og testet akkurat dette treet, og det
    er ingenting nytt å teste. Returnerer None ved minste tvil, slik at køen bygger som vanlig.
    """
    group = event["merge_group"]
    match = QUEUE_REF.fullmatch(group.get("head_ref", ""))
    if not match:
        return None
    head, base = validate_sha(group["head_sha"]), validate_sha(group["base_sha"])
    repository = os.environ["GITHUB_REPOSITORY"]
    pr_head = validate_sha(github_get(f"/repos/{repository}/pulls/{match.group(1)}")["head"]["sha"])

    def git(*args):
        return subprocess.run(["git", "-C", str(root), *args], capture_output=True, text=True)

    if git("cat-file", "-e", f"{pr_head}^{{commit}}").returncode != 0 and \
            git("fetch", "--quiet", "--no-tags", "origin", f"refs/pull/{match.group(1)}/head").returncode != 0:
        return None
    # base må være med i PR-hodet: da ga også PR-kjøringens merge med (en eldre) main det samme treet.
    if git("merge-base", "--is-ancestor", base, pr_head).returncode != 0:
        return None
    trees = [git("rev-parse", f"{sha}^{{tree}}").stdout.strip() for sha in (head, pr_head)]
    if not trees[0] or trees[0] != trees[1]:
        return None
    runs = github_get(f"/repos/{repository}/actions/workflows/{workflow_file}/runs"
                      f"?event=pull_request&status=success&head_sha={pr_head}&per_page=1")
    return pr_head if runs.get("total_count", 0) > 0 else None


def validate_sha(value):
    if not re.fullmatch(r"[a-f0-9]{40}", value):
        raise ValueError("Ugyldig commit-SHA i hendelsen")
    return value


def main():
    root = Path.cwd()
    app_filters = load_app_filters(root)
    with open(os.environ["GITHUB_EVENT_PATH"]) as stream:
        event = json.load(stream)
    event_name = os.environ["GITHUB_EVENT_NAME"]
    if event_name not in SUPPORTED_EVENTS:
        raise ValueError(f"Appvalg støtter bare push, pull_request og merge_group, ikke {event_name}")
    branch = target_branch(event_name, event)
    if event_name == "merge_group" and os.environ.get("GITHUB_TOKEN"):
        try:
            pr_head = tested_pr_head(root, event, "bygg-apper.yaml")
        except (OSError, KeyError, ValueError) as error:
            print(f"::warning::Kunne ikke sjekke om PR-bygget kan gjenbrukes, bygger som vanlig: {error}")
            pr_head = None
        if pr_head:
            with open(os.environ["GITHUB_OUTPUT"], "a") as stream:
                stream.write("apps=[]\nbibliotekgrupper=\nfelles_endret=false\n")
            message = (f"Merge-køen har samme filtre som PR-hodet {pr_head}, som allerede er bygget og testet grønt. "
                       "Hopper over bygg og test.")
            print(message)
            with open(os.environ["GITHUB_STEP_SUMMARY"], "a") as stream:
                stream.write(message + "\n")
            return
    changed_paths = find_changed_files(root, event_name, event)
    apps, unmerged_apps = split_on_merged_module(
        root, select_affected_apps(app_filters, event_name, branch, changed_paths))
    groups = required_library_groups(root, apps)
    felles_changed = any(path.startswith("libs/bidrag-felles/") for path in changed_paths)
    with open(os.environ["GITHUB_OUTPUT"], "a") as stream:
        stream.write(f"apps={json.dumps(apps)}\nbibliotekgrupper={groups}\nfelles_endret={str(felles_changed).lower()}\n")
    message = f"Apper som skal bygges: {', '.join(apps) or 'ingen'}. Bibliotekgrupper: {groups or 'ingen'}."
    if unmerged_apps:
        message += f" Hoppet over fordi appmodulen ikke er merget: {', '.join(unmerged_apps)}."
    print(message)
    with open(os.environ["GITHUB_STEP_SUMMARY"], "a") as stream:
        stream.write(message + "\n")


if __name__ == "__main__":
    main()
