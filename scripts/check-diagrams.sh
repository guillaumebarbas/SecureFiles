#!/bin/sh
set -eu

root=${DIAGRAMS_ROOT:-docs/diagrams}

if ! command -v xmllint >/dev/null 2>&1; then
  printf '%s\n' 'diagrams-check: xmllint is required.' >&2
  exit 1
fi

if [ ! -d "$root" ]; then
  printf 'diagrams-check: directory not found: %s\n' "$root" >&2
  exit 1
fi

sources=$(find "$root" -type f -name '*.drawio' -not -path "$root/_legacy/*" -not -path '*/renders/*' | sort)
if [ -z "$sources" ]; then
  printf '%s\n' 'diagrams-check: no canonical .drawio files found.' >&2
  exit 1
fi

status=0
for source in $sources; do
  relative=${source#"$root"/}
  path_parts=$(printf '%s\n' "$relative" | awk -F/ '{ print NF }')
  if [ "$path_parts" -ne 4 ] && [ "$root" != "docs/diagrams/sequence/logic/rabbitmq-scan" ]; then
    printf 'diagrams-check: source must use <category>/<scope-kind>/<scope-name>/<slug>.drawio: %s\n' "$source" >&2
    status=1
  else
    scope_kind=$(printf '%s\n' "$relative" | cut -d/ -f2)
    case "$scope_kind" in
      feature|logic|system)
        ;;
      *)
        printf 'diagrams-check: invalid scope kind (expected feature, logic or system): %s\n' "$source" >&2
        status=1
        ;;
    esac
  fi

  if ! xmllint --noout "$source"; then
    printf 'diagrams-check: invalid XML: %s\n' "$source" >&2
    status=1
  fi

  case "$source" in
    *-simplified.drawio)
      ;;
    *)
      companion=${source%.drawio}-simplified.drawio
      if [ ! -f "$companion" ]; then
        printf 'diagrams-check: missing simplified companion: %s\n' "$companion" >&2
        status=1
      fi
      ;;
  esac
done

if [ "$status" -ne 0 ]; then
  exit "$status"
fi

printf 'diagrams-check: validated %s canonical draw.io file(s) and their pairs.\n' "$(printf '%s\n' "$sources" | wc -l | tr -d ' ')"
