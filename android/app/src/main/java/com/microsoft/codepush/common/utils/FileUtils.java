package com.microsoft.codepush.common.utils;

import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.codepush.common.exceptions.CodePushFinalizeException;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static com.microsoft.codepush.common.CodePush.LOG_TAG;
import static com.microsoft.codepush.common.utils.CodePushUtils.finalizeResources;

/**
 * Class containing support methods for work with files.
 */
public class FileUtils {

    /**
     * Size of the buffer used when writing files.
     */
    private static final int WRITE_BUFFER_SIZE = 1024 * 8;

    /**
     * Appends file path with one more component.
     *
     * @param basePath            path to be appended.
     * @param appendPathComponent path component to append the base path.
     * @return new path.
     */
    public static String appendPathComponent(String basePath, String appendPathComponent) {
        return new File(basePath, appendPathComponent).getAbsolutePath();
    }

    /**
     * Copies the contents of one directory to another. Copies all teh contents recursively.
     *
     * @param sourceDir path to the directory to copy files from.
     * @param destDir   path to the directory to copy files to.
     * @throws IOException if read/write error occurred while accessing the file system.
     */
    public static void copyDirectoryContents(File sourceDir, File destDir) throws IOException {
        if (!destDir.exists()) {
            if (!destDir.mkdir()) {
                throw new IOException("Unable to copy file from " + sourceDir.getAbsolutePath() + " to " + destDir.getAbsolutePath() + ". Error creating directory.");
            }
        }

        /* For all the files in the folder, perform copy operation. */
        File[] sourceFiles = sourceDir.listFiles();
        if (sourceFiles == null) {
            throw new IOException("Pathname " + sourceDir.getPath() + " doesn't denote a directory.");
        }
        for (File sourceFile : sourceFiles) {
            if (sourceFile.isDirectory()) {
                copyDirectoryContents(new File(appendPathComponent(sourceDir.getPath(), sourceFile.getName())), new File(appendPathComponent(destDir.getPath(), sourceFile.getName())));
            } else {
                File destFile = new File(destDir, sourceFile.getName());
                FileInputStream fromFileStream = null;
                BufferedInputStream fromBufferedStream = null;
                FileOutputStream destStream = null;
                byte[] buffer = new byte[WRITE_BUFFER_SIZE];
                try {
                    fromFileStream = new FileInputStream(sourceFile);
                    fromBufferedStream = new BufferedInputStream(fromFileStream);
                    destStream = new FileOutputStream(destFile);
                    int bytesRead;
                    while ((bytesRead = fromBufferedStream.read(buffer)) > 0) {
                        destStream.write(buffer, 0, bytesRead);
                    }
                } finally {
                    Exception e = finalizeResources(
                            Arrays.asList(fromFileStream, fromBufferedStream, destStream),
                            "Unable to copy file from " + sourceDir.getAbsolutePath() + " to " + destDir.getAbsolutePath() + ". Error closing resources.");
                    if (e != null) {
                        throw new CodePushFinalizeException("Error closing IO resources when copying files.", e);
                    }
                }
            }
        }
    }

    /**
     * Deletes directory by the following path.
     *
     * @param directoryPath path to directory to be deleted. Can't be null.
     * @throws IOException if error occurred while accessing the file system.
     */
    public static void deleteDirectoryAtPath(String directoryPath) throws IOException {
        if (directoryPath == null) {
            throw new IOException("directoryPath cannot be null");
        }
        File file = new File(directoryPath);
        if (file.exists()) {
            deleteFileOrFolderSilently(file);
        }
    }

