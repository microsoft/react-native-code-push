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
            reject(err);
        } else {
            resolve([NSNull null]);
        }
    });
}

RCT_EXPORT_METHOD(applyUpdate:(NSDictionary*)updatePackage
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
    [CodePush loadBundle:[CodePushConfig getRootComponent]];
    resolve([NSNull null]);
}

RCT_EXPORT_METHOD(writeToLocalPackage:(NSString*)packageJsonString
                  callback:(RCTResponseSenderBlock)callback)
{
    NSError *saveError;
    
    // Save the package info too.
    NSString *packageFolderPath = [CodePush getPackageFolderPath];
    if (![[NSFileManager defaultManager] fileExistsAtPath:packageFolderPath]) {
        [[NSFileManager defaultManager] createDirectoryAtPath:packageFolderPath withIntermediateDirectories:YES attributes:nil error:&saveError];
    }
    
    [packageJsonString writeToFile:[CodePush getPackagePath]
                        atomically:YES
                          encoding:NSUTF8StringEncoding
                             error:&saveError];
    
    if (saveError) {
        callback(@[RCTMakeError(@"Error saving file", saveError, [[NSDictionary alloc] initWithObjectsAndKeys:[CodePush getPackagePath],@"packagePath", nil])]);
    } else {
        callback(@[[NSNull null]]);
    }
    
}

RCT_EXPORT_METHOD(removeLocalPackage: (RCTResponseSenderBlock)callback)
{
    NSError *error;
    
    // Save the package info too.
    NSString *packagePath = [CodePush getPackagePath];
    if ([[NSFileManager defaultManager] fileExistsAtPath:packagePath]) {
        [[NSFileManager defaultManager] removeItemAtPath:packagePath error: &error];
    }
         
    if (error) {
        callback(@[RCTMakeError(@"Error saving file", error, [[NSDictionary alloc] initWithObjectsAndKeys:[CodePush getPackagePath],@"packagePath", nil])]);
    } else {
        callback(@[[NSNull null]]);
    }
}


RCT_EXPORT_METHOD(getCurrentPackage:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
    
    NSString *path = [CodePush getPackagePath];
    
    dispatch_async(dispatch_get_main_queue(), ^{
        
        NSError* readError;
        NSString *content = [NSString stringWithContentsOfFile:path encoding:NSUTF8StringEncoding error:&readError];
        if (readError) {
            reject(readError);
        } else {
            NSError * parseError;
            NSData *data = [content dataUsingEncoding:NSUTF8StringEncoding];
            NSDictionary* json = [NSJSONSerialization JSONObjectWithData:data
                                                                 options:kNilOptions
                                                                   error:&parseError];
            if (parseError) {
                reject(parseError);
            } else {
                resolve(json);
            }
        }
    });
}

@end
