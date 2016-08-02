var fs = require("fs");
var glob = require("glob");
var inquirer = require('inquirer');
var path = require("path");
var plist = require("plist");

var ignoreNodeModules = { ignore: "node_modules/**" };
var appDelegatePath = glob.sync("**/AppDelegate.m", ignoreNodeModules)[0];
// Glob only allows foward slashes in patterns: https://www.npmjs.com/package/glob#windows
var plistPath = glob.sync(path.join(path.dirname(appDelegatePath), "*Info.plist").replace(/\\/g, "/"), ignoreNodeModules)[0];

var appDelegateContents = fs.readFileSync(appDelegatePath, "utf8");
var plistContents = fs.readFileSync(plistPath, "utf8");

// 1. Add the header import statement
var codePushHeaderImportStatement = `#import "CodePush.h"`;
if (~appDelegateContents.indexOf(codePushHeaderImportStatement)) {
    console.log(`"CodePush.h" header already imported.`);
} else {
    var appDelegateHeaderImportStatement = `#import "AppDelegate.h"`;
    appDelegateContents = appDelegateContents.replace(appDelegateHeaderImportStatement,
        `${appDelegateHeaderImportStatement}\n${codePushHeaderImportStatement}`);
}

// 2. Modify jsCodeLocation value assignment
var oldJsCodeLocationAssignmentStatement = appDelegateContents.match(/(jsCodeLocation = .*)\n/)[1];
var newJsCodeLocationAssignmentStatement = "jsCodeLocation = [CodePush bundleURL];";
if (~appDelegateContents.indexOf(newJsCodeLocationAssignmentStatement)) {
    console.log(`"jsCodeLocation" already pointing to "[CodePush bundleURL]".`);
} else {
    var jsCodeLocationPatch = `
#ifdef DEBUG
    ${oldJsCodeLocationAssignmentStatement}
#else
    ${newJsCodeLocationAssignmentStatement}
#endif`;
    appDelegateContents = appDelegateContents.replace(oldJsCodeLocationAssignmentStatement,
        jsCodeLocationPatch);
}

// 3. Add CodePushDeploymentKey to plist file
var parsedInfoPlist = plist.parse(plistContents);
if (parsedInfoPlist.CodePushDeploymentKey) {
    console.log(`"CodePushDeploymentKey" already specified in the plist file.`);
    writePatches();
} else {
    inquirer.prompt({
        "type": "input",
        "name": "iosDeploymentKey",
        "message": "What is your CodePush deployment key for iOS (hit <ENTER> to ignore)"
    }).then(function(answer) {
        parsedInfoPlist.CodePushDeploymentKey = answer.iosDeploymentKey || "deployment-key-here";
        plistContents = plist.build(parsedInfoPlist);

        writePatches();
    });
}

function writePatches() {
    fs.writeFileSync(appDelegatePath, appDelegateContents);
    fs.writeFileSync(plistPath, plistContents);
}