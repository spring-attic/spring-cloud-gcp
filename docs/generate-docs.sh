#!/bin/bash -x

# Something in this stack doesn't work with Java 11.
#
# $ update-java-alternatives --list
# $ sudo update-java-alternatives --set $JAVA_8_JDK
#
# Example:
# $ sudo update-java-alternatives -s java-1.8.0-openjdk-amd64
#
# Don't forget to set it back!

# This command regenerates the docs after editing.
# You'll have to run this after editing the src/main/asciidoc/README.adoc

../mvnw clean install -Pdocs
