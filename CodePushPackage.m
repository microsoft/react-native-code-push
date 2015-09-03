#import "CodePush.h"

@implementation CodePushPackage

NSString * const PackageInfoFile = @"packages.json";

+ (NSString *)getCodePushPath
{
    return [NSHomeDirectory() stringByAppendingPathComponent:@"CodePush"];
}

+ (NSString *)getCurrentPackageInfoPath
{
    return [[self getCodePushPath] stringByAppendingPathComponent:PackageInfoFile];
}

+ (NSDictionary *)getCurrentPackageInfo:(NSError **)error
{
    NSString *content = [NSString stringWithContentsOfFile:[self getCurrentPackageInfoPath]
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
    
    return json;
}

+ (void)updateCurrentPackageInfo:(NSDictionary *)packageInfo
                           error:(NSError **)error
{
    
    NSData *packageInfoData = [NSJSONSerialization dataWithJSONObject:packageInfo
                                                              options:0
                                                                error:error];
    
    NSString *packageInfoString = [[NSString alloc] initWithData:packageInfoData
                                                        encoding:NSUTF8StringEncoding];
    [packageInfoString writeToFile:[self getCurrentPackageInfoPath]
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
    
    return [self getPackageFolderPath:info[@"currentPackage"]];
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
    
    if (error) {
        return;
    }
    
    NSURL *url = [[NSURL alloc] initWithString:updatePackage[@"downloadUrl"]];
    NSString *updateContents = [[NSString alloc] initWithContentsOfURL:url
                                                              encoding:NSUTF8StringEncoding
                                                                 error:error];
    if (error) {
        return;
    }
    
    [updateContents writeToFile:[packageFolderPath stringByAppendingPathComponent:@"app.jsbundle"]
                     atomically:YES
                       encoding:NSUTF8StringEncoding
                          error:error];
    if (error) {
        return;
    }
    
    NSData *updateSerializedData = [NSJSONSerialization dataWithJSONObject:updatePackage
                                                                   options:0
                                                                     error:error];
    
    if (error) {
        return;
    }
    
    NSString *packageJsonString = [[NSString alloc] initWithData:updateSerializedData encoding:NSUTF8StringEncoding];
    [packageJsonString writeToFile:[packageFolderPath stringByAppendingPathComponent:@"app.json"]
                        atomically:YES
                          encoding:NSUTF8StringEncoding
                             error:error];
}

+ (void)applyPackage:(NSString *)packageHash
               error:(NSError **)error
{
    NSDictionary *info = [self getCurrentPackageInfo:error];
    
    if (error) {
        return;
    }
    
    [info setValue:packageHash forKey:@"currentPackage"];

    [self updateCurrentPackageInfo:info
                             error:error];
}

@end