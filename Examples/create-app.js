/*
The script serves to generate CodePushified React Native app to reproduce issues or for testing purposes.

Requirements:
    1. npm i -g react-native-cli
    2. npm i -g code-push-cli
    3. code-push register

Usage: node create-app.js <appName> <reactNativeVersion> <reactNativeCodePushVersion>
    1. node create-app.js 
    2. node create-app.js myapp
    3. node create-app.js myapp react-native@0.47.1 react-native-code-push@5.0.0-beta 
    4. node create-app.js myapp react-native@latest Microsoft/react-native-code-push

Parameters:
    1. <appName> - CodePushDemoAppTest
    2. <reactNativeVersion> - react-native@latest
    3. <reactNativeCodePushVersion> - react-native-code-push@latest
*/

let fs = require('fs');
// let plist = require('plist');
let path = require('path');
let nexpect = require('./nexpect');
let child_proces = require('child_process');
let execSync = child_proces.execSync;

let args = process.argv.slice(2);
let appName = args[0] || 'CodePushDemoAppTest';

if (fs.existsSync(appName)) {
    console.error(`Folder with name "${appName}" already exists! Please delete`);
    process.exit();
}

// Checking if yarn is installed
try {
    execSync('yarn bin');
} catch (err) {
    console.error(`You must install 'yarn' to use this script!`);
    process.exit();
}

let appNameAndroid = `${appName}-android`;
let appNameIOS = `${appName}-ios`;
let owner = null;
let reactNativeVersion = args[1] || `react-native@${execSync('npm view react-native version')}`.trim();
let reactNativeVersionIsLowerThanV049 = isReactNativeVersionLowerThan(49);
let reactNativeCodePushVersion = args[2] || `react-native-code-push@${execSync('npm view react-native-code-push version')}`.trim();

console.log(`App name: ${appName}`);
console.log(`React Native version: ${reactNativeVersion}`);
console.log(`React Native Module for CodePush version: ${reactNativeCodePushVersion} \n`);

let androidStagingDeploymentKey = null;
let iosStagingDeploymentKey = null;



//GENERATE START
createCodePushApp(appNameAndroid, 'Android');
createCodePushApp(appNameIOS, 'iOS');

generatePlainReactNativeApp(appName, reactNativeVersion);
process.chdir(appName);
installCodePush(reactNativeCodePushVersion);
linkCodePush(androidStagingDeploymentKey, iosStagingDeploymentKey);
//GENERATE END



function createCodePushApp(name, os) {
    try {
        console.log(`Creating CodePush app "${name}" to release updates for ${os}...`);
        let app = JSON.parse(execSync(`appcenter apps create -n ${name} -d ${name} -o ${os} -p React-Native --output json`));
        owner = app.owner.name;
        console.log(`App "${name}" has been created \n`);
        execSync(`appcenter codepush deployment add -a ${owner}/${name} Staging`);
    } catch (e) {
        console.log(`App "${name}" already exists \n`);
    }
    let deploymentKeys = JSON.parse(execSync(`appcenter codepush deployment list -a ${owner}/${name} -k --output json`));
    let stagingDeploymentKey = deploymentKeys[0][1];
    console.log(`Deployment key for ${os}: ${stagingDeploymentKey}`);
    console.log(`Use "appcenter codepush release-react ${owner}/${name}" command to release updates for ${os} \n`);

    switch (os) {
        case 'Android':
            androidStagingDeploymentKey = stagingDeploymentKey;
            break;
        case 'iOS':
            iosStagingDeploymentKey = stagingDeploymentKey;
            break;
    }
}

function generatePlainReactNativeApp(appName, reactNativeVersion) {
    console.log(`Installing React Native...`);
    execSync(`react-native init ${appName} --version ${reactNativeVersion}`);
    console.log(`React Native has been installed \n`);
}

function installCodePush(reactNativeCodePushVersion) {
    console.log(`Installing React Native Module for CodePush...`);
    execSync(`yarn add ${reactNativeCodePushVersion}`);
    console.log(`React Native Module for CodePush has been installed \n`);
}

