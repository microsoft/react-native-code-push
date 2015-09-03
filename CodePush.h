#import "RCTBridgeModule.h"

@interface CodePush : NSObject <RCTBridgeModule>

+ (NSURL *) getBundleUrl;

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

+ (void)downloadPackage:(NSDictionary *)updatePackage
                            error:(NSError **)error;

+ (void)applyPackage:(NSString *)packageHash;

@end