#import "RCTBridgeModule.h"
#import "RCTConvert.h"
#import "RCTEventDispatcher.h"
#import "RCTRootView.h"
#import "RCTUtils.h"

#import "CodePush.h"

@interface CodePush () <RCTBridgeModule>
@end

@implementation CodePush {
    BOOL _hasResumeListener;
    BOOL _isFirstRunAfterUpdate;
}

RCT_EXPORT_MODULE()

#pragma mark - Private constants

// These constants represent valid deployment statuses
static NSString *const DeploymentFailed = @"DeploymentFailed";
static NSString *const DeploymentSucceeded = @"DeploymentSucceeded";

// These keys represent the names we use to store data in NSUserDefaults
static NSString *const FailedUpdatesKey = @"CODE_PUSH_FAILED_UPDATES";
static NSString *const PendingUpdateKey = @"CODE_PUSH_PENDING_UPDATE";

// These keys are already "namespaced" by the PendingUpdateKey, so
// their values don't need to be obfuscated to prevent collision with app data
static NSString *const PendingUpdateHashKey = @"hash";
static NSString *const PendingUpdateIsLoadingKey = @"isLoading";

// These keys are used to inspect/augment the metadata
// that is associated with an update's package.
static NSString *const AppVersionKey = @"appVersion";
static NSString *const BinaryBundleDateKey = @"binaryDate";
static NSString *const PackageHashKey = @"packageHash";
static NSString *const PackageIsPendingKey = @"isPending";

#pragma mark - Static variables

static BOOL isRunningBinaryVersion = NO;
static BOOL needToReportRollback = NO;
static BOOL testConfigurationFlag = NO;

// These values are used to save the bundleURL and extension for the JS bundle
// in the binary.
static NSString *bundleResourceExtension = @"jsbundle";
static NSString *bundleResourceName = @"main";

#pragma mark - Public Obj-C API

+ (NSURL *)binaryBundleURL
{
    return [[NSBundle mainBundle] URLForResource:bundleResourceName withExtension:bundleResourceExtension];
}

+ (NSURL *)bundleURL
{
    return [self bundleURLForResource:bundleResourceName];
}

+ (NSURL *)bundleURLForResource:(NSString *)resourceName
{
    bundleResourceName = resourceName;
    return [self bundleURLForResource:resourceName
                        withExtension:bundleResourceExtension];
}

+ (NSURL *)bundleURLForResource:(NSString *)resourceName
                  withExtension:(NSString *)resourceExtension
{
    bundleResourceName = resourceName;
    bundleResourceExtension = resourceExtension;
    NSError *error;
    NSString *packageFile = [CodePushPackage getCurrentPackageBundlePath:&error];
    NSURL *binaryBundleURL = [self binaryBundleURL];
    
    NSString *logMessageFormat = @"Loading JS bundle from %@";
    
    if (error || !packageFile) {
        NSLog(logMessageFormat, binaryBundleURL);
        isRunningBinaryVersion = YES;
        return binaryBundleURL;
    }
    
    NSString *binaryAppVersion = [[CodePushConfig current] appVersion];
    NSDictionary *currentPackageMetadata = [CodePushPackage getCurrentPackage:&error];
    if (error || !currentPackageMetadata) {
        NSLog(logMessageFormat, binaryBundleURL);
        isRunningBinaryVersion = YES;
        return binaryBundleURL;
    }
    
    NSString *packageDate = [currentPackageMetadata objectForKey:BinaryBundleDateKey];
    NSString *packageAppVersion = [currentPackageMetadata objectForKey:AppVersionKey];
    
    if ([[CodePushUpdateUtils modifiedDateStringOfFileAtURL:binaryBundleURL] isEqualToString:packageDate] && ([CodePush isUsingTestConfiguration] ||[binaryAppVersion isEqualToString:packageAppVersion])) {
        // Return package file because it is newer than the app store binary's JS bundle
        NSURL *packageUrl = [[NSURL alloc] initFileURLWithPath:packageFile];
        NSLog(logMessageFormat, packageUrl);
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
        
        NSLog(logMessageFormat, binaryBundleURL);
        isRunningBinaryVersion = YES;
        return binaryBundleURL;
    }
}

