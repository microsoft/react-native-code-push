var xml = require('xml');

module.exports = function(stats, options) {
  var data = {
    testsuites: [
      {
        _attr: {
          name: "Mocha Tests",
          tests: 3,
          failures: "1",
          time: "0.006"
        }
      },
      {
        testsuite: [
          {
            _attr: {
              name: "Foo Bar module",
              timestamp: stats.start.toISOString().substr(0,stats.start.toISOString().indexOf('.')),
              tests: "2",
              failures: "1",
              time: "0.002"
            }
          },
          {
            testcase: {
              _attr: {
                name: "Foo can weez the juice",
                classname: "can weez the juice",
                time: "0.001"
              }
            }
          },
          {
            testcase: [
              {
                _attr: {
                  name: "Bar can narfle the garthog",
                  classname: "can narfle the garthog",
                  time: "0.001"
                }
              },
              {
                failure: "expected garthog to be dead"
              }
            ]
          }
        ]
      },
      {
        testsuite: [
          {
            _attr: {
              name: "Another suite!",
              timestamp: stats.start.toISOString().substr(0,stats.start.toISOString().indexOf('.')),
              tests: "1",
              failures: "0",
              time: "0.004"
            }
          },
          {
            testcase: {
              _attr: {
                name: "Another suite",
                classname: "works",
                time: "0.004"
              }
            }
          }
        ]
      }
    ]
  };

  if (options && options.skipPassedTests) {
    data.testsuites[0]._attr.time = "0.005";
    data.testsuites[1].testsuite[0]._attr.time = "0.001";
    data.testsuites[1].testsuite.splice(1, 1);
  }

  if (options && options.properties) {
    var properties = {
      properties: []
    }
    for (var i = 0; i < options.properties.length; i++) {
      var property = options.properties[i];
      properties.properties.push({
        property: [
          {
            _attr: {
              name: property.name,
              value: property.value
            }
          }
        ]
      })
    }
    data.testsuites[1].testsuite.push(properties)
    data.testsuites[2].testsuite.push(properties)
  }

  if (stats.pending) {
    data.testsuites[0]._attr.tests += stats.pending;
    data.testsuites[0]._attr.skipped = stats.pending;
    data.testsuites.push({
      testsuite: [
        {
          _attr: {
            name: "Pending suite!",
            timestamp: stats.start.toISOString().substr(0,stats.start.toISOString().indexOf('.')),
            tests: "1",
            failures: "0",
            skipped: "1",
            time: "0"
          }
        },
        {
          testcase: [
            {
              _attr: {
                name: "Pending suite",
                classname: "pending",
                time: "0"
              }
            },
            {
              skipped: null
            }
          ]
        }
      ]
    });
  }

  return xml(data, {declaration: true});
};
