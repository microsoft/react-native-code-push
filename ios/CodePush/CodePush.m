#if __has_include(<React/RCTAssert.h>)
#import <React/RCTAssert.h>
#import <React/RCTBridgeModule.h>
#import <React/RCTConvert.h>
#import <React/RCTEventDispatcher.h>
#import <React/RCTRootView.h>
#import <React/RCTUtils.h>
#else // back compatibility for RN version < 0.40
#import "RCTAssert.h"
#import "RCTBridgeModule.h"
#import "RCTConvert.h"
#import "RCTEventDispatcher.h"
#import "RCTRootView.h"
#import "RCTUtils.h"
#endif

#import "CodePush.h"
#import "CodePush+RestartManager.h"

@interface CodePush () <RCTBridgeModule, RCTFrameUpdateObserver>

@end

@implementation CodePush {
    BOOL _hasResumeListener;
    BOOL _isFirstRunAfterUpdate;
    int _minimumBackgroundDuration;
    NSDate *_lastResignedDate;
    CodePushInstallMode *_installMode;
    NSTimer *_appSuspendTimer;

    // Used to coordinate the dispatching of download progress events to JS.
    long long _latestExpectedContentLength;
    long long _latestReceivedConentLength;

    BOOL _notifyOnSyncChanged;
    BOOL _didUpdateProgress;
    BOOL _syncInProgress;
    CodePushSyncStatus _syncStatus;
}

@synthesize restartAllowed, restartInProgress, restartQueue;

RCT_EXPORT_MODULE()

#pragma mark - Private constants

// These constants represent emitted events
static NSString *const DownloadProgressEvent = @"CodePushDownloadProgress";
static NSString *const SyncStatusChangedEvent = @"CodePushSyncStatus";

// These keys represent the names we use to store data in NSUserDefaults
static NSString *const FailedUpdatesKey = @"CODE_PUSH_FAILED_UPDATES";
static NSString *const PendingUpdateKey = @"CODE_PUSH_PENDING_UPDATE";

// These keys are already "namespaced" by the PendingUpdateKey, so
// their values don't need to be obfuscated to prevent collision with app data
static NSString *const PendingUpdateHashKey = @"hash";
static NSString *const PendingUpdateIsLoadingKey = @"isLoading";


#pragma mark - Static variables

static BOOL isRunningBinaryVersion = NO;
static BOOL needToReportRollback = NO;
static BOOL testConfigurationFlag = NO;

// These values are used to save the NS bundle, name, extension and subdirectory
// for the JS bundle in the binary.
static NSBundle *bundleResourceBundle = nil;
static NSString *bundleResourceExtension = @"jsbundle";
static NSString *bundleResourceName = @"main";
static NSString *bundleResourceSubdirectory = nil;

+ (void)initialize
{
    [super initialize];
    if (self == [CodePush class]) {
        // Use the mainBundle by default.
        bundleResourceBundle = [NSBundle mainBundle];
    }
}

#pragma mark - Public Obj-C API

+ (NSURL *)binaryBundleURL
{
    return [bundleResourceBundle URLForResource:bundleResourceName
                                  withExtension:bundleResourceExtension
                                   subdirectory:bundleResourceSubdirectory];
}

+ (NSString *)bundleAssetsPath
{
    NSString *resourcePath = [bundleResourceBundle resourcePath];
    if (bundleResourceSubdirectory) {
        resourcePath = [resourcePath stringByAppendingPathComponent:bundleResourceSubdirectory];
    }

    return [resourcePath stringByAppendingPathComponent:[CodePushUpdateUtils assetsFolderName]];
}

+ (NSURL *)bundleURL
{
    return [self bundleURLForResource:bundleResourceName
                        withExtension:bundleResourceExtension
                         subdirectory:bundleResourceSubdirectory
                               bundle:bundleResourceBundle];
}

+ (NSURL *)bundleURLForResource:(NSString *)resourceName
{
    return [self bundleURLForResource:resourceName
                        withExtension:bundleResourceExtension
                         subdirectory:bundleResourceSubdirectory
                               bundle:bundleResourceBundle];
}

+ (NSURL *)bundleURLForResource:(NSString *)resourceName
                  withExtension:(NSString *)resourceExtension
{
    return [self bundleURLForResource:resourceName
                        withExtension:resourceExtension
                         subdirectory:bundleResourceSubdirectory
                               bundle:bundleResourceBundle];
}

+ (NSURL *)bundleURLForResource:(NSString *)resourceName
                  withExtension:(NSString *)resourceExtension
                   subdirectory:(NSString *)resourceSubdirectory
{
    return [self bundleURLForResource:resourceName
                        withExtension:resourceExtension
                         subdirectory:resourceSubdirectory
                               bundle:bundleResourceBundle];
}

+ (NSURL *)bundleURLForResource:(NSString *)resourceName
                  withExtension:(NSString *)resourceExtension
                   subdirectory:(NSString *)resourceSubdirectory
                         bundle:(NSBundle *)resourceBundle
{
    bundleResourceName = resourceName;
    bundleResourceExtension = resourceExtension;
    bundleResourceSubdirectory = resourceSubdirectory;
    bundleResourceBundle = resourceBundle;

    [self ensureBinaryBundleExists];

    NSString *logMessageFormat = @"Loading JS bundle from %@";

    NSError *error;
    NSString *packageFile = [CodePushPackage getCurrentPackageBundlePath:&error];
    NSURL *binaryBundleURL = [self binaryBundleURL];

    if (error || !packageFile) {
        CPLog(logMessageFormat, binaryBundleURL);
        isRunningBinaryVersion = YES;
        return binaryBundleURL;
    }

    NSString *binaryAppVersion = [[CodePushConfig current] appVersion];
    NSDictionary *currentPackageMetadata = [CodePushPackage getCurrentPackage:&error];
    if (error || !currentPackageMetadata) {
        CPLog(logMessageFormat, binaryBundleURL);
        isRunningBinaryVersion = YES;
        return binaryBundleURL;
    }

    NSString *packageDate = [currentPackageMetadata objectForKey:BinaryBundleDateKey];
    NSString *packageAppVersion = [currentPackageMetadata objectForKey:AppVersionKey];

    if ([[CodePushUpdateUtils modifiedDateStringOfFileAtURL:binaryBundleURL] isEqualToString:packageDate] && ([CodePush isUsingTestConfiguration] ||[binaryAppVersion isEqualToString:packageAppVersion])) {
        // Return package file because it is newer than the app store binary's JS bundle
        NSURL *packageUrl = [[NSURL alloc] initFileURLWithPath:packageFile];
        CPLog(logMessageFormat, packageUrl);
        isRunningBinaryVersion = NO;
        return packageUrl;
    } else {
        BOOL isRelease = NO;
#ifndef DEBUG
        isRelease = YES;
#endif

        if (isRelease || ![binaryAppVersion isEqualToString:packageAppVersion]) {
            [CodePush clearUpdates];
        }

        CPLog(logMessageFormat, binaryBundleURL);
        isRunningBinaryVersion = YES;
        return binaryBundleURL;
    }
}

