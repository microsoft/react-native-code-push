package com.microsoft.codepush.react;

import android.content.Context;
import android.util.Base64;

import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.SignedJWT;

import java.security.interfaces.*;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

public class CodePushUpdateUtils {

    public static final String NEW_LINE = System.getProperty("line.separator");

    // Note: The hashing logic here must mirror the hashing logic in other native SDK's, as well as in the
    // CLI. Ensure that any changes here are propagated to these other locations.
    public static boolean isHashIgnored(String relativeFilePath) {
        final String __MACOSX = "__MACOSX/";
        final String DS_STORE = ".DS_Store";
        final String CODEPUSH_METADATA = ".codepushrelease";

        return relativeFilePath.startsWith(__MACOSX)
                || relativeFilePath.equals(DS_STORE)
                || relativeFilePath.endsWith("/" + DS_STORE)
                || relativeFilePath.equals(CODEPUSH_METADATA)
                || relativeFilePath.endsWith("/" + CODEPUSH_METADATA);
    }

    private static void addContentsOfFolderToManifest(String folderPath, String pathPrefix, ArrayList<String> manifest) {
        File folder = new File(folderPath);
        File[] folderFiles = folder.listFiles();
        for (File file : folderFiles) {
            String fileName = file.getName();
            String fullFilePath = file.getAbsolutePath();
            String relativePath = (pathPrefix.isEmpty() ? "" : (pathPrefix + "/")) + fileName;

            if (CodePushUpdateUtils.isHashIgnored(relativePath)) {
                continue;
            }

            if (file.isDirectory()) {
                addContentsOfFolderToManifest(fullFilePath, relativePath, manifest);
            } else {
                try {
                    manifest.add(relativePath + ":" + computeHash(new FileInputStream(file)));
                } catch (FileNotFoundException e) {
                    // Should not happen.
                    throw new CodePushUnknownException("Unable to compute hash of update contents.", e);
                }
            }
        }
    }

