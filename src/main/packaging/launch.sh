#!/bin/sh

EXEC=$(which java)
[ -d "$JAVA_HOME" ] && EXEC=$JAVA_HOME/bin/java
exec $EXEC -jar "$0" "$@"