+ (NSString *)getApplicationSupportDirectory
{
    NSString *applicationSupportDirectory = [NSSearchPathForDirectoriesInDomains(NSApplicationSupportDirectory, NSUserDomainMask, YES) objectAtIndex:0];
    return applicationSupportDirectory;
}

+ (void)overrideAppVersion:(NSString *)appVersion
{
    [CodePushConfig current].appVersion = appVersion;
}

+ (void)setDeploymentKey:(NSString *)deploymentKey
{
    [CodePushConfig current].deploymentKey = deploymentKey;
}

- (BOOL)restartApp:(BOOL)onlyIfUpdateIsPending
{
    // If this is an unconditional restart request, or there
    // is current pending update, then reload the app.
    if (!onlyIfUpdateIsPending || [[self class] isPendingUpdate:nil]) {
        [self loadBundle];
        return YES;
    }

    return NO;
}

- (void) sync:(NSDictionary *)syncOptions withCallback:(void(^)())callback
        notifySyncStatus:(BOOL)notifySyncStatus
        notifyProgress:(BOOL)notifyProgress
{
    _notifyOnSyncChanged = notifySyncStatus ? YES : NO;

    if (_syncInProgress){
        CPLog(@"Sync already in progress.");
        [self syncStatusChanged:CodePushSyncStatusSYNC_IN_PROGRESS];
        callback();
        return;
    }

    _syncInProgress = YES;

    NSMutableDictionary *mergedSyncOptions = [[NSMutableDictionary alloc] initWithObjectsAndKeys:
                                    [NSNull null], DeploymentKeyConfigKey,
                                    @(YES), IgnoreFailedUpdatesKey,
                                    [NSNumber numberWithInt: CodePushInstallModeOnNextRestart],InstallModeKey,
                                    [NSNumber numberWithInt: CodePushInstallModeImmediate], MandatoryInstallModeKey,
                                    [NSNumber numberWithInt: 0],MinimumBackgroundDurationKey,
                                    [NSNull null],UpdateDialogKey,
                                    nil];

    [mergedSyncOptions addEntriesFromDictionary:syncOptions]; //merge with provided options

    [CodePush removePendingUpdate]; // aka NotifyAppReady
    NSDictionary *newStatusReport = [self getNewStatusReport];
    if (newStatusReport){
        [[self class] reportStatus:newStatusReport];
    }

    NSString *deploymentKey = [mergedSyncOptions objectForKey:DeploymentKeyConfigKey];
    if ([deploymentKey isEqual:[NSNull null]]){
        deploymentKey = nil;
    }

    [self syncStatusChanged:CodePushSyncStatusCHECKING_FOR_UPDATE];
    NSDictionary *remotePackage = [self checkForUpdate:deploymentKey];

    BOOL failedInstall = (remotePackage && [[remotePackage objectForKey:FailedInstallKey]boolValue]);
    BOOL updateShouldBeIgnored = (remotePackage && failedInstall);

    if (!remotePackage || updateShouldBeIgnored){
        if (updateShouldBeIgnored){
            CPLog(@"An update is available, but it is being ignored due to having been previously rolled back.");
        }

        NSDictionary *currentPackage = [[self class] getCurrentPackage];
        if (currentPackage && [[currentPackage objectForKey:PackageIsPendingKey]boolValue]){
            [self syncStatusChanged:CodePushSyncStatusUPDATE_INSTALLED];
            [self syncCompleted:callback];
            return;
        } else {
            [self syncStatusChanged:CodePushSyncStatusUP_TO_DATE];
            [self syncCompleted:callback];
            return;
        }
    }

    //download remote package
    [self syncStatusChanged:CodePushSyncStatusDOWNLOADING_PACKAGE];

    //for now only copy code from downloadPackage method in order not to break anything, later remove code duplication
    NSDictionary *mutableUpdatePackage = [remotePackage mutableCopy];
    NSURL *binaryBundleURL = [CodePush binaryBundleURL];
    if (binaryBundleURL != nil) {
        [mutableUpdatePackage setValue:[CodePushUpdateUtils modifiedDateStringOfFileAtURL:binaryBundleURL]
                                forKey:BinaryBundleDateKey];
    }

    if (notifyProgress) {
        // Set up and unpause the frame observer so that it can emit
        // progress events every frame if the progress is updated.
        _didUpdateProgress = NO;
        self.paused = NO;
    }

    if (![mutableUpdatePackage objectForKey:DownloadUrRemotePackageKey]){
        CPLog(@"Cannot download an update without a download url");
        [self syncStatusChanged:CodePushSyncStatusUNKNOWN_ERROR];
        [self syncCompleted:callback];
        return;
    }

    [CodePushPackage
     downloadPackage:mutableUpdatePackage
     expectedBundleFileName:[bundleResourceName stringByAppendingPathExtension:bundleResourceExtension]
     operationQueue:_methodQueue
     // The download is progressing forward
     progressCallback:^(long long expectedContentLength, long long receivedContentLength) {
         // Update the download progress so that the frame observer can notify the JS side
         _latestExpectedContentLength = expectedContentLength;
         _latestReceivedConentLength = receivedContentLength;
         _didUpdateProgress = YES;

         // If the download is completed, stop observing frame
         // updates and synchronously send the last event.
         if (expectedContentLength == receivedContentLength) {
             _didUpdateProgress = NO;
             self.paused = YES;
             [self dispatchDownloadProgressEvent];
         }
     }
     // The download completed
     doneCallback:^{
         NSError *err;
         NSDictionary *updatePackage = [CodePushPackage getPackage:mutableUpdatePackage[PackageHashKey] error:&err];

         if (err) {
             CPLog([NSString stringWithFormat: @"%lu", (long)err.code], err.localizedDescription, err);
             [self syncStatusChanged:CodePushSyncStatusUNKNOWN_ERROR];
             [self syncCompleted:callback];
             return;
         }

         //reporting logic
         NSMutableDictionary *config = [[CodePush getConfiguration] mutableCopy];
         CodePushAquisitionSDKManager *aquisitionSdk = [[CodePushAquisitionSDKManager alloc] initWithConfig:config];
         [aquisitionSdk reportStatusDownload:remotePackage];
         //end reporting logic

         CodePushInstallMode resolvedInstallMode = [[updatePackage objectForKey:IsMandatoryKey] boolValue] ?
            [[mergedSyncOptions objectForKey:MandatoryInstallModeKey] intValue] :
            [[mergedSyncOptions objectForKey:InstallModeKey] intValue];

         int resolvedMinimumBackgroundDuration = [[mergedSyncOptions objectForKey:MinimumBackgroundDurationKey] intValue];

         [self syncStatusChanged:CodePushSyncStatusINSTALLING_UPDATE];

         [self installUpdate:updatePackage
                 installMode:resolvedInstallMode
   minimumBackgroundDuration:resolvedMinimumBackgroundDuration
                       error:&err];

         if (err) {
             CPLog([NSString stringWithFormat: @"%lu", (long)err.code], err.localizedDescription, err);
             [self syncStatusChanged:CodePushSyncStatusUNKNOWN_ERROR];
             [self syncCompleted:callback];
             return;
         } else {
             [self syncStatusChanged:CodePushSyncStatusUPDATE_INSTALLED];
             [self syncCompleted:callback];
         }
     }
     // The download failed
     failCallback:^(NSError *err) {
         if ([CodePushErrorUtils isCodePushError:err]) {
             [self saveFailedUpdate:mutableUpdatePackage];
         }

         // Stop observing frame updates if the download fails.
         _didUpdateProgress = NO;
         self.paused = YES;

         CPLog([NSString stringWithFormat: @"%lu", (long)err.code], err.localizedDescription, err);
         [self syncStatusChanged:CodePushSyncStatusUNKNOWN_ERROR];
         [self syncCompleted:callback];
     }];

    // end download logic
}

