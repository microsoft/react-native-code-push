#import "CodePush.h"
#include <CommonCrypto/CommonDigest.h>
#import "JWT.h"

@implementation CodePushUpdateUtils

NSString * const AssetsFolderName = @"assets";
NSString * const BinaryHashKey = @"CodePushBinaryHash";
NSString * const ManifestFolderPrefix = @"CodePush";
NSString * const BundleJWTFile = @".codepushrelease";

/*
 Ignore list for hashing
 */
NSString * const IgnoreMacOSX= @"__MACOSX/";
NSString * const IgnoreDSStore = @".DS_Store";
NSString * const IgnoreCodePushMetadata = @".codepushrelease";

+ (BOOL)isHashIgnoredFor:(NSString *) relativePath
{
    return [relativePath hasPrefix:IgnoreMacOSX]
    || [relativePath isEqualToString:IgnoreDSStore]
    || [relativePath hasSuffix:[NSString stringWithFormat:@"/%@", IgnoreDSStore]]
    || [relativePath isEqualToString:IgnoreCodePushMetadata]
    || [relativePath hasSuffix:[NSString stringWithFormat:@"/%@", IgnoreCodePushMetadata]];
}

+ (BOOL)addContentsOfFolderToManifest:(NSString *)folderPath
                           pathPrefix:(NSString *)pathPrefix
                             manifest:(NSMutableArray *)manifest
                                error:(NSError **)error
{
    NSArray *folderFiles = [[NSFileManager defaultManager]
                            contentsOfDirectoryAtPath:folderPath
                            error:error];
    if (!folderFiles) {
        return NO;
    }
    
    for (NSString *fileName in folderFiles) {

        NSString *fullFilePath = [folderPath stringByAppendingPathComponent:fileName];
        NSString *relativePath = [pathPrefix stringByAppendingPathComponent:fileName];
        
        if([self isHashIgnoredFor:relativePath]){
            continue;
        }
        
        BOOL isDir = NO;
        if ([[NSFileManager defaultManager] fileExistsAtPath:fullFilePath
                                                 isDirectory:&isDir] && isDir) {
            BOOL result = [self addContentsOfFolderToManifest:fullFilePath
                                                   pathPrefix:relativePath
                                                     manifest:manifest
                                                        error:error];
            if (!result) {
                return NO;
            }
        } else {
            NSData *fileContents = [NSData dataWithContentsOfFile:fullFilePath];
            NSString *fileContentsHash = [self computeHashForData:fileContents];
            [manifest addObject:[[relativePath stringByAppendingString:@":"] stringByAppendingString:fileContentsHash]];
        }
    }
 
    return YES;
}

+ (void)addFileToManifest:(NSURL *)fileURL
                 manifest:(NSMutableArray *)manifest
{
    if ([[NSFileManager defaultManager] fileExistsAtPath:[fileURL path]]) {
        NSData *fileContents = [NSData dataWithContentsOfURL:fileURL];
        NSString *fileContentsHash = [self computeHashForData:fileContents];
        [manifest addObject:[NSString stringWithFormat:@"%@/%@:%@", [self manifestFolderPrefix], [fileURL lastPathComponent], fileContentsHash]];
    }
}

+ (NSString *)computeFinalHashFromManifest:(NSMutableArray *)manifest
                                     error:(NSError **)error
{
    //sort manifest strings to make sure, that they are completely equal with manifest strings has been generated in cli!
    NSArray *sortedManifest = [manifest sortedArrayUsingSelector:@selector(compare:)];
    NSData *manifestData = [NSJSONSerialization dataWithJSONObject:sortedManifest
                                                           options:kNilOptions
                                                             error:error];
    if (!manifestData) {
        return nil;
    }
    
    NSString *manifestString = [[NSString alloc] initWithData:manifestData
                                                     encoding:NSUTF8StringEncoding];
    // The JSON serialization turns path separators into "\/", e.g. "CodePush\/assets\/image.png"
    manifestString = [manifestString stringByReplacingOccurrencesOfString:@"\\/"
                                                               withString:@"/"];
    return [self computeHashForData:[NSData dataWithBytes:manifestString.UTF8String length:[manifestString lengthOfBytesUsingEncoding:NSUTF8StringEncoding]]];
}

+ (NSString *)computeHashForData:(NSData *)inputData
{
    uint8_t digest[CC_SHA256_DIGEST_LENGTH];
    CC_SHA256(inputData.bytes, (CC_LONG)inputData.length, digest);
    NSMutableString* inputHash = [NSMutableString stringWithCapacity:CC_SHA256_DIGEST_LENGTH * 2];
    for (int i = 0; i < CC_SHA256_DIGEST_LENGTH; i++) {
        [inputHash appendFormat:@"%02x", digest[i]];
    }
    
    return inputHash;
}

