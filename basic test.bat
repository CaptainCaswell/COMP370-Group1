@echo off

REM Compile all Java files
javac Server.java
javac Client.java
javac Monitor.java
javac Logger.java

REM Delete old logs
rd /S /Q "log"

REM Start Monitor
start "MONITOR" java Monitor

REM Start clients
start "CLIENT1" java Client

REM Start first server with titled window
start "SERVER1" java ServerProcess 2000

pause