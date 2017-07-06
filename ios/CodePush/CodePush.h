#if __has_include(<React/RCTEventEmitter.h>)
#import <React/RCTEventEmitter.h>
#elif __has_include("RCTEventEmitter.h")
#import "RCTEventEmitter.h"
#else
#import "React/RCTEventEmitter.h"   // Required when used as a Pod in a Swift project
#endif

#import <Foundation/Foundation.h>

@interface CodePush : RCTEventEmitter

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

+ (NSURL *)bundleURLForResource:(NSString *)resourceName
                  withExtension:(NSString *)resourceExtension
                   subdirectory:(NSString *)resourceSubdirectory;

+ (NSURL *)bundleURLForResource:(NSString *)resourceName
                  withExtension:(NSString *)resourceExtension
                   subdirectory:(NSString *)resourceSubdirectory
                         bundle:(NSBundle *)resourceBundle;

+ (NSString *)getApplicationSupportDirectory;

+ (NSString *)bundleAssetsPath;

//this method to be used by brownfield apps from native side
//warning: can still be unstable, use with care
-(NSDictionary *)checkForUpdate:(NSString *)deploymentKey;
//this method to be used by brownfield apps from native side
-(void)sync:(NSDictionary *)syncOptions withCallback:(void(^)())callback;

/*
 * This method allows the version of the app's binary interface
 * to be specified, which would otherwise default to the
 * App Store version of the app.
 */
+ (void)overrideAppVersion:(NSString *)appVersion;

/*
 * This method allows dynamically setting the app's
 * deployment key, in addition to setting it via
 * the Info.plist file's CodePushDeploymentKey setting.
 */
+ (void)setDeploymentKey:(NSString *)deploymentKey;

/*
 * This method checks to see whether a specific package hash
 * has previously failed installation.
 */
+ (BOOL)isFailedHash:(NSString*)packageHash;

/*
 * This method checks to see whether a specific package hash
 * represents a downloaded and installed update, that hasn't
 * been applied yet via an app restart.
 */
+ (BOOL)isPendingUpdate:(NSString*)packageHash;

// The below methods are only used during tests.
+ (BOOL)isUsingTestConfiguration;
+ (void)setUsingTestConfiguration:(BOOL)shouldUseTestConfiguration;
+ (void)clearUpdates;

- (BOOL)restartApp:(BOOL)onlyIfUpdateIsPending;

//restart manager properties
@property (nonatomic) BOOL restartAllowed;
@property (nonatomic) BOOL restartInProgress;
@property (nonatomic) NSMutableArray *restartQueue;

@end

@interface CodePushAquisitionSDKManager : NSObject

@property (copy) NSString *appVersion;
@property (copy) NSString *clientUniqueId;
@property (copy) NSString *deploymentKey;
@property (copy) NSString *ignoreAppVersion;
@property (copy) NSString *serverURL;

- (instancetype)initWithConfig:(NSDictionary *)config;

- (NSDictionary *)queryUpdateWithCurrentPackage:(NSDictionary *)currentPackage;

- (void)reportStatusDeploy:(NSDictionary *)package
                          withStatus:(NSString *)status
           previousLabelOrAppVersion:(NSString *)prevLabelOrAppVersion
               previousDeploymentKey:(NSString *)prevDeploymentKey;

- (void)reportStatusDownload:(NSDictionary *)downloadedPackage;

@end

@interface CodePushConfig : NSObject

@property (copy) NSString *appVersion;
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
@property NSString *downloadUrl;

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

+ (NSDictionary *)getCurrentPackage:(NSError **)error;
+ (NSDictionary *)getPreviousPackage:(NSError **)error;
+ (NSString *)getCurrentPackageFolderPath:(NSError **)error;
+ (NSString *)getCurrentPackageBundlePath:(NSError **)error;
+ (NSString *)getCurrentPackageHash:(NSError **)error;

+ (NSDictionary *)getPackage:(NSString *)packageHash
                       error:(NSError **)error;

+ (NSString *)getPackageFolderPath:(NSString *)packageHash;

+ (BOOL)installPackage:(NSDictionary *)updatePackage
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

+ (BOOL)copyEntriesInFolder:(NSString *)sourceFolder
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

// Constants declarations below

extern NSString *const DeploymentFailed;
extern NSString *const DeploymentSucceeded;

extern NSString *const CodePushSyncStatusChangedNotification;
extern NSString *const StatusKey;
extern NSString *const PreviousDeploymentKey;
extern NSString *const PreviousLabelOrAppVersionKey;
extern NSString *const AppVersionKey;
extern NSString *const ServerURLConfigKey;
extern NSString *const DeploymentKeyConfigKey;
extern NSString *const ClientUniqueIDConfigKey;
extern NSString *const PackageHashKey;
extern NSString *const LabelKey;
extern NSString *const IgonreAppVersionConfigKey;
extern NSString *const UpdateInfoKey;
extern NSString *const IsCompanionKey;
extern NSString *const IsAvailableKey;
extern NSString *const UpdateAppVersionKey;
extern NSString *const DescriptionKey;
extern NSString *const IsMandatoryKey;
extern NSString *const PackageSizeKey;
extern NSString *const DownloadUrlKey;
extern NSString *const DownloadUrRemotePackageKey;
extern NSString *const AppVersionConfigKey;
extern NSString *const BuildVersionConfigKey;
extern NSString *const BinaryBundleDateKey;
extern NSString *const PackageIsPendingKey;
extern NSString *const FailedInstallKey;
extern NSString *const IsFirstRunKey;
extern NSString *const IsDebugOnlyKey;
extern NSString *const SyncStatusKey;
extern NSString *const MinimumBackgroundDurationKey;
extern NSString *const MandatoryInstallModeKey;
extern NSString *const IgnoreFailedUpdatesKey;
extern NSString *const InstallModeKey;
extern NSString *const UpdateDialogKey;
// End constants declaration

void CPLog(NSString *formatString, ...);

typedef NS_ENUM(NSInteger, CodePushInstallMode) {
    CodePushInstallModeImmediate,
    CodePushInstallModeOnNextRestart,
    CodePushInstallModeOnNextResume,
    CodePushInstallModeOnNextSuspend
};

typedef NS_ENUM(NSInteger, CodePushUpdateState) {
    CodePushUpdateStateRunning,
    CodePushUpdateStatePending,
    CodePushUpdateStateLatest
};

typedef NS_ENUM(NSInteger, CodePushSyncStatus) {
    CodePushSyncStatusUP_TO_DATE, // The running app is up-to-date
    CodePushSyncStatusUPDATE_INSTALLED, // The app had an optional/mandatory update that was successfully downloaded and is about to be installed.
    CodePushSyncStatusUPDATE_IGNORED, // The app had an optional update and the end-user chose to ignore it
    CodePushSyncStatusUNKNOWN_ERROR,
    CodePushSyncStatusSYNC_IN_PROGRESS, // There is an ongoing "sync" operation in progress.
    CodePushSyncStatusCHECKING_FOR_UPDATE,
    CodePushSyncStatusAWAITING_USER_ACTION,
    CodePushSyncStatusDOWNLOADING_PACKAGE,
    CodePushSyncStatusINSTALLING_UPDATE
};
