@echo off
REM Build (if needed) and launch the Maze Game. Double-click or run from cmd; re-run any time.
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0run.ps1" %*
