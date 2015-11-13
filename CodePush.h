#import "RCTBridgeModule.h"

@interface CodePush : NSObject<RCTBridgeModule>

+ (NSURL *)getBundleUrl;
+ (NSString *)getDocumentsDirectory;

@end

@interface CodePushConfig : NSObject

+ (void)setDeploymentKey:(NSString *)deploymentKey;
+ (NSString *)getDeploymentKey;

+ (void)setServerUrl:(NSString *)setServerUrl;
+ (NSString *)getServerUrl;

+ (void)setAppVersion:(NSString *)appVersion;
+ (NSString *)getAppVersion;

+ (void)setBuildVersion:(NSString *)buildVersion;
+ (NSString *)getBuildVersion;

+ (void)setRootComponent:(NSString *)rootComponent;

+ (NSString *)getRootComponent;

+ (NSDictionary *)getConfiguration;

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

+ (void)applyPackage:(NSDictionary *)updatePackage
               error:(NSError **)error;

+ (NSDictionary *)getCurrentPackage:(NSError **)error;
+ (NSString *)getCurrentPackageFolderPath:(NSError **)error;
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