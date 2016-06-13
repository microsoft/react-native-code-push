var prompt = require('prompt'),
    colors = require('colors'),
    readline = require('readline'),
    async = require('async');

var prompt = readline.createInterface(process.stdin, process.stdout);

var lines = ["aaa", "abc", "ccc"];

function replaceLine(line, next) {
  var message = "do you want to replace this occurance?";
  process.stdout.write(line);
  prompt.question(message, function(answer){
      if (answer == "y")
          console.log("replaced");
      else
          console.log("skipped");
     next();
  });
}

async.forEachSeries(lines, replaceLine, function (err) {
  console.log("done");
  process.exit(0)
});