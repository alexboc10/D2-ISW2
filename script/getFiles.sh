#!/bin/bash

# shellcheck disable=SC2062
# shellcheck disable=SC2162
# shellcheck disable=SC2164
cd /home/alex/code/ISW2/"$1"
git ls-tree --name-only -r "$2" | grep [.]java | while read filename; do
  echo "$filename"END
done