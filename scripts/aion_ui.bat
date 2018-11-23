::[Bat To Exe Converter]
::
::YAwzoRdxOk+EWAjk
::fBw5plQjdCyDJGmW+0g1Kw9HcAWLM2WFE7wg+/r40+eGq0MhRucsd5rJ2bGdHO8B7XnlfJkj6m1blMcJGCdNdy6oYQIkpmBHuHCWC86fvAHydkmA6UUPEmZ7iVz5vn8EadBnlI0K0C/e
::YAwzuBVtJxjWCl3EqQJgSA==
::ZR4luwNxJguZRRnk
::Yhs/ulQjdF+5
::cxAkpRVqdFKZSzk=
::cBs/ulQjdF+5
::ZR41oxFsdFKZSDk=
::eBoioBt6dFKZSDk=
::cRo6pxp7LAbNWATEpCI=
::egkzugNsPRvcWATEpCI=
::dAsiuh18IRvcCxnZtBJQ
::cRYluBh/LU+EWAnk
::YxY4rhs+aU+JeA==
::cxY6rQJ7JhzQF1fEqQJQ
::ZQ05rAF9IBncCkqN+0xwdVs0
::ZQ05rAF9IAHYFVzEqQJQ
::eg0/rx1wNQPfEVWB+kM9LVsJDGQ=
::fBEirQZwNQPfEVWB+kM9LVsJDGQ=
::cRolqwZ3JBvQF1fEqQJQ
::dhA7uBVwLU+EWDk=
::YQ03rBFzNR3SWATElA==
::dhAmsQZ3MwfNWATElA==
::ZQ0/vhVqMQ3MEVWAtB9wSA==
::Zg8zqx1/OA3MEVWAtB9wSA==
::dhA7pRFwIByZRRnk
::Zh4grVQjdCyDJGmW+0g1Kw9HcAWLM2WFE7wg++vp5vqTsXE8Xe0xT47X1rGabuUL7yU=
::YB416Ek+ZG8=
::
::
::978f952a14a936cc963da21a135fa983

REM This file is used to generate an executable file with the bat2exe tool

@ECHO OFF
set CLASSPATH="%cd%\mod\*;%cd%\lib\*"
set STORAGE_DIR="%USERPROFILE%\.aion"
set LOG_DIR="%STORAGE_DIR%\log"

set LOG_FILE_SUFFIX="%date%__%time:~0,2%_%time:~3,2%_%time:~6,2%"

mkdir "%LOG_DIR%"

"java\bin\java.exe" -Dfile.encoding=UTF-8 -Dlocal.storage.dir="%STORAGE_DIR%" -classpath %CLASSPATH% -Xms300m -Xmx500m org.aion.wallet.WalletApplication > "%LOG_DIR%\log_%LOG_FILE_SUFFIX%" 2>&1
