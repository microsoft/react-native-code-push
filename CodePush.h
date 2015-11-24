#import "RCTBridgeModule.h"

@interface CodePush : NSObject<RCTBridgeModule>

+ (NSURL *)bundleURL;

+ (NSURL *)bundleURLForResourceName:(NSString *)resourceName;

+ (NSURL *)bundleURLForResourceName:(NSString *)resourceName
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

@interface CodePushDownloadHandler : NSObject<NSURLConnectionDelegate>

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