- (NSDictionary *)checkForUpdate:(NSString *)deploymentKey

{
    //do we need to save overrided value here for deployment key in config?
    NSMutableDictionary *configuration = [[CodePush getConfiguration] mutableCopy];

    if (deploymentKey){
        [configuration setValue:deploymentKey forKey:DeploymentKeyConfigKey];
    }

    NSMutableDictionary *localPackage = [[[self class] getCurrentPackage] mutableCopy];

    if (localPackage){
        NSString *packageHash = [localPackage objectForKey:PackageHashKey];

        BOOL isFailedInstall = [[self class] isFailedHash:packageHash];
        BOOL isFirstRun = [self isFirstRun:packageHash];

        [localPackage setObject:[NSNumber numberWithBool:isFailedInstall] forKey:FailedInstallKey];
        [localPackage setObject:[NSNumber numberWithBool:isFirstRun] forKey:IsFirstRunKey];
    }

    NSMutableDictionary *queryPackage;
    if (localPackage){
        queryPackage = localPackage;
    } else {
        queryPackage = [[NSMutableDictionary alloc] initWithObjectsAndKeys:
                        [configuration objectForKey:AppVersionKey],AppVersionKey,
                        nil];

        if ([configuration objectForKey:PackageHashKey]){
            [queryPackage setObject:[configuration objectForKey:PackageHashKey] forKey:PackageHashKey];
        }
    }

    CodePushAquisitionSDKManager *aquisitionSdk = [[CodePushAquisitionSDKManager alloc] initWithConfig:configuration];

    NSDictionary *remotePackage = [aquisitionSdk queryUpdateWithCurrentPackage:queryPackage];

    NSMutableDictionary *update = [remotePackage mutableCopy];

    BOOL updateAppVersion = [[update objectForKey:UpdateAppVersionKey]boolValue];
    BOOL isDebugOnly = [[localPackage objectForKey:IsDebugOnlyKey]boolValue];
    NSString *updatePackageHash = [update objectForKey:PackageHashKey];
    NSString *localPackageHash = [localPackage objectForKey:PackageHashKey];
    NSString *configPackageHash = [configuration objectForKey:PackageHashKey];

    if (!update || updateAppVersion ||
        (localPackage && ([updatePackageHash isEqualToString:localPackageHash])) ||
        ((!localPackage || isDebugOnly) && [configPackageHash isEqualToString:updatePackageHash] )){

        if (update && updateAppVersion){
            CPLog(@"An update is available but it is not targeting the binary version of your app.");
        }
        return nil;
    } else {
        [update setObject:[NSNumber numberWithBool:[[self class] isFailedHash:PackageHashKey]]  forKey:FailedInstallKey];
        if (deploymentKey){
            [update setObject:deploymentKey forKey:DeploymentKeyConfigKey];
        } else {
            [update setObject:[configuration objectForKey:DeploymentKeyConfigKey] forKey:DeploymentKeyConfigKey];
        }
    }
    return update;
}

#pragma mark - Test-only methods

/*
 * WARNING: This cleans up all downloaded and pending updates.
 */
+ (void)clearUpdates
{
    [CodePushPackage clearUpdates];
    [self removePendingUpdate];
    [self removeFailedUpdates];
}

/*
 * This returns a boolean value indicating whether CodePush has
 * been set to run under a test configuration.
 */
+ (BOOL)isUsingTestConfiguration
{
    return testConfigurationFlag;
}

/*
 * This is used to enable an environment in which tests can be run.
 * Specifically, it flips a boolean flag that causes bundles to be
 * saved to a test folder and enables the ability to modify
 * installed bundles on the fly from JavaScript.
 */
+ (void)setUsingTestConfiguration:(BOOL)shouldUseTestConfiguration
{
    testConfigurationFlag = shouldUseTestConfiguration;
}

#pragma mark - Private API methods

@synthesize methodQueue = _methodQueue;
@synthesize pauseCallback = _pauseCallback;
@synthesize paused = _paused;

- (BOOL)isFirstRun:(NSString*)packageHash
{
    NSError *error;
    BOOL isFirstRun = _isFirstRunAfterUpdate
    && nil != packageHash
    && [packageHash length] > 0
    && [packageHash isEqualToString:[CodePushPackage getCurrentPackageHash:&error]];

    return isFirstRun;
}

- (void)setPaused:(BOOL)paused
{
    if (_paused != paused) {
        _paused = paused;
        if (_pauseCallback) {
            _pauseCallback();
        }
    }
}

/*
 * This method is used to clear updates that are installed
 * under a different app version and hence don't apply anymore,
 * during a debug run configuration and when the bridge is
 * running the JS bundle from the dev server.
 */
