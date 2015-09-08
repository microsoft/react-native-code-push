#import "CodePush.h"

#import "RCTBridgeModule.h"
#import "RCTRootView.h"
#import "RCTUtils.h"


@implementation CodePush

RCT_EXPORT_MODULE()

RCTBridge * _bridge;
BOOL usingTestFolder = NO;

@synthesize bridge = _bridge;

+ (NSString *) getBundlePath
{
    NSString * bundleFolderPath = [self getPackageFolderPath];
    NSString* appBundleName = @"main.jsbundle";
    return [bundleFolderPath stringByAppendingPathComponent:appBundleName];
}

+ (NSString *) getPackageFolderPath
{
    NSString* home = NSHomeDirectory();
    NSString* pathExtension = [[@"CodePush/" stringByAppendingString: (usingTestFolder ? @"test/" : @"")] stringByAppendingString: @"currentPackage"];
    NSString* packageFolder = [home stringByAppendingPathComponent:pathExtension];
    return packageFolder;
}

+ (NSString *) getPreviousPackageFolderPath
{
    NSString* home = NSHomeDirectory();
    NSString* pathExtension = [[@"CodePush/" stringByAppendingString: (usingTestFolder ? @"test/" : @"")] stringByAppendingString: @"previous"];
    NSString* packageFolder = [home stringByAppendingPathComponent:pathExtension];
    return packageFolder;
}

+ (NSString *) getPackagePath
{
    NSString *packageFolderPath = [self getPackageFolderPath];
    NSString* appPackageName = @"localpackage.json";
    return [packageFolderPath stringByAppendingPathComponent:appPackageName];
}

+ (NSString *) getPreviousPackagePath
{
    NSString * packageFolderPath = [self getPreviousPackageFolderPath];
    NSString* appPackageName = @"localpackage.json";
    return [packageFolderPath stringByAppendingPathComponent:appPackageName];
}

+ (NSURL *) getNativeBundleURL
{
    return [[NSBundle mainBundle] URLForResource:@"main" withExtension:@"jsbundle"];
}

+ (NSURL *) getBundleUrl
{
    NSError *error;
    NSString *packageFolder = [CodePushPackage getCurrentPackageFolderPath:&error];
    
    if (error || !packageFolder) {
        [self getNativeBundleURL];
    }
    
    NSString *packageFile = [packageFolder stringByAppendingPathComponent:@"app.jsbundle"];
    return [[NSURL alloc] initFileURLWithPath:packageFile];
}

+ (void) loadBundle
{
    dispatch_async(dispatch_get_main_queue(), ^{
        RCTRootView *rootView = [[RCTRootView alloc] initWithBundleURL:[self getBundleUrl]
                                                            moduleName:[CodePushConfig getRootComponent]
                                                         launchOptions:nil];
        
        UIViewController *rootViewController = [[UIViewController alloc] init];
        rootViewController.view = rootView;
        [UIApplication sharedApplication].delegate.window.rootViewController = rootViewController;
    });
}

RCT_EXPORT_METHOD(setUsingTestFolder:(BOOL) shouldUseTestFolder)
{
    usingTestFolder = shouldUseTestFolder;
}

RCT_EXPORT_METHOD(getConfiguration:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
    resolve([CodePushConfig getConfiguration]);
}

RCT_EXPORT_METHOD(downloadUpdate:(NSDictionary*)updatePackage
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
    dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
        NSError *err;
        [CodePushPackage downloadPackage:updatePackage
                                   error:&err];
        
        if (err) {
            return reject(err);
        }
        
        NSDictionary *newPackage = [CodePushPackage getPackage:updatePackage[@"packageHash"]
                                                         error:&err];
        
        if (err) {
            return reject(err);
        }
        
        resolve(newPackage);
    });
}

RCT_EXPORT_METHOD(applyUpdate:(NSDictionary*)updatePackage
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
    
    dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
        NSError *error;
        [CodePushPackage applyPackage:updatePackage
                                error:&error];
        
        if (error) {
            reject(error);
        }
        
        [CodePush loadBundle];
        
        //resolve([NSNull null]);
    });
}

RCT_EXPORT_METHOD(getCurrentPackage:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
    dispatch_async(dispatch_get_main_queue(), ^{
        NSError *error;
        NSDictionary *package = [CodePushPackage getCurrentPackage:&error];
        if (error) {
            reject(error);
        } else {
            resolve(package);
        }
    });
}

@end
