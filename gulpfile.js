var del = require("del");
var gulp = require("gulp");
var path = require("path");
var child_process = require("child_process");
var Q = require("q");
var runSequence = require("run-sequence");

var testPath = "./test";
var binPath = "./bin";
var tsFiles = "/**/*.ts";

var iOSSimulatorProcessName = "Simulator";
var emulatorReadyCheckDelay = 30 * 1000;
var emulatorMaxReadyAttempts = 5;

/* This message is appended to the compiled JS files to avoid contributions to the compiled sources.*/
var compiledSourceWarningMessage = "\n \
/******************************************************************************************** \n \
	 THIS FILE HAS BEEN COMPILED FROM TYPESCRIPT SOURCES. \n \
	 PLEASE DO NOT MODIFY THIS FILE AS YOU WILL LOSE YOUR CHANGES WHEN RECOMPILING. \n \
	 ALSO, PLEASE DO NOT SUBMIT PULL REQUESTS WITH CHANGES TO THIS FILE. \n \
	 INSTEAD, EDIT THE TYPESCRIPT SOURCES UNDER THE WWW FOLDER. \n \
	 FOR MORE INFORMATION, PLEASE SEE CONTRIBUTING.md. \n \
*********************************************************************************************/ \n\n\n";

/* TypeScript compilation parameters */
var tsCompileOptions = {
    "noImplicitAny": true,
    "noEmitOnError": true,
    "target": "ES5",
    "module": "commonjs",
    "sourceMap": false,
    "sortOutput": true,
    "removeComments": true
};

function spawnCommand(command, args, callback, silent, detached) {
    var options = { maxBuffer: 1024 * 1024 };
    if (detached) {
        options.detached = true;
        options.stdio = ["ignore"];
    }
    
    var spawnProcess = child_process.spawn(command, args, options);
        
    if (!silent) spawnProcess.stdout.pipe(process.stdout);
    if (!silent) spawnProcess.stderr.pipe(process.stderr);

    if (!detached) {
        spawnProcess.on('exit', function (code) {
            callback && callback(code === 0 ? undefined : "Error code: " + code);
        });
    }
    
    return spawnProcess;
};

function execCommand(command, args, callback, silent) {
    var execProcess = child_process.exec(command + " " + args.join(" "), { maxBuffer: 1024 * 1024 });
        
    if (!silent) execProcess.stdout.pipe(process.stdout);
    if (!silent) execProcess.stderr.pipe(process.stderr);
    
    execProcess.on('error', function (error) {
        callback && callback(error);
    })
    
    execProcess.on('exit', function (code) {
        callback && callback(code === 0 ? undefined : "Error code: " + code);
    });
    
    return execProcess;
};

function runTests(callback, options) {
    var command = "mocha";
    var args = ["./bin/test"];
    
    // pass arguments supplied by test tasks
    if (options.android) args.push("--android");
    if (options.ios) args.push("--ios");
    if (options.setup) args.push("--setup");
    
    // pass arguments from command line
    // the fourth argument is the first argument after the task name
    for (var i = 3; i < process.argv.length; i++) {
        if (process.argv[i] === "--report") {
            // Set up the mocha junit reporter.
            args.push("--reporter");
            args.push("mocha-junit-reporter");
            
            // Set the mocha reporter to the correct output file.
            args.push("--reporter-options");
            var filename = "./test-results.xml";
            if (options.android && !options.ios) filename = "./test-android.xml";
            else if (options.ios && !options.android) filename = "./test-ios.xml";
            args.push("mochaFile=" + filename);
            // Delete previous test result file so TFS doesn't read the old file if the tests exit before saving
            del(filename);
        } else args.push(process.argv[i]);
    }
    
    execCommand(command, args, callback);
}

gulp.task("compile", function (callback) {
    runSequence("compile-test", callback);
});

gulp.task("compile-test", function () {
    var ts = require("gulp-typescript");
    var insert = require("gulp-insert");

    return gulp.src([testPath + tsFiles])
        .pipe(ts(tsCompileOptions))
        .pipe(insert.prepend(compiledSourceWarningMessage))
        .pipe(gulp.dest(path.join(binPath, testPath)));
});

