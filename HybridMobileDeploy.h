#import "RCTBridgeModule.h"

@interface HybridMobileDeploy : NSObject <RCTBridgeModule>
+ (NSString *) getBundlePath:(NSString*)bundleName;
+ (NSURL *) getNativeBundleURL:(NSString*)bundleName;
+ (NSURL *)appBundleUrl:(NSString*)bundleName
       nativeBundleName:(NSString*)nativeBundleName;
@end
