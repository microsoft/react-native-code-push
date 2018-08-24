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
let reactNativeVersion = args[1] || `react-native@${execSync('npm view react-native version')}`.trim();
let reactNativeVersionIsLowerThanV049 = isReactNativeVesionLowerThan(49);
let reactNativeCodePushVersion = args[2] || `react-native-code-push@${execSync('npm view react-native-code-push version')}`.trim();

console.log(`App name: ${appName}`);
console.log(`React Native version: ${reactNativeVersion}`);
console.log(`React Native Module for CodePush version: ${reactNativeCodePushVersion} \n`);

let androidStagingDeploymentKey = null;
let iosStagingDeploymentKey = null;



//GENERATE START
createCodePushApp(appNameAndroid, 'android');
createCodePushApp(appNameIOS, 'ios');

generatePlainReactNativeApp(appName, reactNativeVersion);
process.chdir(appName);
installCodePush(reactNativeCodePushVersion);
linkCodePush(androidStagingDeploymentKey, iosStagingDeploymentKey);
//GENERATE END



function createCodePushApp(name, platform) {
    try {
        console.log(`Creating CodePush app "${name}" to release updates for ${platform}...`);
        execSync(`code-push app add ${name} ${platform} react-native`);
        console.log(`App "${name}" has been created \n`);
    } catch (e) {
        console.log(`App "${name}" already exists \n`);
    }
    let deploymentKeys = JSON.parse(execSync(`code-push deployment ls ${name} -k --format json`));
    let stagingDeploymentKey = deploymentKeys[1].key;
    console.log(`Deployment key for ${platform}: ${stagingDeploymentKey}`);
    console.log(`Use "code-push release-react ${name} ${platform}" command to release updates for ${platform} \n`);

    switch (platform) {
        case 'android':
            androidStagingDeploymentKey = stagingDeploymentKey;
            break;
        case 'ios':
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

function isReactNativeVesionLowerThan(version) {
    if (!reactNativeVersion ||
        reactNativeVersion == "react-native@latest" ||
        reactNativeVersion == "react-native@next")
        return false;

    let reactNativeVersionNumberString = reactNativeVersion.split("@")[1];
    return reactNativeVersionNumberString.split('.')[1] < version;
}