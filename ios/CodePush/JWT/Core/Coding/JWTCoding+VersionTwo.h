//
//  JWTCoding+VersionTwo.h
//  JWT
//
//  Created by Lobanov Dmitry on 27.11.16.
//  Copyright © 2016 JWTIO. All rights reserved.
//

#import <JWT/JWTCoding.h>

@protocol JWTAlgorithm;
@class JWTClaimsSet;
@class JWTBuilder;

@interface JWT (VersionTwo)
#pragma mark - Builder
+ (JWTBuilder *)encodePayload:(NSDictionary *)payload;
+ (JWTBuilder *)encodeClaimsSet:(JWTClaimsSet *)claimsSet;
+ (JWTBuilder *)decodeMessage:(NSString *)message;
@end

@interface JWTBuilder : NSObject

+ (JWTBuilder *)encodePayload:(NSDictionary *)payload;
+ (JWTBuilder *)encodeClaimsSet:(JWTClaimsSet *)claimsSet;
+ (JWTBuilder *)decodeMessage:(NSString *)message;

/**
 The JWT in it's encoded form. Will be decoded and verified
 */
@property (copy, nonatomic, readonly) NSString *jwtMessage;

/**
 The payload dictionary to encode
 */
@property (copy, nonatomic, readonly) NSDictionary *jwtPayload;

/**
 The header dictionary to encode
 */
@property (copy, nonatomic, readonly) NSDictionary *jwtHeaders;

/**
 The expected JWTClaimsSet to compare against a decoded JWT
 */
@property (copy, nonatomic, readonly) JWTClaimsSet *jwtClaimsSet;

/**
 The algorithm data holders. They contain necessary information about algorithms.
 */
@property (copy, nonatomic, readonly) NSArray *jwtDataHolders;

/**
 The verification key to use when encoding/decoding a JWT
 */
@property (copy, nonatomic, readonly) NSString *jwtSecret;

/**
 The verification key to use when encoding/decoding a JWT in data form
 */
@property (copy, nonatomic, readonly) NSData *jwtSecretData;

/**
 The passphrase for the PKCS12 blob, which represents the certificate containing the private key for the RS algorithms.
 */
@property (copy, nonatomic, readonly) NSString *jwtPrivateKeyCertificatePassphrase;

/**
 Contains the error that occured during an operation, or nil if no error occured
 */
@property (copy, nonatomic, readonly) NSError *jwtError;

/**
 The <JWTAlgorithm> to use for encoding a JWT
 */
@property (strong, nonatomic, readonly) id<JWTAlgorithm> jwtAlgorithm;

/**
 The algorithm name to use for decoding the JWT. Required unless force decode is true
 */
@property (copy, nonatomic, readonly) NSString *jwtAlgorithmName;

/**
 The force decode option. If set to true, a JWT won't be validated before decoding.
 Should only be used for debugging
 */
@property (copy, nonatomic, readonly) NSNumber *jwtOptions;

/*
 Optional algorithm name whitelist. If non-null, a JWT can only be decoded using an algorithm
 specified on this list.
 */
@property (copy, nonatomic, readonly) NSSet *algorithmWhitelist;

/**
 Sets jwtMessage and returns the JWTBuilder to allow for method chaining
 */
@property (copy, nonatomic, readonly) JWTBuilder *(^message)(NSString *message);

/**
 Sets jwtPayload and returns the JWTBuilder to allow for method chaining
 */
@property (copy, nonatomic, readonly) JWTBuilder *(^payload)(NSDictionary *payload);

/**
 Sets jwtHeaders and returns the JWTBuilder to allow for method chaining
 */
@property (copy, nonatomic, readonly) JWTBuilder *(^headers)(NSDictionary *headers);

/**
 Sets jwtClaimsSet and returns the JWTBuilder to allow for method chaining
 */
@property (copy, nonatomic, readonly) JWTBuilder *(^claimsSet)(JWTClaimsSet *claimsSet);

/**
 Sets jwtSecret and returns the JWTBuilder to allow for method chaining
 */
@property (copy, nonatomic, readonly) JWTBuilder *(^secret)(NSString *secret);

/**
 Sets jwtSecretData and returns the JWTBuilder to allow for method chaining
 */
@property (copy, nonatomic, readonly) JWTBuilder *(^secretData)(NSData *secretData);

/**
 Sets jwtPrivateKeyCertificatePassphrase and returns the JWTBuilder to allow for method chaining
 */
@property (copy, nonatomic, readonly) JWTBuilder *(^privateKeyCertificatePassphrase)(NSString *privateKeyCertificatePassphrase);

/**
 Sets jwtAlgorithm and returns the JWTBuilder to allow for method chaining
 */
@property (copy, nonatomic, readonly) JWTBuilder *(^algorithm)(id<JWTAlgorithm>algorithm);

/**
 Sets jwtAlgorithmName and returns the JWTBuilder to allow for method chaining. See list of names in appropriate headers.
 */
@property (copy, nonatomic, readonly) JWTBuilder *(^algorithmName)(NSString *algorithmName);

/**
 Sets jwtOptions and returns the JWTBuilder to allow for method chaining
 */
@property (copy, nonatomic, readonly) JWTBuilder *(^options)(NSNumber *options);

/**
 Sets algorithmWhitelist and returns the JWTBuilder to allow for method chaining
 */
@property (copy, nonatomic, readonly) JWTBuilder *(^whitelist)(NSArray *whitelist);

/**
 Creates the encoded JWT string based on the currently set properties, or nil if an
 error occured
 */
@property (copy, nonatomic, readonly) NSString *encode;

/**
 Decodes and returns the JWT as a dictionary, based on the JWTBuilder's currently set
 properties, or nil, if an error occured.
 */
@property (copy, nonatomic, readonly) NSDictionary *decode;

//@property (copy, nonatomic, readonly) JWTBuilder * (^addDataHolder)(JWTAlgorithmBaseDataHolder *dataHolder) __available_in_release_version(JWTVersion_3_0_0);
//@property (copy, nonatomic, readonly) JWTBuilder * (^constructDataHolder)(id<JWTAlgorithmDataHolderProtocol> (^block)()) __available_in_release_version(JWTVersion_3_0_0);
@end
