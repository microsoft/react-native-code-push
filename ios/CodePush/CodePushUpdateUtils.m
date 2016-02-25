#import "CodePush.h"
#include <CommonCrypto/CommonDigest.h>

@implementation CodePushUpdateUtils

NSString * const ManifestFolderPrefix = @"CodePush";
NSString * const DefaultJsBundleName = @"main.jsbundle";
NSString * const DefaultAssetsFolderName = @"assets";

+ (void)addContentsOfFolderToManifest:(NSString *)folderPath
                           pathPrefix:(NSString *)pathPrefix
                             manifest:(NSMutableArray *)manifest
                                error:(NSError **)error
{
    NSArray* folderFiles = [[NSFileManager defaultManager]
                            contentsOfDirectoryAtPath:folderPath
                            error:error];
    if (*error) {
        return;
    }
    
    for (NSString *fileName in folderFiles) {
        NSString *fullFilePath = [folderPath stringByAppendingPathComponent:fileName];
        NSString *relativePath = [pathPrefix stringByAppendingPathComponent:fileName];
        BOOL isDir = NO;
        if ([[NSFileManager defaultManager] fileExistsAtPath:fullFilePath
                                                 isDirectory:&isDir] && isDir) {
            [self addContentsOfFolderToManifest:fullFilePath
                                     pathPrefix:relativePath
                                       manifest:manifest
                                          error:error];
            if (*error) {
                return;
            }
        } else {
            NSData *fileContents = [NSData dataWithContentsOfFile:fullFilePath];
            NSString *fileContentsHash = [self computeHash:fileContents];
            [manifest addObject:[[relativePath stringByAppendingString:@":"] stringByAppendingString:fileContentsHash]];
        }
    }
}

+ (NSString *)computeHash:(NSData *)inputData
{
    uint8_t digest[CC_SHA256_DIGEST_LENGTH];
    CC_SHA256(inputData.bytes, inputData.length, digest);
    NSMutableString* inputHash = [NSMutableString stringWithCapacity:CC_SHA256_DIGEST_LENGTH * 2];
    for (int i = 0; i < CC_SHA256_DIGEST_LENGTH; i++) {
        [inputHash appendFormat:@"%02x", digest[i]];
    }
    
    return inputHash;
}

+ (void)copyEntriesInFolder:(NSString *)sourceFolder
                 destFolder:(NSString *)destFolder
                      error:(NSError **)error
{
    NSArray* files = [[NSFileManager defaultManager]
                      contentsOfDirectoryAtPath:sourceFolder
                      error:error];
    if (*error) {
        return;
    }
    
    for (NSString *fileName in files) {
        NSString * fullFilePath = [sourceFolder stringByAppendingPathComponent:fileName];
        BOOL isDir = NO;
        if ([[NSFileManager defaultManager] fileExistsAtPath:fullFilePath
                                                 isDirectory:&isDir] && isDir) {
            NSString *nestedDestFolder = [destFolder stringByAppendingPathComponent:fileName];
            [self copyEntriesInFolder:fullFilePath
                           destFolder:nestedDestFolder
                                error:error];
        } else {
            NSString *destFileName = [destFolder stringByAppendingPathComponent:fileName];
            if ([[NSFileManager defaultManager] fileExistsAtPath:destFileName]) {
                [[NSFileManager defaultManager] removeItemAtPath:destFileName error:error];
                if (*error) {
                    return;
                }
            }
            if (![[NSFileManager defaultManager] fileExistsAtPath:destFolder]) {
                [[NSFileManager defaultManager] createDirectoryAtPath:destFolder
                                          withIntermediateDirectories:YES
                                                           attributes:nil
                                                                error:error];
                if (*error) {
                    return;
                }
            }
            
            [[NSFileManager defaultManager] copyItemAtPath:fullFilePath toPath:destFileName error:error];
            if (*error) {
                return;
            }
        }
    }
}