gulp.task("tslint", function () {
    var tslint = require('gulp-tslint');

    // Configuration options adapted from TypeScript project:
    // https://github.com/Microsoft/TypeScript/blob/master/tslint.json

    var config = {
        "rules": {
            "class-name": true,
            "comment-format": [true,
                "check-space"
            ],
            "indent": [true,
                "spaces"
            ],
            "one-line": [true,
                "check-open-brace"
            ],
            "no-unreachable": true,
            "no-unused-variable": true,
            "no-use-before-declare": true,
            "quotemark": [true,
                "double"
            ],
            "semicolon": true,
            "whitespace": [true,
                "check-branch",
                "check-operator",
                "check-separator",
                "check-type"
            ],
            "typedef-whitespace": [true, {
                "call-signature": "nospace",
                "index-signature": "nospace",
                "parameter": "nospace",
                "property-declaration": "nospace",
                "variable-declaration": "nospace"
            }]
        }
    }

    return gulp.src([testPath + tsFiles, "!" + testPath + "/typings/*"])
        .pipe(tslint({ configuration: config }))
        .pipe(tslint.report("verbose"));
});

gulp.task("clean", function () {
    return del([binPath + "/**"], { force: true });
});

gulp.task("default", function (callback) {
    runSequence("clean", "compile", "tslint", callback);
});

////////////////////////////////////////////////////////////////////////
// Test Tasks //////////////////////////////////////////////////////////

////////////////////////////////////////////////////////////////////////
// Standalone Tasks
//
// Run the tests without setting up the test projects.
// Don't run these without running a setup task first!

// Run on Android standalone
gulp.task("test-run-android", function (callback) {
    var options = {
        android: true
    };
    
    runTests(callback, options);
});

// Run on iOS standalone
gulp.task("test-run-ios", function (callback) {
    var options = {
        ios: true,
        ui: true,
    };
    
    runTests(callback, options);
});

////////////////////////////////////////////////////////////////////////
// Setup Tasks
//
// Sets up the test project directories that the tests use and starts emulators.
// Must run before running a standalone suite of tests!

// Sets up the test projects and starts an Android emulator
gulp.task("test-setup-android", function (callback) {
    var options = {
        setup: true,
        android: true
    };
    
    runTests(callback, options);
});

// Sets up the test projects and starts an iOS emulator
gulp.task("test-setup-ios", function (callback) {
    var options = {
        setup: true,
        ios: true
    };
    
    runTests(callback, options);
});

// Sets up the test projects and starts both emulators
gulp.task("test-setup-both", function (callback) {
    var options = {
        setup: true,
        android: true,
        ios: true
    };
    
    runTests(callback, options);
});

// Builds, sets up test projects, and starts the Android emulator
gulp.task("test-setup-build-android", function (callback) {
    runSequence("default", "test-setup-android", callback);
});

// Builds, sets up test projects, and starts the iOS emulator
gulp.task("test-setup-build-ios", function (callback) {
    runSequence("default", "test-setup-ios", callback);
});

// Builds, sets up test projects, and starts both emulators
gulp.task("test-setup-build-both", function (callback) {
    runSequence("default", "test-setup-both", callback);
});

////////////////////////////////////////////////////////////////////////
// Fast Test Tasks
//
// Runs tests but doesn't build or start emulators.

// Run on Android fast
gulp.task("test-android-fast", ["test-setup-android"], function (callback) {
    runSequence("test-run-android", callback);
});

// Run on iOS fast
gulp.task("test-ios-fast", ["test-setup-ios"], function (callback) {
    runSequence("test-run-ios", callback);
});

////////////////////////////////////////////////////////////////////////
// Fast Composition Test Tasks
//
// Run tests but doesn't build or start emulators.

// Run on iOS fast
gulp.task("test-fast", ["test-setup-both"], function (callback) {
    runSequence("test-run-android", "test-run-ios", callback);
});

////////////////////////////////////////////////////////////////////////
// Test Tasks
//
// Run tests, build, and start emulators.

// Run on Android
gulp.task("test-android", ["test-setup-build-android"], function (callback) {
    runSequence("test-run-android", callback);
});

// Run on iOS
gulp.task("test-ios", ["test-setup-build-ios"], function (callback) {
    runSequence("test-run-ios", callback);
});

////////////////////////////////////////////////////////////////////////
// Composition Test Tasks
//
// Run tests, build, and start emulators.

// Run on Android and iOS
gulp.task("test", ["test-setup-build-both"], function (callback) {
    runSequence("test-run-android", "test-run-ios", callback);
});