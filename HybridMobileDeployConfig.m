#import "HybridMobileDeploy.h"

NSMutableDictionary *configuration;

@implementation HybridMobileDeployConfig

+ (void)initialize
{
    NSDictionary *infoDictionary = [[NSBundle mainBundle] infoDictionary];
    
    NSString *version = [infoDictionary objectForKey:@"CFBundleShortVersionString"];
    NSString *buildVersion = [infoDictionary objectForKey:@"CFBundleVersion"];
    NSString *deploymentKey = [infoDictionary objectForKey:@"CodePushDeploymentKey"];
    NSString *serverUrl = [infoDictionary objectForKey:@"CodePushServerUrl"];
    if (!serverUrl) {
        serverUrl = @"http://localhost:3000/";
    }
    NSString *rootComponent = [infoDictionary objectForKey:@"CFBundleName"];
    
    configuration = [[NSMutableDictionary alloc]
                                   initWithObjectsAndKeys:
                                   version,@"versionString",
                                   buildVersion,@"buildVersion",
                                   deploymentKey,@"deploymentKey",
                                   serverUrl,@"serverUrl",
                                   rootComponent,@"rootComponent",
                                   nil];
}

+ (void)setDeploymentKey:(NSString *)deploymentKey
{
    [configuration setValue:deploymentKey forKey:@"deploymentKey"];
}

+ (NSString *)getDeploymentKey
{
    return [configuration objectForKey:@"deploymentKey"];
}

+ (void)setServerUrl:(NSString *)serverUrl
{
    [configuration setValue:serverUrl forKey:@"serverUrl"];
}

+ (NSString *)getServerUrl
{
    return [configuration objectForKey:@"serverUrl"];
}

+ (void)setVersionString:(NSString *)baseUrl
{
    [configuration setValue:baseUrl forKey:@"versionString"];
}

+ (NSString *)getVersionString
{
    return [configuration objectForKey:@"versionString"];
}

+ (void)setBuildVersion:(NSString *)buildVersion
{
    [configuration setValue:buildVersion forKey:@"buildVersion"];
}

+ (NSString *)getBuildVersion
{
    return [configuration objectForKey:@"buildVersion"];
}

+ (void)setRootComponent:(NSString *)buildVersion
{
    [configuration setValue:buildVersion forKey:@"rootComponent"];
}

+ (NSString *)getRootComponent
{
    return [configuration objectForKey:@"rootComponent"];
}

+ (NSDictionary *) getConfiguration
{
    return configuration;
}

@end