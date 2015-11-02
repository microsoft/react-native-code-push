#import "RCTBridgeModule.h"

@interface CodePush : NSObject<RCTBridgeModule>

+ (NSString *)getDocumentsDirectory;
+ (NSURL *)getBundleUrl;

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

@interface CodePushPackage : NSObject

+ (NSString *)getCurrentPackageFolderPath:(NSError **)error;

+ (NSString *)getPackageFolderPath:(NSString *)packageHash;

+ (NSDictionary *)getCurrentPackage:(NSError **)error;

+ (NSDictionary *)getPackage:(NSString *)packageHash
                       error:(NSError **)error;

+ (void)downloadPackage:(NSDictionary *)updatePackage
                            error:(NSError **)error;

+ (void)applyPackage:(NSDictionary *)updatePackage
               error:(NSError **)error;

+ (void)rollbackPackage;

@end