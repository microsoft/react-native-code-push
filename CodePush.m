#import "RCTBridgeModule.h"
#import "RCTConvert.h"
#import "RCTEventDispatcher.h"
#import "RCTRootView.h"
#import "RCTUtils.h"

#import "CodePush.h"

@implementation CodePush {
    BOOL _resumablePendingUpdateAvailable;
}

RCT_EXPORT_MODULE()

static BOOL didUpdate = NO;
static NSTimer *_timer;
static BOOL usingTestFolder = NO;

static NSString *const FailedUpdatesKey = @"CODE_PUSH_FAILED_UPDATES";
static NSString *const PendingUpdateKey = @"CODE_PUSH_PENDING_UPDATE";

// These keys are already "namespaced" by the PendingUpdateKey, so
// their values don't need to be obfuscated to prevent collision with app data
static NSString *const PendingUpdateHashKey = @"hash";
static NSString *const PendingUpdateRollbackTimeoutKey = @"rollbackTimeout";

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

+ (NSString *)getDocumentsDirectory
{
    NSString *documentsDirectory = [NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES) objectAtIndex:0];
    return documentsDirectory;
}

// Private API methods

/*
 * This method cancels the currently running rollback
 * timer, which has the effect of stopping an automatic
 * rollback from occuring. 
 *
 * Note: This method is safe to call from any thread.
 */
- (void)cancelRollbackTimer
{
    dispatch_async(dispatch_get_main_queue(), ^{
        [_timer invalidate];
    });
}

/* 
 * This method checks to see whether a "pending udpate" has been applied
 * (e.g. install was called with a non-immediate mode), but the app hasn't
 * yet been restarted (either naturally or synthentically). If there is one,
 * it will restart the app (if specified), and start the rollback timer.
 *
 * Note: This method is safe to call from any thread.
 */
- (void)checkForPendingUpdate:(BOOL)needsRestart
{
    dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
        NSUserDefaults *preferences = [NSUserDefaults standardUserDefaults];
        NSDictionary *pendingUpdate = [preferences objectForKey:PendingUpdateKey];
        
        if (pendingUpdate) {
            NSError *error;
            NSString *pendingHash = pendingUpdate[PendingUpdateHashKey];
            NSString *currentHash = [CodePushPackage getCurrentPackageHash:&error];
            
            // If the current hash is equivalent to the pending hash, then the app
            // restart "picked up" the new update, but we need to kick off the
            // rollback timer and ensure that the necessary state is setup.
            if ([pendingHash isEqualToString:currentHash]) {
                int rollbackTimeout = [pendingUpdate[PendingUpdateRollbackTimeoutKey] intValue];
                [self initializeUpdateWithRollbackTimeout:rollbackTimeout needsRestart:needsRestart];
            } else {
                // NOTE: We shouldn't ever reach here
            }
                            
            // Clear the pending update and sync
            [preferences removeObjectForKey:PendingUpdateKey];
            [preferences synchronize];
        }
    });
}

/*
 * This method is meant as a handler for the global app
 * resume notification, and therefore, should not be called
 * directly. It simply checks to see whether there is a pending
 * update that is meant to be installed on resume, and if so
 * it applies it and restarts the app.
 */
- (void)checkForPendingUpdateDuringResume
{
    // In order to ensure that CodePush doesn't impact the app's
    // resume experience, we're using a simple boolean check to
    // check whether we need to restart, before reading the defaults store
    if (_resumablePendingUpdateAvailable) {
        [self checkForPendingUpdate:YES];
    }
}

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
        // Do an async check to see whether
        // we need to start the rollback timer
        // due to a pending update being installed at start
        [self checkForPendingUpdate:NO];
        
        // Register for app resume notifications so that we
        // can check for pending updates which support "restart on resume"
        [[NSNotificationCenter defaultCenter] addObserver:self
                                                 selector:@selector(checkForPendingUpdateDuringResume)
                                                     name:UIApplicationWillEnterForegroundNotification
                                                   object:[UIApplication sharedApplication]];
    }
    
    return self;
}

/*
 * This method performs the actual initialization work for a update
 * to ensure that the neccessary state is setup, including:
 * --------------------------------------------------------
 * 1. Updating the current bundle URL to point at the latest update on disk
 * 2. Optionally restarting the app to load the new bundle
 * 3. Optionally starting the rollback protection timer
 */
