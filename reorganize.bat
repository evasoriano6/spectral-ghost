@echo off
echo Creating standard directories...
mkdir src\main\java\com\spectral\ghost
mkdir src\main\res

echo Moving source code...
move ui src\main\java\com\spectral\ghost\ui
move data src\main\java\com\spectral\ghost\data
move domain src\main\java\com\spectral\ghost\domain
move di src\main\java\com\spectral\ghost\di

echo Moving resources...
xcopy res src\main\res /E /I /Y
rmdir /S /Q res

echo Moving manifest...
move AndroidManifest.xml src\main\

echo DONE.
