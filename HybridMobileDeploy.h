#import "RCTBridgeModule.h"

@interface HybridMobileDeploy : NSObject <RCTBridgeModule>

+ (NSURL *) getBundleUrl;

@end

@interface HybridMobileDeployConfig : NSObject

+ (void)setDeploymentKey:(NSString *)deploymentKey;
+ (NSString *)getDeploymentKey;

+ (void)setBaseUrl:(NSString *)baseUrl;
+ (NSString *)getBaseUrl;

+ (void)setVersionString:(NSString *)versionString;
+ (NSString *)getVersionString;

+ (void)setBuildVersion:(NSString *)buildVersion;
+ (NSString *)getBuildVersion;

+ (void)setRootComponent:(NSString *)rootComponent;
+ (NSString *)getRootComponent;

+ (NSDictionary *)getConfiguration;

@end