#import "CodePush.h"
#import "SSZipArchive.h"

@implementation CodePushPackage

NSString * const CodePushErrorDomain = @"CodePushError";
const int CodePushErrorCode = -1;
NSString * const DiffManifestFileName = @"hotcodepush.json";
NSString * const DownloadFileName = @"download.zip";
NSString * const RelativeBundlePathKey = @"bundlePath";
NSString * const StatusFile = @"codepush.json";
NSString * const UpdateBundleFileName = @"app.jsbundle";
NSString * const UnzippedFolderName = @"unzipped";

+ (NSString *)getCodePushPath
{
    NSString* codePushPath = [[CodePush getApplicationSupportDirectory] stringByAppendingPathComponent:@"CodePush"];
    if ([CodePush isUsingTestConfiguration]) {
        codePushPath = [codePushPath stringByAppendingPathComponent:@"TestPackages"];
    }
    
    return codePushPath;
}

+ (NSString *)getDownloadFilePath
{
    return [[self getCodePushPath] stringByAppendingPathComponent:DownloadFileName];
}

+ (NSString *)getUnzippedFolderPath
{
    return [[self getCodePushPath] stringByAppendingPathComponent:UnzippedFolderName];
}

+ (NSString *)getStatusFilePath
{
    return [[self getCodePushPath] stringByAppendingPathComponent:StatusFile];
}

+ (NSMutableDictionary *)getCurrentPackageInfo:(NSError **)error
{
    NSString *statusFilePath = [self getStatusFilePath];
    if (![[NSFileManager defaultManager] fileExistsAtPath:statusFilePath]) {
        return [NSMutableDictionary dictionary];
    }
    
    NSString *content = [NSString stringWithContentsOfFile:statusFilePath
                                                  encoding:NSUTF8StringEncoding
                                                     error:error];
    if (*error) {
        return NULL;
    }
    
    NSData *data = [content dataUsingEncoding:NSUTF8StringEncoding];
    NSDictionary* json = [NSJSONSerialization JSONObjectWithData:data
                                                         options:kNilOptions
                                                           error:error];
    if (*error) {
        return NULL;
    }
    
    return [json mutableCopy];
}

+ (void)updateCurrentPackageInfo:(NSDictionary *)packageInfo
                           error:(NSError **)error
{
    
    NSData *packageInfoData = [NSJSONSerialization dataWithJSONObject:packageInfo
                                                              options:0
                                                                error:error];
    
    NSString *packageInfoString = [[NSString alloc] initWithData:packageInfoData
                                                        encoding:NSUTF8StringEncoding];
    [packageInfoString writeToFile:[self getStatusFilePath]
                        atomically:YES
                          encoding:NSUTF8StringEncoding
                             error:error];
}

+ (NSString *)getCurrentPackageFolderPath:(NSError **)error
{
    NSDictionary *info = [self getCurrentPackageInfo:error];
    
    if (*error) {
        return NULL;
    }
    
    NSString *packageHash = info[@"currentPackage"];
    
    if (!packageHash) {
        return NULL;
    }
    
    return [self getPackageFolderPath:packageHash];
}

+ (NSString *)getCurrentPackageBundlePath:(NSError **)error
{
    NSString *packageFolder = [self getCurrentPackageFolderPath:error];
    
    if(*error) {
        return NULL;
    }
    
    NSDictionary *currentPackage = [self getCurrentPackage:error];
    
    if(*error) {
        return NULL;
    }
    
    NSString *relativeBundlePath = [currentPackage objectForKey:RelativeBundlePathKey];
    if (relativeBundlePath) {
        return [packageFolder stringByAppendingPathComponent:relativeBundlePath];
    } else {
        return [packageFolder stringByAppendingPathComponent:UpdateBundleFileName];
    }
}

+ (NSString *)getCurrentPackageHash:(NSError **)error
{
    NSDictionary *info = [self getCurrentPackageInfo:error];
    if (*error) {
        return NULL;
    }
    
    return info[@"currentPackage"];
}

+ (NSString *)getPreviousPackageHash:(NSError **)error
{
    NSDictionary *info = [self getCurrentPackageInfo:error];
    if (*error) {
        return NULL;
    }
    
    return info[@"previousPackage"];
}

+ (NSDictionary *)getCurrentPackage:(NSError **)error
{
    NSString *folderPath = [CodePushPackage getCurrentPackageFolderPath:error];
    if (!*error) {
        if (!folderPath) {
            return [NSDictionary dictionary];
        }
        
        NSString *packagePath = [folderPath stringByAppendingPathComponent:@"app.json"];
        NSString *content = [NSString stringWithContentsOfFile:packagePath
                                                      encoding:NSUTF8StringEncoding
                                                         error:error];
        if (!*error) {
            NSData *data = [content dataUsingEncoding:NSUTF8StringEncoding];
            NSDictionary* jsonDict = [NSJSONSerialization JSONObjectWithData:data
                                                                     options:kNilOptions
                                                                       error:error];

            return jsonDict;
        }
    }
    
    return NULL;
}

