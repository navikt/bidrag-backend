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
    split_on_merged_module,
    target_branch,
    tested_pr_head,
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

    def test_merge_group_diffs_the_queue_base_against_the_candidate(self):
        # Køen har allerede merget kandidaten med main-tuppen (base_sha), så diffen
        # mellom de to er nøyaktig det køen vil legge til i main. Ingen merge-base her.
        event = {"merge_group": {"base_sha": "b" * 40, "head_sha": "a" * 40,
                                 "base_ref": "refs/heads/main"}}
        with patch("finn_berorte_apper.subprocess.check_output", return_value=b"apps/a/X.kt\0") as git:
            self.assertEqual(find_changed_files(ROOT, "merge_group", event), ["apps/a/X.kt"])
        self.assertEqual(git.call_count, 1)
        self.assertNotIn("merge-base", git.call_args.args[0])
        self.assertIn("b" * 40, git.call_args.args[0])
        self.assertIn("a" * 40, git.call_args.args[0])

    def test_merge_group_with_an_invalid_sha_is_not_used_as_a_git_argument(self):
        with patch("finn_berorte_apper.subprocess.check_output") as git:
            with self.assertRaises(ValueError):
                find_changed_files(ROOT, "merge_group", {"merge_group": {"base_sha": "--all", "head_sha": "a" * 40}})
            git.assert_not_called()

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

    def test_merge_group_uses_the_same_filters_as_push_and_pr(self):
        for patterns in self.app_filters.values():
            for pattern in patterns:
                sample = pattern.replace("**", "nested/File").replace("*", "File")
                self.assertEqual(self.selected(sample, event="merge_group"), self.selected(sample))

    def test_branch_is_read_from_the_right_place_per_event(self):
        self.assertEqual(target_branch("push", {"ref": "refs/heads/main"}), "main")
        self.assertEqual(target_branch("pull_request", {"pull_request": {"base": {"ref": "main"}}}), "main")
        self.assertEqual(target_branch("merge_group", {"merge_group": {"base_ref": "refs/heads/main"}}), "main")

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
        # Apper uten en merget modul faller ut av appvalget, se split_on_merged_module.
        app_names, unmerged = split_on_merged_module(ROOT, list(load_app_filters(ROOT)))
        felles_apps = [app for app in app_names if app not in {"bidrag-oppgave", "bidrag-sjablon"}]
        scenarios = (
            (["apps/bidrag-behandling/src/main/kotlin/App.kt"], ["bidrag-behandling"], "felles,beregn", "false"),
            ([f"apps/{app}/pom.xml" for app in unmerged], [], "", "false"),
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

    def test_merge_group_event_selects_apps_like_a_push_to_main(self):
        with tempfile.TemporaryDirectory() as directory:
            event = Path(directory) / "event.json"
            output = Path(directory) / "output"
            event.write_text(json.dumps({"merge_group": {
                "base_ref": "refs/heads/main", "base_sha": "b" * 40, "head_sha": "a" * 40}}))
            env = {"GITHUB_EVENT_NAME": "merge_group", "GITHUB_EVENT_PATH": str(event),
                   "GITHUB_OUTPUT": str(output), "GITHUB_STEP_SUMMARY": str(Path(directory) / "summary")}
            with patch.dict(os.environ, env), patch("finn_berorte_apper.Path.cwd", return_value=ROOT), \
                    patch("finn_berorte_apper.find_changed_files",
                          return_value=["apps/bidrag-behandling/src/main/kotlin/App.kt"]), patch("builtins.print"):
                main()
            values = dict(line.split("=", 1) for line in output.read_text().splitlines())
            self.assertEqual(json.loads(values["apps"]), ["bidrag-behandling"])
            self.assertEqual(values["bibliotekgrupper"], "felles,beregn")


class MergeQueueReuseTest(unittest.TestCase):
    """Merge-køen hopper over bygget bare når PR-hodet med identisk tre allerede er grønt."""

    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.root = Path(self.directory.name)
        self.git("init", "--quiet", "--initial-branch=main")
        self.git("config", "user.email", "ci@example.com")
        self.git("config", "user.name", "CI")
        self.git("config", "commit.gpgsign", "false")
        self.main = self.commit("README.md", "main")
        self.pr_head = self.commit("apps/a/A.kt", "pr")
        # Køen legger PR-endringen oppå main: samme tre som PR-hodet, men en ny commit.
        self.git("checkout", "--quiet", "--detach", self.main)
        self.queue_head = self.commit("apps/a/A.kt", "pr", message="kø")
        self.env = patch.dict(os.environ, {"GITHUB_REPOSITORY": "navikt/bidrag-backend", "GITHUB_TOKEN": "t"})
        self.env.start()

    def tearDown(self):
        self.env.stop()
        self.directory.cleanup()

    def git(self, *args):
        return subprocess.check_output(["git", "-C", str(self.root), *args], text=True).strip()

    def commit(self, path, content, message="endring"):
        file = self.root / path
        file.parent.mkdir(parents=True, exist_ok=True)
        file.write_text(content)
        self.git("add", path)
        self.git("commit", "--quiet", "-m", message)
        return self.git("rev-parse", "HEAD")

    def event(self, head=None, base=None, pr=7):
        return {"merge_group": {"head_sha": head or self.queue_head, "base_sha": base or self.main,
                                "base_ref": "refs/heads/main",
                                "head_ref": f"refs/heads/gh-readonly-queue/main/pr-{pr}-{base or self.main}"}}

    def api(self, successful_runs=1):
        def get(path):
            if path == "/repos/navikt/bidrag-backend/pulls/7":
                return {"head": {"sha": self.pr_head}}
            self.assertIn(f"head_sha={self.pr_head}", path)
            self.assertIn("event=pull_request", path)
            self.assertIn("status=success", path)
            self.assertIn("/actions/workflows/bygg-apper.yaml/runs", path)
            return {"total_count": successful_runs}
        return patch("finn_berorte_apper.github_get", side_effect=get)

    def test_identical_tree_with_green_pr_build_is_reused(self):
        with self.api():
            self.assertEqual(tested_pr_head(self.root, self.event(), "bygg-apper.yaml"), self.pr_head)

    def test_without_a_green_pr_build_the_queue_builds(self):
        with self.api(successful_runs=0):
            self.assertIsNone(tested_pr_head(self.root, self.event(), "bygg-apper.yaml"))

    def test_pr_behind_main_is_built_even_with_a_green_pr_build(self):
        # main har fått en ny commit etter PR-bygget: køens tre er ikke testet før.
        self.git("checkout", "--quiet", "--detach", self.main)
        new_main = self.commit("apps/b/B.kt", "ny main")
        queue_head = self.commit("apps/a/A.kt", "pr", message="kø")
        with self.api():
            self.assertIsNone(tested_pr_head(self.root, self.event(queue_head, new_main), "bygg-apper.yaml"))

    def test_different_tree_is_built(self):
        self.git("checkout", "--quiet", "--detach", self.main)
        other = self.commit("apps/a/A.kt", "noe annet", message="kø")
        with self.api():
            self.assertIsNone(tested_pr_head(self.root, self.event(other), "bygg-apper.yaml"))

    def test_unknown_queue_ref_does_not_call_the_api(self):
        event = self.event()
        event["merge_group"]["head_ref"] = "refs/heads/something-else"
        with patch("finn_berorte_apper.github_get") as get:
            self.assertIsNone(tested_pr_head(self.root, event, "bygg-apper.yaml"))
        get.assert_not_called()

    def run_main(self, **patches):
        with tempfile.TemporaryDirectory() as directory:
            event = Path(directory) / "event.json"
            output = Path(directory) / "output"
            event.write_text(json.dumps(self.event()))
            env = {"GITHUB_EVENT_NAME": "merge_group", "GITHUB_EVENT_PATH": str(event),
                   "GITHUB_OUTPUT": str(output), "GITHUB_STEP_SUMMARY": str(Path(directory) / "summary")}
            with patch.dict(os.environ, env), patch("finn_berorte_apper.Path.cwd", return_value=ROOT), \
                    patch("finn_berorte_apper.find_changed_files",
                          return_value=["apps/bidrag-behandling/src/main/kotlin/App.kt"]), \
                    patch("builtins.print"), patch("finn_berorte_apper.tested_pr_head", **patches):
                main()
            return dict(line.split("=", 1) for line in output.read_text().splitlines())

    def test_main_skips_all_apps_when_the_pr_build_is_reused(self):
        values = self.run_main(return_value=self.pr_head)
        self.assertEqual(json.loads(values["apps"]), [])
        self.assertEqual(values["bibliotekgrupper"], "")

    def test_main_builds_as_usual_when_the_check_fails(self):
        values = self.run_main(side_effect=OSError("nettverksfeil"))
        self.assertEqual(json.loads(values["apps"]), ["bidrag-behandling"])


class MergedModuleTest(unittest.TestCase):
    def app_root(self, directory, maven_options="-B -fae -pl apps/bidrag-ny"):
        root = Path(directory)
        workflows = root / ".github/workflows"
        workflows.mkdir(parents=True)
        (workflows / "bidrag-ny.yaml").write_text(yaml.safe_dump({
            "env": {"APP_PATHS": "apps/bidrag-ny/**"},
            "jobs": {"bygg_test_og_deploy": {"uses": "./.github/workflows/bygg_og_deploy.yaml",
                                             "with": {"maven_options": maven_options}}},
        }))
        return root

    def test_app_whose_module_is_not_merged_yet_is_held_back(self):
        with tempfile.TemporaryDirectory() as directory:
            root = self.app_root(directory)
            self.assertEqual(split_on_merged_module(root, ["bidrag-ny"]), ([], ["bidrag-ny"]))

    def test_app_is_built_as_soon_as_the_module_lands(self):
        with tempfile.TemporaryDirectory() as directory:
            root = self.app_root(directory)
            (root / "apps/bidrag-ny").mkdir(parents=True)
            (root / "apps/bidrag-ny/pom.xml").write_text("<project/>")
            self.assertEqual(split_on_merged_module(root, ["bidrag-ny"]), (["bidrag-ny"], []))

    def test_maven_options_without_a_module_fails(self):
        with tempfile.TemporaryDirectory() as directory:
            root = self.app_root(directory, maven_options="-B -fae")
            with self.assertRaisesRegex(ValueError, "-pl"):
                split_on_merged_module(root, ["bidrag-ny"])


class WorkflowIntegrationTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.app_filters = load_app_filters(ROOT)
        cls.build_workflow = workflow("bygg-apper.yaml")

    def test_library_jobs_before_all_app_jobs(self):
        # Biblioteklogikken er delt i to jobber som begge bruker klargjor-biblioteker:
        # "biblioteker" bygger raskt uten tester (appjobbene trenger bare jar-ene), mens
        # "biblioteker_tester" kjører selve testsuiten i parallell med app-byggene - uten at
        # appjobbene venter på den. Den gater i stedet merge via "Alle bygg fullført".
        jobs = self.build_workflow["jobs"]
        library_jobs = {name: job for name, job in jobs.items()
                        if any(step.get("uses") == "./.github/actions/klargjor-biblioteker"
                               for step in job.get("steps", []))}
        self.assertEqual(set(library_jobs), {"biblioteker", "biblioteker_tester"})

        library_job = jobs["biblioteker"]
        self.assertEqual(library_job["needs"], "detect_changes")
        self.assertEqual(library_job["if"], "needs.detect_changes.outputs.apps != '[]'")
        self.assertEqual(library_job["permissions"], {"contents": "read"})
        self.assertEqual(library_job["outputs"]["artefaktnavn"], "${{ steps.artefakt.outputs.navn }}")
        prepare = next(step for step in library_job["steps"]
                       if step.get("uses") == "./.github/actions/klargjor-biblioteker")
        self.assertEqual(prepare["with"], {
            "grupper": "${{ needs.detect_changes.outputs.bibliotekgrupper }}",
            "felles_endret": "${{ needs.detect_changes.outputs.felles_endret }}",
            "skip_tester": True,
        })

        tester_job = jobs["biblioteker_tester"]
        self.assertEqual(tester_job["needs"], "detect_changes")
        self.assertEqual(tester_job["if"], "needs.detect_changes.outputs.bibliotekgrupper != ''")
        self.assertEqual(tester_job["permissions"], {"contents": "read"})
        self.assertNotIn("outputs", tester_job)
        test_step = next(step for step in tester_job["steps"]
                         if step.get("uses") == "./.github/actions/klargjor-biblioteker")
        self.assertEqual(test_step["with"], {
            "grupper": "${{ needs.detect_changes.outputs.bibliotekgrupper }}",
            "felles_endret": "${{ needs.detect_changes.outputs.felles_endret }}",
            "bruk_bibliotekcache": False,
            "kjor_ktlint": True,
        })

        self.assertEqual(jobs["detect_changes"]["outputs"]["felles_endret"], "${{ steps.appvalg.outputs.felles_endret }}")
        self.assertEqual(set(jobs) - set(library_jobs) - {"detect_changes", "alle_bygg_fullfort"},
                         set(self.app_filters))
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

    def test_build_workflow_runs_on_pr_merge_queue_and_main_without_path_filters(self):
        on = triggers(self.build_workflow)
        self.assertEqual(on["push"], {"branches": ["main"]})
        self.assertEqual(on["pull_request"], {})
        self.assertEqual(on["merge_group"], {"types": ["checks_requested"]})
        self.assertEqual(set(on), {"push", "pull_request", "merge_group"})

    def test_no_wildcard_push_workflow_runs_on_merge_queue_branches(self):
        # Merge-køen pusher til gh-readonly-queue/main/..., som treffer 'branches: **'.
        # Workflowene som deployer til q1/dev fra en hvilken som helst branch, ville
        # dermed deployet fra en midlertidig kø-branch som slettes rett etterpå.
        for path in sorted((ROOT / ".github/workflows").glob("*.y*ml")):
            on = triggers(workflow(path.name)) or {}
            branches = (on.get("push") or {}).get("branches", []) if isinstance(on, dict) else []
            if "**" in branches:
                with self.subTest(workflow=path.name):
                    self.assertIn("!gh-readonly-queue/**", branches)

    def test_merge_queue_builds_but_never_deploys(self):
        # Køen kjører før merge. Ingen app skal kunne deploye fra en merge_group-kjøring,
        # derfor må alle deploy-flaggene kreve enten push til main eller workflow_dispatch.
        for app in self.app_filters:
            with self.subTest(app=app):
                config = workflow(f"{app}.yaml")["jobs"]["bygg_test_og_deploy"]["with"]
                for key, expression in config.items():
                    if key.startswith("deploy_"):
                        self.assertNotIn("merge_group", expression)
                        for clause in expression.split("||"):
                            self.assertTrue(
                                "github.event_name == 'workflow_dispatch'" in clause
                                or ("github.event_name == 'push'" in clause
                                    and "refs/heads/main" in clause),
                                f"{app}.{key} kan deploye fra en merge_group-kjøring: {clause}")

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

    def test_image_is_only_uploaded_when_the_run_actually_deploys(self):
        # Et image per PR-push per app fylte registeret uten at noen brukte dem. Bygget
        # beholdes, så en ødelagt Dockerfile fortsatt fanges i PR-en, men opplastingen og
        # attesteringen skjer bare når kjøringen skal deploye. Lagcachen eksporteres aldri.
        document = workflow("bygg_og_deploy.yaml")
        deploy_flags = [name for name in triggers(document)["workflow_call"]["inputs"]
                        if name.startswith("deploy_")]
        self.assertTrue(deploy_flags)
        job = document["jobs"]["bygg_test_og_image"]
        step = next(s for s in job["steps"] if s.get("uses", "").startswith("nais/docker-build-push@"))
        self.assertIs(step["with"]["no_cache"], True)
        self.assertNotIn("cache_to", step["with"])
        for flag in deploy_flags:
            with self.subTest(flag=flag):
                # Alle miljøer som kan deploye, må også utløse opplasting og attestering.
                self.assertIn(f"inputs.{flag}", step["with"]["push_image"])
                self.assertIn(f"inputs.{flag}", document["jobs"]["salsa"]["if"])

    def test_deploy_concurrency_uses_app_identity(self):
        jobs = workflow("bygg_og_deploy.yaml")["jobs"]
        for environment in ("q1", "q2", "prod"):
            group = jobs[f"deploy_{environment}"]["concurrency"]["group"]
            self.assertIn("inputs.image_suffix", group)
            self.assertTrue(group.endswith(f"-{environment}"))

    def test_only_test_environments_may_cancel_a_running_deploy(self):
        # En prod-deploy som avbrytes midt i nais-rolloutet, etterlater et applied
        # manifest ingen har verifisert, og feller samlejobben med resultatet
        # `cancelled`. q1/q2 startes manuelt, der er avbrytelse ønsket.
        jobs = workflow("bygg_og_deploy.yaml")["jobs"]
        self.assertIs(jobs["deploy_prod"]["concurrency"]["cancel-in-progress"], False)
        for environment in ("q1", "q2"):
            self.assertIs(jobs[f"deploy_{environment}"]["concurrency"]["cancel-in-progress"], True)

    def test_apps_without_a_merged_module_are_skipped_and_not_built(self):
        missing_modules = set()
        for app in self.app_filters:
            config = workflow(f"{app}.yaml")["jobs"]["bygg_test_og_deploy"]["with"]
            args = shlex.split(config["maven_options"])
            module = args[args.index("-pl") + 1]
            if not (ROOT / module / "pom.xml").is_file():
                missing_modules.add(app)
        # En ny app kobles inn i CI før appmodulen merges. Appvalget skal hoppe over den,
        # ikke felle bygget for alle de andre appene.
        merged, unmerged = split_on_merged_module(ROOT, list(self.app_filters))
        self.assertEqual(set(unmerged), missing_modules)
        self.assertEqual(set(merged), set(self.app_filters) - missing_modules)
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
                   "SKIP_TESTS": "false", "KJOR_KTLINT": "true", "TEST_FELLES": "true", **state,
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
        self.assertIn("-Dmaven.antrun.skip=true", self.run_build(
            FELLES="true", TEST_FELLES="false", KJOR_KTLINT="false"))

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
        self.assertIn("-Dmaven.test.skip=true", calls[1])

    def test_cached_unchanged_felles_is_not_rebuilt(self):
        args = self.run_build(FELLES="true", FELLES_CACHE="true", TEST_FELLES="false", BEREGN="true")
        self.assertNotIn("libs/bidrag-felles/", args[args.index("-pl") + 1])
        self.assertNotIn("-Dmaven.test.skip=true", args)

    def test_oppgave_does_not_build_felles(self):
        args = self.run_build(OPPGAVE="true", SKIP_TESTS="true")
        self.assertIn("-Dmaven.test.skip=true", args)
        selected = args[args.index("-pl") + 1]
        self.assertNotIn("libs/bidrag-felles/", selected)
        self.assertIn("libs/bidrag-oppgave-client", selected)

    def test_app_library_build_skips_test_compilation_and_ktlint(self):
        self.assertEqual(self.action["inputs"]["kjor_ktlint"]["default"], "false")
        self.assertEqual(self.build["env"]["KJOR_KTLINT"], "${{ inputs.kjor_ktlint }}")
        calls = self.run_build_calls(FELLES="true", BEREGN="true", OPPGAVE="true",
                                     TEST_FELLES="false", SKIP_TESTS="true", KJOR_KTLINT="false")
        self.assertEqual(len(calls), 2)
        for args in calls:
            self.assertIn("-Dmaven.test.skip=true", args)
            self.assertIn("-Dmaven.antrun.skip=true", args)
            self.assertNotIn("-DskipTests", args)

    def test_caches_are_exact_and_beregn_includes_its_felles_inputs(self):
        restores = {step["id"]: step for step in self.steps
                    if step.get("uses", "").startswith("actions/cache/restore@")}
        self.assertEqual(set(restores), {"felles", "felles_uten_tester", "beregn", "oppgave"})
        for step in restores.values():
            self.assertIn("inputs.bruk_bibliotekcache == 'true'", step["if"])
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
            self.assertIn("inputs.bruk_bibliotekcache == 'true'", step["if"])
            self.assertIn("cache-primary-key", step["with"]["key"])

    def test_test_job_builds_all_selected_groups_without_library_cache(self):
        select = self.steps[0]
        self.assertEqual(self.action["inputs"]["bruk_bibliotekcache"]["default"], "true")
        self.assertEqual(select["env"]["LIBRARY_CACHE"], "${{ inputs.bruk_bibliotekcache }}")
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory)
            fake_maven = path / "mvn"
            fake_maven.write_text("#!/bin/sh\nprintf 'Apache Maven 3.9.0\\nJava version: 21\\n'\n")
            fake_maven.chmod(0o755)
            output = path / "output"
            subprocess.run(["bash", "-euo", "pipefail", "-c", select["run"]],
                           env={**os.environ, "PATH": f"{path}:{os.environ['PATH']}",
                                "LIBRARY_GROUPS": "felles,beregn,oppgave", "SKIP_TESTS": "false",
                                "LIBRARY_CACHE": "false", "FELLES_ENDRET": "false",
                                "GITHUB_OUTPUT": str(output)}, check=True)
            self.assertIn("test_felles=true", output.read_text())
        args = self.run_build(FELLES="true", BEREGN="true", OPPGAVE="true", TEST_FELLES="true")
        projects = args[args.index("-pl") + 1]
        self.assertIn("libs/bidrag-felles/bidrag-domene", projects)
        self.assertIn("libs/bidrag-beregn-felles/bidrag-beregn-core", projects)
        self.assertIn("libs/bidrag-oppgave-client", projects)
        self.assertNotIn("-DskipTests", args)
        self.assertNotIn("-Dmaven.test.skip=true", args)
        self.assertNotIn("-Dmaven.antrun.skip=true", args)

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