- (void)clearDebugUpdates
{
    dispatch_async(dispatch_get_main_queue(), ^{
        if ([super.bridge.bundleURL.scheme hasPrefix:@"http"]) {
            NSError *error;
            NSString *binaryAppVersion = [[CodePushConfig current] appVersion];
            NSDictionary *currentPackageMetadata = [CodePushPackage getCurrentPackage:&error];
            if (currentPackageMetadata) {
                NSString *packageAppVersion = [currentPackageMetadata objectForKey:AppVersionKey];
                if (![binaryAppVersion isEqualToString:packageAppVersion]) {
                    [CodePush clearUpdates];
                }
            }
        }
    });
}

/*
 * This method is used by the React Native bridge to allow
 * our plugin to expose constants to the JS-side. In our case
 * we're simply exporting enum values so that the JS and Native
 * sides of the plugin can be in sync.
 */
- (NSDictionary *)constantsToExport
{
    // Export the values of the CodePushInstallMode and CodePushUpdateState
    // enums so that the script-side can easily stay in sync
    return @{
             @"codePushInstallModeOnNextRestart":@(CodePushInstallModeOnNextRestart),
             @"codePushInstallModeImmediate": @(CodePushInstallModeImmediate),
             @"codePushInstallModeOnNextResume": @(CodePushInstallModeOnNextResume),
             @"codePushInstallModeOnNextSuspend": @(CodePushInstallModeOnNextSuspend),

             @"codePushUpdateStateRunning": @(CodePushUpdateStateRunning),
             @"codePushUpdateStatePending": @(CodePushUpdateStatePending),
             @"codePushUpdateStateLatest": @(CodePushUpdateStateLatest),

             @"codePushSyncStatusUpToDate": @(CodePushSyncStatusUP_TO_DATE),
             @"codePushSyncStatusUpdateInstalled": @(CodePushSyncStatusUPDATE_INSTALLED),
             @"codePushSyncStatusUpdateIgnored": @(CodePushSyncStatusUPDATE_IGNORED),
             @"codePushSyncStatusUnknownError": @(CodePushSyncStatusUNKNOWN_ERROR),
             @"codePushSyncStatusSyncInProgress": @(CodePushSyncStatusSYNC_IN_PROGRESS),
             @"codePushSyncStatusCheckingForUpdate": @(CodePushSyncStatusCHECKING_FOR_UPDATE),
             @"codePushSyncStatusAwaitingUserAction": @(CodePushSyncStatusAWAITING_USER_ACTION),
             @"codePushSyncStatusDownloadingPackage": @(CodePushSyncStatusDOWNLOADING_PACKAGE),
             @"codePushSyncStatusInstallingUpdate": @(CodePushSyncStatusINSTALLING_UPDATE),
            };
};

- (void)dealloc
{
    // Ensure the global resume handler is cleared, so that
    // this object isn't kept alive unnecessarily
    [[NSNotificationCenter defaultCenter] removeObserver:self];
}

- (void)dispatchDownloadProgressEvent {
  // Notify the script-side about the progress
  [self sendEventWithName:DownloadProgressEvent
                     body:@{
                       @"totalBytes" : [NSNumber
                           numberWithLongLong:_latestExpectedContentLength],
                       @"receivedBytes" : [NSNumber
                           numberWithLongLong:_latestReceivedConentLength]
                     }];
}

- (void)dispatchSyncChangedEvent {
    //notify the script side about sync status changed
    [self sendEventWithName:SyncStatusChangedEvent body:@{
                                                          @"syncStatus": [NSNumber numberWithInt:_syncStatus]
                                                          }];
}


+ (NSDictionary *)getConfiguration
{
    NSDictionary *configuration = [[CodePushConfig current] configuration];
    NSError *error;

    if (isRunningBinaryVersion) {
        // isRunningBinaryVersion will not get set to "YES" if running against the packager.
        NSString *binaryHash = [CodePushUpdateUtils getHashForBinaryContents:[CodePush binaryBundleURL] error:&error];
        if (error) {
            CPLog(@"Error obtaining hash for binary contents: %@", error);
            return configuration;
        }

        if (binaryHash == nil) {
            // The hash was not generated either due to a previous unknown error or the fact that
            // the React Native assets were not bundled in the binary (e.g. during dev/simulator)
            // builds.
            return configuration;
        }

        NSMutableDictionary *mutableConfiguration = [configuration mutableCopy];
        [mutableConfiguration setObject:binaryHash forKey:PackageHashKey];
        return mutableConfiguration;
    }
    return configuration;
}

/*
 * This method ensures that the app was packaged with a JS bundle
 * file, and if not, it throws the appropriate exception.
 */
+ (void)ensureBinaryBundleExists
{
    if (![self binaryBundleURL]) {
        NSString *errorMessage;

    #ifdef DEBUG
        #if TARGET_IPHONE_SIMULATOR
            errorMessage = @"React Native doesn't generate your app's JS bundle by default when deploying to the simulator. "
            "If you'd like to test CodePush using the simulator, you can do one of three things depending on your React "
            "Native version and/or preferred workflow:\n\n"

            "1. Update your AppDelegate.m file to load the JS bundle from the packager instead of from CodePush. "
            "You can still test your CodePush update experience using this workflow (debug builds only).\n\n"

            "2. Force the JS bundle to be generated in simulator builds by removing the if block that echoes "
            "\"Skipping bundling for Simulator platform\" in the \"node_modules/react-native/packager/react-native-xcode.sh\" file.\n\n"

            "3. Deploy a release build to the simulator, which unlike debug builds, will generate the JS bundle (React Native >=0.22.0 only).";
        #else
            errorMessage = [NSString stringWithFormat:@"The specified JS bundle file wasn't found within the app's binary. Is \"%@\" the correct file name?", [bundleResourceName stringByAppendingPathExtension:bundleResourceExtension]];
        #endif
    #else
        errorMessage = @"Something went wrong. Please verify if generated JS bundle is correct. ";
    #endif

        RCTFatal([CodePushErrorUtils errorWithMessage:errorMessage]);
    }
}

- (instancetype)init
{
    self = [super init];

    if (self) {
        [self initializeUpdateAfterRestart];
        restartAllowed = YES; //set allowed by default
    }

    return self;
}

- (void) syncCompleted: (void(^)())callback
{
    _syncInProgress = NO;
    callback();
}

