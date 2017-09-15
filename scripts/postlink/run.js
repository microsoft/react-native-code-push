var postlinks = [
    require("./ios/postlink"),
    require("./android/postlink")
];

//run them sequentially
var result = postlinks.reduce((p, fn) => p.then(fn), Promise.resolve());
result.catch((err) => {
    console.error(err.message);
}); 