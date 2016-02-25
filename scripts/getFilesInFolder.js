var fs = require("fs");

// Utility function that collects the stats of every file in a directory
// as well as in its subdirectories.
function getFilesInFolder(folderName, fileList) {
    var folderFiles = fs.readdirSync(folderName);
    folderFiles.forEach(function(file) {
        var fileStats = fs.statSync(folderName + "/" + file);
        if (fileStats.isDirectory()) {
            getFilesInFolder(folderName + "/" + file, fileList);
        } else {
            fileStats.path = folderName + "/" + file;
            fileList.push(fileStats);
        }
    });
}

module.exports = getFilesInFolder;