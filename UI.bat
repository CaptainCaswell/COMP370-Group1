@echo off

REM Compile all Java files
javac -d bin Server.java Client.java Monitor.java Logger.java MonitorUI.java

REM Delete old logs
rd /S /Q "log"

REM Start Monitor
start "MONITOR" java -cp bin Monitor

pause