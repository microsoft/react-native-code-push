#import "CodePush.h"

@interface NSDictionary (UrlEncoding)

-(NSString*) urlEncodedString;

@end

// helper function: get the string form of any object
static NSString *toString(id object) {
    return [NSString stringWithFormat: @"%@", object];
}

// helper function: get the url encoded string form of any object
static NSString *urlEncode(id object) {
    NSString *string = toString(object);
    return [string stringByAddingPercentEscapesUsingEncoding: NSUTF8StringEncoding];
}

@implementation NSDictionary (UrlEncoding)

-(NSString*) urlEncodedString {
    NSMutableArray *parts = [NSMutableArray array];
    for (id key in self) {
        id value = [self objectForKey: key];
        NSString *part = [NSString stringWithFormat: @"%@=%@", urlEncode(key), urlEncode(value)];
        [parts addObject: part];
    }
    return [parts componentsJoinedByString: @"&"];
}

@end




@implementation CodePushAquisitionSDKManager

- (instancetype) initWithConfig:(NSDictionary *)config
{
    self.serverURL = [config objectForKey:ServerURLConfigKey];
    self.appVersion = [config objectForKey:AppVersionConfigKey];
    self.deploymentKey = [config objectForKey:DeploymentKeyConfigKey];
    self.clientUniqueId = [config objectForKey:ClientUniqueIDConfigKey];
    self.ignoreAppVersion = [config objectForKey:IgonreAppVersionConfigKey] ? @"YES" : @"NO";

    return self;
}

//TODO: replace this two methods below with new methods using NSURLSession plus add ability to use callbacks. Perhaps replace with httpRequester class if needed
+ (NSData *)peformHTTPPostRequest:(NSString *)requestUrl
                   withBody:(NSData *)body
{

    NSMutableURLRequest *request = [[NSMutableURLRequest alloc] init];
    [request setHTTPMethod:@"POST"];
    [request setHTTPBody:body];
    [request setValue:[NSString stringWithFormat:@"%lu", (unsigned long)[body length]] forHTTPHeaderField:@"Content-Length"];
    [request setURL:[NSURL URLWithString:requestUrl]];

    NSError *error = nil;
    NSHTTPURLResponse *responseCode = nil;

    NSData *oResponseData = [NSURLConnection sendSynchronousRequest:request returningResponse:&responseCode error:&error];

    if (error){
        NSLog(@"An Error occured getting %@, error message: %@", requestUrl, [error localizedDescription]);
        return nil;
    }

    if([responseCode statusCode] != 200){
        NSLog(@"Error getting %@, HTTP status code %li", requestUrl, (long)[responseCode statusCode]);
        return nil;
    }

    return oResponseData;
}

+ (NSData *)peformHTTPGetRequest:(NSString *)requestUrl
{
    NSMutableURLRequest *request = [[NSMutableURLRequest alloc] init];
    [request setHTTPMethod:@"GET"];
    [request setURL:[NSURL URLWithString:requestUrl]];
    [request setCachePolicy:NSURLRequestReloadIgnoringCacheData];

    NSError *error = nil;
    NSHTTPURLResponse *responseCode = nil;

    NSData *oResponseData = [NSURLConnection sendSynchronousRequest:request returningResponse:&responseCode error:&error];

    if (error){
        NSLog(@"An Error occured getting %@, error message: %@", requestUrl, [error localizedDescription]);
        return nil;
    }

    if([responseCode statusCode] != 200){
        NSLog(@"Error getting %@, HTTP status code %li", requestUrl, (long)[responseCode statusCode]);
        return nil;
    }

    return oResponseData;
}

