@echo off

javac ServerProcess.java
javac Client.java

start java ServerProcess

timeout /t 1

:loop

start java Client

pause

goto loop

echo --- Program Done ---

pause