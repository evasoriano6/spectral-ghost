@echo off
set GIT_PATH="C:\Program Files\Git\cmd\git.exe"

echo Adding fixes...
%GIT_PATH% add .
if %errorlevel% neq 0 exit /b %errorlevel%

echo Committing...
%GIT_PATH% commit -m "Fix CI: Add plugin versions and repositories"
if %errorlevel% neq 0 echo Nothing to commit.

echo Pushing...
%GIT_PATH% push origin main
if %errorlevel% neq 0 exit /b %errorlevel%

echo DONE.
