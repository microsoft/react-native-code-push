var postlinks = [
    require("./android/postlink"),
    require("./ios/postlink")
];

//run them sequentially
postlinks
    .reduce((p, fn) => p.then(fn), Promise.resolve())
    .catch((err) => {
        console.error(err.message);
    }); 