    /**
     * Deletes file or folder throwing no errors if something went wrong (errors will be logged instead).
     *
     * @param file path to file/folder to be deleted.
     */
    @SuppressWarnings("WeakerAccess")
    public static void deleteFileOrFolderSilently(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files == null) {
                AppCenterLog.error(LOG_TAG, "Pathname " + file.getAbsolutePath() + " doesn't denote a directory.");
                return;
            }
            for (File fileEntry : files) {
                if (fileEntry.isDirectory()) {
                    deleteFileOrFolderSilently(fileEntry);
                } else {
                    if (!fileEntry.delete()) {
                        AppCenterLog.error(LOG_TAG, "Error deleting file " + file.getName());
                    }
                }
            }
        }
        if (!file.delete()) {
            AppCenterLog.error(LOG_TAG, "Error deleting file " + file.getName());
        }
    }

    /**
     * Checks whether a file by the following path exists.
     *
     * @param filePath path to be checked.
     * @return <code>true</code> if exists, <code>false</code> if not.
     */
    public static boolean fileAtPathExists(String filePath) {
        return filePath != null && new File(filePath).exists();
    }

    /**
     * Moves file from one folder to another.
     *
     * @param fileToMove  path to the file to be moved.
     * @param newFolder   path to be moved to.
     * @param newFileName new name for the file to be moved.
     * @throws IOException if read/write error occurred while accessing the file system.
     */
    public static void moveFile(File fileToMove, File newFolder, String newFileName) throws IOException {
        if (!newFolder.exists()) {
            if (!newFolder.mkdirs()) {
                throw new IOException("Unable to move file from " + fileToMove.getAbsolutePath() + " to " + newFolder.getAbsolutePath() + ". Error creating folder.");
            }
        }
        File newFilePath = new File(newFolder, newFileName);
        if (!fileToMove.renameTo(newFilePath)) {
            throw new IOException("Unable to move file from " + fileToMove.getAbsolutePath() + " to " + newFilePath.getAbsolutePath() + ".");
        }
    }

    /**
     * Reads the contents of file.
     *
     * @param filePath path to file to be read.
     * @return string with contents of the file.
     * @throws IOException if read/write error occurred while accessing the file system.
     */
    public static String readFileToString(String filePath) throws IOException {
        FileInputStream fileInputStream = null;
        BufferedReader reader = null;
        try {
            File readFile = new File(filePath);
            fileInputStream = new FileInputStream(readFile);
            reader = new BufferedReader(new InputStreamReader(fileInputStream));
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line).append("\n");
            }
            return stringBuilder.toString().trim();
        } finally {
            Exception e = finalizeResources(
                    Arrays.asList(reader, fileInputStream),
                    "Error closing IO resources when reading file.");
            if (e != null) {
                throw new CodePushFinalizeException("Error closing IO resources when reading file.", e);
            }
        }
    }

    /**
     * Gets files from archive.
     *
     * @param zipFile           path to zip-archive.
     * @param destinationFolder path for the unzipped files to be saved.
     * @throws IOException exception occurred during read/write operations.
     */
    public static void unzipFile(File zipFile, File destinationFolder) throws IOException {
        FileInputStream fileStream = null;
        BufferedInputStream bufferedStream = null;
        ZipInputStream zipStream = null;
        try {
            fileStream = new FileInputStream(zipFile);
            bufferedStream = new BufferedInputStream(fileStream);
            zipStream = new ZipInputStream(bufferedStream);
            ZipEntry entry;
            if (destinationFolder.exists()) {
                deleteFileOrFolderSilently(destinationFolder);
            }
            if (!destinationFolder.mkdirs()) {
                throw new IOException("Error creating folder for unzipping");
            }
            byte[] buffer = new byte[WRITE_BUFFER_SIZE];
            while ((entry = zipStream.getNextEntry()) != null) {
                String fileName = entry.getName();
                File file = new File(destinationFolder, fileName);
                unzipSingleFile(entry, file, buffer, zipStream);
            }
        } finally {
            Exception e = finalizeResources(
                    Arrays.<Closeable>asList(zipStream, bufferedStream, fileStream),
                    "Error closing IO resources when reading file.");
            if (e != null) {
                throw new CodePushFinalizeException("Error closing IO resources when copying file.", e);
            }
        }
    }

    public static void unzipSingleFile(ZipEntry entry, File file, byte[] buffer, ZipInputStream zipStream) throws IOException {
        if (entry.isDirectory()) {
            if (!file.mkdirs()) {
                throw new IOException("Error while unzipping. Cannot create directory.");
            }
        } else {
            File parent = file.getParentFile();
            if (!parent.exists()) {
                if (!parent.mkdirs()) {
                    throw new IOException("Error while unzipping. Cannot create directory.");
                }
            }
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            try {
                int numBytesRead;
                while ((numBytesRead = zipStream.read(buffer)) != -1) {
                    fileOutputStream.write(buffer, 0, numBytesRead);
                }
            } finally {
                try {
                    fileOutputStream.close();
                } catch (IOException e) {
                    throw new IOException("Error closing IO resources when reading files.");
                }
            }
        }
        long time = entry.getTime();
        if (time > 0) {
            if (!file.setLastModified(time)) {
                throw new IOException("Error while unzipping. Cannot set last modified time to file.");
            }
        }
    }

    /**
     * Writes some content to a file.
     *
     * @param content  content to be written to a file.
     * @param filePath path to a file.
     * @throws IOException if error occurred while accessing the file system.
     */
    public static void writeStringToFile(String content, String filePath) throws IOException {
        PrintWriter printWriter = null;
        try {
            printWriter = new PrintWriter(filePath);
            printWriter.print(content);
        } finally {
            if (printWriter != null) {
                printWriter.close();
                if (printWriter.checkError()) {
                    throw new CodePushFinalizeException("Error closing IO resources when writing to file.");
                }
            }
        }
    }
}
