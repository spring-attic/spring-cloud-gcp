#!/bin/bash
#
# Finds modified/added files with outdated license year and updates to current
# year.
# Pass in a commit hash or branch to find files changed relative to it.

baseline=${1:-HEAD}
cur_year=$(date +'%Y');
echo "Updating year to ${cur_year} on files different from ${baseline}"

git diff --name-only --diff-filter=ACMR $baseline -- '*.java' | \
  xargs --no-run-if-empty grep --files-without-match "^ \* Copyright ....-$cur_year" | \
  xargs --no-run-if-empty sed -i -e "s/^\( \* Copyright ....\)-..../\1-$cur_year/"
