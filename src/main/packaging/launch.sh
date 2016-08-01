#!/bin/sh

EXEC=$(which java 2>/dev/null)
[ -d "$JAVA_HOME" ] && EXEC=$JAVA_HOME/bin/java
exec $EXEC -jar "$0" "$@"
