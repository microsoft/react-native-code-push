#import "RCTBridgeModule.h"
#import "RCTEventDispatcher.h"
#import "RCTConvert.h"
#import "RCTRootView.h"
#import "RCTUtils.h"
#import "CodePush.h"

@implementation CodePush

RCT_EXPORT_MODULE()

NSTimer *_timer;
BOOL usingTestFolder = NO;
BOOL didUpdate = NO;

NSString * const FailedUpdatesKey = @"CODE_PUSH_FAILED_UPDATES";
NSString * const PendingUpdateKey = @"CODE_PUSH_PENDING_UPDATE";

// These keys are already "namespaced" by the PendingUpdateKey, so
// their values don't need to be obfuscated to prevent collision with app data
NSString * const PendingUpdateAllowsRestartOnResumeKey = @"allowsRestartOnResume";
NSString * const PendingUpdateHashKey = @"hash";
NSString * const PendingUpdateRollbackTimeoutKey = @"rollbackTimeout";

@synthesize bridge = _bridge;

// Public Obj-C API
+ (NSString *)getDocumentsDirectory
{
    NSString *documentsDirectory = [NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES) objectAtIndex:0];
    return documentsDirectory;
}

+ (NSURL *)getBundleUrl
{
    NSError *error;
    NSString *packageFile = [CodePushPackage getCurrentPackageBundlePath:&error];
    NSURL *binaryJsBundleUrl = [[NSBundle mainBundle] URLForResource:@"main" withExtension:@"jsbundle"];
    
    if (error || !packageFile)
    {
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

// Internal API methods
- (void)cancelRollbackTimer
{
    dispatch_async(dispatch_get_main_queue(), ^{
        [_timer invalidate];
    });
}

- (void)checkForPendingUpdate:(BOOL)isAppStart {
    dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
        NSUserDefaults *preferences = [NSUserDefaults standardUserDefaults];
        NSDictionary *pendingUpdate = [preferences objectForKey:PendingUpdateKey];
        
        if (pendingUpdate)
        {
            NSError *error;
            NSString *pendingHash = pendingUpdate[PendingUpdateHashKey];
            NSString *currentHash = [CodePushPackage getCurrentPackageHash:&error];
            
            // If the current hash is equivalent to the pending hash, then the app
            // restart "picked up" the new update, but we need to kick off the
            // rollback timer and ensure that the necessary state is setup.
            if ([pendingHash isEqualToString:currentHash]) {
                // We only want to initialize the rollback timer in two scenarios:
                // 1) The app has been restarted, and therefore, the pending update is already applied
                // 2) The app has been resumed, and the pending update indicates it supports being restarted on resume
                if (isAppStart || (!isAppStart && [pendingUpdate[PendingUpdateAllowsRestartOnResumeKey] boolValue]))
                {
                    int rollbackTimeout = [pendingUpdate[PendingUpdateRollbackTimeoutKey] intValue];
                    
                    // If the app wasn't restarted "naturally", then we need to restart it manually
                    BOOL needsRestart = !isAppStart;
                    [self initializeUpdateWithRollbackTimeout:rollbackTimeout needsRestart:needsRestart];
                    
                    // Clear the pending update and sync
                    [preferences removeObjectForKey:PendingUpdateKey];
                    [preferences synchronize];
                }
            }
        }
    });
}

- (void)checkForPendingUpdateDuringResume {
    [self checkForPendingUpdate:NO];
}

- (NSDictionary *)constantsToExport
{
    // Export the values of the CodePushRestartMode enum
    // so that the script-side can easily stay in sync
    return @{ @"codePushRestartModeNone": @(CodePushRestartModeNone),
              @"codePushRestartModeImmediate": @(CodePushRestartModeImmediate),
              @"codePushRestartModeOnNextResume": @(CodePushRestartModeOnNextResume)
              };
};

- (void)dealloc {
    [[NSNotificationCenter defaultCenter] removeObserver:self];
}

- (CodePush *)init
{
    self = [super init];
    
    if (self) {
        [self checkForPendingUpdate:YES];
        
        // Register for app resume notifications so that we
        // can check for pending updates which support "restart on resume"
        [[NSNotificationCenter defaultCenter] addObserver:self
                                                 selector:@selector(checkForPendingUpdateDuringResume)
                                                     name:UIApplicationWillEnterForegroundNotification
                                                   object:[UIApplication sharedApplication]];
    }
    
    return self;
}

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