- (void) syncStatusChanged:(CodePushSyncStatus)syncStatus
{
    _syncStatus = syncStatus;

    //notify js side
    if (_notifyOnSyncChanged){
        [self dispatchSyncChangedEvent];
    }

    //Notify obervers about sync status changed event
    NSDictionary *userInfo = [NSDictionary dictionaryWithObject:[NSNumber numberWithInt: syncStatus] forKey:SyncStatusKey];

    [[NSNotificationCenter defaultCenter] postNotificationName:
     CodePushSyncStatusChangedNotification object:nil userInfo:userInfo];
}

/*
 * This method is used when the app is started to either
 * initialize a pending update or rollback a faulty update
 * to the previous version.
 */
- (void)initializeUpdateAfterRestart
{
#ifdef DEBUG
    [self clearDebugUpdates];
#endif
    self.paused = YES;
    NSUserDefaults *preferences = [NSUserDefaults standardUserDefaults];
    NSDictionary *pendingUpdate = [preferences objectForKey:PendingUpdateKey];
    if (pendingUpdate) {
        _isFirstRunAfterUpdate = YES;
        BOOL updateIsLoading = [pendingUpdate[PendingUpdateIsLoadingKey] boolValue];
        if (updateIsLoading) {
            // Pending update was initialized, but notifyApplicationReady was not called.
            // Therefore, deduce that it is a broken update and rollback.
            CPLog(@"Update did not finish loading the last time, rolling back to a previous version.");
            needToReportRollback = YES;
            [self rollbackPackage];
        } else {
            // Mark that we tried to initialize the new update, so that if it crashes,
            // we will know that we need to rollback when the app next starts.
            [self savePendingUpdate:pendingUpdate[PendingUpdateHashKey]
                          isLoading:YES];
        }
    }
}

/*
 * This method checks to see whether a specific package hash
 * has previously failed installation.
 */
+ (BOOL)isFailedHash:(NSString*)packageHash
{
    NSUserDefaults *preferences = [NSUserDefaults standardUserDefaults];
    NSMutableArray *failedUpdates = [preferences objectForKey:FailedUpdatesKey];
    if (failedUpdates == nil || packageHash == nil) {
        return NO;
    } else {
        for (NSDictionary *failedPackage in failedUpdates)
        {
            // Type check is needed for backwards compatibility, where we used to just store
            // the failed package hash instead of the metadata. This only impacts "dev"
            // scenarios, since in production we clear out old information whenever a new
            // binary is applied.
            if ([failedPackage isKindOfClass:[NSDictionary class]]) {
                NSString *failedPackageHash = [failedPackage objectForKey:PackageHashKey];
                if ([packageHash isEqualToString:failedPackageHash]) {
                    return YES;
                }
            }
        }

        return NO;
    }
}

/*
 * This method checks to see whether a specific package hash
 * represents a downloaded and installed update, that hasn't
 * been applied yet via an app restart.
 */
+ (BOOL)isPendingUpdate:(NSString*)packageHash
{
    NSUserDefaults *preferences = [NSUserDefaults standardUserDefaults];
    NSDictionary *pendingUpdate = [preferences objectForKey:PendingUpdateKey];

    // If there is a pending update whose "state" isn't loading, then we consider it "pending".
    // Additionally, if a specific hash was provided, we ensure it matches that of the pending update.
    BOOL updateIsPending = pendingUpdate &&
                           [pendingUpdate[PendingUpdateIsLoadingKey] boolValue] == NO &&
                           (!packageHash || [pendingUpdate[PendingUpdateHashKey] isEqualToString:packageHash]);

    return updateIsPending;
}

+ (NSDictionary *)getUpdateMetadataFor:(CodePushUpdateState)updateState
            currentPackageGettingError:(NSError **)error
{
    NSError *__autoreleasing internalError;

    NSMutableDictionary *package = [[CodePushPackage getCurrentPackage:&internalError] mutableCopy];
    if (internalError){
        error = &internalError;
        return nil;
    }

    if (package == nil){
        // An error occured on getting package or
        // The app hasn't downloaded any CodePush updates yet,
        // so we simply return nil regardless if the user
        // wanted to retrieve the pending or running update.
        return nil;
    }

    // We have a CodePush update, so let's see if it's currently in a pending state.
    BOOL currentUpdateIsPending = [[self class] isPendingUpdate:[package objectForKey:PackageHashKey]];

    if (updateState == CodePushUpdateStatePending && !currentUpdateIsPending) {
        // The caller wanted a pending update
        // but there isn't currently one.
        return nil;
    } else if (updateState == CodePushUpdateStateRunning && currentUpdateIsPending) {
        // The caller wants the running update, but the current
        // one is pending, so we need to grab the previous.
        return [CodePushPackage getPreviousPackage:error];
    } else {
        // The current package satisfies the request:
        // 1) Caller wanted a pending, and there is a pending update
        // 2) Caller wanted the running update, and there isn't a pending
        // 3) Caller wants the latest update, regardless if it's pending or not
        if (isRunningBinaryVersion) {
            // This only matters in Debug builds. Since we do not clear "outdated" updates,
            // we need to indicate to the JS side that somehow we have a current update on
            // disk that is not actually running.
            [package setObject:@(YES) forKey:IsDebugOnlyKey];
        }
        // Enable differentiating pending vs. non-pending updates
        [package setObject:@(currentUpdateIsPending) forKey:PackageIsPendingKey];
        return package;
    }
}

+ (NSDictionary *)getCurrentPackage
{
    NSError *error;
    NSDictionary *currentPackage = [[self class] getUpdateMetadataFor:CodePushUpdateStateLatest currentPackageGettingError:&error];
    if (error){
        CPLog(@"An error occured: %@", [error localizedDescription]);
        return nil;
    }
    return currentPackage;
}

