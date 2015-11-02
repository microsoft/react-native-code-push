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
                            error:(NSError **)error;

+ (void)rollbackPackage;

@end