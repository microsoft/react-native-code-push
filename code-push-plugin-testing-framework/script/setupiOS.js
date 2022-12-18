import path from "path";

import * as TestConfig from "./testConfig";
import { TestUtil } from "./testUtil";

const projectDirectory = TestConfig.testRunDirectory;
const iOSProject = path.join(projectDirectory, TestConfig.TestAppName, "ios");
const infoPlistPath = path.join(iOSProject, TestConfig.TestAppName, "Info.plist");
const appDelegatePath = path.join(iOSProject, TestConfig.TestAppName, "AppDelegate.m");

// Install the Podfile
return TestUtil.getProcessOutput("pod install", { cwd: iOSProject })
    // Put the IOS deployment key in the Info.plist
    .then(TestUtil.replaceString.bind(undefined, infoPlistPath,
        "</dict>\n</plist>",
        "<key>CodePushDeploymentKey</key>\n\t<string>" + this.getDefaultDeploymentKey() + "</string>\n\t<key>CodePushServerURL</key>\n\t<string>" + this.getServerUrl() + "</string>\n\t</dict>\n</plist>"))
    // Set the app version to 1.0.0 instead of 1.0 in the Info.plist
    .then(TestUtil.replaceString.bind(undefined, infoPlistPath, "1.0", "1.0.0"))
    // Fix the linker flag list in project.pbxproj (pod install adds an extra comma)
    .then(TestUtil.replaceString.bind(undefined, path.join(iOSProject, TestConfig.TestAppName + ".xcodeproj", "project.pbxproj"),
        "\"[$][(]inherited[)]\",\\s*[)];", "\"$(inherited)\"\n\t\t\t\t);"))
    // Add the correct bundle identifier
    .then(TestUtil.replaceString.bind(undefined, path.join(iOSProject, TestConfig.TestAppName + ".xcodeproj", "project.pbxproj"),
        "PRODUCT_BUNDLE_IDENTIFIER = [^;]*", "PRODUCT_BUNDLE_IDENTIFIER = \"" + TestConfig.TestNamespace + "\""))
    // Copy the AppDelegate.m to the project
    .then(TestUtil.copyFile.bind(undefined,
        path.join(TestConfig.templatePath, "ios", TestConfig.TestAppName, "AppDelegate.m"),
        appDelegatePath, true))
    .then(TestUtil.replaceString.bind(undefined, appDelegatePath, TestUtil.CODE_PUSH_TEST_APP_NAME_PLACEHOLDER, TestConfig.TestAppName));