- (NSDictionary *)queryUpdateWithCurrentPackage:(NSDictionary *)currentPackage
{
    NSDictionary *response = nil;
    if (!currentPackage || ![currentPackage objectForKey:AppVersionKey]){
        CPLog(@"Unable to query update, no package provided or calling with incorrect package");
        return response;
    }

    NSDictionary *updateRequest = [[NSDictionary alloc] initWithObjectsAndKeys:
                                   [self deploymentKey ], DeploymentKeyConfigKey,
                                   [self appVersion], AppVersionKey,
                                   [currentPackage objectForKey:PackageHashKey],PackageHashKey,
                                   [self ignoreAppVersion], IsCompanionKey,
                                   [currentPackage objectForKey:LabelKey],LabelKey,
                                   [self clientUniqueId],ClientUniqueIDConfigKey,
                                   nil];

    NSString *urlEncodedString = [updateRequest urlEncodedString];
    NSString *requestUrl = [NSString stringWithFormat:@"%@%@%@", [self serverURL], @"updateCheck?", urlEncodedString];

    NSData *oResponseData = [[self class] peformHTTPGetRequest:requestUrl];
    if (oResponseData){
        NSError *error;
        NSDictionary *dictionaryResponse = [NSJSONSerialization JSONObjectWithData:oResponseData options:0 error:&error];

        if (error){
            CPLog(@"An error occured on deserializing data: @", [error localizedDescription]);
            return nil;
        }

        if (dictionaryResponse && [dictionaryResponse objectForKey:UpdateInfoKey]){
            response = [dictionaryResponse objectForKey:UpdateInfoKey];
        } else {
            return nil;
        }

        BOOL updateAppVersion = [[response objectForKey:UpdateAppVersionKey]boolValue];
        if (updateAppVersion){
            return @{ UpdateAppVersionKey: @(YES),
                      AppVersionKey:[response objectForKey:AppVersionKey]
                      };
        }

        BOOL isAvaliable = [[response objectForKey:IsAvailableKey]boolValue];
        if (isAvaliable == NO){
            return nil;
        }

        NSDictionary *remotePackage = [[NSDictionary alloc] initWithObjectsAndKeys:
                                       [self deploymentKey ],DeploymentKeyConfigKey,
                                       [response objectForKey:DescriptionKey],DescriptionKey,
                                       [response objectForKey:LabelKey],LabelKey,
                                       [response objectForKey:AppVersionKey],AppVersionKey,
                                       [response objectForKey:LabelKey],LabelKey,
                                       [response objectForKey:IsMandatoryKey],IsMandatoryKey,
                                       [response objectForKey:PackageHashKey],PackageHashKey,
                                       [response objectForKey:PackageSizeKey],PackageSizeKey,
                                       [response objectForKey:DownloadUrlKey],DownloadUrRemotePackageKey,
                                       nil];

        return remotePackage;
    } else {
        return nil;
    }
}


- (void)reportStatusDeploy:(NSDictionary *)package
                          withStatus:(NSString *)status
           previousLabelOrAppVersion:(NSString *)prevLabelOrAppVersion
               previousDeploymentKey:(NSString *)prevDeploymentKey
{
    NSString *requestUrl = [NSString stringWithFormat:@"%@%@", [self serverURL], @"reportStatus/deploy"];

    NSMutableDictionary *body = [[NSMutableDictionary alloc] initWithObjectsAndKeys:
                          [self appVersion], AppVersionKey,
                          [self deploymentKey ], DeploymentKeyConfigKey,
                          nil];

    if ([self clientUniqueId]){
        [body setValue:[self clientUniqueId] forKey:ClientUniqueIDConfigKey];
    }

    if (package){
        [body setValue:[package objectForKey:LabelKey] forKey:LabelKey];
        [body setValue:[package objectForKey:AppVersionKey] forKey:AppVersionKey];

        if ([status isEqualToString:DeploymentSucceeded] || [status isEqualToString:DeploymentFailed]){
            [body setValue:status forKey:StatusKey];
        }


    }
    if (prevLabelOrAppVersion){
        [body setValue:prevLabelOrAppVersion forKey:PreviousLabelOrAppVersionKey];
    }
    if (prevDeploymentKey){
        [body setValue:prevDeploymentKey forKey:PreviousDeploymentKey];
    }
    NSError *error;
    NSData *postData = [NSJSONSerialization dataWithJSONObject:body options:0 error:&error];

    if (error){
        CPLog(@"An error occured on JSON data serialization: @", [error localizedDescription]);
        return;
    }

    [[self class] peformHTTPPostRequest:requestUrl withBody:postData];
    return;
}

- (void)reportStatusDownload:(NSDictionary *)downloadedPackage
{
    NSString *requestUrl = [NSString stringWithFormat:@"%@%@", [self serverURL], @"reportStatus/download"];
    NSDictionary *body = [[NSDictionary alloc] initWithObjectsAndKeys:
                          [self clientUniqueId],ClientUniqueIDConfigKey,
                          [self deploymentKey ], DeploymentKeyConfigKey,
                          [downloadedPackage objectForKey:LabelKey],LabelKey,
                          nil];
    NSError *error;
    NSData *postData = [NSJSONSerialization dataWithJSONObject:body options:0 error:&error];

    if (error){
        CPLog(@"An error occured on JSON data serialization: @", [error localizedDescription]);
        return;
    }

    [[self class] peformHTTPPostRequest:requestUrl withBody:postData];
    return;
}

@end