- (BOOL)isFailedHash:(NSString*)packageHash
{
    NSUserDefaults *preferences = [NSUserDefaults standardUserDefaults];
    NSMutableArray *failedUpdates = [preferences objectForKey:FailedUpdatesKey];
    return (failedUpdates != nil && [failedUpdates containsObject:packageHash]);
}

- (void)loadBundle
{
    // Reset the runtime's bundle to be
    // the latest URL, and then force a refresh
    _bridge.bundleURL = [CodePush getBundleUrl];
    [_bridge reload];
}

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

- (void)saveFailedUpdate:(NSString *)packageHash {
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

- (void)savePendingUpdate:(NSString *)packageHash
          rollbackTimeout:(int)rollbackTimeout
     allowRestartOnResume:(BOOL)allowRestartOnResume
{
    // Since we're not restarting, we need to store the fact that the update
    // was applied, but hasn't yet become "active".
    NSUserDefaults *preferences = [NSUserDefaults standardUserDefaults];
    NSDictionary *pendingUpdate = [[NSDictionary alloc] initWithObjectsAndKeys:
                                   packageHash,PendingUpdateHashKey,
                                   [NSNumber numberWithInt:rollbackTimeout],PendingUpdateRollbackTimeoutKey,
                                   [NSNumber numberWithBool:allowRestartOnResume],PendingUpdateAllowsRestartOnResumeKey,
                                   nil];
    
    [preferences setObject:pendingUpdate forKey:PendingUpdateKey];
    [preferences synchronize];
}

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
RCT_EXPORT_METHOD(applyUpdate:(NSDictionary*)updatePackage
                  rollbackTimeout:(int)rollbackTimeout
                  restartMode:(CodePushRestartMode)restartMode
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
    dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
        NSError *error;
        [CodePushPackage applyPackage:updatePackage
                                error:&error];
        
        if (error) {
            reject(error);
        } else {
            if (restartMode == CodePushRestartModeImmediate) {
                [self initializeUpdateWithRollbackTimeout:rollbackTimeout needsRestart:YES];
            } else {
                BOOL allowsRestartOnResume = (restartMode == CodePushRestartModeOnNextResume);
                [self savePendingUpdate:updatePackage[@"packageHash"]
                        rollbackTimeout:rollbackTimeout
                   allowRestartOnResume:allowsRestartOnResume];
            }
        }
    });
}

RCT_EXPORT_METHOD(downloadUpdate:(NSDictionary*)updatePackage
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
    [CodePushPackage downloadPackage:updatePackage
                    progressCallback:^(long expectedContentLength, long receivedContentLength) {
                        [self.bridge.eventDispatcher
                         sendAppEventWithName:@"CodePushDownloadProgress"
                         body:@{
                                @"totalBytes":[NSNumber numberWithLong:expectedContentLength],
                                @"receivedBytes":[NSNumber numberWithLong:receivedContentLength]
                                }];
                    }
                        doneCallback:^{
                            NSError *err;
                            NSDictionary *newPackage = [CodePushPackage
                                                        getPackage:updatePackage[@"packageHash"]
                                                        error:&err];
                            
                            if (err) {
                                return reject(err);
                            }
                            
                            resolve(newPackage);
                        }
                        failCallback:^(NSError *err) {
                            reject(err);
                        }];
}

RCT_EXPORT_METHOD(getConfiguration:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
    resolve([CodePushConfig getConfiguration]);
}

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

RCT_EXPORT_METHOD(isFailedUpdate:(NSString *)packageHash
                  resolve:(RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject)
{
    BOOL isFailedHash = [self isFailedHash:packageHash];
    resolve(@(isFailedHash));
}

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

RCT_EXPORT_METHOD(notifyApplicationReady:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
    [self cancelRollbackTimer];
    resolve([NSNull null]);
}

RCT_EXPORT_METHOD(setUsingTestFolder:(BOOL)shouldUseTestFolder)
{
    usingTestFolder = shouldUseTestFolder;
}

@end