#import "RCTBridgeModule.h"

@interface CodePush : NSObject <RCTBridgeModule>

/*
 * This method is used to retrieve the URL for the most recent
 * version of the JavaScript bundle. This could be either the
 * bundle that was packaged with the app binary, or the bundle
 * that was downloaded as part of a CodePush update. The value returned
 * should be used to "bootstrap" the React Native bridge.
 *
 * This method assumes that your JS bundle is named "main.jsbundle"
 * and therefore, if it isn't, you should use either the bundleURLForResource:
 * or bundleURLForResource:withExtension: methods to override that behavior.
*/
+ (NSURL *)bundleURL;

+ (NSURL *)bundleURLForResource:(NSString *)resourceName;

+ (NSURL *)bundleURLForResource:(NSString *)resourceName
                  withExtension:(NSString *)resourceExtension;

+ (NSString *)getDocumentsDirectory;

@end

@interface CodePushConfig : NSObject

@property (readonly) NSString *appVersion;
@property (readonly) NSString *buildVersion;
@property (readonly) NSDictionary *configuration;
@property (copy) NSString *deploymentKey;
@property (copy) NSString *serverURL;

+ (instancetype)current;

@end

@interface CodePushDownloadHandler : NSObject <NSURLConnectionDelegate>

@property (strong) NSOutputStream *outputFileStream;
@property long expectedContentLength;
@property long receivedContentLength;
@property (copy) void (^progressCallback)(long, long);
@property (copy) void (^doneCallback)();
@property (copy) void (^failCallback)(NSError *err);

- (id)init:(NSString *)downloadFilePath
progressCallback:(void (^)(long, long))progressCallback
doneCallback:(void (^)())doneCallback
failCallback:(void (^)(NSError *err))failCallback;

- (void)download:(NSString*)url;

@end

@interface CodePushPackage : NSObject

+ (void)installPackage:(NSDictionary *)updatePackage
               error:(NSError **)error;

+ (NSDictionary *)getCurrentPackage:(NSError **)error;
+ (NSString *)getCurrentPackageFolderPath:(NSError **)error;
+ (NSString *)getCurrentPackageBundlePath:(NSError **)error;
+ (NSString *)getCurrentPackageHash:(NSError **)error;

+ (NSDictionary *)getPackage:(NSString *)packageHash
                       error:(NSError **)error;

+ (NSString *)getPackageFolderPath:(NSString *)packageHash;


+ (void)downloadPackage:(NSDictionary *)updatePackage
       progressCallback:(void (^)(long, long))progressCallback
           doneCallback:(void (^)())doneCallback
           failCallback:(void (^)(NSError *err))failCallback;

+ (void)rollbackPackage;

@end

typedef NS_ENUM(NSInteger, CodePushInstallMode) {
    CodePushInstallModeImmediate,
    CodePushInstallModeOnNextRestart,
    CodePushInstallModeOnNextResume
};