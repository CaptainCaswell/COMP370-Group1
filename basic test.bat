@echo off

REM Compile all Java files
javac -d bin Server.java Client.java Monitor.java Logger.java

REM Delete old logs
rd /S /Q "log"

REM Start Monitor
start "MONITOR" java -cp bin Monitor

REM Start clients
start "CLIENT1" java -cp bin Client 1

REM Start first server with titled window
start "SERVER1" java -cp bin Server 2000

pause