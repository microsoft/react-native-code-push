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
    NSString* bundleFolder = [home stringByAppendingPathComponent:@"HybridMobileDeploy"];
    return bundleFolder;
}

+ (NSString *) getBundlePath
{
    NSString * bundleFolderPath = [self getBundleFolderPath];
    NSString* appBundleName = @"main.jsbundle";
    return [bundleFolderPath stringByAppendingPathComponent:appBundleName];
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

- (void) reloadBundle
{
    dispatch_async(dispatch_get_main_queue(), ^{
        self.bridge.bundleURL = [HybridMobileDeploy getBundleUrl];
        [self.bridge reload];
    });
}

RCT_EXPORT_METHOD(getConfiguration:(RCTResponseSenderBlock)callback)
{
        callback(@[[NSNull null], [HybridMobileDeployConfig getConfiguration]]);
}

RCT_EXPORT_METHOD(installUpdateFromUrl:(NSString*)updateUrl
                  callback:(RCTResponseSenderBlock)callback)
{
    dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
        NSURL* url = [NSURL URLWithString:updateUrl];
        NSError *err;

        NSString *updateContents = [[NSString alloc] initWithContentsOfURL:url
                                                                  encoding:NSUTF8StringEncoding
                                                                     error:&err];
        
        if (err) {
            // TODO send download url
            callback(@[RCTMakeError(@"Error downloading url", err, [[NSDictionary alloc] initWithObjectsAndKeys:updateUrl,@"updateUrl", nil])]);
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
                    [self reloadBundle];
                    callback(@[[NSNull null]]);
                }
            });
        }
    });
}

@end
