#import "RCTBridgeModule.h"
#import "RCTConvert.h"
#import "RCTEventDispatcher.h"
#import "RCTRootView.h"
#import "RCTUtils.h"

#import "CodePush.h"

@implementation CodePush {
    BOOL _hasResumeListener;
    BOOL _isFirstRunAfterUpdate;
}

RCT_EXPORT_MODULE()

static BOOL usingTestFolder = NO;

// These keys represent the names we use to store data in NSUserDdefaults
static NSString *const FailedUpdatesKey = @"CODE_PUSH_FAILED_UPDATES";
static NSString *const PendingUpdateKey = @"CODE_PUSH_PENDING_UPDATE";

// These keys are already "namespaced" by the PendingUpdateKey, so
// their values don't need to be obfuscated to prevent collision with app data
static NSString *const PendingUpdateHashKey = @"hash";
static NSString *const PendingUpdateIsLoadingKey = @"isLoading";

// These keys are used to inspect/augment the metada
// that is associated with an update's package.
static NSString *const PackageHashKey = @"packageHash";
static NSString *const PackageIsPendingKey = @"isPending";

@synthesize bridge = _bridge;

// Public Obj-C API (see header for method comments)
+ (NSURL *)bundleURL
{
    return [self bundleURLForResource:@"main"];
}

+ (NSURL *)bundleURLForResource:(NSString *)resourceName
{
    return [self bundleURLForResource:resourceName
                        withExtension:@"jsbundle"];
}

+ (NSURL *)bundleURLForResource:(NSString *)resourceName
                  withExtension:(NSString *)resourceExtension
{
    NSError *error;
    NSString *packageFile = [CodePushPackage getCurrentPackageBundlePath:&error];
    NSURL *binaryJsBundleUrl = [[NSBundle mainBundle] URLForResource:resourceName withExtension:resourceExtension];
    
    if (error || !packageFile) {
        return binaryJsBundleUrl;
    }
    
    NSDictionary *binaryFileAttributes = [[NSFileManager defaultManager] attributesOfItemAtPath:[binaryJsBundleUrl path] error:nil];
    NSDictionary *appFileAttribs = [[NSFileManager defaultManager] attributesOfItemAtPath:packageFile error:nil];
    NSDate *binaryDate = [binaryFileAttributes objectForKey:NSFileModificationDate];
    NSDate *packageDate = [appFileAttribs objectForKey:NSFileModificationDate];
    
    if ([binaryDate compare:packageDate] == NSOrderedAscending) {
        // Return package file because it is newer than the app store binary's JS bundle
        return [[NSURL alloc] initFileURLWithPath:packageFile];
    } else {
        return binaryJsBundleUrl;
    }
}

+ (NSString *)getApplicationSupportDirectory
{
    NSString *applicationSupportDirectory = [NSSearchPathForDirectoriesInDomains(NSApplicationSupportDirectory, NSUserDomainMask, YES) objectAtIndex:0];
    return applicationSupportDirectory;
}

// Private API methods

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
    return (failedUpdates != nil && [failedUpdates containsObject:packageHash]);
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

    // If there is a pending update, whose hash is equal to the one
    // specified, and its "state" isn't loading, then we consider it "pending".
    BOOL updateIsPending = pendingUpdate &&
                           [pendingUpdate[PendingUpdateIsLoadingKey] boolValue] == NO &&
                           [pendingUpdate[PendingUpdateHashKey] isEqualToString:packageHash];
    
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
        if (![_bridge.bundleURL.scheme hasPrefix:@"http"]) {
            _bridge.bundleURL = [CodePush bundleURL];
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
    NSString *packageHash = [CodePushPackage getCurrentPackageHash:&error];
    
    // Write the current package's hash to the "failed list"
    [self saveFailedUpdate:packageHash];
    
    // Rollback to the previous version and de-register the new update
    [CodePushPackage rollbackPackage];
    [self removePendingUpdate];
    [self loadBundle];
}

/*
 * When an update failed to apply, this method can be called
 * to store its hash so that it can be ignored on future
 * attempts to check the server for an update.
 */
