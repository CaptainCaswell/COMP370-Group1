@echo off

REM Compile all Java files
javac -d bin Server.java Client.java Monitor.java Logger.java

REM Start Monitor
start "MONITOR" java -cp bin Monitor

REM Start clients
start "CLIENT1" java -cp bin Client 1
start "CLIENT2" java -cp bin Client 2

REM Start first server with titled window
start "SERVER1" java -cp bin Server 2000

REM Wait 5 seconds
timeout /t 5

REM Start additional servers
start "SERVER2"  java -cp bin Server 3000
start "SERVER3"  java -cp bin Server 4000

pause