+ (NSString *)findMainBundleInFolder:(NSString *)folderPath
                               error:(NSError **)error
{
    NSArray* folderFiles = [[NSFileManager defaultManager]
                            contentsOfDirectoryAtPath:folderPath
                            error:error];
    if (*error) {
        return nil;
    }
    
    for (NSString *fileName in folderFiles) {
        NSString *fullFilePath = [folderPath stringByAppendingPathComponent:fileName];
        BOOL isDir = NO;
        if ([[NSFileManager defaultManager] fileExistsAtPath:fullFilePath
                                                 isDirectory:&isDir] && isDir) {
            NSString *mainBundlePathInFolder = [self findMainBundleInFolder:fullFilePath error:error];
            if (*error) {
                return nil;
            }
            
            if (mainBundlePathInFolder) {
                return [fileName stringByAppendingPathComponent:mainBundlePathInFolder];
            }
        } else if ([[fileName pathExtension] isEqualToString:@"bundle"] ||
                   [[fileName pathExtension] isEqualToString:@"jsbundle"] ||
                   [[fileName pathExtension] isEqualToString:@"js"]) {
            return fileName;
        }
    }
    
    return nil;
}

+ (NSString *)getDefaultAssetsFolderName
{
    return DefaultAssetsFolderName;
}

+ (NSString *)getDefaultJsBundleName
{
    return DefaultJsBundleName;
}

+ (NSString *)getHashForBinaryContents:(NSURL *)binaryBundleUrl
                                 error:(NSError **)error
{
    NSString *assetsPath = [CodePushPackage getBinaryAssetsPath];
    NSMutableArray *manifest = [NSMutableArray array];
    [self addContentsOfFolderToManifest:assetsPath
                             pathPrefix:[NSString stringWithFormat:@"%@/%@", [self getManifestFolderPrefix], @"assets"]
                               manifest:manifest
                                  error:error];
    if (*error) {
        return nil;
    }
    
    NSData *jsBundleContents = [NSData dataWithContentsOfURL:binaryBundleUrl];
    NSString *jsBundleContentsHash = [self computeHash:jsBundleContents];
    [manifest addObject:[[NSString stringWithFormat:@"%@/%@", [self getManifestFolderPrefix], [self getDefaultJsBundleName]] stringByAppendingString:jsBundleContentsHash]];
    
    NSArray *sortedManifest = [manifest sortedArrayUsingSelector:@selector(compare:)];
    NSData *manifestData = [NSJSONSerialization dataWithJSONObject:sortedManifest
                                                           options:kNilOptions
                                                             error:error];
    if (*error) {
        return nil;
    }
    
    NSString *manifestString = [[NSString alloc] initWithData:manifestData
                                                    encoding:NSUTF8StringEncoding];
    // The JSON serialization turns path separators into "\/", e.g. "CodePush\/assets\/image.png"
    manifestString = [manifestString stringByReplacingOccurrencesOfString:@"\\/"
                                                               withString:@"/"];
    NSString *manifestHash = [self computeHash:[NSData dataWithBytes:manifestString.UTF8String length:manifestString.length]];
    return manifestHash;
}

+ (NSString *)getManifestFolderPrefix
{
    return ManifestFolderPrefix;
}

+ (BOOL)verifyHashForDiffUpdate:(NSString *)finalUpdateFolder
                   expectedHash:(NSString *)expectedHash
                          error:(NSError **)error
{
    NSMutableArray *updateContentsManifest = [NSMutableArray array];
    [self addContentsOfFolderToManifest:finalUpdateFolder
                             pathPrefix:@""
                               manifest:updateContentsManifest
                                  error:error];
    if (*error) {
        return NO;
    }
    
    NSArray *sortedUpdateContentsManifest = [updateContentsManifest sortedArrayUsingSelector:@selector(compare:)];
    NSData *updateContentsManifestData = [NSJSONSerialization dataWithJSONObject:sortedUpdateContentsManifest
                                                                         options:kNilOptions
                                                                           error:error];
    if (*error) {
        return NO;
    }

    NSString *updateContentsManifestString = [[NSString alloc] initWithData:updateContentsManifestData
                                                                   encoding:NSUTF8StringEncoding];
    // The JSON serialization turns path separators into "\/", e.g. "CodePush\/assets\/image.png"
    updateContentsManifestString = [updateContentsManifestString stringByReplacingOccurrencesOfString:@"\\/"
                                                                                           withString:@"/"];
    NSString *updateContentsManifestHash = [self computeHash:[NSData dataWithBytes:updateContentsManifestString.UTF8String length:updateContentsManifestString.length]];
    return [updateContentsManifestHash isEqualToString:expectedHash];
}

@end