+ (NSString *)getApplicationSupportDirectory
{
    NSString *applicationSupportDirectory = [NSSearchPathForDirectoriesInDomains(NSApplicationSupportDirectory, NSUserDomainMask, YES) objectAtIndex:0];
    return applicationSupportDirectory;
}

/*
 * This returns a boolean value indicating whether CodePush has
 * been set to run under a test configuration.
 */
+ (BOOL)isUsingTestConfiguration
{
    return testConfigurationFlag;
}

+ (void)setDeploymentKey:(NSString *)deploymentKey
{
    [CodePushConfig current].deploymentKey = deploymentKey;
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

/*
 * WARNING: This cleans up all downloaded and pending updates.
 */
+ (void)clearUpdates
{
    [CodePushPackage clearUpdates];
    [self removePendingUpdate];
    [self removeFailedUpdates];
}

#pragma mark - Private API methods

@synthesize bridge = _bridge;
@synthesize methodQueue = _methodQueue;

/*
 * This method is used by the React Native bridge to allow
 * our plugin to expose constants to the JS-side. In our case
 * we're simply exporting enum values so that the JS and Native
 * sides of the plugin can be in sync.
 */
- (NSDictionary *)constantsToExport
{
    // Export the values of the CodePushInstallMode enum
    // so that the script-side can easily stay in sync
    return @{
             @"codePushInstallModeOnNextRestart":@(CodePushInstallModeOnNextRestart),
             @"codePushInstallModeImmediate": @(CodePushInstallModeImmediate),
             @"codePushInstallModeOnNextResume": @(CodePushInstallModeOnNextResume)
            };
};

- (void)dealloc
{
    // Ensure the global resume handler is cleared, so that
    // this object isn't kept alive unnecessarily
    [[NSNotificationCenter defaultCenter] removeObserver:self];
}

- (instancetype)init
{
    self = [super init];
    
    if (self) {
        [self initializeUpdateAfterRestart];
    }
    
    return self;
}

/*
 * This method is used when the app is started to either
 * initialize a pending update or rollback a faulty update
 * to the previous version.
 */
- (void)initializeUpdateAfterRestart
{
    NSUserDefaults *preferences = [NSUserDefaults standardUserDefaults];
    NSDictionary *pendingUpdate = [preferences objectForKey:PendingUpdateKey];
    if (pendingUpdate) {
        _isFirstRunAfterUpdate = YES;
        BOOL updateIsLoading = [pendingUpdate[PendingUpdateIsLoadingKey] boolValue];
        if (updateIsLoading) {
            // Pending update was initialized, but notifyApplicationReady was not called.
            // Therefore, deduce that it is a broken update and rollback.
            NSLog(@"Update did not finish loading the last time, rolling back to a previous version.");
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
- (BOOL)isFailedHash:(NSString*)packageHash
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
- (BOOL)isPendingUpdate:(NSString*)packageHash
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

/*
 * This method updates the React Native bridge's bundle URL
 * to point at the latest CodePush update, and then restarts
 * the bridge. This isn't meant to be called directly.
 */
- (void)loadBundle
{
    // This needs to be async dispatched because the _bridge is not set on init
    // when the app first starts, therefore rollbacks will not take effect.
    dispatch_async(dispatch_get_main_queue(), ^{
        // If the current bundle URL is using http(s), then assume the dev
        // is debugging and therefore, shouldn't be redirected to a local
        // file (since Chrome wouldn't support it). Otherwise, update
        // the current bundle URL to point at the latest update
        if ([CodePush isUsingTestConfiguration] || ![_bridge.bundleURL.scheme hasPrefix:@"http"]) {
            [_bridge setValue:[CodePush bundleURL] forKey:@"bundleURL"];
        }
        
        [_bridge reload];
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
    
    // Write the current package's metadata to the "failed list"
    [self saveFailedUpdate:failedPackage];
    
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

#pragma mark - JavaScript-exported module methods

/*
 * This is native-side of the RemotePackage.download method
 */
RCT_EXPORT_METHOD(downloadUpdate:(NSDictionary*)updatePackage
                        resolver:(RCTPromiseResolveBlock)resolve
                        rejecter:(RCTPromiseRejectBlock)reject)
{
    NSDictionary *mutableUpdatePackage = [updatePackage mutableCopy];
    NSURL *binaryBundleURL = [CodePush binaryBundleURL];
    if (binaryBundleURL != nil) {
        [mutableUpdatePackage setValue:[CodePushUpdateUtils modifiedDateStringOfFileAtURL:binaryBundleURL]
                                forKey:BinaryBundleDateKey];
    }
    
    [CodePushPackage downloadPackage:mutableUpdatePackage
        // The download is progressing forward
        progressCallback:^(long long expectedContentLength, long long receivedContentLength) {
            dispatch_async(_methodQueue, ^{
                // Notify the script-side about the progress
                [self.bridge.eventDispatcher
                    sendDeviceEventWithName:@"CodePushDownloadProgress"
                    body:@{
                            @"totalBytes":[NSNumber numberWithLongLong:expectedContentLength],
                            @"receivedBytes":[NSNumber numberWithLongLong:receivedContentLength]
                          }];
            });
        }
        // The download completed
        doneCallback:^{
            dispatch_async(_methodQueue, ^{
                NSError *err;
                NSDictionary *newPackage = [CodePushPackage getPackage:mutableUpdatePackage[PackageHashKey] error:&err];
                    
                if (err) {
                    return reject([NSString stringWithFormat: @"%lu", (long)err.code], err.localizedDescription, err);
                }
                    
                resolve(newPackage);
            });
        }
        // The download failed
        failCallback:^(NSError *err) {
            dispatch_async(_methodQueue, ^{
                if ([CodePushPackage isCodePushError:err]) {
                    [self saveFailedUpdate:mutableUpdatePackage];
                }
                
                reject([NSString stringWithFormat: @"%lu", (long)err.code], err.localizedDescription, err);
            });
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
    NSDictionary *configuration = [[CodePushConfig current] configuration];
    NSError *error;
    if (isRunningBinaryVersion) {
        // isRunningBinaryVersion will not get set to "YES" if running against the packager.
        NSString *binaryHash = [CodePushUpdateUtils getHashForBinaryContents:[CodePush binaryBundleURL] error:&error];
        if (error) {
            NSLog(@"Error obtaining hash for binary contents: %@", error);
            resolve(configuration);
            return;
        }
        
        if (binaryHash == nil) {
            // The hash was not generated either due to a previous unknown error or the fact that
            // the React Native assets were not bundled in the binary (e.g. during dev/simulator)
            // builds.
            resolve(configuration);
            return;
        }
        
        NSMutableDictionary *mutableConfiguration = [configuration mutableCopy];
        [mutableConfiguration setObject:binaryHash forKey:PackageHashKey];
        resolve(mutableConfiguration);
        return;
    }
    
    resolve(configuration);
}

/*
 * This method is the native side of the CodePush.getCurrentPackage method.
 */
RCT_EXPORT_METHOD(getCurrentPackage:(RCTPromiseResolveBlock)resolve
                           rejecter:(RCTPromiseRejectBlock)reject)
{
    NSError *error;
    NSMutableDictionary *package = [[CodePushPackage getCurrentPackage:&error] mutableCopy];
    
    if (error) {
        reject([NSString stringWithFormat: @"%lu", (long)error.code], error.localizedDescription, error);
        return;
    } else if (package == nil) {
        resolve(nil);
        return;
    }
    
    if (isRunningBinaryVersion) {
        // This only matters in Debug builds. Since we do not clear "outdated" updates,
        // we need to indicate to the JS side that somehow we have a current update on
        // disk that is not actually running.
        [package setObject:@(YES) forKey:@"_isDebugOnly"];
    }
    
    // Add the "isPending" virtual property to the package at this point, so that
    // the script-side doesn't need to immediately call back into native to populate it.
    BOOL isPendingUpdate = [self isPendingUpdate:[package objectForKey:PackageHashKey]];
    [package setObject:@(isPendingUpdate) forKey:PackageIsPendingKey];
    
    resolve(package);
}

/*
 * This method is the native side of the LocalPackage.install method.
 */
RCT_EXPORT_METHOD(installUpdate:(NSDictionary*)updatePackage
                    installMode:(CodePushInstallMode)installMode
                       resolver:(RCTPromiseResolveBlock)resolve
                       rejecter:(RCTPromiseRejectBlock)reject)
{
    NSError *error;
    [CodePushPackage installPackage:updatePackage
                removePendingUpdate:[self isPendingUpdate:nil]
                              error:&error];
    
    if (error) {
        reject([NSString stringWithFormat: @"%lu", (long)error.code], error.localizedDescription, error);
    } else {
        [self savePendingUpdate:updatePackage[PackageHashKey]
                      isLoading:NO];
        
        if (installMode == CodePushInstallModeOnNextResume && !_hasResumeListener) {
            // Ensure we do not add the listener twice.
            // Register for app resume notifications so that we
            // can check for pending updates which support "restart on resume"
            [[NSNotificationCenter defaultCenter] addObserver:self
                                                     selector:@selector(loadBundle)
                                                         name:UIApplicationWillEnterForegroundNotification
                                                       object:[UIApplication sharedApplication]];
            _hasResumeListener = YES;
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
    BOOL isFailedHash = [self isFailedHash:packageHash];
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
    NSError *error;
    BOOL isFirstRun = _isFirstRunAfterUpdate
                        && nil != packageHash
                        && [packageHash length] > 0
                        && [packageHash isEqualToString:[CodePushPackage getCurrentPackageHash:&error]];
    
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
 * This method is checks if a new status update exists (new version was installed,
 * or an update failed) and return its details (version label, status).
 */
RCT_EXPORT_METHOD(getNewStatusReport:(RCTPromiseResolveBlock)resolve
                            rejecter:(RCTPromiseRejectBlock)reject)
{    
    if (needToReportRollback) {
        needToReportRollback = NO;
        NSUserDefaults *preferences = [NSUserDefaults standardUserDefaults];
        NSMutableArray *failedUpdates = [preferences objectForKey:FailedUpdatesKey];
        if (failedUpdates) {
            NSDictionary *lastFailedPackage = [failedUpdates lastObject];
            if (lastFailedPackage) {
                resolve([CodePushTelemetryManager getRollbackReport:lastFailedPackage]);
                return;
            }
        }
    } else if (_isFirstRunAfterUpdate) {
        NSError *error;
        NSDictionary *currentPackage = [CodePushPackage getCurrentPackage:&error];
        if (!error && currentPackage) {
            resolve([CodePushTelemetryManager getUpdateReport:currentPackage]);
            return;
        }
    } else if (isRunningBinaryVersion) {
        NSString *appVersion = [[CodePushConfig current] appVersion];
        resolve([CodePushTelemetryManager getBinaryUpdateReport:appVersion]);
        return;
    }
    
    resolve(nil);
}

/*
 * This method is the native side of the CodePush.restartApp() method.
 */
RCT_EXPORT_METHOD(restartApp:(BOOL)onlyIfUpdateIsPending)
{
    // If this is an unconditional restart request, or there
    // is current pending update, then reload the app.
    if (!onlyIfUpdateIsPending || [self isPendingUpdate:nil]) {
        [self loadBundle];
    }
}

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

@end