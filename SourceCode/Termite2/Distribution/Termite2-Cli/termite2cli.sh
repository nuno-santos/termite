#!/bin/bash

if [ -z "$TERMITE2_CLI_PATH" ]
then 
	echo "Error: environment variable TERMITE2_CLI_PATH undefined."
	exit -1
fi

jline=$TERMITE2_CLI_PATH/libs/jline-2.13.jar
commonios=$TERMITE2_CLI_PATH/libs/commons-io-2.6.jar
termite2=$TERMITE2_CLI_PATH/libs/Termite2-Cli.jar
deps="$jline:$commonios:$termite2"

java -cp $deps main.pt.inesc.termite2.cli.Main $@
