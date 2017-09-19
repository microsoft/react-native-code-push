var postlinks = [
    require("./ios/postlink"),
    require("./android/postlink")
];

//run them sequentially
postlinks
    .reduce((p, fn) => p.then(fn), Promise.resolve())
    .catch((err) => {
        console.error(err.message);
    }); 