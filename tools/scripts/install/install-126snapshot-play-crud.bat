set VERSION=1.2.6-SNAPSHOT
call ..\set-play-home-%VERSION%.bat

set MODULE_NAME=crud

call install-play-module-without-jar.bat