+ (BOOL)copyEntriesInFolder:(NSString *)sourceFolder
                 destFolder:(NSString *)destFolder
                      error:(NSError **)error
{
    NSArray *files = [[NSFileManager defaultManager]
                      contentsOfDirectoryAtPath:sourceFolder
                      error:error];
    if (!files) {
        return NO;
    }
    
    for (NSString *fileName in files) {
        NSString * fullFilePath = [sourceFolder stringByAppendingPathComponent:fileName];
        BOOL isDir = NO;
        if ([[NSFileManager defaultManager] fileExistsAtPath:fullFilePath
                                                 isDirectory:&isDir] && isDir) {
            NSString *nestedDestFolder = [destFolder stringByAppendingPathComponent:fileName];
            BOOL result = [self copyEntriesInFolder:fullFilePath
                                         destFolder:nestedDestFolder
                                              error:error];

            if (!result) {
                return NO;
            }

        } else {
            NSString *destFileName = [destFolder stringByAppendingPathComponent:fileName];
            if ([[NSFileManager defaultManager] fileExistsAtPath:destFileName]) {
                BOOL result = [[NSFileManager defaultManager] removeItemAtPath:destFileName error:error];
                if (!result) {
                    return NO;
                }
            }
            if (![[NSFileManager defaultManager] fileExistsAtPath:destFolder]) {
                BOOL result = [[NSFileManager defaultManager] createDirectoryAtPath:destFolder
                                          withIntermediateDirectories:YES
                                                           attributes:nil
                                                                error:error];
                if (!result) {
                    return NO;
                }
            }
            
            BOOL result = [[NSFileManager defaultManager] copyItemAtPath:fullFilePath toPath:destFileName error:error];
            if (!result) {
                return NO;
            }
        }
    }
    return YES;
}

+ (NSString *)findMainBundleInFolder:(NSString *)folderPath
                    expectedFileName:(NSString *)expectedFileName
                               error:(NSError **)error
{
    NSArray* folderFiles = [[NSFileManager defaultManager]
                            contentsOfDirectoryAtPath:folderPath
                            error:error];
    if (!folderFiles) {
        return nil;
    }
    
    for (NSString *fileName in folderFiles) {
        NSString *fullFilePath = [folderPath stringByAppendingPathComponent:fileName];
        BOOL isDir = NO;
        if ([[NSFileManager defaultManager] fileExistsAtPath:fullFilePath
                                                 isDirectory:&isDir] && isDir) {
            NSString *mainBundlePathInFolder = [self findMainBundleInFolder:fullFilePath
                                                           expectedFileName:expectedFileName
                                                                      error:error];
            if (mainBundlePathInFolder) {
                return [fileName stringByAppendingPathComponent:mainBundlePathInFolder];
            }
        } else if ([fileName isEqualToString:expectedFileName]) {
            return fileName;
        }
    }
    
    return nil;
}

+ (NSString *)assetsFolderName
{
    return AssetsFolderName;
}

+ (NSString *)getHashForBinaryContents:(NSURL *)binaryBundleUrl
                                 error:(NSError **)error
{
    // Get the cached hash from user preferences if it exists.
    NSString *binaryModifiedDate = [self modifiedDateStringOfFileAtURL:binaryBundleUrl];
    NSUserDefaults *preferences = [NSUserDefaults standardUserDefaults];
    NSMutableDictionary *binaryHashDictionary = [preferences objectForKey:BinaryHashKey];
    NSString *binaryHash = nil;
    if (binaryHashDictionary != nil) {
        binaryHash = [binaryHashDictionary objectForKey:binaryModifiedDate];
        if (binaryHash == nil) {
            [preferences removeObjectForKey:BinaryHashKey];
            [preferences synchronize];
        } else {
            return binaryHash;
        }
    }
    
    binaryHashDictionary = [NSMutableDictionary dictionary];
    NSMutableArray *manifest = [NSMutableArray array];
    
    // If the app is using assets, then add
    // them to the generated content manifest.
    NSString *assetsPath = [CodePush bundleAssetsPath];
    if ([[NSFileManager defaultManager] fileExistsAtPath:assetsPath]) {
        
        BOOL result = [self addContentsOfFolderToManifest:assetsPath
                                               pathPrefix:[NSString stringWithFormat:@"%@/%@", [self manifestFolderPrefix], @"assets"]
                                                 manifest:manifest
                                                    error:error];
        if (!result) {
            return nil;
        }
    }
    
    [self addFileToManifest:binaryBundleUrl manifest:manifest];
    [self addFileToManifest:[binaryBundleUrl URLByAppendingPathExtension:@"meta"] manifest:manifest];

    binaryHash = [self computeFinalHashFromManifest:manifest error:error];
    
    // Cache the hash in user preferences. This assumes that the modified date for the
    // JS bundle changes every time a new bundle is generated by the packager.
    [binaryHashDictionary setObject:binaryHash forKey:binaryModifiedDate];
    [preferences setObject:binaryHashDictionary forKey:BinaryHashKey];
    [preferences synchronize];
    return binaryHash;
}

+ (NSString *)manifestFolderPrefix
{
    return ManifestFolderPrefix;
}

