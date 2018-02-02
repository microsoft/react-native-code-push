cd coverageTestApp

IF NOT EXIST "%cd%\node_modules" (
call npm i
)

rmdir /S /Q node_modules\react-native-code-push\android
xcopy  /I /Q /E /S ..\android node_modules\react-native-code-push\android /Exclude:%cd%\..\coverageCopyIgnore.txt

cd android
call gradlew :react-native-code-push:clean
call gradlew :react-native-code-push:build
call gradlew :react-native-code-push:coverageReport

cd ..\..

echo "Coverage report results: file://%cd%/coverageTestApp/node_modules/react-native-code-push/android/app/build/reports/jacoco/coverageReport/html/index.html"