+ (NSDictionary *)getPackage:(NSString *)packageHash
                       error:(NSError **)error
{
    NSString *folderPath = [self getPackageFolderPath:packageHash];
    
    if (!folderPath) {
        return [NSDictionary dictionary];
    }
    
    NSString *packageFilePath = [folderPath stringByAppendingPathComponent:@"app.json"];
    
    NSString *content = [NSString stringWithContentsOfFile:packageFilePath
                                                  encoding:NSUTF8StringEncoding
                                                     error:error];
    if (!*error) {
        NSData *data = [content dataUsingEncoding:NSUTF8StringEncoding];
        NSDictionary* jsonDict = [NSJSONSerialization JSONObjectWithData:data
                                                                 options:kNilOptions
                                                                   error:error];
        
        return jsonDict;
    }
    
    return NULL;
}

+ (NSString *)getPackageFolderPath:(NSString *)packageHash
{
    return [[self getCodePushPath] stringByAppendingPathComponent:packageHash];
}

+ (void)downloadPackage:(NSDictionary *)updatePackage
       progressCallback:(void (^)(long long, long long))progressCallback
           doneCallback:(void (^)())doneCallback
           failCallback:(void (^)(NSError *err))failCallback
{
    NSString *newPackageFolderPath = [self getPackageFolderPath:updatePackage[@"packageHash"]];
    NSError *error = nil;
    
    if (![[NSFileManager defaultManager] fileExistsAtPath:newPackageFolderPath]) {
        [[NSFileManager defaultManager] createDirectoryAtPath:newPackageFolderPath
                                  withIntermediateDirectories:YES
                                                   attributes:nil
                                                        error:&error];
    }
    
    if (error) {
        return failCallback(error);
    }
    
    NSString *downloadFilePath = [self getDownloadFilePath];
    NSString *bundleFilePath = [newPackageFolderPath stringByAppendingPathComponent:UpdateBundleFileName];
    
    CodePushDownloadHandler *downloadHandler = [[CodePushDownloadHandler alloc]
        init:downloadFilePath
        progressCallback:progressCallback
        doneCallback:^(BOOL isZip) {
            NSError *error = nil;
            NSString * unzippedFolderPath = [CodePushPackage getUnzippedFolderPath];
            NSMutableDictionary * mutableUpdatePackage = [updatePackage mutableCopy];
            if (isZip) {
                NSError *nonFailingError = nil;
                
                [SSZipArchive unzipFileAtPath:downloadFilePath
                                toDestination:unzippedFolderPath];
                [[NSFileManager defaultManager] removeItemAtPath:downloadFilePath
                                                           error:&nonFailingError];
                if (nonFailingError) {
                    NSLog(@"Error deleting downloaded file: %@", nonFailingError);
                    nonFailingError = nil;
                }
                
                NSString *diffManifestFilePath = [unzippedFolderPath stringByAppendingPathComponent:DiffManifestFileName];
                
                if ([[NSFileManager defaultManager] fileExistsAtPath:diffManifestFilePath]) {
                    // Copy the current package to the new package.
                    NSString *currentPackageFolderPath = [self getCurrentPackageFolderPath:&error];
                    if (error) {
                        failCallback(error);
                        return;
                    }
                    
                    [CodePushPackage copyEntriesInFolder:currentPackageFolderPath
                                              destFolder:newPackageFolderPath
                                                   error:&error];
                    if (error) {
                        failCallback(error);
                        return;
                    }
                    
                    // Delete files mentioned in the manifest.
                    NSString *manifestContent = [NSString stringWithContentsOfFile:diffManifestFilePath
                                                                          encoding:NSUTF8StringEncoding
                                                                             error:&error];
                    if (error) {
                        failCallback(error);
                        return;
                    }
                    
                    NSData *data = [manifestContent dataUsingEncoding:NSUTF8StringEncoding];
                    NSDictionary *manifestJSON = [NSJSONSerialization JSONObjectWithData:data
                                                                                 options:kNilOptions
                                                                                   error:&error];
                    NSArray *deletedFiles = manifestJSON[@"deletedFiles"];
                    for (NSString *deletedFileName in deletedFiles) {
                        [[NSFileManager defaultManager] removeItemAtPath:[newPackageFolderPath stringByAppendingPathComponent:deletedFileName]
                                                                   error:&nonFailingError];
                        
                        if (nonFailingError) {
                            NSLog(@"Error deleting file from current package: %@", nonFailingError);
                            nonFailingError = nil;
                        }
                    }
                }
                
                [CodePushPackage copyEntriesInFolder:unzippedFolderPath
                                          destFolder:newPackageFolderPath
                                               error:&error];
                [[NSFileManager defaultManager] removeItemAtPath:unzippedFolderPath
                                                           error:&nonFailingError];
                if (nonFailingError) {
                    NSLog(@"Error deleting downloaded file: %@", nonFailingError);
                    nonFailingError = nil;
                }
                
                NSString *relativeBundlePath = [self findMainBundleInFolder:newPackageFolderPath
                                                                      error:&error];
                if (error) {
                    failCallback(error);
                    return;
                }
                
                if (relativeBundlePath) {
                    NSString *absoluteBundlePath = [newPackageFolderPath stringByAppendingPathComponent:relativeBundlePath];
                    NSDictionary *bundleFileAttributes = [[[NSFileManager defaultManager] attributesOfItemAtPath:absoluteBundlePath error:&error] mutableCopy];
                    if (error) {
                        failCallback(error);
                        return;
                    }
                    
                    [bundleFileAttributes setValue:[NSDate date] forKey:NSFileModificationDate];
                    [[NSFileManager defaultManager] setAttributes:bundleFileAttributes
                                                     ofItemAtPath:absoluteBundlePath
                                                            error:&error];
                    if (error) {
                        failCallback(error);
                        return;
                    }
                    
                    [mutableUpdatePackage setValue:relativeBundlePath forKey:RelativeBundlePathKey];
                } else {
                    error = [[NSError alloc] initWithDomain:CodePushErrorDomain
                                                       code:CodePushErrorCode
                                                   userInfo:@{
                                                              NSLocalizedDescriptionKey:
                                                                  NSLocalizedString(@"Update is invalid - no files with extension .jsbundle or .bundle were found in the update package.", nil)
                                                              }];
                    failCallback(error);
                    return;
                }
            } else {
                if ([[NSFileManager defaultManager] fileExistsAtPath:bundleFilePath]) {
                    [[NSFileManager defaultManager] removeItemAtPath:bundleFilePath error:&error];
                    if (error) {
                        failCallback(error);
                        return;
                    }
                }
                
                [[NSFileManager defaultManager] moveItemAtPath:downloadFilePath
                                                        toPath:bundleFilePath
                                                         error:&error];
                if (error) {
                    failCallback(error);
                    return;
                }
            }
            
            NSData *updateSerializedData = [NSJSONSerialization dataWithJSONObject:mutableUpdatePackage
                                                                           options:0
                                                                             error:&error];
            NSString *packageJsonString = [[NSString alloc] initWithData:updateSerializedData
                                                                encoding:NSUTF8StringEncoding];
            
            [packageJsonString writeToFile:[newPackageFolderPath stringByAppendingPathComponent:@"app.json"]
                                atomically:YES
                                  encoding:NSUTF8StringEncoding
                                     error:&error];
            if (error) {
                failCallback(error);
            } else {
                doneCallback();
            }
        }

        failCallback:failCallback];
    
    [downloadHandler download:updatePackage[@"downloadUrl"]];
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
            [[fileName pathExtension] isEqualToString:@"jsbundle"]) {
            return fileName;
        }
    }
    
    return nil;
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

