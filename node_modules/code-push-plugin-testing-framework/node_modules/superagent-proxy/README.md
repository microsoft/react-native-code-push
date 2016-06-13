superagent-proxy
================
### `Request#proxy(uri)` superagent extension

This module extends [`visionmedia/superagent`][superagent]'s `Request` class with
a `.proxy(uri)` function. This allows you to proxy the HTTP request through a
proxy of some kind.

It is backed by the [`proxy-agent`][proxy-agent] module, so see
[its README for more details][proxy-agent-readme].


Installation
------------

Install with `npm`:

``` bash
$ npm install superagent-proxy
```


Example
-------

``` js
var request = require('superagent');

// extend with Request#proxy()
require('superagent-proxy')(request);

// HTTP, HTTPS, or SOCKS proxy to use
var proxy = process.env.http_proxy || 'http://168.63.43.102:3128';

request
  .get(process.argv[2] || 'https://encrypted.google.com/')
  .proxy(proxy)
  .end(onresponse);

function onresponse (res) {
  console.log(res.status, res.headers);
  console.log(res.body);
}
```


License
-------

(The MIT License)

Copyright (c) 2013 Nathan Rajlich &lt;nathan@tootallnate.net&gt;

Permission is hereby granted, free of charge, to any person obtaining
a copy of this software and associated documentation files (the
'Software'), to deal in the Software without restriction, including
without limitation the rights to use, copy, modify, merge, publish,
distribute, sublicense, and/or sell copies of the Software, and to
permit persons to whom the Software is furnished to do so, subject to
the following conditions:

The above copyright notice and this permission notice shall be
included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED 'AS IS', WITHOUT WARRANTY OF ANY KIND,
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

[superagent]: https://github.com/visionmedia/superagent
[proxy-agent]: https://github.com/TooTallNate/node-proxy-agent
[proxy-agent-readme]: https://github.com/TooTallNate/node-proxy-agent/blob/master/README.md
