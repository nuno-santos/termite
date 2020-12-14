@echo off

if "%TERMITE2_CLI_PATH%"=="" (
    echo "Error: environment variable TERMITE2_CLI_PATH undefined."
	goto end
)

set jline=%TERMITE2_CLI_PATH%\libs\jline-2.13.jar
set commonios=%TERMITE2_CLI_PATH%\libs\commons-io-2.6.jar
set termite2=%TERMITE2_CLI_PATH%\libs\Termite-Cli.jar
set deps="%jline%;%commonios%;%termite2%"
java -cp %deps% main.pt.inesc.termite2.cli.Main %*

:end