+ (void)installPackage:(NSDictionary *)updatePackage
               error:(NSError **)error
{
    NSString *packageHash = updatePackage[@"packageHash"];
    NSMutableDictionary *info = [self getCurrentPackageInfo:error];
    
    if (*error) {
        return;
    }
    
    NSString *previousPackageHash = [self getPreviousPackageHash:error];
    if (!*error && previousPackageHash && ![previousPackageHash isEqualToString:packageHash]) {
        NSString *previousPackageFolderPath = [self getPackageFolderPath:previousPackageHash];
        // Error in deleting old package will not cause the entire operation to fail.
        NSError *deleteError;
        [[NSFileManager defaultManager] removeItemAtPath:previousPackageFolderPath
                                                   error:&deleteError];
        if (deleteError) {
            NSLog(@"Error deleting old package: %@", deleteError);
        }
    }
    
    [info setValue:info[@"currentPackage"] forKey:@"previousPackage"];
    [info setValue:packageHash forKey:@"currentPackage"];

    [self updateCurrentPackageInfo:info
                             error:error];
}

+ (void)rollbackPackage
{
    NSError *error;
    NSMutableDictionary *info = [self getCurrentPackageInfo:&error];
    
    if (error) {
        return;
    }
    
    [info setValue:info[@"previousPackage"] forKey:@"currentPackage"];
    [info removeObjectForKey:@"previousPackage"];
    
    [self updateCurrentPackageInfo:info error:&error];
}

+ (void)downloadAndReplaceCurrentBundle:(NSString *)remoteBundleUrl
{
    NSURL *urlRequest = [NSURL URLWithString:remoteBundleUrl];
    NSError *error = nil;
    NSString *downloadedBundle = [NSString stringWithContentsOfURL:urlRequest
                                                          encoding:NSUTF8StringEncoding
                                                             error:&error];
    
    if (error) {
        NSLog(@"Error downloading from URL %@", remoteBundleUrl);
    } else {
        NSString *currentPackageBundlePath = [self getCurrentPackageBundlePath:&error];
        [downloadedBundle writeToFile:currentPackageBundlePath
                           atomically:YES
                             encoding:NSUTF8StringEncoding
                                error:&error];
    }
}

+ (void)clearUpdates
{
    [[NSFileManager defaultManager] removeItemAtPath:[self getCodePushPath] error:nil];
    [[NSFileManager defaultManager] removeItemAtPath:[self getStatusFilePath] error:nil];
}

@end
