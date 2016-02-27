var fs = require("fs");
var path = require("path");

// Utility function that collects the stats of every file in a directory
// as well as in its subdirectories.
function getFilesInFolder(folderName, fileList) {
    var folderFiles = fs.readdirSync(folderName);
    folderFiles.forEach(function(file) {
        var fileStats = fs.statSync(path.join(folderName, file));
        if (fileStats.isDirectory()) {
            getFilesInFolder(path.join(folderName, file), fileList);
        } else {
            fileStats.path = path.join(folderName, file);
            fileList.push(fileStats);
        }
    });
}

module.exports = getFilesInFolder;