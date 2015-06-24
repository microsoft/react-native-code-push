#import "HybridMobileDeploy.h"

#import "RCTRootView.h"
#import "RCTUtils.h"

@implementation HybridMobileDeploy

RCT_EXPORT_MODULE()

+ (NSString *) getBundleFolderPath
{
    NSString* home = NSHomeDirectory();
    NSString* bundleFolder = [home stringByAppendingPathComponent:@"HybridMobileDeploy"];
    return bundleFolder;
}

+ (NSString *) getBundlePath:(NSString*)bundleName
{
    NSString * bundleFolderPath = [self getBundleFolderPath];
    NSString* appBundleName = [bundleName stringByAppendingString:@".jsbundle"];
    return [bundleFolderPath stringByAppendingPathComponent:appBundleName];
}

+ (NSURL *) getNativeBundleURL:(NSString*)bundleName
{
    return [[NSBundle mainBundle] URLForResource:bundleName withExtension:@"jsbundle"];
}

+ (NSURL *) appBundleUrl:(NSString*)bundleName
        nativeBundleName:(NSString*)nativeBundleName
{
    NSFileManager *fileManager = [NSFileManager defaultManager];

    NSString *bundlePath = [self getBundlePath:bundleName];
    if ([fileManager fileExistsAtPath:bundlePath]) {
        return [[NSURL alloc] initFileURLWithPath:bundlePath];
    } else {
        return [self getNativeBundleURL:nativeBundleName];
    }
}

+ (void) loadBundle:(NSString*)moduleName
   nativeBundleName:(NSString*)nativeBundleName
{
    dispatch_async(dispatch_get_main_queue(), ^{
        RCTRootView *rootView = [[RCTRootView alloc] initWithBundleURL:[self appBundleUrl:moduleName nativeBundleName:nativeBundleName]
                                                            moduleName:moduleName
                                                         launchOptions:nil];

        UIViewController *rootViewController = [[UIViewController alloc] init];
        rootViewController.view = rootView;
        [UIApplication sharedApplication].delegate.window.rootViewController = rootViewController;
    });
}

RCT_EXPORT_METHOD(installUpdateFromUrl:(NSString*)updateUrl
                  bundleName:(NSString*)bundleName
                  nativeBundleName:(NSString*)nativeBundleName
                  failureCallback:(RCTResponseSenderBlock)failureCallback
                  successCallback:(RCTResponseSenderBlock)successCallback)
{
    NSError *parameterError;
    NSMutableDictionary *errorData;
    if (!updateUrl) {
        errorData = [NSMutableDictionary dictionary];
        [errorData setValue:@"missing-updateUrl" forKey:NSLocalizedDescriptionKey];
    } else if (!bundleName) {
        errorData = [NSMutableDictionary dictionary];
        [errorData setValue:@"missing-bundleName" forKey:NSLocalizedDescriptionKey];
    }

    if (errorData) {
        parameterError = [NSError errorWithDomain:@"HybridMobileDeploy"code:200 userInfo:errorData];
        NSDictionary *rctError = RCTMakeError(@"Error with input to installUpdateFromUrl", parameterError, errorData);
        failureCallback(@[rctError]);
    } else {
        dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
            NSURL* url = [NSURL URLWithString:updateUrl];
            NSError *err;

            NSString *updateContents = [[NSString alloc] initWithContentsOfURL:url
                                                                      encoding:NSUTF8StringEncoding
                                                                         error:&err];
            if (err) {
                failureCallback(@[err]);
            } else {
                dispatch_async(dispatch_get_main_queue(), ^{
                    NSError *saveError;
                    NSString *bundleFolderPath = [HybridMobileDeploy getBundleFolderPath];
                    if (![[NSFileManager defaultManager] fileExistsAtPath:bundleFolderPath]) {
                        [[NSFileManager defaultManager] createDirectoryAtPath:bundleFolderPath withIntermediateDirectories:YES attributes:nil error:&saveError];
                    }
                    [updateContents writeToFile:[HybridMobileDeploy getBundlePath:bundleName]
                                     atomically:YES
                                       encoding:NSUTF8StringEncoding
                                          error:&saveError];
                    if (saveError) {
                        failureCallback(@[saveError]);
                    } else {
                        [HybridMobileDeploy loadBundle:bundleName nativeBundleName:nativeBundleName];
                        successCallback(@[]);
                    }
                });
            }
        });
    }
}

@end
