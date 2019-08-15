//
//  JWTCoding+ResultTypes.m
//  JWT
//
//  Created by Lobanov Dmitry on 30.11.16.
//  Copyright © 2016 JWTIO. All rights reserved.
//

#import "JWTCoding+ResultTypes.h"

NSString *JWTCodingResultHeaders = @"header";
NSString *JWTCodingResultPayload = @"payload";

@implementation JWT (ResultTypes) @end

// Protected?
@protocol JWTMutableCodingResultTypeSuccessEncodedProtocol <JWTCodingResultTypeSuccessEncodedProtocol>
@property (copy, nonatomic, readwrite) NSString *encoded;
@property (copy, nonatomic, readwrite) NSString *token;
@end

// Protected?
@protocol JWTMutableCodingResultTypeSuccessDecodedProtocol <JWTCodingResultTypeSuccessDecodedProtocol>
@property (copy, nonatomic, readwrite) NSDictionary *headers;
@property (copy, nonatomic, readwrite) NSDictionary *payload;
@property (nonatomic, readwrite) JWTClaimsSet *claimsSet;
@end

// Protected?
@protocol JWTMutableCodingResultTypeErrorProtocol <JWTCodingResultTypeErrorProtocol>
@property (copy, nonatomic, readwrite) NSError *error;
@end

@interface JWTCodingResultTypeSuccess () <JWTMutableCodingResultTypeSuccessEncodedProtocol, JWTMutableCodingResultTypeSuccessDecodedProtocol> @end

@implementation JWTCodingResultTypeSuccess
@synthesize encoded = _encoded;
@synthesize headers = _headers;
@synthesize payload = _payload;
@synthesize claimsSet = _claimsSet;
//Not used yet. Could be replacement for _encoded.
@synthesize token = _token;

- (NSDictionary *)headerAndPayloadDictionary {
    if (self.headers && self.payload) {
        return @{
                 JWTCodingResultHeaders: self.headers,
                 JWTCodingResultPayload: self.payload
        };
    }
    return nil;
}
- (instancetype)initWithEncoded:(NSString *)encoded {
    if (self = [super init]) {
        self.encoded = encoded;
    }
    return self;
}
- (instancetype)initWithToken:(NSString *)token {
    if (self = [super init]) {
        self.token = token;
    }
    return self;
}
- (instancetype)initWithHeaders:(NSDictionary *)headers withPayload:(NSDictionary *)payload {
    if (self = [super init]) {
        self.headers = headers;
        self.payload = payload;
    }
    return self;
}
- (instancetype)initWithClaimsSet:(JWTClaimsSet *)claimsSet {
    if (self = [super init]) {
        self.claimsSet = claimsSet;
    }
    return self;
}
@end

@interface JWTCodingResultTypeError () <JWTMutableCodingResultTypeErrorProtocol> @end

@implementation JWTCodingResultTypeError
@synthesize error = _error;
- (instancetype)initWithError:(NSError *)error {
    if (self = [super init]) {
        self.error = error;
    }
    return self;
}
@end

@interface JWTCodingResultType ()
@property (strong, nonatomic, readwrite) JWTCodingResultTypeSuccess *successResult;
@property (strong, nonatomic, readwrite) JWTCodingResultTypeError *errorResult;
@end

@implementation JWTCodingResultType
- (instancetype)initWithSuccessResult:(JWTCodingResultTypeSuccess *)success {
    if (self = [super init]) {
        self.successResult = success;
    }
    return self;
}
- (instancetype)initWithErrorResult:(JWTCodingResultTypeError *)error {
    if (self = [super init]) {
        self.errorResult = error;
    }
    return self;
}

@end
