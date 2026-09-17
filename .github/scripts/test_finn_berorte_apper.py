#!/usr/bin/env python3
"""Tester appvalg, bibliotekbygg og koblingen mellom workflowene."""

import json
import os
from pathlib import Path
import shlex
import subprocess
import tempfile
import unittest
from unittest.mock import patch

import yaml

from finn_berorte_apper import (
    find_changed_files,
    load_app_filters,
    main,
    path_matches_filters,
    required_library_groups,
    select_affected_apps,
)


ROOT = Path(__file__).resolve().parents[2]


def workflow(name):
    return yaml.safe_load((ROOT / ".github/workflows" / name).read_text())


def triggers(document):
    return document.get("on", document.get(True))


class PathMatchingTest(unittest.TestCase):
    def test_star_does_not_cross_directory_boundary(self):
        self.assertTrue(path_matches_filters("apps/a/File.kt", ["apps/a/*.kt"]))
        self.assertFalse(path_matches_filters("apps/a/src/File.kt", ["apps/a/*.kt"]))

    def test_double_star_matches_nested_and_direct_paths(self):
        self.assertTrue(path_matches_filters("apps/a/src/File.kt", ["apps/a/**"]))
        self.assertTrue(path_matches_filters("apps/a/File.kt", ["apps/a/**/*.kt"]))
        self.assertTrue(path_matches_filters(".nais/a/nested/prod.yaml", [".nais/a/**.yaml"]))

    def test_negative_patterns_are_ordered(self):
        patterns = ["libs/**", "!libs/a/**", "libs/a/public/**"]
        self.assertFalse(path_matches_filters("libs/a/internal/X.kt", patterns))
        self.assertTrue(path_matches_filters("libs/a/public/X.kt", patterns))

    def test_literal_unicode_and_question_mark(self):
        self.assertTrue(path_matches_filters("apps/dokumenthåndtering/a.kt", ["apps/dokumenthåndtering/?.kt"]))
        self.assertFalse(path_matches_filters("apps/dokumenthåndtering/ab.kt", ["apps/dokumenthåndtering/?.kt"]))

    def test_unsupported_glob_fails_instead_of_silently_skipping_apps(self):
        with self.assertRaisesRegex(ValueError, "Glob"):
            path_matches_filters("apps/a/A.kt", ["apps/[ab]/**"])


class GitDiffTest(unittest.TestCase):
    def test_pr_uses_merge_base_and_both_sides_of_renames(self):
        event = {"pull_request": {"head": {"sha": "a" * 40}, "base": {"sha": "b" * 40}}}
        with patch("finn_berorte_apper.subprocess.check_output", side_effect=[
            ("c" * 40).encode(), b"apps/old/X.kt\0apps/new/X.kt\0",
        ]) as git:
            self.assertEqual(find_changed_files(ROOT, "pull_request", event), ["apps/old/X.kt", "apps/new/X.kt"])
        self.assertIn("merge-base", git.call_args_list[0].args[0])
        self.assertIn("--no-renames", git.call_args_list[1].args[0])
        self.assertIn("c" * 40, git.call_args_list[1].args[0])
        self.assertIn("a" * 40, git.call_args_list[1].args[0])

    def test_push_uses_before_and_after_not_merge_base(self):
        event = {"before": "b" * 40, "after": "a" * 40}
        with patch("finn_berorte_apper.subprocess.check_output", return_value=b"apps/a/X.kt\0") as git:
            self.assertEqual(find_changed_files(ROOT, "push", event), ["apps/a/X.kt"])
        self.assertEqual(git.call_count, 1)
        self.assertIn("b" * 40, git.call_args.args[0])
        self.assertNotIn("merge-base", git.call_args.args[0])

    def test_initial_push_uses_empty_tree(self):
        event = {"before": "0" * 40, "after": "a" * 40}
        with patch("finn_berorte_apper.subprocess.check_output", side_effect=[
            b"4b825dc642cb6eb9a060e54bf8d69288fbee4904\n", b"pom.xml\0",
        ]) as git:
            self.assertEqual(find_changed_files(ROOT, "push", event), ["pom.xml"])
        self.assertEqual(git.call_args_list[0].kwargs["input"], b"")

    def test_branch_deletion_does_not_build(self):
        self.assertEqual(find_changed_files(ROOT, "push", {"before": "b" * 40, "after": "0" * 40}), [])

    def test_invalid_sha_is_not_used_as_a_git_argument(self):
        with patch("finn_berorte_apper.subprocess.check_output") as git:
            with self.assertRaises(ValueError):
                find_changed_files(ROOT, "push", {"before": "b" * 40, "after": "--all"})
            git.assert_not_called()


class AppSelectionTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.app_filters = load_app_filters(ROOT)

    def selected(self, *paths, event="pull_request", branch="main"):
        return select_affected_apps(self.app_filters, event, branch, paths)

    def test_app_only_change_selects_only_that_app(self):
        self.assertEqual(self.selected("apps/bidrag-belopshistorikk/src/main/kotlin/Foo.kt"),
                         ["bidrag-belopshistorikk"])

    def test_henvendelse_is_selected_for_app_and_nais_changes(self):
        for event in ("pull_request", "push"):
            for path in ("apps/bidrag-henvendelse/pom.xml", ".nais/bidrag-henvendelse/q2.yaml"):
                with self.subTest(event=event, path=path):
                    self.assertEqual(self.selected(path, event=event), ["bidrag-henvendelse"])

    def test_root_pom_selects_all_34_apps(self):
        self.assertEqual(set(self.selected("pom.xml")), set(self.app_filters))
        self.assertEqual(len(self.app_filters), 34)

    def test_felles_preserves_existing_consumers_not_oppgave_or_sjablon(self):
        selected = self.selected("libs/bidrag-felles/bidrag-transport/src/main/kotlin/Dto.kt")
        self.assertEqual(len(selected), 32)
        self.assertIn("bidrag-henvendelse", selected)
        self.assertNotIn("bidrag-oppgave", selected)
        self.assertNotIn("bidrag-sjablon", selected)

    def test_beregn_only_selects_existing_four_app_workflows(self):
        self.assertEqual(set(self.selected("libs/bidrag-beregn-felles/bidrag-beregn-core/pom.xml")), {
            "bidrag-automatisk-jobb", "bidrag-behandling", "bidrag-bidragskalkulator", "bidrag-statistikk",
        })

    def test_oppgave_libraries_only_select_oppgave(self):
        self.assertEqual(self.selected("libs/bidrag-oppgave-client/pom.xml"), ["bidrag-oppgave"])

    def test_admin_parent_selects_both_nested_apps(self):
        self.assertEqual(set(self.selected("apps/bidrag-admin/pom.xml")), {"bidrag-admin", "bidrag-admin-fss"})

    def test_non_main_push_does_not_select_apps(self):
        self.assertEqual(self.selected("pom.xml", event="push", branch="feature"), [])
        self.assertEqual(set(self.selected("pom.xml", branch="feature")), set(self.app_filters))

    def test_push_and_pr_use_the_same_app_paths(self):
        for patterns in self.app_filters.values():
            for pattern in patterns:
                sample = pattern.replace("**", "nested/File").replace("*", "File")
                self.assertEqual(self.selected(sample, event="push"), self.selected(sample))

    def test_unsupported_event_fails(self):
        with self.assertRaisesRegex(ValueError, "Appvalg støtter bare"):
            self.selected("pom.xml", event="workflow_dispatch")

    def test_unrelated_change_does_not_build_libraries_or_apps(self):
        self.assertEqual(self.selected("README.md", "util/a.js"), [])
        self.assertEqual(required_library_groups(ROOT, []), "")

    def test_group_selection_includes_beregn_only_when_needed(self):
        self.assertEqual(required_library_groups(ROOT, ["bidrag-admin", "bidrag-belopshistorikk"]), "felles")
        self.assertEqual(required_library_groups(ROOT, ["bidrag-henvendelse"]), "felles")
        self.assertEqual(required_library_groups(ROOT, ["bidrag-behandling"]), "felles,beregn")
        self.assertEqual(required_library_groups(ROOT, ["bidrag-oppgave"]), "oppgave")
        self.assertEqual(required_library_groups(ROOT, ["bidrag-sjablon"]), "")
        self.assertEqual(required_library_groups(ROOT, list(self.app_filters)), "felles,beregn,oppgave")


