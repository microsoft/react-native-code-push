
1.0.0 / 2015-07-15
==================

  * bumping to v1.0.0 for stricter defined semver semantics

0.4.0 / 2015-07-15
==================

  * update to "proxy-agent" v2 API
  * use %o debug() formatter

0.3.2 / 2015-05-18
==================

  * package: update "debug" to v2.2.0
  * package: still limit "superagent" to < v2
  * package: support superagent >= 1.0 (#14, @Jxck)

0.3.1 / 2014-08-21
==================

  * package: looser "superagnet" peerDependency version

0.3.0 / 2014-01-13
==================

  * index: fix the redirect http -> https scenario
  * index: use `debug` component
  * package: update "proxy-agent" to v1

0.2.0 / 2013-11-20
==================

  * index: use "proxy-agent"

0.1.0 / 2013-11-16
==================

  * index: implement an LRU cache to lower the creation of `http.Agent` instances
  * index: include the ":" when generating the cache URI
  * index: add support for calling the proxy function directly
  * index: cleanup and refactor to allow for an opts object to be used
  * test: add some basic tests for the .proxy() function

0.0.2 / 2013-11-15
==================

  * package: add "superagent" to `devDependencies`
  * only invoke `url.parse()` when the proxy is a string
  * README++

0.0.1 / 2013-07-11
==================

  * Initial release, currently supports:
    * `http:`
    * `https:`
    * `socks:` (version 4a)
