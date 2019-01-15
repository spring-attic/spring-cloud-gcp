#!/bin/sh
#
# Finds modified/added files with outdated license year and updates to current
# year.

cur_year=$(date +'%Y');
echo "Updating year to ${cur_year}"

git diff --name-only HEAD -- '*.java' | \
  xargs grep --files-without-match "^ \* Copyright ....-$cur_year" | \
  xargs sed -i -e "s/^\( \* Copyright ....\)-..../\1-$cur_year/"