+ (void)reportStatus:(NSDictionary *)statusReport
{
    NSMutableDictionary *configuration = [[CodePush getConfiguration] mutableCopy];
    NSString *prevLabelOrAppVersion = [statusReport objectForKey:PreviousLabelOrAppVersionKey];
    NSString *prevDeploymentKey = [statusReport objectForKey:PreviousDeploymentKey];

    if (!prevDeploymentKey){
        prevDeploymentKey = [configuration objectForKey:DeploymentKeyConfigKey];
    }

    if ([statusReport objectForKey:AppVersionKey]){
        CPLog(@"Reporting binary update  %@", [statusReport objectForKey:AppVersionKey]);

        CodePushAquisitionSDKManager *aquisitionSdk = [[CodePushAquisitionSDKManager alloc] initWithConfig:configuration];
        [aquisitionSdk reportStatusDeploy:nil
                               withStatus:nil
                previousLabelOrAppVersion:prevLabelOrAppVersion
                    previousDeploymentKey:prevDeploymentKey];
    } else {
        NSDictionary *package = [statusReport objectForKey:@"package"];
        NSString *label = [package objectForKey:LabelKey];
        NSString *reportStatus = [statusReport objectForKey:StatusKey];

        if ([reportStatus  isEqual: DeploymentSucceeded]){
            CPLog(@"Reporting CodePush update success %@", label);
        } else {
            CPLog(@"Reporting CodePush update rollback %@", label);
        }

        NSString *configDeploymentKey = [package objectForKey:DeploymentKeyConfigKey];
        [configuration setObject:configDeploymentKey forKey:DeploymentKeyConfigKey];

        CodePushAquisitionSDKManager *aquisitionSdk = [[CodePushAquisitionSDKManager alloc] initWithConfig:configuration];
        [aquisitionSdk reportStatusDeploy:package
                               withStatus:reportStatus
                previousLabelOrAppVersion:prevLabelOrAppVersion
                    previousDeploymentKey:prevDeploymentKey];

    }

    [CodePushTelemetryManager recordStatusReported:statusReport]; //aka recordStatusReported
}

- (NSDictionary *)getNewStatusReport
{
    if (needToReportRollback) {
        needToReportRollback = NO;
        NSUserDefaults *preferences = [NSUserDefaults standardUserDefaults];
        NSMutableArray *failedUpdates = [preferences objectForKey:FailedUpdatesKey];
        if (failedUpdates) {
            NSDictionary *lastFailedPackage = [failedUpdates lastObject];
            if (lastFailedPackage) {
                return [CodePushTelemetryManager getRollbackReport:lastFailedPackage];
            }
        }
    } else if (_isFirstRunAfterUpdate) {
        NSError *error;
        NSDictionary *currentPackage = [CodePushPackage getCurrentPackage:&error];
        if (!error && currentPackage) {
            return [CodePushTelemetryManager getUpdateReport:currentPackage];
        }
    } else if (isRunningBinaryVersion) {
        NSString *appVersion = [[CodePushConfig current] appVersion];
        return [CodePushTelemetryManager getBinaryUpdateReport:appVersion];
    } else {
        NSDictionary *retryStatusReport = [CodePushTelemetryManager getRetryStatusReport];
        if (retryStatusReport) {
            return retryStatusReport;
        }
    }
    return nil;
}

/*
 * This method updates the React Native bridge's bundle URL
 * to point at the latest CodePush update, and then restarts
 * the bridge. This isn't meant to be called directly.
 */
- (void)loadBundle
{
    // This needs to be async dispatched because the bridge is not set on init
    // when the app first starts, therefore rollbacks will not take effect.
    dispatch_async(dispatch_get_main_queue(), ^{
        // If the current bundle URL is using http(s), then assume the dev
        // is debugging and therefore, shouldn't be redirected to a local
        // file (since Chrome wouldn't support it). Otherwise, update
        // the current bundle URL to point at the latest update
        if ([CodePush isUsingTestConfiguration] || ![super.bridge.bundleURL.scheme hasPrefix:@"http"]) {
            [super.bridge setValue:[CodePush bundleURL] forKey:@"bundleURL"];
        }

        [super.bridge reload];
    });
}

/*
 * This method is used when an update has failed installation
 * and the app needs to be rolled back to the previous bundle.
 * This method is automatically called when the rollback timer
 * expires without the app indicating whether the update succeeded,
 * and therefore, it shouldn't be called directly.
 */
- (void)rollbackPackage
{
    NSError *error;
    NSDictionary *failedPackage = [CodePushPackage getCurrentPackage:&error];
    if (!failedPackage) {
        if (error) {
            CPLog(@"Error getting current update metadata during rollback: %@", error);
        } else {
            CPLog(@"Attempted to perform a rollback when there is no current update");
        }
    } else {
        // Write the current package's metadata to the "failed list"
        [self saveFailedUpdate:failedPackage];
    }

    // Rollback to the previous version and de-register the new update
    [CodePushPackage rollbackPackage];
    [CodePush removePendingUpdate];
    [self loadBundle];
}

/*
 * When an update failed to apply, this method can be called
 * to store its hash so that it can be ignored on future
 * attempts to check the server for an update.
 */
- (void)saveFailedUpdate:(NSDictionary *)failedPackage
{
    NSUserDefaults *preferences = [NSUserDefaults standardUserDefaults];
    NSMutableArray *failedUpdates = [preferences objectForKey:FailedUpdatesKey];
    if (failedUpdates == nil) {
        failedUpdates = [[NSMutableArray alloc] init];
    } else {
        // The NSUserDefaults sytem always returns immutable
        // objects, regardless if you stored something mutable.
        failedUpdates = [failedUpdates mutableCopy];
    }

    [failedUpdates addObject:failedPackage];
    [preferences setObject:failedUpdates forKey:FailedUpdatesKey];
    [preferences synchronize];
}

/*
 * This method is used to clear away failed updates in the event that
 * a new app store binary is installed.
 */
+ (void)removeFailedUpdates
{
    NSUserDefaults *preferences = [NSUserDefaults standardUserDefaults];
    [preferences removeObjectForKey:FailedUpdatesKey];
    [preferences synchronize];
}

/*
 * This method is used to register the fact that a pending
 * update succeeded and therefore can be removed.
 */
+ (void)removePendingUpdate
{
    NSUserDefaults *preferences = [NSUserDefaults standardUserDefaults];
    [preferences removeObjectForKey:PendingUpdateKey];
    [preferences synchronize];
}

/*
 * When an update is installed whose mode isn't IMMEDIATE, this method
 * can be called to store the pending update's metadata (e.g. packageHash)
 * so that it can be used when the actual update application occurs at a later point.
 */
- (void)savePendingUpdate:(NSString *)packageHash
                isLoading:(BOOL)isLoading
{
    // Since we're not restarting, we need to store the fact that the update
    // was installed, but hasn't yet become "active".
    NSUserDefaults *preferences = [NSUserDefaults standardUserDefaults];
    NSDictionary *pendingUpdate = [[NSDictionary alloc] initWithObjectsAndKeys:
                                   packageHash,PendingUpdateHashKey,
                                   [NSNumber numberWithBool:isLoading],PendingUpdateIsLoadingKey, nil];

    [preferences setObject:pendingUpdate forKey:PendingUpdateKey];
    [preferences synchronize];
}

- (NSArray<NSString *> *)supportedEvents {
    return @[DownloadProgressEvent, SyncStatusChangedEvent];
}

