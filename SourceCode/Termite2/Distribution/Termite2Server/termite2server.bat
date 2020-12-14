@echo off

if "%TERMITE2_SERVER_PATH%"=="" (
    echo "Error: environment variable TERMITE2_SERVER_PATH undefined."
	goto end
)

set jline=%TERMITE2_SERVER_PATH%\libs\jline-2.13.jar
set termite2server=%TERMITE2_SERVER_PATH%\libs\Termite2Server.jar
set deps="%jline%;%termite2server%"
java -cp %deps% pt.inesc.termite.server.Main %*

:end