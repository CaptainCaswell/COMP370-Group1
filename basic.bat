@echo off

REM Compile all Java files
javac -d bin *.java

REM Start Monitor
start "MONITOR" java -cp bin Monitor

REM Start clients
start "CLIENT1" java -cp bin Client 1

REM Start first server with titled window
start "SERVER1" java -cp bin Server 2000

pause