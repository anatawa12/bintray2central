
.\gradlew jpackage

pushd .\build\jpackage

Compress-Archive -Path .\bintray2central -DestinationPath .\bintray2central.zip

Remove-Item -Path .\bintray2central -Recurse -Force

popd

New-Item -Path "." -Name "built" -ItemType "directory"

Move-Item .\build\jpackage\* .\built\