    private static String computeHash(InputStream dataStream) {
        MessageDigest messageDigest = null;
        DigestInputStream digestInputStream = null;
        try {
            messageDigest = MessageDigest.getInstance("SHA-256");
            digestInputStream = new DigestInputStream(dataStream, messageDigest);
            byte[] byteBuffer = new byte[1024 * 8];
            while (digestInputStream.read(byteBuffer) != -1) ;
        } catch (NoSuchAlgorithmException | IOException e) {
            // Should not happen.
            throw new CodePushUnknownException("Unable to compute hash of update contents.", e);
        } finally {
            try {
                if (digestInputStream != null) {
                    digestInputStream.close();
                }
                if (dataStream != null) {
                    dataStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        byte[] hash = messageDigest.digest();
        return String.format("%064x", new java.math.BigInteger(1, hash));
    }

    public static void copyNecessaryFilesFromCurrentPackage(String diffManifestFilePath, String currentPackageFolderPath, String newPackageFolderPath) throws IOException {
        if (currentPackageFolderPath == null || !new File(currentPackageFolderPath).exists()) {
            CodePushUtils.log("Unable to copy files from current package during diff update, because currentPackageFolderPath is invalid.");
            return;
        }
        FileUtils.copyDirectoryContents(currentPackageFolderPath, newPackageFolderPath);
        JSONObject diffManifest = CodePushUtils.getJsonObjectFromFile(diffManifestFilePath);
        try {
            JSONArray deletedFiles = diffManifest.getJSONArray("deletedFiles");
            for (int i = 0; i < deletedFiles.length(); i++) {
                String fileNameToDelete = deletedFiles.getString(i);
                File fileToDelete = new File(newPackageFolderPath, fileNameToDelete);
                if (fileToDelete.exists()) {
                    fileToDelete.delete();
                }
            }
        } catch (JSONException e) {
            throw new CodePushUnknownException("Unable to copy files from current package during diff update", e);
        }
    }

    public static String findJSBundleInUpdateContents(String folderPath, String expectedFileName) {
        File folder = new File(folderPath);
        File[] folderFiles = folder.listFiles();
        for (File file : folderFiles) {
            String fullFilePath = CodePushUtils.appendPathComponent(folderPath, file.getName());
            if (file.isDirectory()) {
                String mainBundlePathInSubFolder = findJSBundleInUpdateContents(fullFilePath, expectedFileName);
                if (mainBundlePathInSubFolder != null) {
                    return CodePushUtils.appendPathComponent(file.getName(), mainBundlePathInSubFolder);
                }
            } else {
                String fileName = file.getName();
                if (fileName.equals(expectedFileName)) {
                    return fileName;
                }
            }
        }

        return null;
    }

    public static String getHashForBinaryContents(Context context, boolean isDebugMode) {
        try {
            return CodePushUtils.getStringFromInputStream(context.getAssets().open(CodePushConstants.CODE_PUSH_HASH_FILE_NAME));
        } catch (IOException e) {
            try {
                return CodePushUtils.getStringFromInputStream(context.getAssets().open(CodePushConstants.CODE_PUSH_OLD_HASH_FILE_NAME));
            } catch (IOException ex) {
                if (!isDebugMode) {
                    // Only print this message in "Release" mode. In "Debug", we may not have the
                    // hash if the build skips bundling the files.
                    CodePushUtils.log("Unable to get the hash of the binary's bundled resources - \"codepush.gradle\" may have not been added to the build definition.");
                }
            }
            return null;
        }
    }

    // Hashing algorithm:
    // 1. Recursively generate a sorted array of format <relativeFilePath>: <sha256FileHash>
    // 2. JSON stringify the array
    // 3. SHA256-hash the result
    public static void verifyFolderHash(String folderPath, String expectedHash) {
        CodePushUtils.log("Verifying hash for folder path: " + folderPath);
        ArrayList<String> updateContentsManifest = new ArrayList<>();
        addContentsOfFolderToManifest(folderPath, "", updateContentsManifest);
        //sort manifest strings to make sure, that they are completely equal with manifest strings has been generated in cli!
        Collections.sort(updateContentsManifest);
        JSONArray updateContentsJSONArray = new JSONArray();
        for (String manifestEntry : updateContentsManifest) {
            updateContentsJSONArray.put(manifestEntry);
        }

        // The JSON serialization turns path separators into "\/", e.g. "CodePush\/assets\/image.png"
        String updateContentsManifestString = updateContentsJSONArray.toString().replace("\\/", "/");
        CodePushUtils.log("Manifest string: " + updateContentsManifestString);

        String updateContentsManifestHash = computeHash(new ByteArrayInputStream(updateContentsManifestString.getBytes()));

        CodePushUtils.log("Expected hash: " + expectedHash + ", actual hash: " + updateContentsManifestHash);
        if (!expectedHash.equals(updateContentsManifestHash)) {
            throw new CodePushInvalidUpdateException("The update contents failed the data integrity check.");
        }

        CodePushUtils.log("The update contents succeeded the data integrity check.");
    }

    public static Map<String, Object> verifyAndDecodeJWT(String jwt, PublicKey publicKey) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(jwt);
            JWSVerifier verifier = new RSASSAVerifier((RSAPublicKey) publicKey);
            if (signedJWT.verify(verifier)) {
                Map<String, Object> claims = signedJWT.getJWTClaimsSet().getClaims();
                CodePushUtils.log("JWT verification succeeded, payload content: " + claims.toString());
                return claims;
            }
            return null;
        } catch (Exception ex) {
            CodePushUtils.log(ex.getMessage());
            CodePushUtils.log(ex.getStackTrace().toString());
            return null;
        }
    }

    public static PublicKey parsePublicKey(String stringPublicKey) {
        try {
            //remove unnecessary "begin/end public key" entries from string
            stringPublicKey = stringPublicKey
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replace(NEW_LINE, "");
            byte[] byteKey = Base64.decode(stringPublicKey.getBytes(), Base64.DEFAULT);
            X509EncodedKeySpec X509Key = new X509EncodedKeySpec(byteKey);
            KeyFactory kf = KeyFactory.getInstance("RSA");

            return kf.generatePublic(X509Key);
        } catch (Exception e) {
            CodePushUtils.log(e.getMessage());
            CodePushUtils.log(e.getStackTrace().toString());
            return null;
        }
    }

    public static String getSignatureFilePath(String updateFolderPath) {
        return CodePushUtils.appendPathComponent(
                CodePushUtils.appendPathComponent(updateFolderPath, CodePushConstants.CODE_PUSH_FOLDER_PREFIX),
                CodePushConstants.BUNDLE_JWT_FILE
        );
    }

    public static String getSignature(String folderPath) {
        final String signatureFilePath = getSignatureFilePath(folderPath);

        try {
            return FileUtils.readFileToString(signatureFilePath);
        } catch (IOException e) {
            CodePushUtils.log(e.getMessage());
            CodePushUtils.log(e.getStackTrace().toString());
            return null;
        }
    }

    public static void verifyUpdateSignature(String folderPath, String packageHash, String stringPublicKey) throws CodePushInvalidUpdateException {
        CodePushUtils.log("Verifying signature for folder path: " + folderPath);

        final PublicKey publicKey = parsePublicKey(stringPublicKey);
        if (publicKey == null) {
            throw new CodePushInvalidUpdateException("The update could not be verified because no public key was found.");
        }

        final String signature = getSignature(folderPath);
        if (signature == null) {
            throw new CodePushInvalidUpdateException("The update could not be verified because no signature was found.");
        }

        final Map<String, Object> claims = verifyAndDecodeJWT(signature, publicKey);
        if (claims == null) {
            throw new CodePushInvalidUpdateException("The update could not be verified because it was not signed by a trusted party.");
        }

        final String contentHash = (String) claims.get("contentHash");
        if (contentHash == null) {
            throw new CodePushInvalidUpdateException("The update could not be verified because the signature did not specify a content hash.");
        }

        if (!contentHash.equals(packageHash)) {
            throw new CodePushInvalidUpdateException("The update contents failed the code signing check.");
        }

        CodePushUtils.log("The update contents succeeded the code signing check.");
    }
}