- (BOOL)installUpdate:(NSDictionary*)updatePackage
          installMode:(CodePushInstallMode)intallMode
          minimumBackgroundDuration:(int)minimumBackgroundDuration
                error:(NSError **)error
{
    [CodePushPackage installPackage:updatePackage
                removePendingUpdate:[[self class] isPendingUpdate:nil]
                              error:error];

    if (error) {
        return NO;
    } else {
        [self savePendingUpdate:updatePackage[PackageHashKey]
                      isLoading:NO];

        _installMode = intallMode;
        if (_installMode == CodePushInstallModeOnNextResume || _installMode == CodePushInstallModeOnNextSuspend) {
            _minimumBackgroundDuration = minimumBackgroundDuration;

            if (!_hasResumeListener) {
                // Ensure we do not add the listener twice.
                // Register for app resume notifications so that we
                // can check for pending updates which support "restart on resume"
                [[NSNotificationCenter defaultCenter] addObserver:self
                                                         selector:@selector(applicationWillEnterForeground)
                                                             name:UIApplicationWillEnterForegroundNotification
                                                           object:RCTSharedApplication()];

                [[NSNotificationCenter defaultCenter] addObserver:self
                                                         selector:@selector(applicationWillResignActive)
                                                             name:UIApplicationWillResignActiveNotification
                                                           object:RCTSharedApplication()];

                _hasResumeListener = YES;
            }
        }
        if (_installMode == CodePushInstallModeImmediate){
            [self restartApplication:NO];
        } else {
            [self clearPendingRestart];
        }

        // Signal to JS that the update has been applied.
        return YES;

    }
}

#pragma mark - Application lifecycle event handlers

// These two handlers will only be registered when there is
// a resume-based update still pending installation.
- (void)applicationWillEnterForeground
{
    if (_appSuspendTimer) {
        [_appSuspendTimer invalidate];
        _appSuspendTimer = nil;
    }
    // Determine how long the app was in the background and ensure
    // that it meets the minimum duration amount of time.
    int durationInBackground = 0;
    if (_lastResignedDate) {
        durationInBackground = [[NSDate date] timeIntervalSinceDate:_lastResignedDate];
    }

    if (durationInBackground >= _minimumBackgroundDuration && restartAllowed) {
        [self loadBundle];
    }
}

- (void)applicationWillResignActive
{
    // Save the current time so that when the app is later
    // resumed, we can detect how long it was in the background.
    _lastResignedDate = [NSDate date];

    if (_installMode == CodePushInstallModeOnNextSuspend && [[self class] isPendingUpdate:nil]) {
        _appSuspendTimer = [NSTimer scheduledTimerWithTimeInterval:_minimumBackgroundDuration
                                                         target:self
                                                       selector:@selector(loadBundleOnTick:)
                                                       userInfo:nil
                                                        repeats:NO];
    }
}

-(void)loadBundleOnTick:(NSTimer *)timer {
    [self loadBundle];
}

#pragma mark - JavaScript-exported module methods (Public)

/*
 * This is native-side of the RemotePackage.download method
 */
RCT_EXPORT_METHOD(downloadUpdate:(NSDictionary*)updatePackage
                  notifyProgress:(BOOL)notifyProgress
                        resolver:(RCTPromiseResolveBlock)resolve
                        rejecter:(RCTPromiseRejectBlock)reject)
{
    NSDictionary *mutableUpdatePackage = [updatePackage mutableCopy];
    NSURL *binaryBundleURL = [CodePush binaryBundleURL];
    if (binaryBundleURL != nil) {
        [mutableUpdatePackage setValue:[CodePushUpdateUtils modifiedDateStringOfFileAtURL:binaryBundleURL]
                                forKey:BinaryBundleDateKey];
    }

    if (notifyProgress) {
        // Set up and unpause the frame observer so that it can emit
        // progress events every frame if the progress is updated.
        _didUpdateProgress = NO;
        self.paused = NO;
    }

    [CodePushPackage
        downloadPackage:mutableUpdatePackage
        expectedBundleFileName:[bundleResourceName stringByAppendingPathExtension:bundleResourceExtension]
        operationQueue:_methodQueue
        // The download is progressing forward
        progressCallback:^(long long expectedContentLength, long long receivedContentLength) {
            // Update the download progress so that the frame observer can notify the JS side
            _latestExpectedContentLength = expectedContentLength;
            _latestReceivedConentLength = receivedContentLength;
            _didUpdateProgress = YES;

            // If the download is completed, stop observing frame
            // updates and synchronously send the last event.
            if (expectedContentLength == receivedContentLength) {
                _didUpdateProgress = NO;
                self.paused = YES;
                [self dispatchDownloadProgressEvent];
            }
        }
        // The download completed
        doneCallback:^{
            NSError *err;
            NSDictionary *newPackage = [CodePushPackage getPackage:mutableUpdatePackage[PackageHashKey] error:&err];

            if (err) {
                return reject([NSString stringWithFormat: @"%lu", (long)err.code], err.localizedDescription, err);
            }
            resolve(newPackage);
        }
        // The download failed
        failCallback:^(NSError *err) {
            if ([CodePushErrorUtils isCodePushError:err]) {
                [self saveFailedUpdate:mutableUpdatePackage];
            }

            // Stop observing frame updates if the download fails.
            _didUpdateProgress = NO;
            self.paused = YES;
            reject([NSString stringWithFormat: @"%lu", (long)err.code], err.localizedDescription, err);
        }];
}

/*
 * This is the native side of the CodePush.getConfiguration method. It isn't
 * currently exposed via the "react-native-code-push" module, and is used
 * internally only by the CodePush.checkForUpdate method in order to get the
 * app version, as well as the deployment key that was configured in the Info.plist file.
 */
RCT_EXPORT_METHOD(getConfiguration:(RCTPromiseResolveBlock)resolve
                          rejecter:(RCTPromiseRejectBlock)reject)
{
    NSDictionary *configuration = [[self class] getConfiguration];
    resolve(configuration);
}

/*
 * This method is the native side of the CodePush.getUpdateMetadata method.
 */
RCT_EXPORT_METHOD(getUpdateMetadata:(CodePushUpdateState)updateState
                           resolver:(RCTPromiseResolveBlock)resolve
                           rejecter:(RCTPromiseRejectBlock)reject)
{
    NSError *error;
    NSDictionary *updateMetadata = [[self class] getUpdateMetadataFor:updateState currentPackageGettingError:&error];
    if (error){
        return reject([NSString stringWithFormat: @"%lu", (long)error.code], error.localizedDescription, error);
    } else {
        return resolve(updateMetadata);
    }
}

