@echo off
set GIT_PATH="C:\Program Files\Git\cmd\git.exe"

echo Initializing repository...
%GIT_PATH% init
if %errorlevel% neq 0 exit /b %errorlevel%

echo Adding files...
%GIT_PATH% add .
if %errorlevel% neq 0 exit /b %errorlevel%

echo Configuring user identity...
%GIT_PATH% config user.email "lauraleomilan@gmail.com"
%GIT_PATH% config user.name "Spectral Ghost User"

echo Committing...
%GIT_PATH% commit -m "Initial commit - SPECTRAL-01 v1.0"
if %errorlevel% neq 0 echo Commit failed or nothing to commit. Continuing...

echo Renaming branch to main...
%GIT_PATH% branch -M main
if %errorlevel% neq 0 exit /b %errorlevel%

echo Adding remote...
%GIT_PATH% remote add origin https://github.com/evasoriano6/spectral-ghost.git
if %errorlevel% neq 0 echo Remote might already exist. Continuing...

echo Pushing to GitHub...
%GIT_PATH% push -u origin main
if %errorlevel% neq 0 exit /b %errorlevel%

echo DONE.
