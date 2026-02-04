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
start "CLIENT2" java Client

REM Start first server with titled window
start "SERVER1" java ServerProcess 2000

REM Wait 5 seconds
timeout /t 5

REM Start additional servers
start "SERVER2"  java ServerProcess 3000
start "SERVER3"  java ServerProcess 4000

pause