- (void)initializeUpdateWithRollbackTimeout:(int)rollbackTimeout
                               needsRestart:(BOOL)needsRestart
{
    didUpdate = YES;
    
    if (needsRestart) {
        [self loadBundle];
    }
    
    if (0 != rollbackTimeout) {
        dispatch_async(dispatch_get_main_queue(), ^{
            [self startRollbackTimer:rollbackTimeout];
        });
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
 * This method updates the React Native bridge's bundle URL
 * to point at the latest CodePush update, and then restarts
 * the bridge. This isn't meant to be called directly.
 */
- (void)loadBundle
{
    // If the current bundle URL is using http(s), then assume the dev
    // is debugging and therefore, shouldn't be redirected to a local
    // file (since Chrome wouldn't support it). Otherwise, update
    // the current bundle URL to point at the latest update
    if (![_bridge.bundleURL.scheme hasPrefix:@"http"]) {
        _bridge.bundleURL = [CodePush bundleURL];
    }
    
    [_bridge reload];
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
    
    // Do the actual rollback and then
    // refresh the app with the previous package
    [CodePushPackage rollbackPackage];
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
 * When an update is installed whose mode isn't IMMEDIATE, this method
 * can be called to store the pending update's metadata (e.g. rollbackTimeout)
 * so that it can be used when the actual update application occurs at a later point.
 */
- (void)savePendingUpdate:(NSString *)packageHash
          rollbackTimeout:(int)rollbackTimeout
{
    // Since we're not restarting, we need to store the fact that the update
    // was installed, but hasn't yet become "active".
    NSUserDefaults *preferences = [NSUserDefaults standardUserDefaults];
    NSDictionary *pendingUpdate = [[NSDictionary alloc] initWithObjectsAndKeys:
                                   packageHash,PendingUpdateHashKey,
                                   [NSNumber numberWithInt:rollbackTimeout],PendingUpdateRollbackTimeoutKey, nil];
    
    [preferences setObject:pendingUpdate forKey:PendingUpdateKey];
    [preferences synchronize];
}

/*
 * This method handles starting the actual rollback timer
 * after an update has been installed.
 */
- (void)startRollbackTimer:(int)rollbackTimeout
{
    double timeoutInSeconds = rollbackTimeout / 1000;
    _timer = [NSTimer scheduledTimerWithTimeInterval:timeoutInSeconds
                                              target:self
                                            selector:@selector(rollbackPackage)
                                            userInfo:nil
                                             repeats:NO];
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
        progressCallback:^(long expectedContentLength, long receivedContentLength) {
            // Notify the script-side about the progress
            [self.bridge.eventDispatcher
                sendAppEventWithName:@"CodePushDownloadProgress"
                body:@{
                        @"totalBytes":[NSNumber numberWithLong:expectedContentLength],
                        @"receivedBytes":[NSNumber numberWithLong:receivedContentLength]
                      }];
        }
        // The download completed
        doneCallback:^{
            NSError *err;
            NSDictionary *newPackage = [CodePushPackage getPackage:updatePackage[@"packageHash"] error:&err];
                
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
        NSDictionary *package = [CodePushPackage getCurrentPackage:&error];
        if (error) {
            reject(error);
        } else {
            resolve(package);
        }
    });
}

/*
 * This method is the native side of the LocalPackage.install method.
 */
RCT_EXPORT_METHOD(installUpdate:(NSDictionary*)updatePackage
                rollbackTimeout:(int)rollbackTimeout
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
            if (installMode != CodePushInstallModeImmediate) {
                _resumablePendingUpdateAvailable = (installMode == CodePushInstallModeOnNextResume);
                [self savePendingUpdate:updatePackage[@"packageHash"]
                        rollbackTimeout:rollbackTimeout];
            }
            // Signal to JS that the update has been applied.
            resolve(nil);
        }
    });
}

/*
 * This method isn't publically exposed via the "react-native-code-push"
 * module, and is only used internally to populate the RemotePackage.failedApply property.
 */
RCT_EXPORT_METHOD(isFailedUpdate:(NSString *)packageHash
                         resolve:(RCTPromiseResolveBlock)resolve
                          reject:(RCTPromiseRejectBlock)reject)
{
    BOOL isFailedHash = [self isFailedHash:packageHash];
    resolve(@(isFailedHash));
}

/*
 * This method isn't publically exposed via the "react-native-code-push"
 * module, and is only used internally to populate the LocalPackage.isFirstRun property.
 */
RCT_EXPORT_METHOD(isFirstRun:(NSString *)packageHash
                     resolve:(RCTPromiseResolveBlock)resolve
                    rejecter:(RCTPromiseRejectBlock)reject)
{
    NSError *error;
    BOOL isFirstRun = didUpdate
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
    [self cancelRollbackTimer];
    resolve([NSNull null]);
}

/*
 * This method isn't publically exposed via the "react-native-code-push"
 * module, and is only used internally to support immediately installed updates.
 */
RCT_EXPORT_METHOD(restartImmediateUpdate:(int)rollbackTimeout)
{
    [self initializeUpdateWithRollbackTimeout:rollbackTimeout needsRestart:YES];
}

/*
 * This method is the native side of the CodePush.restartPendingUpdate() method.
 */
RCT_EXPORT_METHOD(restartPendingUpdate)
{
    [self checkForPendingUpdate:YES];
}

RCT_EXPORT_METHOD(setUsingTestFolder:(BOOL)shouldUseTestFolder)
{
    usingTestFolder = shouldUseTestFolder;
}

@end