function linkCodePush(androidStagingDeploymentKey, iosStagingDeploymentKey) {
    console.log(`Linking React Native Module for CodePush...`);
    if (isReactNativeVersionLowerThan(60)) {
    nexpect.spawn(`react-native link react-native-code-push`)
        .wait("What is your CodePush deployment key for Android (hit <ENTER> to ignore)")
        .sendline(androidStagingDeploymentKey)
        .wait("What is your CodePush deployment key for iOS (hit <ENTER> to ignore)")
        .sendline(iosStagingDeploymentKey)
        .run(function (err) {
            if (!err) {
                console.log(`React Native Module for CodePush has been linked \n`);
                setupAssets();
            }
            else {
                console.log(err);
            }
        });
    } else {
        androidSetup();
        iosSetup();
        setupAssets();
        console.log(`React Native Module for CodePush has been linked \n`);
    }
}

function setupAssets() {
    let fileToEdit;
    if (reactNativeVersionIsLowerThanV049) {
        fs.unlinkSync('./index.ios.js');
        fs.unlinkSync('./index.android.js');

        fs.writeFileSync('demo.js', fs.readFileSync('../CodePushDemoApp-pre0.49/demo.js'));
        fs.writeFileSync('index.ios.js', fs.readFileSync('../CodePushDemoApp-pre0.49/index.ios.js'));
        fs.writeFileSync('index.android.js', fs.readFileSync('../CodePushDemoApp-pre0.49/index.android.js'));
        fileToEdit = 'demo.js'
    } else {
        fs.writeFileSync('index.js', fs.readFileSync('../CodePushDemoApp/index.js'));
        fs.writeFileSync('App.js', fs.readFileSync('../CodePushDemoApp/App.js'));
        fileToEdit = 'index.js'
    }

    copyRecursiveSync('../CodePushDemoApp/images', './images');

    fs.readFile(fileToEdit, 'utf8', function (err, data) {
        if (err) {
            return console.error(err);
        }
        var result = data.replace(/CodePushDemoApp/g, appName);

        fs.writeFile(fileToEdit, result, 'utf8', function (err) {
            if (err) return console.error(err);

            if (!/^win/.test(process.platform)) {
                optimizeToTestInDebugMode();
                process.chdir('../');
                grantAccess(appName);
            }
            console.log(`\nReact Native app "${appName}" has been generated and CodePushified!`);
            process.exit();
        });
    });
}

function optimizeToTestInDebugMode() {
    let rnXcodeShLocationFolder = 'scripts';
    try {
        let rnVersions = JSON.parse(execSync(`npm view react-native versions --json`));
        let currentRNversion = JSON.parse(fs.readFileSync('./package.json'))['dependencies']['react-native'];
        if (rnVersions.indexOf(currentRNversion) > -1 &&
            rnVersions.indexOf(currentRNversion) < rnVersions.indexOf("0.46.0-rc.0")) {
            rnXcodeShLocationFolder = 'packager';
        }
    } catch(e) {}

    let rnXcodeShPath = `node_modules/react-native/${rnXcodeShLocationFolder}/react-native-xcode.sh`;
    // Replace "if [[ "$PLATFORM_NAME" == *simulator ]]; then" with "if false; then" to force bundling
    execSync(`sed -ie 's/if \\[\\[ "\$PLATFORM_NAME" == \\*simulator \\]\\]; then/if false; then/' ${rnXcodeShPath}`);
    execSync(`perl -i -p0e 's/#ifdef DEBUG.*?#endif/jsCodeLocation = [CodePush bundleURL];/s' ios/${appName}/AppDelegate.m`);
    execSync(`sed -ie 's/targetName.toLowerCase().contains("release")/true/' node_modules/react-native/react.gradle`);
}

function grantAccess(folderPath) {
    execSync('chown -R `whoami` ' + folderPath);
}

function copyRecursiveSync(src, dest) {
    var exists = fs.existsSync(src);
    var stats = exists && fs.statSync(src);
    var isDirectory = exists && stats.isDirectory();
    if (exists && isDirectory) {
        fs.mkdirSync(dest);
        fs.readdirSync(src).forEach(function (childItemName) {
            copyRecursiveSync(path.join(src, childItemName),
                path.join(dest, childItemName));
        });
    } else {
        fs.linkSync(src, dest);
    }
}

function isReactNativeVersionLowerThan(version) {
    if (!reactNativeVersion ||
        reactNativeVersion == "react-native@latest" ||
        reactNativeVersion == "react-native@next")
        return false;

    let reactNativeVersionNumberString = reactNativeVersion.split("@")[1];
    return reactNativeVersionNumberString.split('.')[1] < version;
}

