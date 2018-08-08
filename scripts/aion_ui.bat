ECHO OFF
set CLASSPATH="%cd%\mod\*;%cd%\lib\*"
set STORAGE_DIR="%USERPROFILE%\.aion"

"%JAVA_HOME%\bin\java" -Dfile.encoding=UTF-8 -Dlocal.storage.dir=%STORAGE_DIR% -classpath %CLASSPATH% -Xms300m -Xmx500m org.aion.wallet.WalletApplication
