#import "RCTBridgeModule.h"

@interface HybridMobileDeploy : NSObject <RCTBridgeModule>
+ (NSString *) getBundleFolderPath:(NSString*)bundleName;
+ (NSURL *) getNativeBundleURL:(NSString*)bundleName;
+ (NSURL *)appBundleUrl:(NSString*)bundleName
       nativeBundleName:(NSString*)nativeBundleName;
@end
