#import "CodePush.h"

@interface CodePush (Reporting)

+ (void)reportStatusDownload:(NSDictionary *)updatePackage withConfiguraton:(NSDictionary*)config;

+ (void)reportStatusDeploy:(NSDictionary *)statusReport withConfiguraton:(NSDictionary*)config;

@end
