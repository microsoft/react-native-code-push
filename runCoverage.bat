cd coverageTestApp

IF NOT EXIST "%cd%\node_modules" (
call npm i
)

rmdir /S /Q node_modules\react-native-code-push\android
xcopy  /I /Q ..\android node_modules\react-native-code-push\android

cd android
gradlew :react-native-code-push:build
gradlew :react-native-code-push:coverageReport

cd ..

echo "Coverage report results: file://%cd%/node_modules/react-native-code-push/android/app/build/reports/jacoco/coverageReport/html/com.microsoft.codepush.common/index.html"