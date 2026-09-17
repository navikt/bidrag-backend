#!/usr/bin/env python3
"""Finn hvilke apper og biblioteker som må bygges ut fra filene som er endret."""

import json
import os
from pathlib import Path
import re
import shlex
import subprocess

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


def select_affected_apps(app_filters, event_name, branch, changed_paths):
    if event_name not in {"push", "pull_request"}:
        raise ValueError(f"Appvalg støtter bare push og pull_request, ikke {event_name}")
    if event_name == "push" and branch != "main":
        return []
    return [app for app, patterns in app_filters.items()
            if any(path_matches_filters(path, patterns) for path in changed_paths)]


def find_changed_files(root, event_name, event):
    def git_output(*args):
        return subprocess.check_output(["git", "-C", str(root), *args])

    def validate_sha(value):
        if not re.fullmatch(r"[a-f0-9]{40}", value):
            raise ValueError("Ugyldig commit-SHA i hendelsen")
        return value

    if event_name == "pull_request":
        pr = event["pull_request"]
        head = validate_sha(pr["head"]["sha"])
        base = git_output("merge-base", validate_sha(pr["base"]["sha"]), head).decode().strip()
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
        if str(config.get("java-version", "21")) != "21":
            raise ValueError(f"{app} bruker en annen Java-versjon enn bibliotekjobben, som bruker Java 21")
        configured = config.get("bibliotekgrupper", "felles")
        selected = set(configured.split(",")) if configured else set()
        if not selected <= {"felles", "beregn", "oppgave"}:
            raise ValueError(f"Ukjente bibliotekgrupper for {app}: {configured}")
        groups.update(selected)
    if "beregn" in groups:
        groups.add("felles")
    return ",".join(group for group in ("felles", "beregn", "oppgave") if group in groups)


def main():
    root = Path.cwd()
    app_filters = load_app_filters(root)
    with open(os.environ["GITHUB_EVENT_PATH"]) as stream:
        event = json.load(stream)
    event_name = os.environ["GITHUB_EVENT_NAME"]
    if event_name not in {"push", "pull_request"}:
        raise ValueError(f"Appvalg støtter bare push og pull_request, ikke {event_name}")
    branch = (event["pull_request"]["base"]["ref"] if event_name == "pull_request"
              else event["ref"].removeprefix("refs/heads/"))
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
