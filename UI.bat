@echo off

REM Compile all Java files
javac -d bin *.java

REM Delete old logs
rd /S /Q "log"

REM Start Monitor
start "MONITOR" java -cp bin Monitor ui

pause