#!/bin/sh
set -eu

root=${DIAGRAMS_ROOT:-docs/diagrams}

sh scripts/check-diagrams.sh

drawio_bin=${DRAWIO_BIN:-}
if [ -n "$drawio_bin" ] && [ "${drawio_bin#/}" = "$drawio_bin" ]; then
  drawio_bin=$(command -v "$drawio_bin" 2>/dev/null || true)
fi

if [ -z "$drawio_bin" ]; then
  for candidate in \
    "$(command -v drawio 2>/dev/null || true)" \
    "/Applications/draw.io.app/Contents/MacOS/draw.io" \
    "/Applications/diagrams.net.app/Contents/MacOS/diagrams.net"
  do
    if [ -n "$candidate" ] && [ -x "$candidate" ]; then
      drawio_bin=$candidate
      break
    fi
  done
fi

if [ -z "$drawio_bin" ] || [ ! -x "$drawio_bin" ]; then
  printf '%s\n' 'diagrams-export: draw.io desktop CLI not found.' >&2
  printf '%s\n' 'Set DRAWIO_BIN to the executable path, for example DRAWIO_BIN=/Applications/draw.io.app/Contents/MacOS/draw.io.' >&2
  exit 1
fi

sources=$(find "$root" -type f -name '*.drawio' -not -path "$root/_legacy/*" -not -path '*/renders/*' | sort)
for source in $sources; do
  directory=$(dirname "$source")
  name=$(basename "$source" .drawio)
  output_directory="$directory/renders"
  output_png="$output_directory/$name.png"

  mkdir -p "$output_directory"
  "$drawio_bin" -x -f png -o "$output_png" "$source"
  if [ ! -s "$output_png" ]; then
    printf 'diagrams-export: empty PNG: %s\n' "$output_png" >&2
    exit 1
  fi

  if [ "${DIAGRAM_EXPORT_JPG:-0}" = '1' ]; then
    output_jpg="$output_directory/$name.jpg"
    "$drawio_bin" -x -f jpg -o "$output_jpg" "$source"
    if [ ! -s "$output_jpg" ]; then
      printf 'diagrams-export: empty JPG: %s\n' "$output_jpg" >&2
      exit 1
    fi
  fi

done

printf 'diagrams-export: exported PNG previews from %s.\n' "$drawio_bin"
if [ "${DIAGRAM_EXPORT_JPG:-0}" = '1' ]; then
  printf '%s\n' 'diagrams-export: JPG previews enabled.'
fi
