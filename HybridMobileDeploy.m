#import "HybridMobileDeploy.h"

#import "RCTBridgeModule.h"
#import "RCTRootView.h"
#import "RCTUtils.h"

@implementation HybridMobileDeploy

RCT_EXPORT_MODULE()

RCTBridge * _bridge;

@synthesize bridge = _bridge;

+ (NSString *) getBundleFolderPath
{
    NSString* home = NSHomeDirectory();
    NSString* bundleFolder = [home stringByAppendingPathComponent:@"HybridMobileDeploy/bundle"];
    return bundleFolder;
}

+ (NSString *) getBundlePath
{
    NSString * bundleFolderPath = [self getBundleFolderPath];
    NSString* appBundleName = @"main.jsbundle";
    return [bundleFolderPath stringByAppendingPathComponent:appBundleName];
}

+ (NSString *) getPackageFolderPath
{
    NSString* home = NSHomeDirectory();
    NSString* packageFolder = [home stringByAppendingPathComponent:@"HybridMobileDeploy/package"];
    return packageFolder;
}

+ (NSString *) getPackagePath
{
    NSString * packageFolderPath = [self getPackageFolderPath];
    NSString* appPackageName = @"localpackage.json";
    return [packageFolderPath stringByAppendingPathComponent:appPackageName];
}


+ (NSURL *) getNativeBundleURL
{
    return [[NSBundle mainBundle] URLForResource:@"main" withExtension:@"jsbundle"];
}

+ (NSURL *) getBundleUrl
{
    NSFileManager *fileManager = [NSFileManager defaultManager];

    NSString *bundlePath = [self getBundlePath];
    if ([fileManager fileExistsAtPath:bundlePath]) {
        return [[NSURL alloc] initFileURLWithPath:bundlePath];
    } else {
        return [self getNativeBundleURL];
    }
}

+ (void) loadBundle:(NSString*)rootComponent
{
    dispatch_async(dispatch_get_main_queue(), ^{
        RCTRootView *rootView = [[RCTRootView alloc] initWithBundleURL:[self getBundleUrl]
                                                            moduleName:rootComponent
                                                         launchOptions:nil];
        
        UIViewController *rootViewController = [[UIViewController alloc] init];
        rootViewController.view = rootView;
        [UIApplication sharedApplication].delegate.window.rootViewController = rootViewController;
    });
}

RCT_EXPORT_METHOD(getConfiguration:(RCTResponseSenderBlock)callback)
{
        callback(@[[NSNull null], [HybridMobileDeployConfig getConfiguration]]);
}

RCT_EXPORT_METHOD(installUpdate:(NSDictionary*)updatePackage
                  packageJsonString:(NSString*) packageJsonString
                  callback:(RCTResponseSenderBlock)callback)
{
    dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
        NSURL* url = [NSURL URLWithString:updatePackage[@"downloadUrl"]];
        NSError *err;

        NSString *updateContents = [[NSString alloc] initWithContentsOfURL:url
                                                                  encoding:NSUTF8StringEncoding
                                                                     error:&err];
        if (err) {
            // TODO send download url
            callback(@[RCTMakeError(@"Error downloading url", err, [[NSDictionary alloc] initWithObjectsAndKeys:[url absoluteString],@"updateUrl", nil])]);
        } else {
            dispatch_async(dispatch_get_main_queue(), ^{
                NSError *saveError;
                NSString *bundleFolderPath = [HybridMobileDeploy getBundleFolderPath];
                if (![[NSFileManager defaultManager] fileExistsAtPath:bundleFolderPath]) {
                    [[NSFileManager defaultManager] createDirectoryAtPath:bundleFolderPath withIntermediateDirectories:YES attributes:nil error:&saveError];
                }
                
                [updateContents writeToFile:[HybridMobileDeploy getBundlePath]
                                 atomically:YES
                                   encoding:NSUTF8StringEncoding
                                      error:&saveError];
                if (saveError) {
                    // TODO send file path
                    callback(@[RCTMakeError(@"Error saving file", err, [[NSDictionary alloc] initWithObjectsAndKeys:[HybridMobileDeploy getBundlePath],@"bundlePath", nil])]);
                } else {
                    // save the package info too
                    NSString *packageFolderPath = [HybridMobileDeploy getPackageFolderPath];
                    if (![[NSFileManager defaultManager] fileExistsAtPath:packageFolderPath]) {
                        [[NSFileManager defaultManager] createDirectoryAtPath:packageFolderPath withIntermediateDirectories:YES attributes:nil error:&saveError];
                    }
                    
                    [packageJsonString writeToFile:[HybridMobileDeploy getPackagePath]
                                     atomically:YES
                                       encoding:NSUTF8StringEncoding
                                          error:&saveError];
                    
                    if (saveError) {
                        callback(@[RCTMakeError(@"Error saving file", err, [[NSDictionary alloc] initWithObjectsAndKeys:[HybridMobileDeploy getPackagePath],@"packagePath", nil])]);
                    } else {
                        [HybridMobileDeploy loadBundle:[HybridMobileDeployConfig getRootComponent]];
                        callback(@[[NSNull null]]);
                    }
                }
            });
        }
    });
}



RCT_EXPORT_METHOD(getLocalPackage: (RCTResponseSenderBlock)callback)
{
    
    NSString *path = [HybridMobileDeploy getPackagePath];
    
    dispatch_async(dispatch_get_main_queue(), ^{
        
        NSError* readError;
        NSString *content = [NSString stringWithContentsOfFile:path encoding:NSUTF8StringEncoding error:&readError];
        if (readError) {
            callback(@[RCTMakeError(@"Error finding local package ", readError, [[NSDictionary alloc] initWithObjectsAndKeys:path,@"packagePath", nil]), [NSNull null]]);
        } else {
            NSError * parseError;
            NSData *data = [content dataUsingEncoding:NSUTF8StringEncoding];
            NSDictionary* json = [NSJSONSerialization JSONObjectWithData:data
                                                                 options:kNilOptions
                                                                   error:&parseError];
            if (parseError) {
                callback(@[RCTMakeError(@"Error parsing contents of local package ", parseError, [[NSDictionary alloc] initWithObjectsAndKeys:path,@"packagePath", nil]), [NSNull null]]);
            } else {
                callback(@[[NSNull null], json]);
            }
        }
    });
    
}

@end
