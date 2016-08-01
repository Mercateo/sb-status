#!/bin/sh

EXEC=$(which java 2>&1 >/dev/null)
[ -d "$JAVA_HOME" ] && EXEC=$JAVA_HOME/bin/java
exec $EXEC -jar "$0" "$@"
