#import "CodePush.h"

@implementation CodePushPackage

NSString * const StatusFile = @"codepush.json";

+ (NSString *)getCodePushPath
{
    return [[CodePush getDocumentsDirectory] stringByAppendingPathComponent:@"CodePush"];
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
                            error:(NSError **)error
{
    NSString *packageFolderPath = [self getPackageFolderPath:updatePackage[@"packageHash"]];
    
    if (![[NSFileManager defaultManager] fileExistsAtPath:packageFolderPath]) {
        [[NSFileManager defaultManager] createDirectoryAtPath:packageFolderPath
                                  withIntermediateDirectories:YES
                                                   attributes:nil
                                                        error:error];
    }
    
    if (*error) {
        return;
    }
    
    NSURL *url = [[NSURL alloc] initWithString:updatePackage[@"downloadUrl"]];
    NSString *updateContents = [[NSString alloc] initWithContentsOfURL:url
                                                              encoding:NSUTF8StringEncoding
                                                                 error:error];
    if (*error) {
        return;
    }
    
    [updateContents writeToFile:[packageFolderPath stringByAppendingPathComponent:@"app.jsbundle"]
                     atomically:YES
                       encoding:NSUTF8StringEncoding
                          error:error];
    if (*error) {
        return;
    }
    
    NSData *updateSerializedData = [NSJSONSerialization dataWithJSONObject:updatePackage
                                                                   options:0
                                                                     error:error];
    
    if (*error) {
        return;
    }
    
    NSString *packageJsonString = [[NSString alloc] initWithData:updateSerializedData encoding:NSUTF8StringEncoding];
    [packageJsonString writeToFile:[packageFolderPath stringByAppendingPathComponent:@"app.json"]
                        atomically:YES
                          encoding:NSUTF8StringEncoding
                             error:error];
}

+ (void)applyPackage:(NSDictionary *)updatePackage
               error:(NSError **)error
{
    NSString *packageHash = updatePackage[@"packageHash" ];
    NSMutableDictionary *info = [self getCurrentPackageInfo:error];
    
    if (*error) {
        return;
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

@end