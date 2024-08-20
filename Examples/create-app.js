/*
The script serves to generate CodePushified React Native app to reproduce issues or for testing purposes.

NOTE: Use CodePushDemoApp and CodePushDemoAppCpp as reference how to implement CodePush in your app. For creating a working demo app
follow steps bellow.

Requirements:
    1. npm i -g react-native-cli
    2. npm i -g appcenter-cli
    3. appcenter login
    4. If you use this script on macOS for react-native v0.60+ then you need to have CocoaPods installed. Use this command to (re)install CocoaPods:
        sudo gem install cocoapods -n /usr/local/bin

Usage: node create-app.js <appName> <reactNativeVersion> <reactNativeCodePushVersion>
    1. node create-app.js 
    2. node create-app.js myapp
    3. node create-app.js myapp react-native@0.62 react-native-code-push@6.1.0 
    4. node create-app.js myapp react-native@latest Microsoft/react-native-code-push

Parameters:
    1. <appName> - CodePushDemoAppTest
    2. <reactNativeVersion> - react-native@latest
    3. <reactNativeCodePushVersion> - react-native-code-push@latest
*/

const fs = require('fs');
const path = require('path');
const nexpect = require('./nexpect');
const child_process = require('child_process');
const execSync = child_process.execSync;

const args = process.argv.slice(2);
const appName = args[0] || 'CodePushDemoAppTest';

if (fs.existsSync(appName)) {
    console.error(`Folder with name "${appName}" already exists! Please delete`);
    process.exit();
}


const appNameAndroid = `${appName}-android`;
const appNameIOS = `${appName}-ios`;
let owner = null;
const reactNativeVersion = args[1] || `react-native@${execCommand('npm view react-native version')}`.trim();
const reactNativeVersionIsLowerThanV049 = isReactNativeVersionLowerThan(49);
const reactNativeCodePushVersion = args[2] || `react-native-code-push@${execCommand('npm view react-native-code-push version')}`.trim();

const reactNativeVersionIsLowerThanV073 = isReactNativeVersionLowerThan(73)
if (!isReactNativeVersionLowerThan(60) && process.platform === "darwin") {
    try {
        console.log("Verify that CocoaPods installed");
        execCommand("pod --version");
        console.log("CocoaPods has installed");
    } catch {
        console.error(`'CocoaPods' are required to run the script, you can install it with\n'sudo gem install cocoapods -n /usr/local/bin'\ncommand`);
        process.exit();
    }
}

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
        try {
            const appResult = execCommand(`appcenter apps create -d ${name} -n ${name} -o ${os} -p React-Native --output json`);
            const app = JSON.parse(appResult);
            owner = app.owner.name;
            console.log(`App "${name}" has been created \n`);
        } catch (e) {
            console.error(`Error: Unable to create CodePush app. Please check that you haven't application with "${name}" name on portal.`,);
            console.error("Error: ", e.toString());
        }
        execCommand(`appcenter codepush deployment add -a ${owner}/${name} Staging`);
    } catch (e) {
        console.error("Error", e.toString());
    }

    try {
        const deploymentKeysResult = execCommand(`appcenter codepush deployment list -a ${owner}/${name} -k --output json`);
        const deploymentKeys = JSON.parse(deploymentKeysResult);
        const stagingDeploymentKey = deploymentKeys[0][1];
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
    } catch (e) {
        console.error("Error: Unable to load deployment keys");
        console.error("Error: ", e.toString());
    }

}

function generatePlainReactNativeApp(appName, reactNativeVersion) {
    console.log(`Installing React Native...`);
    execCommand(`react-native init ${appName} --version ${reactNativeVersion.split('@')[1]}`);
    console.log(`React Native has been installed \n`);
}