// Configuring android applications for react-native version higher than 0.60
function androidSetup() {
    let buildGradlePath = path.join('android', 'app', 'build.gradle');
    let mainApplicationPath = path.join('android', 'app', 'src', 'main', 'java', 'com', appName, 'MainApplication.java');
    let stringsResourcesPath = path.join('android', 'app', 'src', 'main', 'res', 'values', 'strings.xml');

    let stringsResourcesContent = fs.readFileSync(stringsResourcesPath, "utf8");
    let insertAfterString = "<resources>";
    let deploymentKeyString = `\t<string moduleConfig="true" name="CodePushDeploymentKey">${androidStagingDeploymentKey || "deployment-key-here"}</string>`;
    stringsResourcesContent = stringsResourcesContent.replace(insertAfterString,`${insertAfterString}\n${deploymentKeyString}`);
    fs.writeFileSync(stringsResourcesPath, stringsResourcesContent);

    var buildGradleContents = fs.readFileSync(buildGradlePath, "utf8");
    var reactGradleLink = buildGradleContents.match(/\napply from: ["'].*?react\.gradle["']/)[0];
    var codePushGradleLink = `\napply from: "../../node_modules/react-native-code-push/android/codepush.gradle"`;
    buildGradleContents = buildGradleContents.replace(reactGradleLink,
        `${reactGradleLink}${codePushGradleLink}`);
        fs.writeFileSync(buildGradlePath, buildGradleContents);

    let getJSBundleFileOverride = `
    @Override
    protected String getJSBundleFile(){
        return CodePush.getJSBundleFile();
    }
    `;
    let mainApplicationContents = fs.readFileSync(mainApplicationPath, "utf8");
    let reactNativeHostInstantiation = "new ReactNativeHost(this) {";
    mainApplicationContents = mainApplicationContents.replace(reactNativeHostInstantiation,
        `${reactNativeHostInstantiation}${getJSBundleFileOverride}`);

    let importCodePush = `\nimport com.microsoft.codepush.react.CodePush;`;
    let reactNativeHostInstantiationImport = "import android.app.Application;";
    mainApplicationContents = mainApplicationContents.replace(reactNativeHostInstantiationImport,
        `${reactNativeHostInstantiationImport}${importCodePush}`);
    fs.writeFileSync(mainApplicationPath, mainApplicationContents);
}

// Configuring ios applications for react-native version higher than 0.60
function iosSetup() {
    let plistPath = path.join('ios', appName, 'Info.plist');
    let appDelegatePath = path.join('ios', appName, 'AppDelegate.m');

    let plistContents = fs.readFileSync(plistPath, "utf8");
    // let parsedInfoPlist = plist.parse(plistContents);
    // parsedInfoPlist.CodePushDeploymentKey = iosStagingDeploymentKey || 'deployment-key-here';
    // plistContents = plist.build(parsedInfoPlist);
    // fs.writeFileSync(plistPath, plistContents);
    let falseInfoPlist = `<false/>`;
    let codePushDeploymentKey = iosStagingDeploymentKey || 'deployment-key-here';
    plistContents = plistContents.replace(falseInfoPlist, 
        `${falseInfoPlist}\n\t<key>CodePushDeploymentKey</key>\n\t<string>${codePushDeploymentKey}</string>`);
    fs.writeFileSync(plistPath, plistContents);

    let appDelegateContents = fs.readFileSync(appDelegatePath, "utf8");
    let appDelegateHeaderImportStatement = `#import "AppDelegate.h"`;
    let codePushHeaderImportStatementFormatted = `\n#import <CodePush/CodePush.h>`;
    appDelegateContents = appDelegateContents.replace(appDelegateHeaderImportStatement,
        `${appDelegateHeaderImportStatement}${codePushHeaderImportStatementFormatted}`);
    

    let oldBundleUrl = "[[NSBundle mainBundle] URLForResource:@\"main\" withExtension:@\"jsbundle\"]";
    let codePushBundleUrl = "[CodePush bundleURL]";
    appDelegateContents = appDelegateContents.replace(oldBundleUrl,codePushBundleUrl);
    fs.writeFileSync(appDelegatePath, appDelegateContents);

    execSync(`cd ios && pod install && cd ..`);
}