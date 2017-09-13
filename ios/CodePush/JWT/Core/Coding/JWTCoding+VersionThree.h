//
//  JWTCoding+VersionThree.h
//  JWT
//
//  Created by Lobanov Dmitry on 27.11.16.
//  Copyright © 2016 JWTIO. All rights reserved.
//

#import <JWTCoding.h>

// encode and decode options
@protocol JWTAlgorithm;
@class JWTClaimsSet;
@class JWTCodingBuilder;
@class JWTEncodingBuilder;
@class JWTDecodingBuilder;
@class JWTAlgorithmDataHolderChain;
@protocol JWTAlgorithmDataHolderProtocol;
@class JWTCodingResultType;

@interface JWT (VersionThree)
+ (JWTEncodingBuilder *)encodeWithHolders:(NSArray *)holders;
+ (JWTEncodingBuilder *)encodeWithChain:(JWTAlgorithmDataHolderChain *)chain;
+ (JWTDecodingBuilder *)decodeWithHolders:(NSArray *)holders;
+ (JWTDecodingBuilder *)decodeWithChain:(JWTAlgorithmDataHolderChain *)chain;
@end

@interface JWTCodingBuilder : NSObject
#pragma mark - Create
// each element should conform to JWTAlgorithmDataHolderProtocol
+ (instancetype)createWithHolders:(NSArray *)holders;
+ (instancetype)createWithChain:(JWTAlgorithmDataHolderChain *)chain;
+ (instancetype)createWithEmptyChain;

#pragma mark - Internal
@property (nonatomic, readonly) JWTAlgorithmDataHolderChain *internalChain;
@property (copy, nonatomic, readonly) NSNumber *internalOptions;

#pragma mark - Fluent
@property (copy, nonatomic, readonly) JWTCodingBuilder *(^chain)(JWTAlgorithmDataHolderChain *chain);
@property (copy, nonatomic, readonly) JWTCodingBuilder *(^constructChain)(JWTAlgorithmDataHolderChain *(^block)());
@property (copy, nonatomic, readonly) JWTCodingBuilder *(^modifyChain)(JWTAlgorithmDataHolderChain *(^block)(JWTAlgorithmDataHolderChain * chain));
@property (copy, nonatomic, readonly) JWTCodingBuilder *(^options)(NSNumber *options);
@property (copy, nonatomic, readonly) JWTCodingBuilder *(^addHolder)(id<JWTAlgorithmDataHolderProtocol> holder);
//@property (copy, nonatomic, readonly) JWTCodingBuilder *(^constructHolder)(id<JWTAlgorithmDataHolderProtocol>(^block)(id<JWTAlgorithmDataHolderProtocol> holder));
@end

@interface JWTCodingBuilder (Sugar)
- (instancetype)and;
- (instancetype)with;
@end

@interface JWTCodingBuilder (Coding)
@property (nonatomic, readonly) JWTCodingResultType *result;
@end

@interface JWTEncodingBuilder : JWTCodingBuilder
#pragma mark - Create
+ (instancetype)encodePayload:(NSDictionary *)payload;
+ (instancetype)encodeClaimsSet:(JWTClaimsSet *)claimsSet;

#pragma mark - Internal
@property (copy, nonatomic, readonly) NSDictionary *internalPayload;
@property (copy, nonatomic, readonly) NSDictionary *internalHeaders;
@property (nonatomic, readonly) JWTClaimsSet *internalClaimsSet;

#pragma mark - Fluent
@property (copy, nonatomic, readonly) JWTEncodingBuilder *(^payload)(NSDictionary *payload);
@property (copy, nonatomic, readonly) JWTEncodingBuilder *(^headers)(NSDictionary *headers);
@property (copy, nonatomic, readonly) JWTEncodingBuilder *(^claimsSet)(JWTClaimsSet *claimsSet);

@end

@interface JWTEncodingBuilder (Coding)
@property (nonatomic, readonly) JWTCodingResultType *encode;
@end

@interface JWTDecodingBuilder : JWTCodingBuilder
#pragma mark - Create
+ (instancetype)decodeMessage:(NSString *)message;

#pragma mark - Internal
@property (copy, nonatomic, readonly) NSString *internalMessage;
@property (nonatomic, readonly) JWTClaimsSet *internalClaimsSet;

#pragma mark - Fluent
@property (copy, nonatomic, readonly) JWTDecodingBuilder *(^message)(NSString *message);
@property (copy, nonatomic, readonly) JWTDecodingBuilder *(^claimsSet)(JWTClaimsSet *claimsSet);

@end

@interface JWTDecodingBuilder (Coding)
@property (nonatomic, readonly) JWTCodingResultType *decode;
@end