function installCodePush(reactNativeCodePushVersion) {
    console.log(`Installing React Native Module for CodePush...`);
    execCommand(`npm i ${reactNativeCodePushVersion}`);
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
        if (process.platform === 'darwin') {
            iosSetup();
        } else {
            console.log('Your OS is not "Mac OS" so the iOS application will not be configured')
        }
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
        fs.writeFileSync('index.js', fs.readFileSync(__dirname + '/CodePushDemoApp/index.js'));
        fs.writeFileSync('App.js', fs.readFileSync(__dirname + '/CodePushDemoApp/App.js'));
        fileToEdit = 'index.js'
    }

    copyRecursiveSync(__dirname + '/CodePushDemoApp/images', './images');

    fs.readFile(fileToEdit, 'utf8', function (err, data) {
        if (err) {
            return console.error(err);
        }
        const result = data.replace(/CodePushDemoApp/g, appName);

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
    const rnXcodeShLocationFolder = 'scripts';
    try {
        const rnVersions = JSON.parse(execCommand(`npm view react-native versions --json`));
        const currentRNversion = JSON.parse(fs.readFileSync('./package.json'))['dependencies']['react-native'];
        if (rnVersions.indexOf(currentRNversion) > -1 &&
            rnVersions.indexOf(currentRNversion) < rnVersions.indexOf("0.46.0-rc.0")) {
            rnXcodeShLocationFolder = 'packager';
        }
    } catch (e) { }

    const rnXcodeShPath = `node_modules/react-native/${rnXcodeShLocationFolder}/react-native-xcode.sh`;
    // Replace "if [[ "$PLATFORM_NAME" == *simulator ]]; then" with "if false; then" to force bundling
    execCommand(`sed -ie 's/if \\[\\[ "\$PLATFORM_NAME" == \\*simulator \\]\\]; then/if false; then/' ${rnXcodeShPath}`);
    execCommand(`perl -i -p0e 's/#ifdef DEBUG.*?#endif/jsCodeLocation = [CodePush bundleURL];/s' ios/${appName}/${getAppDelegateName()}`);
    reactNativeVersionIsLowerThanV073 && execCommand(`sed -ie 's/targetName.toLowerCase().contains("release")/true/' node_modules/react-native/react.gradle`);
}

function grantAccess(folderPath) {
    execCommand('chown -R `whoami` ' + folderPath);
}

function copyRecursiveSync(src, dest) {
    const exists = fs.existsSync(src);
    const stats = exists && fs.statSync(src);
    const isDirectory = exists && stats.isDirectory();
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

    const reactNativeVersionNumberString = reactNativeVersion.split("@")[1];
    return reactNativeVersionNumberString.split('.')[1] < version;
}

// Configuring android applications for react-native version higher than 0.60
function androidSetup() {
    const buildGradlePath = path.join('android', 'app', 'build.gradle');
    const settingsGradlePath = path.join('android', 'settings.gradle');
    const mainApplicationType = reactNativeVersionIsLowerThanV073 ? 'java' : 'kt';
    const mainApplicationPath = path.join('android', 'app', 'src', 'main', 'java', 'com', appName, `MainApplication.${mainApplicationType}`);
    const stringsResourcesPath = path.join('android', 'app', 'src', 'main', 'res', 'values', 'strings.xml');

    let stringsResourcesContent = fs.readFileSync(stringsResourcesPath, "utf8");
    const insertAfterString = "<resources>";
    const deploymentKeyString = `\t<string moduleConfig="true" name="CodePushDeploymentKey">${androidStagingDeploymentKey || "deployment-key-here"}</string>`;
    stringsResourcesContent = stringsResourcesContent.replace(insertAfterString, `${insertAfterString}\n${deploymentKeyString}`);
    fs.writeFileSync(stringsResourcesPath, stringsResourcesContent);

    let buildGradleContents = fs.readFileSync(buildGradlePath, "utf8");
    const reactGradleLink = buildGradleContents.match(/\napply from: ["'].*?react\.gradle["']/);
    const codePushGradleLink = `\napply from: "../../node_modules/react-native-code-push/android/codepush.gradle"`;
    if (reactGradleLink != null) {
        buildGradleContents = buildGradleContents.replace(reactGradleLink[0],
            `${reactGradleLink[0]}${codePushGradleLink}`);
        fs.writeFileSync(buildGradlePath, buildGradleContents);
    }
    // react.gradle script removed from 0.71 thus this workaround
    else {
        const appPluginLastLine = buildGradleContents.match(/apply plugin: "com.facebook.react"/)[0];
        buildGradleContents = buildGradleContents.replace(appPluginLastLine, `${appPluginLastLine}${codePushGradleLink}`)
        fs.writeFileSync(buildGradlePath, buildGradleContents);
    }

    let settingsGradleContents = fs.readFileSync(settingsGradlePath, "utf8");
    const settingsGradleInclude = "include \':app\'";
    const codePushProjectImport = `':react-native-code-push'\nproject(':react-native-code-push').projectDir = new File(rootProject.projectDir, '../node_modules/react-native-code-push/android/app')`;
    settingsGradleContents = settingsGradleContents.replace(settingsGradleInclude,
        `${settingsGradleInclude}, ${codePushProjectImport}`);
    fs.writeFileSync(settingsGradlePath, settingsGradleContents);

    let importCodePush = `\nimport com.microsoft.codepush.react.CodePush;`;
    let reactNativeHostInstantiationImport = "import android.app.Application;";
    let mainApplicationContents = fs.readFileSync(mainApplicationPath, "utf8");
    let getJSBundleFileOverride = "";
    let reactNativeHostInstantiation = "";

    // handle react-native version with java
    if (reactNativeVersionIsLowerThanV073) {
        reactNativeHostInstantiation = "new ReactNativeHost(this) {";

        getJSBundleFileOverride = `
        @Override
        protected String getJSBundleFile(){
            return CodePush.getJSBundleFile();
        }
        `;
    }
    // handle react-native version with kotlin
    else {
        reactNativeHostInstantiation = "object : DefaultReactNativeHost(this) {"
        getJSBundleFileOverride = `
         override fun getJSBundleFile(): String {
            return CodePush.getJSBundleFile() 
        }
        `;
        importCodePush = importCodePush.replace(';', '');
        reactNativeHostInstantiationImport = reactNativeHostInstantiationImport.replace(';', '');
    }
    mainApplicationContents = mainApplicationContents.replace(reactNativeHostInstantiation,
        `${reactNativeHostInstantiation}${getJSBundleFileOverride}`);

    mainApplicationContents = mainApplicationContents.replace(reactNativeHostInstantiationImport,
        `${reactNativeHostInstantiationImport}${importCodePush}`);
    fs.writeFileSync(mainApplicationPath, mainApplicationContents);
}

function getAppDelegateName() { return fs.readdirSync(path.join('ios', appName)).find(file => file === ('AppDelegate.mm') || file === 'AppDelegate.m') };

// Configuring ios applications for react-native version higher than 0.60
function iosSetup() {
    const plistPath = path.join('ios', appName, 'Info.plist');

    const appDelegatePath = path.join('ios', appName, getAppDelegateName());

    let plistContents = fs.readFileSync(plistPath, "utf8");

    const dictPlistTag = `</dict>\n</plist>`;

    const codePushDeploymentKey = iosStagingDeploymentKey || 'deployment-key-here';
    plistContents = plistContents.replace(dictPlistTag,
        `\t<key>CodePushDeploymentKey</key>\n\t<string>${codePushDeploymentKey}</string>${dictPlistTag}`);
    fs.writeFileSync(plistPath, plistContents);

    let appDelegateContents = fs.readFileSync(appDelegatePath, "utf8");
    const appDelegateHeaderImportStatement = `#import "AppDelegate.h"`;
    const codePushHeaderImportStatementFormatted = `\n#import <CodePush/CodePush.h>`;
    appDelegateContents = appDelegateContents.replace(appDelegateHeaderImportStatement,
        `${appDelegateHeaderImportStatement}${codePushHeaderImportStatementFormatted}`);


    const oldBundleUrl = "[[NSBundle mainBundle] URLForResource:@\"main\" withExtension:@\"jsbundle\"]";
    const codePushBundleUrl = "[CodePush bundleURL]";
    appDelegateContents = appDelegateContents.replace(oldBundleUrl, codePushBundleUrl);
    fs.writeFileSync(appDelegatePath, appDelegateContents);
    execCommand(`cd ios && pod install && cd ..`);
}

function execCommand(command) {
    console.log(`\n\x1b[2m${command}\x1b[0m\n`);
    const result = execSync(command).toString();
    return result;
}