/*
 * This method is the native side of the LocalPackage.install method.
 */
RCT_EXPORT_METHOD(installUpdate:(NSDictionary*)updatePackage
                    installMode:(CodePushInstallMode)installMode
      minimumBackgroundDuration:(int)minimumBackgroundDuration
                       resolver:(RCTPromiseResolveBlock)resolve
                       rejecter:(RCTPromiseRejectBlock)reject)
{
    NSError *error;
    [CodePushPackage installPackage:updatePackage
                removePendingUpdate:[[self class] isPendingUpdate:nil]
                              error:&error];

    if (error) {
        reject([NSString stringWithFormat: @"%lu", (long)error.code], error.localizedDescription, error);
    } else {
        [self savePendingUpdate:updatePackage[PackageHashKey]
                      isLoading:NO];

        _installMode = installMode;
        if (_installMode == CodePushInstallModeOnNextResume || _installMode == CodePushInstallModeOnNextSuspend) {
            _minimumBackgroundDuration = minimumBackgroundDuration;

            if (!_hasResumeListener) {
                // Ensure we do not add the listener twice.
                // Register for app resume notifications so that we
                // can check for pending updates which support "restart on resume"
                [[NSNotificationCenter defaultCenter] addObserver:self
                                                         selector:@selector(applicationWillEnterForeground)
                                                             name:UIApplicationWillEnterForegroundNotification
                                                           object:RCTSharedApplication()];

                [[NSNotificationCenter defaultCenter] addObserver:self
                                                         selector:@selector(applicationWillResignActive)
                                                             name:UIApplicationWillResignActiveNotification
                                                           object:RCTSharedApplication()];

                _hasResumeListener = YES;
            }
        }

        // Signal to JS that the update has been applied.
        resolve(nil);
    }
}

/*
 * This method isn't publicly exposed via the "react-native-code-push"
 * module, and is only used internally to populate the RemotePackage.failedInstall property.
 */
RCT_EXPORT_METHOD(isFailedUpdate:(NSString *)packageHash
                         resolve:(RCTPromiseResolveBlock)resolve
                          reject:(RCTPromiseRejectBlock)reject)
{
    BOOL isFailedHash = [[self class] isFailedHash:packageHash];
    resolve(@(isFailedHash));
}

/*
 * This method isn't publicly exposed via the "react-native-code-push"
 * module, and is only used internally to populate the LocalPackage.isFirstRun property.
 */
RCT_EXPORT_METHOD(isFirstRun:(NSString *)packageHash
                     resolve:(RCTPromiseResolveBlock)resolve
                    rejecter:(RCTPromiseRejectBlock)reject)
{
    BOOL isFirstRun = [self isFirstRun:packageHash];
    resolve(@(isFirstRun));
}

/*
 * This method is the native side of the CodePush.notifyApplicationReady() method.
 */
RCT_EXPORT_METHOD(notifyApplicationReady:(RCTPromiseResolveBlock)resolve
                                rejecter:(RCTPromiseRejectBlock)reject)
{
    [CodePush removePendingUpdate];
    resolve(nil);
}

/*
 * This method is the native side of the CodePush.restartApp() method.
 */
RCT_EXPORT_METHOD(restartApp:(BOOL)onlyIfUpdateIsPending
                     resolve:(RCTPromiseResolveBlock)resolve
                    rejecter:(RCTPromiseRejectBlock)reject)
{
    BOOL result = [self restartApp:onlyIfUpdateIsPending];
    if (result){
        resolve(@(YES));
        return;
    }
    resolve(@(NO));
}

//this is native implementation of restartManager.restartApp function
RCT_EXPORT_METHOD(restartApplication:(BOOL)onlyIfUpdateIsPending
                  resolve:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
    [self restartApplication:onlyIfUpdateIsPending];
    resolve(@(YES));
}

RCT_EXPORT_METHOD(clearPendingRestart:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
    [self clearPendingRestart];
    resolve(@(YES));
}

RCT_EXPORT_METHOD(allowRestart:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
    [self allowRestart];
    resolve(@(YES));
}

RCT_EXPORT_METHOD(disallowRestart:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
    [self disallowRestart];
    resolve(@(YES));
}



RCT_EXPORT_METHOD(sync:(NSDictionary *)syncOptions
                  notifySyncStatus:(BOOL)notifySyncStatus
                  notifyProgress:(BOOL)notifyProgress
                  resolve:(RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject)
{
    [self sync:syncOptions withCallback:^{
        resolve(@(YES));
    }
    notifySyncStatus:notifySyncStatus
    notifyProgress:notifyProgress];
}

RCT_EXPORT_METHOD(checkForUpdate:(NSString *)deploymentKey
                  resolve:(RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject)
{
    NSDictionary *remotePackage = [self checkForUpdate:deploymentKey];
    resolve(remotePackage);
}

#pragma mark - JavaScript-exported module methods (Private)

/*
 * This method is the native side of the CodePush.downloadAndReplaceCurrentBundle()
 * method, which replaces the current bundle with the one downloaded from
 * removeBundleUrl. It is only to be used during tests and no-ops if the test
 * configuration flag is not set.
 */
RCT_EXPORT_METHOD(downloadAndReplaceCurrentBundle:(NSString *)remoteBundleUrl)
{
    if ([CodePush isUsingTestConfiguration]) {
        [CodePushPackage downloadAndReplaceCurrentBundle:remoteBundleUrl];
    }
}

/*
 * This method is checks if a new status update exists (new version was installed,
 * or an update failed) and return its details (version label, status).
 */
RCT_EXPORT_METHOD(getNewStatusReport:(RCTPromiseResolveBlock)resolve
                            rejecter:(RCTPromiseRejectBlock)reject)
{
    NSDictionary *newStatusReport = [self getNewStatusReport];
    resolve(newStatusReport);
}

RCT_EXPORT_METHOD(recordStatusReported:(NSDictionary *)statusReport)
{
    [CodePushTelemetryManager recordStatusReported:statusReport];
}

RCT_EXPORT_METHOD(saveStatusReportForRetry:(NSDictionary *)statusReport)
{
    [CodePushTelemetryManager saveStatusReportForRetry:statusReport];
}

#pragma mark - RCTFrameUpdateObserver Methods

- (void)didUpdateFrame:(RCTFrameUpdate *)update
{
    if (!_didUpdateProgress) {
        return;
    }

    [self dispatchDownloadProgressEvent];
    _didUpdateProgress = NO;
}

@end