- (void)saveFailedUpdate:(NSString *)packageHash
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
    
    [failedUpdates addObject:packageHash];
    [preferences setObject:failedUpdates forKey:FailedUpdatesKey];
    [preferences synchronize];
}

/*
 * This method  is used to register the fact that a pending
 * update succeeded and therefore can be removed.
 */
- (void)removePendingUpdate
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

// JavaScript-exported module methods

/*
 * This is native-side of the RemotePackage.download method
 */
RCT_EXPORT_METHOD(downloadUpdate:(NSDictionary*)updatePackage
                        resolver:(RCTPromiseResolveBlock)resolve
                        rejecter:(RCTPromiseRejectBlock)reject)
{
    [CodePushPackage downloadPackage:updatePackage
        // The download is progressing forward
        progressCallback:^(long long expectedContentLength, long long receivedContentLength) {
            // Notify the script-side about the progress
            [self.bridge.eventDispatcher
                sendDeviceEventWithName:@"CodePushDownloadProgress"
                body:@{
                        @"totalBytes":[NSNumber numberWithLongLong:expectedContentLength],
                        @"receivedBytes":[NSNumber numberWithLongLong:receivedContentLength]
                      }];
        }
        // The download completed
        doneCallback:^{
            NSError *err;
            NSDictionary *newPackage = [CodePushPackage getPackage:updatePackage[PackageHashKey] error:&err];
                
            if (err) {
                return reject(err);
            }
                
            resolve(newPackage);
        }
        // The download failed
        failCallback:^(NSError *err) {
            reject(err);
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
    resolve([[CodePushConfig current] configuration]);
}

/*
 * This method is the native side of the CodePush.getCurrentPackage method.
 */
RCT_EXPORT_METHOD(getCurrentPackage:(RCTPromiseResolveBlock)resolve
                           rejecter:(RCTPromiseRejectBlock)reject)
{
    dispatch_async(dispatch_get_main_queue(), ^{
        NSError *error;
        NSMutableDictionary *package = [[CodePushPackage getCurrentPackage:&error] mutableCopy];
        
        if (error) {
            reject(error);
        }
        
        // Add the "isPending" virtual property to the package at this point, so that
        // the script-side doesn't need to immediately call back into native to populate it.
        BOOL isPendingUpdate = [self isPendingUpdate:[package objectForKey:PackageHashKey]];
        [package setObject:@(isPendingUpdate) forKey:PackageIsPendingKey];

        resolve(package);
    });
}

/*
 * This method is the native side of the LocalPackage.install method.
 */
RCT_EXPORT_METHOD(installUpdate:(NSDictionary*)updatePackage
                    installMode:(CodePushInstallMode)installMode
                       resolver:(RCTPromiseResolveBlock)resolve
                       rejecter:(RCTPromiseRejectBlock)reject)
{
    dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
        NSError *error;
        [CodePushPackage installPackage:updatePackage
                                  error:&error];
        
        if (error) {
            reject(error);
        } else {
            [self savePendingUpdate:updatePackage[PackageHashKey]
                          isLoading:NO];
            
            if (installMode == CodePushInstallModeImmediate) {
                [self loadBundle];
            } else if (installMode == CodePushInstallModeOnNextResume) {
                // Ensure we do not add the listener twice.
                if (!_hasResumeListener) {
                    // Register for app resume notifications so that we
                    // can check for pending updates which support "restart on resume"
                    [[NSNotificationCenter defaultCenter] addObserver:self
                                                             selector:@selector(loadBundle)
                                                                 name:UIApplicationWillEnterForegroundNotification
                                                               object:[UIApplication sharedApplication]];
                    _hasResumeListener = YES;
                }
            }
            // Signal to JS that the update has been applied.
            resolve(nil);
        }
    });
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
    [self removePendingUpdate];
    resolve([NSNull null]);
}

/*
 * This method is the native side of the CodePush.restartApp() method.
 */
RCT_EXPORT_METHOD(restartApp)
{
    [self loadBundle];
}

RCT_EXPORT_METHOD(setUsingTestFolder:(BOOL)shouldUseTestFolder)
{
    usingTestFolder = shouldUseTestFolder;
}

@end