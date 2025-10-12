#!/usr/bin/env sh
# find all subdirectories of the script's directory
SCRIPT_DIR=$(dirname "$0")
find "$SCRIPT_DIR" -type d | while read -r dir; do
  # find all *.j2 file in directory
  find "$dir" -maxdepth 1 -type f -name '*.j2' | while read -r j2file; do
    filename=$(basename "$j2file" .j2)
    foldername=$(basename "$dir")
    # normalize output path $SCRIPT_DIR/../src/resources/rules/$foldername-${filename}

    jinja2 "$j2file" "$dir/values.yaml" -o "$SCRIPT_DIR/../src/main/resources/rules/_${foldername}-${filename}"
  done
done
