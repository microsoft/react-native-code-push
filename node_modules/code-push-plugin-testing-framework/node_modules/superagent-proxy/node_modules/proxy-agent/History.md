
2.0.0 / 2015-07-15
==================

  * package: add test case dev dependencies
  * test: don't use `url.parse()` for one of the tests
  * test: introduce some real test cases
  * README: document new, simpler API
  * refactor to inherit from "agent-base"

1.1.1 / 2015-07-01
==================

  * package: remove "superagent" dev dep (not used)
  * use new socks-proxy-agent (#6, @MatthewMueller)
  * README: use SVG for Travis-CI badge
  * add more proxy types for socks proxy (#5, @andyhu)
  * index: fix passing the `secure` flag to PacProxyAgent

1.1.0 / 2014-01-12
==================

  * gitignore: ignore development test files
  * index: use "pac-proxy-agent" for "pac+*" proxy URLs
  * package: update "lru-cache" to v2.5.0

1.0.0 / 2013-11-21
==================

  * add .travis.yml file
  * test: add initial tests
  * test: add a test for unsupported protocols
  * index: extract the `types` array logic into a function
  * index: throw a TypeError if no "uri" is passed in
  * index: use Object.create(null) for the proxies dict
  * package: add "socks" as a keyword

0.0.2 / 2013-11-20
==================

  * index: walk up the prototype chain to get the proxy types

0.0.1 / 2013-11-20
==================

  * intial release
