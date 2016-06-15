'use strict';

function Test(fullTitle, title, duration) {
  return {
    title: title,
    duration: duration,
    fullTitle: function() { return fullTitle; },
    slow: function() {}
  };
}

module.exports = Test
