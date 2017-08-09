/*
 * nexpect.js: Top-level include for the `nexpect` module.
 *
 * (C) 2011, Elijah Insua, Marak Squires, Charlie Robbins.
 *
 */

var spawn = require('child_process').spawn;
var util = require('util');
var AssertionError = require('assert').AssertionError;

function chain (context) {
  return {
    expect: function (expectation) {
      var _expect = function _expect (data) {
        return testExpectation(data, expectation);
      };

      _expect.shift = true;
      _expect.expectation = expectation;
      _expect.description = '[expect] ' + expectation;
      _expect.requiresInput = true;
      context.queue.push(_expect);

      return chain(context);
    },
    wait: function (expectation, callback) {
      var _wait = function _wait (data) {
        var val = testExpectation(data, expectation);
        if (val === true && typeof callback === 'function') {
          callback(data);
        }
        return val;
      };

      _wait.shift = false;
      _wait.expectation = expectation;
      _wait.description = '[wait] ' + expectation;
      _wait.requiresInput = true;
      context.queue.push(_wait);
      return chain(context);
    },
    sendline: function (line) {
      var _sendline = function _sendline () {
        context.process.stdin.write(line + '\n');

        if (context.verbose) {
          process.stdout.write(line + '\n');
        }
      };

      _sendline.shift = true;
      _sendline.description = '[sendline] ' + line;
      _sendline.requiresInput = false;
      context.queue.push(_sendline);
      return chain(context);
    },
    sendEof: function() {
      var _sendEof = function _sendEof () {
        context.process.stdin.destroy();
      };
      _sendEof.shift = true;
      _sendEof.description = '[sendEof]';
      _sendEof.requiresInput = false;
      context.queue.push(_sendEof);
      return chain(context);
    },
    run: function (callback) {
      var errState = null,
          responded = false,
          stdout = [],
          options;

      //
      // **onError**
      //
      // Helper function to respond to the callback with a
      // specified error. Kills the child process if necessary.
      //
      function onError (err, kill) {
        if (errState || responded) {
          return;
        }

        errState = err;
        responded = true;

        if (kill) {
          try { context.process.kill(); }
          catch (ex) { }
        }

        callback(err);
      }

      //
      // **validateFnType**
      //
      // Helper function to validate the `currentFn` in the
      // `context.queue` for the target chain.
      //
      function validateFnType (currentFn) {
        if (typeof currentFn !== 'function') {
          //
          // If the `currentFn` is not a function, short-circuit with an error.
          //
          onError(new Error('Cannot process non-function on nexpect stack.'), true);
          return false;
        }
        else if (['_expect', '_sendline', '_wait', '_sendEof'].indexOf(currentFn.name) === -1) {
          //
          // If the `currentFn` is a function, but not those set by `.sendline()` or
          // `.expect()` then short-circuit with an error.
          //
          onError(new Error('Unexpected context function name: ' + currentFn.name), true);
          return false;
        }

        return true;
      }

      //
      // **evalContext**
      //
      // Core evaluation logic that evaluates the next function in
      // `context.queue` against the specified `data` where the last
      // function run had `name`.
      //
      function evalContext (data, name) {
        var currentFn = context.queue[0];

        if (!currentFn || (name === '_expect' && currentFn.name === '_expect')) {
          //
          // If there is nothing left on the context or we are trying to
          // evaluate two consecutive `_expect` functions, return.
          //
          return;
        }

        if (currentFn.shift) {
          context.queue.shift();
        }

        if (!validateFnType(currentFn)) {
          return;
        }

        if (currentFn.name === '_expect') {
          //
          // If this is an `_expect` function, then evaluate it and attempt
          // to evaluate the next function (in case it is a `_sendline` function).
          //
          return currentFn(data) === true ?
            evalContext(data, '_expect') :
            onError(createExpectationError(currentFn.expectation, data), true);
        }
        else if (currentFn.name === '_wait') {
          //
          // If this is a `_wait` function, then evaluate it and if it returns true,
          // then evaluate the function (in case it is a `_sendline` function).
          //
          if (currentFn(data) === true) {
            context.queue.shift();
            evalContext(data, '_expect');
          }
        }
        else {
          //
          // If the `currentFn` is any other function then evaluate it
          //
          currentFn();

          // Evaluate the next function if it does not need input
          var nextFn = context.queue[0];
          if (nextFn && !nextFn.requiresInput)
            evalContext(data);
        }
      }

      //
      // **onLine**
      //
      // Preprocesses the `data` from `context.process` on the
      // specified `context.stream` and then evaluates the processed lines:
      //
      // 1. Stripping ANSI colors (if necessary)
      // 2. Removing case sensitivity (if necessary)
      // 3. Splitting `data` into multiple lines.
      //
      function onLine (data) {
        data = data.toString();

        if (context.stripColors) {
          data = data.replace(/\u001b\[\d{0,2}m/g, '');
        }

        if (context.ignoreCase) {
          data = data.toLowerCase();
        }

        var lines = data.split('\n').filter(function (line) { return line.length > 0; });
        stdout = stdout.concat(lines);

        while (lines.length > 0) {
          evalContext(lines.shift(), null);
        }
      }

      //
      // **flushQueue**
      //
      // Helper function which flushes any remaining functions from
      // `context.queue` and responds to the `callback` accordingly.
      //
      function flushQueue () {
        var remainingQueue = context.queue.slice(),
            currentFn = context.queue.shift(),
            lastLine = stdout[stdout.length - 1];

        if (!lastLine) {
          onError(createUnexpectedEndError(
            'No data from child with non-empty queue.', remainingQueue));
          return false;
        }
        else if (context.queue.length > 0) {
          onError(createUnexpectedEndError(
            'Non-empty queue on spawn exit.', remainingQueue));
          return false;
        }
        else if (!validateFnType(currentFn)) {
          // onError was called
          return false;
        }
        else if (currentFn.name === '_sendline') {
          onError(new Error('Cannot call sendline after the process has exited'));
          return false;
        }
        else if (currentFn.name === '_wait' || currentFn.name === '_expect') {
          if (currentFn(lastLine) !== true) {
            onError(createExpectationError(currentFn.expectation, lastLine));
            return false;
          }
        }

        return true;
      }

      //
      // **onData**
      //
      // Helper function for writing any data from a stream
      // to `process.stdout`.
      //
      function onData (data) {
        process.stdout.write(data);
      }

      options = {
        cwd: context.cwd,
        env: context.env
      };

      //
      // Spawn the child process and begin processing the target
      // stream for this chain.
      //
      if (!/^win/.test(process.platform)) {
        context.process = spawn(context.command, context.params, options);
      } else {
        context.process = spawn('cmd', ['/c', `${context.command}`].concat(context.params), options);
      }

      if (context.verbose) {
        context.process.stdout.on('data', onData);
        context.process.stderr.on('data', onData);
      }

      if (context.stream === 'all') {
        context.process.stdout.on('data', onLine);
        context.process.stderr.on('data', onLine);
      } else {
        context.process[context.stream].on('data', onLine);
      }

      context.process.on('error', onError);

      //
      // When the process exits, check the output `code` and `signal`,
      // flush `context.queue` (if necessary) and respond to the callback
      // appropriately.
      //
      context.process.on('close', function (code, signal) {
        if (code === 127) {
          // XXX(sam) Not how node works (anymore?), 127 is what /bin/sh returns,
          // but it appears node does not, or not in all conditions, blithely
          // return 127 to user, it emits an 'error' from the child_process.

          //
          // If the response code is `127` then `context.command` was not found.
          //
          return onError(new Error('Command not found: ' + context.command));
        }
        else if (context.queue.length && !flushQueue()) {
          // if flushQueue returned false, onError was called
          return;
        }

        callback(null, stdout, signal || code);
      });

      return context.process;
    }
  };
}

function testExpectation(data, expectation) {
  if (util.isRegExp(expectation)) {
    return expectation.test(data);
  } else {
    return data.indexOf(expectation) > -1;
  }
}

function createUnexpectedEndError(message, remainingQueue) {
  var desc = remainingQueue.map(function(it) { return it.description; });
  var msg = message + '\n' + desc.join('\n');
  return new AssertionError({
    message: msg,
    expected: [],
    actual: desc
  });
}

function createExpectationError(expected, actual) {
  var expectation;
  if (util.isRegExp(expected))
    expectation = 'to match ' + expected;
  else
    expectation = 'to contain ' + JSON.stringify(expected);

  var err = new AssertionError({
    message: util.format('expected %j %s', actual, expectation),
    actual: actual,
    expected: expected
  });
  return err;
}

function nspawn (command, params, options) {
  if (arguments.length === 2) {
    if (Array.isArray(arguments[1])) {
      options = {};
    }
    else {
      options = arguments[1];
      params = null;
    }
  }

  if (Array.isArray(command)) {
    params  = command;
    command = params.shift();
  }
  else if (typeof command === 'string') {
    command = command.split(' ');
    params  = params || command.slice(1);
    command = command[0];
  }

  options = options || {};
  context = {
    command: command,
    cwd: options.cwd || undefined,
    env: options.env || undefined,
    ignoreCase: options.ignoreCase,
    params: params,
    queue: [],
    stream: options.stream || 'stdout',
    stripColors: options.stripColors,
    verbose: options.verbose
  };

  return chain(context);
}

//
// Export the core `nspawn` function as well as `nexpect.nspawn` for
// backwards compatibility.
//
module.exports.spawn  = nspawn;
module.exports.nspawn = {
  spawn: nspawn
};
