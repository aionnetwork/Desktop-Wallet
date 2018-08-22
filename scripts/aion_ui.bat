::[Bat To Exe Converter]
::
::YAwzoRdxOk+EWAjk
::fBw5plQjdCyDJFqd81U5Jk0AcAyNMW6GBbAS/Pri/Na3sEIXUeEra7Pa07uAH/Ua1lb2dqog13NUpNkJHxRNbBGufTM7u2l+t22KOfuLsgPtT1y180IMGGp5l2zeiSUvc+9hmcwNwBy/9ULxoKwT3nbAd5taKmrizqImMcoPnQ==
::YAwzuBVtJxjWCl3EqQJgSA==
::ZR4luwNxJguZRRnk
::Yhs/ulQjdF+5
::cxAkpRVqdFKZSjk=
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
::Zh4grVQjdCyDJFqd81U5Jk0AcAyNMW6GBbAS/Pri/Na3sEIXUeEra7Pa07uAH/Ua1lbnZ589wmlmucIDAixZch6uekExsWsi
::YB416Ek+ZG8=
::
::
::978f952a14a936cc963da21a135fa983
ECHO OFF
set CLASSPATH="%cd%\mod\*;%cd%\lib\*"
set STORAGE_DIR="%USERPROFILE%\.aion"

java -Dfile.encoding=UTF-8 -Dlocal.storage.dir=%STORAGE_DIR% -classpath %CLASSPATH% -Xms300m -Xmx500m org.aion.wallet.WalletApplication
