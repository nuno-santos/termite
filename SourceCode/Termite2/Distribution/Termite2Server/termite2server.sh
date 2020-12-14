#!/bin/bash

if [ -z "$TERMITE2_SERVER_PATH" ]
then 
	echo "Error: environment variable TERMITE2_SERVER_PATH undefined."
	exit -1
fi

jline=$TERMITE2_SERVER_PATH/libs/jline-2.13.jar
termite2server=$TERMITE2_SERVER_PATH/libs/Termite2Server.jar
deps="$jline:$termite2server"

java -cp $deps pt.inesc.termite.server.Main $@
