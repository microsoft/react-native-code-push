#import "CodePush.h"

NSMutableDictionary *configuration;

@implementation CodePushConfig

+ (void)initialize
{
    NSDictionary *infoDictionary = [[NSBundle mainBundle] infoDictionary];
    
    NSString *appVersion = [infoDictionary objectForKey:@"CFBundleShortVersionString"];
    NSString *buildVersion = [infoDictionary objectForKey:(NSString *)kCFBundleVersionKey];
    NSString *deploymentKey = [infoDictionary objectForKey:@"CodePushDeploymentKey"];
    NSString *serverUrl = [infoDictionary objectForKey:@"CodePushServerUrl"];
    if (!serverUrl) {
        serverUrl = @"https://codepush.azurewebsites.net/";
    }
    NSString *rootComponent = [infoDictionary objectForKey:@"CFBundleName"];
    
    configuration = [[NSMutableDictionary alloc]
                                   initWithObjectsAndKeys:
                                   appVersion,@"appVersion",
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

+ (void)setAppVersion:(NSString *)appVersion
{
    [configuration setValue:appVersion forKey:@"appVersion"];
}

+ (NSString *)getAppVersion
{
    return [configuration objectForKey:@"appVersion"];
}

+ (void)setBuildVersion:(NSString *)buildVersion
{
    [configuration setValue:buildVersion forKey:@"buildVersion"];
}

+ (NSString *)getBuildVersion
{
    return [configuration objectForKey:@"buildVersion"];
}

+ (void)setRootComponent:(NSString *)rootComponent
{
    [configuration setValue:rootComponent forKey:@"rootComponent"];
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
