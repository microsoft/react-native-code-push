package com.microsoft.codepush.common.utils;

import android.support.annotation.Nullable;

import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.codepush.common.CodePush;
import com.microsoft.codepush.common.exceptions.CodePushFinalizeException;
import com.microsoft.codepush.common.exceptions.CodePushFinalizeException.OperationType;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Class containing support methods simplifying work with files.
 */
public class FileUtils {

    /**
     * Instance of the class (singleton).
     */
    private static FileUtils INSTANCE;

    /**
     * Gets and instance of {@link FileUtils}.
     *
     * @return instance of the class.
     */
    public static FileUtils getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new FileUtils();
        }
        return INSTANCE;
    }

    /**
     * Private constructor to prevent direct creating the instance of the class.
     */
    private FileUtils() {
    }

    /**
     * Size of the buffer used when writing to files.
     */
    private final int WRITE_BUFFER_SIZE = 1024 * 8;

    /**
     * Appends file path with one more component.
     *
     * @param basePath            path to be appended.
     * @param appendPathComponent path component to be appended to the base path.
     * @return new path.
     */
    public String appendPathComponent(String basePath, String appendPathComponent) {
        return new File(basePath, appendPathComponent).getAbsolutePath();
    }

    /**
     * Copies the contents of one directory to another. Copies all the contents recursively.
     *
     * @param sourceDir path to the directory to copy files from.
     * @param destDir   path to the directory to copy files to.
     * @throws IOException read/write error occurred while accessing the file system.
     */
    public void copyDirectoryContents(File sourceDir, File destDir) throws IOException {
        if (!destDir.exists()) {
            if (!destDir.mkdirs()) {
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
                        throw new CodePushFinalizeException(OperationType.COPY, e);
                    }
                }
            }
        }
    }

    /**
     * Deletes directory located by the following path.
     *
     * @param directoryPath path to directory to be deleted. Can't be <code>null</code>.
     * @throws IOException read/write error occurred while accessing the file system.
     */
    public void deleteDirectoryAtPath(String directoryPath) throws IOException {
        if (directoryPath == null) {
            throw new IOException("directoryPath cannot be null");
        }
        File file = new File(directoryPath);
        if (file.exists()) {
            deleteFileOrFolderSilently(file);
        }
    }

    /**
     * Deletes file or folder throwing no errors if something goes wrong (errors will be logged instead).
     *
     * @param file path to file/folder to be deleted.
     */
    @SuppressWarnings("WeakerAccess")
    public void deleteFileOrFolderSilently(File file) throws IOException {

        /* First, if this is a directory, delete all the files it contains. */
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files == null) {
                throw new IOException("Pathname " + file.getAbsolutePath() + " doesn't denote a directory.");
            }
            for (File fileEntry : files) {
                if (fileEntry.isDirectory()) {
                    deleteFileOrFolderSilently(fileEntry);
                } else {
                    if (!fileEntry.delete()) {
                        throw new IOException("Error deleting file " + file.getName());
                    }
                }
            }
        }
        if (!file.delete()) {
            throw new IOException("Error deleting file " + file.getName());
        }
    }

    /**
     * Checks whether a file by the following path exists.
     *
     * @param filePath path to be checked.
     * @return <code>true</code> if exists, <code>false</code> otherwise.
     */
    public boolean fileAtPathExists(String filePath) {
        return filePath != null && new File(filePath).exists();
    }

    /**
     * Moves file from one folder to another.
     *
     * @param fileToMove  path to the file to be moved.
     * @param newFolder   path to be moved to.
     * @param newFileName new name for the file to be moved.
     * @throws IOException read/write error occurred while accessing the file system.
     */
    public void moveFile(File fileToMove, File newFolder, String newFileName) throws IOException {
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
     * Reads the contents of file to a string.
     *
     * @param filePath path to file to be read.
     * @return string with contents of the file.
     * @throws IOException read/write error occurred while accessing the file system.
     */
    public String readFileToString(String filePath) throws IOException {
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
                throw new CodePushFinalizeException(OperationType.READ, e);
            }
        }
    }

    /**
     * Gets files from archive.
     *
     * @param zipFile           path to zip-archive.
     * @param destinationFolder path for the unzipped files to be saved.
     * @throws IOException read/write error occurred while accessing the file system.
     */
    public void unzipFile(File zipFile, File destinationFolder) throws IOException {
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
                throw new CodePushFinalizeException(OperationType.COPY, e);
            }
        }
    }

    /**
     * Saves file from one zip entry to the specified location.
     *
     * @param entry     zip entry.
     * @param file      path for the unzipped file.
     * @param buffer    read buffer.
     * @param zipStream stream with zip file.
     * @throws IOException read/write error occurred while accessing the file system.
     */
    public void unzipSingleFile(ZipEntry entry, File file, byte[] buffer, ZipInputStream zipStream) throws IOException {
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
                Exception e = finalizeResources(
                        Arrays.<Closeable>asList(fileOutputStream),
                        "Error closing IO resources when reading file.");
                if (e != null) {
                    throw new CodePushFinalizeException(OperationType.COPY, e);
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
     * Writes some content to a file, existing file will be overwritten.
     *
     * @param content  content to be written to a file.
     * @param filePath path to a file.
     * @throws IOException read/write error occurred while accessing the file system.
     */
    public void writeStringToFile(String content, String filePath) throws IOException {
        FileWriter writer = null;
        try {
            writer = new FileWriter(filePath, false);
            writer.write(content);
        } finally {
            Exception e = finalizeResources(
                    Arrays.<Closeable>asList(writer),
                    "Error closing IO resources when reading file.");
            if (e != null) {
                throw new CodePushFinalizeException(OperationType.COPY, e);
            }
        }
    }

    /**
     * Gracefully finalizes {@link Closeable} resources. Method iterates through resources and invokes
     * {@link Closeable#close()} on them if necessary. If an exception is thrown during <code>.close()</code> call,
     * it is be logged using <code>logErrorMessage</code> parameter as general message.
     *
     * @param resources       resources to finalize.
     * @param logErrorMessage general logging message for errors occurred during resource finalization.
     * @return last {@link IOException} thrown during resource finalization, null if no exception was thrown.
     */
    @SuppressWarnings("WeakerAccess")
    public IOException finalizeResources(List<Closeable> resources, @Nullable String logErrorMessage) {
        IOException lastException = null;
        for (int i = 0; i < resources.size(); i++) {
            Closeable resource = resources.get(i);
            try {
                if (resource != null) {
                    resource.close();
                }
            } catch (IOException e) {
                lastException = e;
                if (logErrorMessage != null) {
                    AppCenterLog.error(CodePush.LOG_TAG, logErrorMessage, e);
                }
            }
        }
        return lastException;
    }
}
