#import "CodePush.h"

#import "RCTBridgeModule.h"
#import "RCTRootView.h"
#import "RCTUtils.h"


@implementation CodePush

RCT_EXPORT_MODULE()

RCTBridge * _bridge;
BOOL usingTestFolder = NO;

@synthesize bridge = _bridge;

+ (NSString *) getBundleFolderPath
{
    NSString* home = NSHomeDirectory();
    NSString* pathExtension = [[@"CodePush/" stringByAppendingString: (usingTestFolder ? @"test/" : @"")] stringByAppendingString: @"bundle"];
    NSString* bundleFolder = [home stringByAppendingPathComponent:pathExtension];
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
    NSString* pathExtension = [[@"CodePush/" stringByAppendingString: (usingTestFolder ? @"test/" : @"")] stringByAppendingString: @"package"];
    NSString* packageFolder = [home stringByAppendingPathComponent:pathExtension];
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

RCT_EXPORT_METHOD(setUsingTestFolder:(BOOL) shouldUseTestFolder)
{
    usingTestFolder = shouldUseTestFolder;
}

RCT_EXPORT_METHOD(getConfiguration:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
    resolve([CodePushConfig getConfiguration]);
}

RCT_EXPORT_METHOD(installUpdate:(NSDictionary*)updatePackage
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
    dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
        NSURL* url = [NSURL URLWithString:updatePackage[@"downloadUrl"]];
        NSError *err;

        NSString *updateContents = [[NSString alloc] initWithContentsOfURL:url
                                                                  encoding:NSUTF8StringEncoding
                                                                     error:&err];
        if (err) {
            // TODO send download url
            return reject(err);
        }
        
        dispatch_async(dispatch_get_main_queue(), ^{
            NSError *saveError;
            NSString *bundleFolderPath = [CodePush getBundleFolderPath];
            if (![[NSFileManager defaultManager] fileExistsAtPath:bundleFolderPath]) {
                [[NSFileManager defaultManager] createDirectoryAtPath:bundleFolderPath withIntermediateDirectories:YES attributes:nil error:&saveError];
            }
            
            [updateContents writeToFile:[CodePush getBundlePath]
                             atomically:YES
                               encoding:NSUTF8StringEncoding
                                  error:&saveError];
            if (saveError) {
                // TODO send file path
                return reject(saveError);
            }
            
            // Save the package info too.
            NSString *packageFolderPath = [CodePush getPackageFolderPath];
            if (![[NSFileManager defaultManager] fileExistsAtPath:packageFolderPath]) {
                [[NSFileManager defaultManager] createDirectoryAtPath:packageFolderPath withIntermediateDirectories:YES attributes:nil error:&saveError];
            }
            
            NSError *updateSerializeError;
            NSData *updateSerializedData = [NSJSONSerialization dataWithJSONObject:updatePackage options:0 error:&updateSerializeError];
            
            if (updateSerializeError) {
                return reject(updateSerializeError);
            }
            
            NSString *packageJsonString = [[NSString alloc] initWithData:updateSerializedData encoding:NSUTF8StringEncoding];
            [packageJsonString writeToFile:[CodePush getPackagePath]
                                atomically:YES
                                  encoding:NSUTF8StringEncoding
                                     error:&saveError];
            
            if (saveError) {
                return reject(saveError);
            }
            
            [CodePush loadBundle:[CodePushConfig getRootComponent]];
            resolve([NSNull null]);
        });
    });
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
