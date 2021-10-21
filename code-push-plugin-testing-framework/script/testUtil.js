"use strict";
var archiver = require("archiver");
var child_process = require("child_process");
var fs = require("fs");
var replace = require("replace");
var Q = require("q");
var TestUtil = (function () {
    function TestUtil() {
    }
    //// Command Line Input Functions
    /**
     * Reads a command line option passed to mocha and returns a default if unspecified.
     */
    TestUtil.readMochaCommandLineOption = function (optionName, defaultValue) {
        var optionValue = undefined;
        for (var i = 0; i < process.argv.length; i++) {
            if (process.argv[i] === optionName) {
                if (i + 1 < process.argv.length) {
                    optionValue = process.argv[i + 1];
                }
                break;
            }
        }
        if (!optionValue)
            optionValue = defaultValue;
        return optionValue;
    };
    /**
     * Reads command line options passed to mocha.
     */
    TestUtil.readMochaCommandLineFlag = function (optionName) {
        for (var i = 0; i < process.argv.length; i++) {
            if (process.argv[i] === optionName) {
                return true;
            }
        }
        return false;
    };
    //// Utility Functions
    /**
     * Executes a child process and returns a promise that resolves with its output or rejects with its error.
     */
    TestUtil.getProcessOutput = function (command, options) {
        var deferred = Q.defer();
        options = options || {};
        // set default options
        if (options.maxBuffer === undefined)
            options.maxBuffer = 1024 * 1024 * 500;
        if (options.timeout === undefined)
            options.timeout = 10 * 60 * 1000;
        if (!options.noLogCommand)
            console.log("Running command: " + command);
        var execProcess = child_process.exec(command, options, function (error, stdout, stderr) {
            if (error) {
                if (!options.noLogStdErr)
                    console.error("" + error);
                deferred.reject(error);
            }
            else {
                deferred.resolve(stdout.toString());
            }
        });
        if (!options.noLogStdOut)
            execProcess.stdout.pipe(process.stdout);
        if (!options.noLogStdErr)
            execProcess.stderr.pipe(process.stderr);
        execProcess.on('error', function (error) {
            if (!options.noLogStdErr)
                console.error("" + error);
            deferred.reject(error);
        });
        return deferred.promise;
    };
    /**
     * Returns the name of the plugin that is being tested.
     */
    TestUtil.getPluginName = function () {
        var packageFile = JSON.parse(fs.readFileSync("./package.json", "utf8"));
        return packageFile.name;
    };

    TestUtil.getPluginVersion = function () {
        var packageFile = JSON.parse(fs.readFileSync("./package.json", "utf8"));
        return packageFile.version;
    };
    /**
     * Replaces a regex in a file with a given string.
     */
    TestUtil.replaceString = function (filePath, regex, replacement) {
        console.log("replacing \"" + regex + "\" with \"" + replacement + "\" in " + filePath);
        replace({ regex: regex, replacement: replacement, recursive: false, silent: true, paths: [filePath] });
    };
    /**
     * Copies a file from a given location to another.
     */
    TestUtil.copyFile = function (source, destination, overwrite) {
        var deferred = Q.defer();
        try {
            var errorHandler = function (error) {
                deferred.reject(error);
            };
            if (overwrite && fs.existsSync(destination)) {
                fs.unlinkSync(destination);
            }
            var readStream = fs.createReadStream(source);
            readStream.on("error", errorHandler);
            var writeStream = fs.createWriteStream(destination);
            writeStream.on("error", errorHandler);
            writeStream.on("close", deferred.resolve.bind(undefined, undefined));
            readStream.pipe(writeStream);
        }
        catch (e) {
            deferred.reject(e);
        }
        return deferred.promise;
    };
    /**
     * Archives the contents of sourceFolder and puts it in an archive at archivePath in targetFolder.
     */
    TestUtil.archiveFolder = function (sourceFolder, targetFolder, archivePath, isDiff) {
        var deferred = Q.defer();
        var archive = archiver.create("zip", {});
        console.log("Creating an update archive at: " + archivePath);
        if (fs.existsSync(archivePath)) {
            fs.unlinkSync(archivePath);
        }
        var writeStream = fs.createWriteStream(archivePath);
        writeStream.on("close", function () {
            deferred.resolve(archivePath);
        });
        archive.on("error", function (e) {
            deferred.reject(e);
        });
        if (isDiff) {
            archive.append("{\"deletedFiles\":[]}", { name: "hotcodepush.json" });
        }
        archive.directory(sourceFolder, targetFolder);
        archive.pipe(writeStream);
        archive.finalize();
        return deferred.promise;
    };

    /**
     * Check that boolean environment variable string is 'true.
     */
    TestUtil.resolveBooleanVariables = function(variable) {
        if (variable) {
            return variable.toLowerCase() === 'true';
        }
    
        return false;
    }
    //// Placeholders
    // Used in the template to represent data that needs to be added by the testing framework at runtime.
    TestUtil.ANDROID_KEY_PLACEHOLDER = "CODE_PUSH_ANDROID_DEPLOYMENT_KEY";
    TestUtil.IOS_KEY_PLACEHOLDER = "CODE_PUSH_IOS_DEPLOYMENT_KEY";
    TestUtil.SERVER_URL_PLACEHOLDER = "CODE_PUSH_SERVER_URL";
    TestUtil.INDEX_JS_PLACEHOLDER = "CODE_PUSH_INDEX_JS_PATH";
    TestUtil.CODE_PUSH_APP_VERSION_PLACEHOLDER = "CODE_PUSH_APP_VERSION";
    TestUtil.CODE_PUSH_TEST_APP_NAME_PLACEHOLDER = "CODE_PUSH_TEST_APP_NAME";
    TestUtil.CODE_PUSH_APP_ID_PLACEHOLDER = "CODE_PUSH_TEST_APPLICATION_ID";
    TestUtil.PLUGIN_VERSION_PLACEHOLDER = "CODE_PUSH_PLUGIN_VERSION";
    return TestUtil;
}());
exports.TestUtil = TestUtil;