+ (NSString *)modifiedDateStringOfFileAtURL:(NSURL *)fileURL
{
    if (fileURL != nil) {
        NSDictionary *fileAttributes = [[NSFileManager defaultManager] attributesOfItemAtPath:[fileURL path] error:nil];
        NSDate *modifiedDate = [fileAttributes objectForKey:NSFileModificationDate];
        return [NSString stringWithFormat:@"%f", [modifiedDate timeIntervalSince1970]];
    } else {
        return nil;
    }
}

+ (BOOL)verifyFolderHash:(NSString *)finalUpdateFolder
                   expectedHash:(NSString *)expectedHash
                          error:(NSError **)error
{
    CPLog(@"Verifying hash for folder path: %@", finalUpdateFolder);
    
    NSMutableArray *updateContentsManifest = [NSMutableArray array];
    BOOL result = [self addContentsOfFolderToManifest:finalUpdateFolder
                                           pathPrefix:@""
                                             manifest:updateContentsManifest
                                                error:error];
    
    CPLog(@"Manifest string: %@", updateContentsManifest);
    
    if (!result) {
        return NO;
    }

    NSString *updateContentsManifestHash = [self computeFinalHashFromManifest:updateContentsManifest
                                                                        error:error];
    if (!updateContentsManifestHash) {
        return NO;
    }
    
    CPLog(@"Expected hash: %@, actual hash: %@", expectedHash, updateContentsManifestHash);
    
    return [updateContentsManifestHash isEqualToString:expectedHash];
}

// remove BEGIN / END tags and line breaks from public key string
+ (NSString *)getKeyValueFromPublicKeyString:(NSString *)publicKeyString
{
    publicKeyString = [publicKeyString stringByReplacingOccurrencesOfString:@"-----BEGIN PUBLIC KEY-----\n"
                                                                 withString:@""];
    publicKeyString = [publicKeyString stringByReplacingOccurrencesOfString:@"-----END PUBLIC KEY-----"
                                                                 withString:@""];
    publicKeyString = [publicKeyString stringByReplacingOccurrencesOfString:@"\n"
                                                                 withString:@""];

    return publicKeyString;
}

+ (NSString *)getSignatureFilePath:(NSString *)updateFolderPath
{
    return [NSString stringWithFormat:@"%@/%@/%@", updateFolderPath, ManifestFolderPrefix, BundleJWTFile];
}

+ (NSString *)getSignatureFor:(NSString *)folderPath
                        error:(NSError **)error
{
    NSString *signatureFilePath = [self getSignatureFilePath:folderPath];
    if ([[NSFileManager defaultManager] fileExistsAtPath:signatureFilePath]) {
        return [NSString stringWithContentsOfFile:signatureFilePath encoding:NSUTF8StringEncoding error:error];
    } else {
        *error = [CodePushErrorUtils errorWithMessage:[NSString stringWithFormat: @"Cannot find signature at %@", signatureFilePath]];
        return nil;
    }
}

+ (NSDictionary *) verifyAndDecodeJWT:(NSString *)jwt
     withPublicKey:(NSString *)publicKey
             error:(NSError **)error
{
    id <JWTAlgorithmDataHolderProtocol> verifyDataHolder = [JWTAlgorithmRSFamilyDataHolder new].keyExtractorType([JWTCryptoKeyExtractor publicKeyWithPEMBase64].type).algorithmName(@"RS256").secret(publicKey);
    
    JWTCodingBuilder *verifyBuilder = [JWTDecodingBuilder decodeMessage:jwt].addHolder(verifyDataHolder);
    JWTCodingResultType *verifyResult = verifyBuilder.result;
    if (verifyResult.successResult) {
        return verifyResult.successResult.payload;
    }
    else {
        *error = verifyResult.errorResult.error;
        return nil;
    }
}

+ (BOOL)verifyUpdateSignatureFor:(NSString *)folderPath
                    expectedHash:(NSString *)newUpdateHash
                   withPublicKey:(NSString *)publicKeyString
                           error:(NSError **)error
{
    NSLog(@"Verifying signature for folder path: %@", folderPath);
    
    NSString *publicKey = [self getKeyValueFromPublicKeyString: publicKeyString];
    
    NSError *signatureVerificationError;
    NSString *signature = [self getSignatureFor: folderPath
                                          error: &signatureVerificationError];
    if (signatureVerificationError) {
        CPLog(@"The update could not be verified because no signature was found. %@", signatureVerificationError);
        *error = signatureVerificationError;
        return false;
    }
    
    NSError *payloadDecodingError;
    NSDictionary *envelopedPayload = [self verifyAndDecodeJWT:signature withPublicKey:publicKey error:&payloadDecodingError];
    if(payloadDecodingError){
        CPLog(@"The update could not be verified because it was not signed by a trusted party. %@", payloadDecodingError);
        *error = payloadDecodingError;
        return false;
    }
    
    CPLog(@"JWT signature verification succeeded, payload content:  %@", envelopedPayload);
    
    if(![envelopedPayload objectForKey:@"contentHash"]){
        CPLog(@"The update could not be verified because the signature did not specify a content hash.");
        return false;
    }
    
    NSString *contentHash = envelopedPayload[@"contentHash"];
    
    return [contentHash isEqualToString:newUpdateHash];
}

@end
