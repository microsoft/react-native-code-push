//
//  JWTCoding+ResultTypes.h
//  JWT
//
//  Created by Lobanov Dmitry on 30.11.16.
//  Copyright © 2016 JWTIO. All rights reserved.
//

#import <JWTCoding.h>
@class JWTClaimsSet;

extern NSString *JWTCodingResultHeaders;
extern NSString *JWTCodingResultPayload;

@interface JWT (ResultTypes) @end

/*
            ResultType
                /\
               /  \
              /    \
          Success  Error
 
    Protocols: Mutable and Immutable (?!?)
 */

// Public
@protocol JWTCodingResultTypeSuccessEncodedProtocol <NSObject>
@property (copy, nonatomic, readonly) NSString *encoded;
- (instancetype)initWithEncoded:(NSString *)encoded;
@property (copy, nonatomic, readonly) NSString *token;
- (instancetype)initWithToken:(NSString *)token;
@end

// Public
@protocol JWTCodingResultTypeSuccessDecodedProtocol <NSObject>
@property (copy, nonatomic, readonly) NSDictionary *headers;
@property (copy, nonatomic, readonly) NSDictionary *payload;

// dictionary @{
//  JWTCodingResultHeaders : self.headers,
//  JWTCodingResultPayload : self.payload
//}
@property (copy, nonatomic, readonly) NSDictionary *headerAndPayloadDictionary;

@property (nonatomic, readonly) JWTClaimsSet *claimsSet;
- (instancetype)initWithHeaders:(NSDictionary *)headers withPayload:(NSDictionary *)payload;
- (instancetype)initWithClaimsSet:(JWTClaimsSet *)claimsSet;
@end

// Public
@interface JWTCodingResultTypeSuccess : NSObject <JWTCodingResultTypeSuccessEncodedProtocol,JWTCodingResultTypeSuccessDecodedProtocol> @end

// Public
@protocol JWTCodingResultTypeErrorProtocol <NSObject>
@property (copy, nonatomic, readonly) NSError *error;
- (instancetype)initWithError:(NSError *)error;
@end

@interface JWTCodingResultTypeError : NSObject <JWTCodingResultTypeErrorProtocol> @end

@interface JWTCodingResultType : NSObject
- (instancetype)initWithSuccessResult:(JWTCodingResultTypeSuccess *)success;
- (instancetype)initWithErrorResult:(JWTCodingResultTypeError *)error;
@property (strong, nonatomic, readonly) JWTCodingResultTypeSuccess *successResult;
@property (strong, nonatomic, readonly) JWTCodingResultTypeError *errorResult;
@end