class AppFiltersTest(unittest.TestCase):
    def load(self, documents):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            workflows = root / ".github/workflows"
            workflows.mkdir(parents=True)
            for name, document in documents.items():
                (workflows / name).write_text(yaml.safe_dump(document))
            return load_app_filters(root)

    def test_reads_one_pattern_per_line_and_preserves_negative_pattern_order(self):
        self.assertEqual(self.load({
            "bidrag-a.yaml": {"env": {"APP_PATHS": "libs/**\n!libs/internal/**\n\napps/a/**\n"}},
            "unrelated.yaml": {"env": {"OTHER": "value"}},
        }), {"bidrag-a": ["libs/**", "!libs/internal/**", "apps/a/**"]})

    def test_empty_or_non_string_paths_fail(self):
        for value in ("", " \n ", None, ["apps/a/**"]):
            with self.subTest(value=value), self.assertRaisesRegex(ValueError, "APP_PATHS"):
                self.load({"bidrag-a.yaml": {"env": {"APP_PATHS": value}}})

    def test_no_app_metadata_fails_instead_of_skipping_all_builds(self):
        with self.assertRaisesRegex(ValueError, "Fant ingen"):
            self.load({"unrelated.yaml": {"name": "Annen workflow"}})

    def test_invalid_app_name_fails(self):
        with self.assertRaisesRegex(ValueError, "Ugyldig appnavn"):
            self.load({"other.yaml": {"env": {"APP_PATHS": "apps/a/**"}}})

    def test_script_outputs_affected_apps_and_required_library_groups(self):
        app_names = list(load_app_filters(ROOT))
        felles_apps = [app for app in app_names if app not in {"bidrag-oppgave", "bidrag-sjablon"}]
        scenarios = (
            (["apps/bidrag-behandling/src/main/kotlin/App.kt"], ["bidrag-behandling"], "felles,beregn", "false"),
            (["apps/bidrag-henvendelse/pom.xml"], ["bidrag-henvendelse"], "felles", "false"),
            (["apps/bidrag-sjablon/pom.xml"], ["bidrag-sjablon"], "", "false"),
            (["README.md"], [], "", "false"),
            (["pom.xml"], app_names, "felles,beregn,oppgave", "false"),
            (["libs/bidrag-felles/bidrag-domene/pom.xml"], felles_apps, "felles,beregn", "true"),
            (["libs/bidrag-felles/bidrag-commons/src/test/kotlin/Test.kt"], felles_apps, "felles,beregn", "true"),
            (["libs/bidrag-felles-extra/pom.xml"], [], "", "false"),
        )
        with tempfile.TemporaryDirectory() as directory:
            event = Path(directory) / "event.json"
            output = Path(directory) / "output"
            event.write_text(json.dumps({"pull_request": {"base": {"ref": "main"}}}))
            env = {"GITHUB_EVENT_NAME": "pull_request", "GITHUB_EVENT_PATH": str(event),
                   "GITHUB_OUTPUT": str(output), "GITHUB_STEP_SUMMARY": str(Path(directory) / "summary")}
            for changed, apps, groups, felles_changed in scenarios:
                output.write_text("")
                with self.subTest(changed=changed), patch.dict(os.environ, env), \
                        patch("finn_berorte_apper.Path.cwd", return_value=ROOT), \
                        patch("finn_berorte_apper.find_changed_files", return_value=changed), patch("builtins.print"):
                    main()
                values = dict(line.split("=", 1) for line in output.read_text().splitlines())
                self.assertEqual(json.loads(values["apps"]), apps)
                self.assertEqual(values["bibliotekgrupper"], groups)
                self.assertEqual(values["felles_endret"], felles_changed)


class WorkflowIntegrationTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.app_filters = load_app_filters(ROOT)
        cls.build_workflow = workflow("bygg-apper.yaml")

    def test_one_library_job_before_all_app_jobs(self):
        jobs = self.build_workflow["jobs"]
        library_jobs = [job for job in jobs.values()
                        if any(step.get("uses") == "./.github/actions/klargjor-biblioteker"
                               for step in job.get("steps", []))]
        self.assertEqual(len(library_jobs), 1)
        library_job = jobs["biblioteker"]
        self.assertEqual(library_jobs[0], library_job)
        self.assertEqual(library_job["needs"], "detect_changes")
        self.assertEqual(library_job["if"], "needs.detect_changes.outputs.apps != '[]'")
        self.assertEqual(library_job["permissions"], {"contents": "read"})
        self.assertEqual(library_job["outputs"]["artefaktnavn"], "${{ steps.artefakt.outputs.navn }}")
        prepare = next(step for step in library_job["steps"]
                       if step.get("uses") == "./.github/actions/klargjor-biblioteker")
        self.assertEqual(prepare["with"], {
            "grupper": "${{ needs.detect_changes.outputs.bibliotekgrupper }}",
            "felles_endret": "${{ needs.detect_changes.outputs.felles_endret }}",
        })
        self.assertEqual(jobs["detect_changes"]["outputs"]["felles_endret"], "${{ steps.appvalg.outputs.felles_endret }}")
        self.assertEqual(set(jobs) - {"detect_changes", "biblioteker", "alle_bygg_fullfort"}, set(self.app_filters))
        for app in self.app_filters:
            with self.subTest(app=app):
                self.assertEqual(set(jobs[app]["needs"]), {"detect_changes", "biblioteker"})
                self.assertEqual(jobs[app]["uses"], f"./.github/workflows/{app}.yaml")
                self.assertIn(f"'{app}'", jobs[app]["if"])
                self.assertEqual(jobs[app]["with"]["bibliotekartefakt"],
                                 "${{ needs.biblioteker.outputs.artefaktnavn }}")

    def test_alle_bygg_fullfort_needs_every_other_job(self):
        # alle_bygg_fullfort er required status check. Den håndskrevne needs-listen må
        # dekke alle andre jobber i workflowen, ellers kan samlejobben bli
        # grønn uten at en nylig lagt til jobb (f.eks. en ny app) faktisk har kjørt/blitt
        # kontrollert. Denne testen sammenligner needs mot selve jobb-settet i workflowen
        # slik at den også fanger opp fremtidige infrastruktur-jobber, ikke bare nye apper.
        jobs = self.build_workflow["jobs"]
        gate = jobs["alle_bygg_fullfort"]
        self.assertEqual(set(gate["needs"]), set(jobs) - {"alle_bygg_fullfort"})
        self.assertEqual(gate["if"], "always()")

    def test_reusable_workflow_tree_stays_within_github_limits(self):
        called, documents = set(), {}

        def visit(name, ancestors):
            self.assertNotIn(name, ancestors)
            self.assertLessEqual(len(ancestors) + 1, 10)
            if name not in documents:
                documents[name] = workflow(name)
            for job in documents[name]["jobs"].values():
                reference = job.get("uses", "")
                if reference.startswith("./.github/workflows/"):
                    child = reference.removeprefix("./.github/workflows/")
                    called.add(child)
                    visit(child, [*ancestors, name])

        visit("bygg-apper.yaml", [])
        self.assertLessEqual(len(called), 50)

    def test_build_workflow_has_no_path_filter_on_either_trigger(self):
        on = triggers(self.build_workflow)
        self.assertEqual(on["push"], {"branches": ["main"]})
        self.assertEqual(on["pull_request"], {})

    def test_app_workflows_have_no_duplicate_automatic_triggers(self):
        for app in self.app_filters:
            with self.subTest(app=app):
                app_workflow = workflow(f"{app}.yaml")
                on = triggers(app_workflow)
                self.assertEqual(set(on), {"workflow_call", "workflow_dispatch"})
                self.assertIn("miljo", on["workflow_dispatch"]["inputs"])
                self.assertEqual(on["workflow_call"]["inputs"]["bibliotekartefakt"]["default"], "")
                job = app_workflow["jobs"]["bygg_test_og_deploy"]
                self.assertNotIn("-am", job["with"]["maven_options"].split())
                self.assertNotIn("libs/", job["with"]["ktlint_paths"])
                self.assertEqual(job["with"]["bibliotekartefakt"], "${{ inputs.bibliotekartefakt }}")
                if "prod" in on["workflow_dispatch"]["inputs"]["miljo"]["options"]:
                    self.assertIn("github.event_name == 'push'", job["with"]["deploy_prod"])
                else:
                    self.assertNotIn("deploy_prod", job["with"])
                self.assertIn("inputs.hopp_over_tester", job["with"]["skip_tester"])
                self.assertEqual(set(app_workflow["jobs"]), {"bygg_test_og_deploy"})
                self.assertNotIn("needs", job)
                self.assertNotIn("if", job)
                self.assertEqual(app_workflow["env"]["APP_PATHS"].splitlines(), self.app_filters[app])

    def test_all_apps_using_the_shared_build_are_registered(self):
        apps = set()
        for path in (ROOT / ".github/workflows").glob("bidrag-*.yaml"):
            document = workflow(path.name)
            if any(job.get("uses") == "./.github/workflows/bygg_og_deploy.yaml"
                   for job in document.get("jobs", {}).values()):
                apps.add(path.stem)
        self.assertEqual(apps, set(self.app_filters))

    def test_shared_workflow_imports_or_prepares_but_never_both(self):
        document = workflow("bygg_og_deploy.yaml")
        steps = document["jobs"]["bygg_test_og_image"]["steps"]
        prepare = [s for s in steps if s.get("uses") == "./.github/actions/klargjor-biblioteker"]
        action = yaml.safe_load((ROOT / ".github/actions/klargjor-biblioteker/action.yaml").read_text())
        download = [s for s in action["runs"]["steps"] if s.get("uses", "").startswith("actions/download-artifact@")]
        self.assertEqual(len(prepare), 1)
        self.assertEqual(len(download), 1)
        self.assertEqual(prepare[0]["with"]["artefaktnavn"], "${{ inputs.bibliotekartefakt }}")
        self.assertEqual(prepare[0]["with"]["felles_endret"], "${{ steps.felles_endringer.outputs.endret == 'true' }}")
        self.assertEqual(download[0]["if"], "inputs.artefaktnavn != ''")
        self.assertNotIn("run-id", download[0]["with"])
        self.assertNotIn("github-token", download[0]["with"])
        self.assertNotIn("github.run_attempt", str(download[0]))
        build = next(s for s in action["runs"]["steps"] if "FELLES_MODULER" in s.get("env", {}))
        self.assertEqual(build["if"], "inputs.artefaktnavn == ''")
        self.assertTrue(any(s.get("if") == "inputs.skip_tester && inputs.deploy_prod" for s in steps))

    def test_deploy_concurrency_uses_app_identity(self):
        jobs = workflow("bygg_og_deploy.yaml")["jobs"]
        for environment in ("q1", "q2", "prod"):
            group = jobs[f"deploy_{environment}"]["concurrency"]["group"]
            self.assertIn("inputs.image_suffix", group)
            self.assertTrue(group.endswith(f"-{environment}"))

    def test_maven_modules_exist_except_henvendelse_pending_merge(self):
        missing_modules = set()
        for app in self.app_filters:
            config = workflow(f"{app}.yaml")["jobs"]["bygg_test_og_deploy"]["with"]
            args = shlex.split(config["maven_options"])
            module = args[args.index("-pl") + 1]
            if not (ROOT / module / "pom.xml").is_file():
                missing_modules.add((app, module))
        # Henvendelse kobles inn før appmodulen merges. Manglende modul skal feile i appjobben.
        self.assertLessEqual(missing_modules, {("bidrag-henvendelse", "apps/bidrag-henvendelse")})
        default = triggers(workflow("bygg_og_deploy.yaml"))["workflow_call"]["inputs"]["bibliotekgrupper"]["default"]
        self.assertEqual(default, "felles")


class LibraryBuildTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.action = yaml.safe_load((ROOT / ".github/actions/klargjor-biblioteker/action.yaml").read_text())
        cls.steps = cls.action["runs"]["steps"]
        cls.build = next(step for step in cls.steps if "FELLES_MODULER" in step.get("env", {}))

    def run_build_calls(self, **state):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory)
            fake_maven = path / "mvn"
            fake_maven.write_text('#!/bin/sh\nprintf "%s\\n" "--maven-call--" "$@" >> "$MAVEN_ARGS_OUTPUT"\n')
            fake_maven.chmod(0o755)
            env = {**os.environ, **self.build["env"],
                   "FELLES": "false", "BEREGN": "false", "OPPGAVE": "false",
                   "FELLES_CACHE": "", "BEREGN_CACHE": "", "OPPGAVE_CACHE": "",
                   "SKIP_TESTS": "false", "TEST_FELLES": "true", **state,
                   "PATH": f"{path}:{os.environ['PATH']}",
                   "MAVEN_ARGS_OUTPUT": str(path / "args"), "GITHUB_STEP_SUMMARY": str(path / "summary")}
            subprocess.run(["bash", "-euo", "pipefail", "-c", self.build["run"]], env=env, check=True)
            return [call.splitlines() for call in (path / "args").read_text().split("--maven-call--\n")[1:]]

    def run_build(self, **state):
        calls = self.run_build_calls(**state)
        self.assertEqual(len(calls), 1)
        return calls[0]

    def test_cold_cache_selects_groups_once_and_lets_maven_order_them(self):
        args = self.run_build(FELLES="true", BEREGN="true")
        projects = args[args.index("-pl") + 1].split(",")
        self.assertNotIn("-am", args)
        self.assertNotIn("-Dmaven.test.skip=true", args)
        self.assertNotIn("-DskipTests", args)
        self.assertEqual(args[args.index("-T") + 1], "1C")
        self.assertEqual(len(projects), len(set(projects)))
        self.assertEqual(len(projects), 3 + 5 + 9)
        self.assertFalse(any("bidrag-oppgave" in project for project in projects))
        for project in projects:
            self.assertTrue((ROOT / project / "pom.xml").is_file(), project)

    def test_warm_cache_does_not_select_library_modules(self):
        args = self.run_build(FELLES="true", BEREGN="true", OPPGAVE="true",
                              FELLES_CACHE="true", BEREGN_CACHE="true", OPPGAVE_CACHE="true")
        self.assertEqual(args[args.index("-pl") + 1],
                         ".,apps/bidrag-admin,apps/bidrag-dokumenthåndtering")

    def test_cached_felles_is_not_rebuilt_for_beregn(self):
        args = self.run_build(FELLES="true", FELLES_CACHE="true", BEREGN="true")
        selected = args[args.index("-pl") + 1]
        self.assertNotIn("libs/bidrag-felles/", selected)
        self.assertIn("libs/bidrag-beregn-felles/", selected)

    def test_unchanged_felles_cache_miss_skips_test_compilation_and_execution(self):
        args = self.run_build(FELLES="true", TEST_FELLES="false")
        self.assertIn("-Dmaven.test.skip=true", args)
        projects = args[args.index("-pl") + 1].split(",")
        self.assertEqual(len(projects), 3 + 5)
        self.assertIn("libs/bidrag-felles/bidrag-commons-test", projects)
        self.assertNotIn("-am", args)
        self.assertNotIn("-Dmaven.antrun.skip=true", args)

    def test_unchanged_felles_is_built_first_without_skipping_other_library_tests(self):
        calls = self.run_build_calls(FELLES="true", BEREGN="true", OPPGAVE="true", TEST_FELLES="false")
        self.assertEqual(len(calls), 2)
        felles, other = calls
        self.assertIn("-Dmaven.test.skip=true", felles)
        self.assertNotIn("libs/bidrag-beregn-felles/", felles[felles.index("-pl") + 1])
        self.assertNotIn("-Dmaven.test.skip=true", other)
        self.assertNotIn("-DskipTests", other)
        projects = other[other.index("-pl") + 1].split(",")
        self.assertEqual(len(projects), 9 + 2)
        self.assertFalse(any(project.startswith("libs/bidrag-felles/") for project in projects))
        self.assertNotIn("-am", other)

    def test_manual_test_skip_also_skips_compilation_for_felles(self):
        calls = self.run_build_calls(FELLES="true", BEREGN="true", TEST_FELLES="false", SKIP_TESTS="true")
        self.assertEqual(len(calls), 2)
        self.assertIn("-Dmaven.test.skip=true", calls[0])
        self.assertIn("-DskipTests", calls[1])

    def test_cached_unchanged_felles_is_not_rebuilt(self):
        args = self.run_build(FELLES="true", FELLES_CACHE="true", TEST_FELLES="false", BEREGN="true")
        self.assertNotIn("libs/bidrag-felles/", args[args.index("-pl") + 1])
        self.assertNotIn("-Dmaven.test.skip=true", args)

    def test_oppgave_does_not_build_felles(self):
        args = self.run_build(OPPGAVE="true", SKIP_TESTS="true")
        self.assertIn("-DskipTests", args)
        selected = args[args.index("-pl") + 1]
        self.assertNotIn("libs/bidrag-felles/", selected)
        self.assertIn("libs/bidrag-oppgave-client", selected)

    def test_caches_are_exact_and_beregn_includes_its_felles_inputs(self):
        restores = {step["id"]: step for step in self.steps
                    if step.get("uses", "").startswith("actions/cache/restore@")}
        self.assertEqual(set(restores), {"felles", "felles_uten_tester", "beregn", "oppgave"})
        for step in restores.values():
            key = step["with"]["key"]
            if step["id"] in ("beregn", "oppgave"):
                self.assertIn("inputs.skip_tester", key)
            self.assertIn("outputs.toolchain", key)
            self.assertIn("'pom.xml'", key)
            self.assertNotIn("restore-keys", step["with"])
            self.assertNotIn("/target", key)
        self.assertIn("libs/bidrag-felles/*/src/**", restores["beregn"]["with"]["key"])
        self.assertNotIn("bidrag-felles", restores["oppgave"]["with"]["key"])
        saves = [step for step in self.steps if step.get("uses", "").startswith("actions/cache/save@")]
        self.assertEqual(len(saves), 4)
        for step in saves:
            self.assertIn("cache-primary-key", step["with"]["key"])

    def test_untested_cache_cannot_satisfy_a_build_that_requires_felles_tests(self):
        restores = {step["id"]: step for step in self.steps
                    if step.get("uses", "").startswith("actions/cache/restore@")}
        tested, untested = restores["felles"], restores["felles_uten_tester"]
        self.assertNotIn("test_felles", tested["if"])
        self.assertIn("steps.grupper.outputs.test_felles == 'false'", untested["if"])
        self.assertIn("steps.felles.outputs.cache-hit != 'true'", untested["if"])
        self.assertEqual(tested["with"]["key"].replace("-testet-", "-uten-tester-"), untested["with"]["key"])
        self.assertLess(self.steps.index(tested), self.steps.index(untested))
        self.assertEqual(self.build["env"]["FELLES_CACHE"],
                         "${{ steps.felles.outputs.cache-hit == 'true' || steps.felles_uten_tester.outputs.cache-hit == 'true' }}")
        saves = {step["with"]["key"]: step for step in self.steps
                 if step.get("uses", "").startswith("actions/cache/save@")}
        self.assertIn("steps.grupper.outputs.test_felles == 'true'",
                      saves["${{ steps.felles.outputs.cache-primary-key }}"]["if"])
        self.assertIn("steps.grupper.outputs.test_felles == 'false'",
                      saves["${{ steps.felles_uten_tester.outputs.cache-primary-key }}"]["if"])

    def test_stale_internal_artifacts_are_removed_but_settings_are_untouched(self):
        clean = next(step for step in self.steps if "rm -rf" in step.get("run", ""))
        with tempfile.TemporaryDirectory() as directory:
            home = Path(directory)
            repo = home / ".m2/repository/no/nav/bidrag"
            repo.mkdir(parents=True)
            for artifact in ("bidrag-backend-domene", "bidrag-beregn-core", "bidrag-oppgave-dto", "other"):
                (repo / artifact).mkdir()
                (repo / artifact / "old.jar").write_text("old")
            settings = home / ".m2/settings.xml"
            settings.write_text("settings")
            subprocess.run(["bash", "-euo", "pipefail", "-c", clean["run"]],
                           env={**os.environ, "HOME": str(home)}, check=True)
            self.assertEqual({p.name for p in repo.iterdir()}, {"other"})
            self.assertTrue(settings.exists())

    def test_artifact_transfer_contains_only_internal_maven_directories(self):
        upload = next(step for step in workflow("bygg-apper.yaml")["jobs"]["biblioteker"]["steps"]
                      if step.get("uses", "").startswith("actions/upload-artifact@"))
        for path in upload["with"]["path"].splitlines():
            self.assertTrue(path.startswith("~/.m2/repository/no/nav/bidrag/bidrag-"))
        self.assertEqual(upload["with"]["if-no-files-found"], "error")
        self.assertEqual(upload["with"]["name"], "${{ steps.artefakt.outputs.navn }}")
        self.assertEqual(upload["with"]["retention-days"], 14)


if __name__ == "__main__":
    unittest.main(verbosity=2)
