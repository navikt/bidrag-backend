#!/usr/bin/env bash
set -euo pipefail

# Installerte interne biblioteker gjør at Maven kan bygge én app uten å skanne hele reactoren.
read -r -a opts <<< "$MAVEN_OPTIONS"
args=()
modul=""
i=0
while [ "$i" -lt "${#opts[@]}" ]; do
  arg="${opts[$i]}"
  case "$arg" in
    -pl)
      if [ -n "$modul" ] || [ "$((i + 1))" -ge "${#opts[@]}" ]; then
        echo "::error::Appbygget støtter kun én -pl med modul"
        exit 1
      fi
      i=$((i + 1))
      modul="${opts[$i]}"
      case "$modul" in *,*)
        echo "::error::Appbygget støtter kun én modul i -pl"
        exit 1 ;;
      esac ;;
    -am|--also-make)
      echo "::error::Appbygg med ferdige biblioteker skal ikke bruke -am"
      exit 1 ;;
    *) args+=("$arg") ;;
  esac
  i=$((i + 1))
done
if [ -z "$modul" ]; then
  echo "::error::Appbygget må velge modul med -pl"
  exit 1
fi
args+=(-f "$modul/pom.xml")
if [ "$SKIP_TESTS" = true ]; then args+=(-DskipTests); fi
# Maven skal bruke interne SNAPSHOT-er som allerede er installert lokalt.
exec mvn -T 1C -nsu -Dmaven.artifact.threads=16 "${args[@]}" clean verify -Dmaven.antrun.skip=true
