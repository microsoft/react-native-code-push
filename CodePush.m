#import "RCTBridgeModule.h"
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
NSString * const UpdateBundleFileName = @"app.jsbundle";

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
    NSString *packageFolder = [CodePushPackage getCurrentPackageFolderPath:&error];
    NSURL *binaryJsBundleUrl = [[NSBundle mainBundle] URLForResource:@"main" withExtension:@"jsbundle"];
    
    if (error || !packageFolder)
    {
        return binaryJsBundleUrl;
    }
    
    NSString *packageFile = [packageFolder stringByAppendingPathComponent:UpdateBundleFileName];
    
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

- (CodePush *)init
{
    self = [super init];
    
    if (self) {
        NSUserDefaults *preferences = [NSUserDefaults standardUserDefaults];
        NSDictionary *pendingUpdate = [preferences objectForKey:PendingUpdateKey];
        
        if (pendingUpdate)
        {
            NSError *error;
            NSString *pendingHash = pendingUpdate[@"hash"];
            NSString *currentHash = [CodePushPackage getCurrentPackageHash:&error];
            
            // If the current hash is equivalent to the pending hash, then the app
            // restart "picked up" the new update, but we need to kick off the
            // rollback timer and ensure that the necessary state is setup.
            if ([pendingHash isEqualToString:currentHash]) {
                int rollbackTimeout = [pendingUpdate[@"rollbackTimeout"] intValue];
                [self initializeUpdateWithRollbackTimeout:rollbackTimeout needsRestart:NO];
                
                // Clear the pending update and sync
                [preferences removeObjectForKey:PendingUpdateKey];
                [preferences synchronize];
            }
        }
    }
    
    return self;
}

- (void)initializeUpdateWithRollbackTimeout:(int)rollbackTimeout needsRestart:(BOOL)needsRestart {
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

- (BOOL)isFailedHash:(NSString*)packageHash {
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
          rollbackTimeout:(int)rollbackTimeout {
    // Since we're not restarting, we need to store the fact that the update
    // was applied, but hasn't yet become "active".
    NSUserDefaults *preferences = [NSUserDefaults standardUserDefaults];
    NSDictionary *pendingUpdate = [[NSDictionary alloc] initWithObjectsAndKeys:
                                   packageHash,@"hash",
                                   rollbackTimeout,@"rollbackTimeout", nil];
    
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
           restartImmediately:(BOOL)restartImmediately
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
            if (restartImmediately) {
                [self initializeUpdateWithRollbackTimeout:rollbackTimeout needsRestart:YES];
            } else {
                [self savePendingUpdate:updatePackage[@"packageHash"] rollbackTimeout:rollbackTimeout];
            }
        }
    });
}

RCT_EXPORT_METHOD(downloadUpdate:(NSDictionary*)updatePackage
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
    dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
        NSError *err;
        [CodePushPackage downloadPackage:updatePackage
                                   error:&err];
        
        if (err) {
            return reject(err);
        }
        
        NSDictionary *newPackage = [CodePushPackage getPackage:updatePackage[@"packageHash"]
                                                         error:&err];
        
        if (err) {
            return reject(err);
        }
        
        resolve(newPackage);
    });
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