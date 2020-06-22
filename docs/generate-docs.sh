#!/bin/bash -x

# This command regenerates the docs after editing.
# You'll have to run this after editing the src/main/asciidoc/README.adoc

../mvnw clean install -Pdocs
