#import <Foundation/Foundation.h>

@interface CodePush : NSObject

+ (NSURL *)binaryBundleURL;
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

+ (NSString *)getApplicationSupportDirectory;

/*
 * This methods allows dynamically setting the app's
 * deployment key, in addition to setting it via
 * the Info.plist file's CodePushDeploymentKey setting.
 */
+ (void)setDeploymentKey:(NSString *)deploymentKey;

// The below methods are only used during tests.
+ (BOOL)isUsingTestConfiguration;
+ (void)setUsingTestConfiguration:(BOOL)shouldUseTestConfiguration;
+ (void)clearUpdates;

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
@property long long expectedContentLength;
@property long long receivedContentLength;
@property dispatch_queue_t operationQueue;
@property (copy) void (^progressCallback)(long long, long long);
@property (copy) void (^doneCallback)(BOOL);
@property (copy) void (^failCallback)(NSError *err);

- (id)init:(NSString *)downloadFilePath
operationQueue:(dispatch_queue_t)operationQueue
progressCallback:(void (^)(long long, long long))progressCallback
doneCallback:(void (^)(BOOL))doneCallback
failCallback:(void (^)(NSError *err))failCallback;

- (void)download:(NSString*)url;

@end

@interface CodePushErrorUtils : NSObject

+ (NSError *)errorWithMessage:(NSString *)errorMessage;
+ (BOOL)isCodePushError:(NSError *)error;

@end

@interface CodePushPackage : NSObject

+ (void)downloadPackage:(NSDictionary *)updatePackage
 expectedBundleFileName:(NSString *)expectedBundleFileName
         operationQueue:(dispatch_queue_t)operationQueue
       progressCallback:(void (^)(long long, long long))progressCallback
           doneCallback:(void (^)())doneCallback
           failCallback:(void (^)(NSError *err))failCallback;

+ (NSString *)getBinaryAssetsPath;
+ (NSDictionary *)getCurrentPackage:(NSError **)error;
+ (NSDictionary *)getPreviousPackage:(NSError **)error;
+ (NSString *)getCurrentPackageFolderPath:(NSError **)error;
+ (NSString *)getCurrentPackageBundlePath:(NSError **)error;
+ (NSString *)getCurrentPackageHash:(NSError **)error;

+ (NSDictionary *)getPackage:(NSString *)packageHash
                       error:(NSError **)error;

+ (NSString *)getPackageFolderPath:(NSString *)packageHash;

+ (void)installPackage:(NSDictionary *)updatePackage
   removePendingUpdate:(BOOL)removePendingUpdate
                 error:(NSError **)error;

+ (void)rollbackPackage;

// The below methods are only used during tests.
+ (void)clearUpdates;
+ (void)downloadAndReplaceCurrentBundle:(NSString *)remoteBundleUrl;

@end

@interface CodePushTelemetryManager : NSObject

+ (NSDictionary *)getBinaryUpdateReport:(NSString *)appVersion;
+ (NSDictionary *)getRetryStatusReport;
+ (NSDictionary *)getRollbackReport:(NSDictionary *)lastFailedPackage;
+ (NSDictionary *)getUpdateReport:(NSDictionary *)currentPackage;
+ (void)recordStatusReported:(NSDictionary *)statusReport;
+ (void)saveStatusReportForRetry:(NSDictionary *)statusReport;

@end

@interface CodePushUpdateUtils : NSObject

+ (void)copyEntriesInFolder:(NSString *)sourceFolder
                 destFolder:(NSString *)destFolder
                      error:(NSError **)error;

+ (NSString *)findMainBundleInFolder:(NSString *)folderPath
                    expectedFileName:(NSString *)expectedFileName
                               error:(NSError **)error;

+ (NSString *)assetsFolderName;
+ (NSString *)getHashForBinaryContents:(NSURL *)binaryBundleUrl
                                 error:(NSError **)error;

+ (NSString *)manifestFolderPrefix;
+ (NSString *)modifiedDateStringOfFileAtURL:(NSURL *)fileURL;

+ (BOOL)verifyHashForDiffUpdate:(NSString *)finalUpdateFolder
                   expectedHash:(NSString *)expectedHash
                          error:(NSError **)error;

@end

void CPLog(NSString *formatString, ...);

typedef NS_ENUM(NSInteger, CodePushInstallMode) {
    CodePushInstallModeImmediate,
    CodePushInstallModeOnNextRestart,
    CodePushInstallModeOnNextResume
};

typedef NS_ENUM(NSInteger, CodePushUpdateState) {
    CodePushUpdateStateRunning,
    CodePushUpdateStatePending,
    CodePushUpdateStateLatest
};