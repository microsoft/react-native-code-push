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