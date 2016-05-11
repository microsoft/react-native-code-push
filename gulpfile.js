var gulp = require("gulp");
var path = require("path");
var child_process = require("child_process");
var Q = require("q");
var runSequence = require("run-sequence");

var sourcePath = "./www";
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
    var options = {};
    if (detached) {
        options.detached = true;
        options.stdio = ["ignore"];
    }
    
    var process = child_process.spawn(command, args, options);

    process.stdout.on('data', function (data) {
        if (!silent) console.log("" + data);
    });

    process.stderr.on('data', function (data) {
        if (!silent) console.error("" + data);
    });

    if (!detached) {
        process.on('exit', function (code) {
            callback && callback(code === 0 ? undefined : "Error code: " + code);
        });
    }
    
    return process;
};

function execCommand(command, args, callback, silent) {
    var process = child_process.exec(command + " " + args.join(" "));

    process.stdout.on('data', function (data) {
        if (!silent) console.log("" + data);
    });

    process.stderr.on('data', function (data) {
        if (!silent) console.error("" + data);
    });
    
    process.on('error', function (error) {
        callback && callback(error);
    })
    
    process.on('exit', function (code) {
        callback && callback(code === 0 ? undefined : "Error code: " + code);
    });
    
    return process;
};

/**
 * Executes a child process and returns its output in the promise as a string
 */
function execCommandWithPromise(command, options, logOutput) {
    var deferred = Q.defer();

    options = options || {};
    options.maxBuffer = 1024 * 500;
    // abort processes that run longer than five minutes
    options.timeout = 5 * 60 * 1000;

    console.log("Running command: " + command);
    child_process.exec(command, options, (error, stdout, stderr) => {

        if (logOutput) stdout && console.log(stdout);
        stderr && console.error(stderr);

        if (error) {
            console.error(error);
            deferred.reject(error);
        } else {
            deferred.resolve(stdout.toString());
        }
    });

    return deferred.promise;
}

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
        args.push(process.argv[i]);
    }
    
    execCommand(command, args, callback);
}

gulp.task("compile", function (callback) {
    runSequence("compile-src", "compile-test", callback);
});

gulp.task("compile-test", function () {
    var ts = require("gulp-typescript");
    var insert = require("gulp-insert");

    return gulp.src([testPath + tsFiles])
        .pipe(ts(tsCompileOptions))
        .pipe(insert.prepend(compiledSourceWarningMessage))
        .pipe(gulp.dest(path.join(binPath, testPath)));
});

gulp.task("compile-src", function () {
    var ts = require("gulp-typescript");
    var insert = require("gulp-insert");

    return gulp.src([sourcePath + tsFiles])
        .pipe(ts(tsCompileOptions))
        .pipe(insert.prepend(compiledSourceWarningMessage))
        .pipe(gulp.dest(path.join(binPath, sourcePath)));
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

    return gulp.src([sourcePath + tsFiles, testPath + tsFiles])
        .pipe(tslint({ configuration: config }))
        .pipe(tslint.report("verbose"));
});

gulp.task("clean", function () {
    var del = require("del");
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