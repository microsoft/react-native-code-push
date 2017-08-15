//
//  JWT.h
//  JWT
//
//  Created by Klaas Pieter Annema on 31-05-13.
//  Copyright (c) 2013 Karma. All rights reserved.
//

#import <Foundation/Foundation.h>

#import "JWTAlgorithm.h"
#import "JWTClaimsSet.h"
#import "JWTAlgorithmDataHolder.h"

typedef NS_ENUM(NSInteger, JWTError) {
    JWTInvalidFormatError = -100,
    JWTUnsupportedAlgorithmError,
    JWTAlgorithmNameMismatchError,
    JWTInvalidSignatureError,
    JWTNoPayloadError,
    JWTNoHeaderError,
    JWTEncodingHeaderError,
    JWTEncodingPayloadError,
    JWTEncodingSigningError,
    JWTClaimsSetVerificationFailed,
    JWTInvalidSegmentSerializationError,
    JWTUnspecifiedAlgorithmError,
    JWTBlacklistedAlgorithmError,
    JWTDecodingHeaderError,
    JWTDecodingPayloadError,
};

@class JWTBuilder;
/**
 @discussion JWT is a general interface for decoding and encoding.
 Now it is to complex and fat to support.
 Possible solution: split interface into several pieces.
 
 JWT_1_0 -> JWT with plain old functions.
 JWT_2_0 -> JWT with builder usage.
 JWT_3_0 -> JWT with splitted apart algorithm data and payload data.
 */
@interface JWT : NSObject

#pragma mark - Encode
+ (NSString *)encodeClaimsSet:(JWTClaimsSet *)theClaimsSet withSecret:(NSString *)theSecret __deprecated_and_will_be_removed_in_release_version(JWTVersion_3_0_0);
+ (NSString *)encodeClaimsSet:(JWTClaimsSet *)theClaimsSet withSecret:(NSString *)theSecret algorithm:(id<JWTAlgorithm>)theAlgorithm __deprecated_and_will_be_removed_in_release_version(JWTVersion_3_0_0);

+ (NSString *)encodePayload:(NSDictionary *)thePayload withSecret:(NSString *)theSecret __deprecated_and_will_be_removed_in_release_version(JWTVersion_3_0_0);
+ (NSString *)encodePayload:(NSDictionary *)thePayload withSecret:(NSString *)theSecret algorithm:(id<JWTAlgorithm>)theAlgorithm __deprecated_and_will_be_removed_in_release_version(JWTVersion_3_0_0);

+ (NSString *)encodePayload:(NSDictionary *)thePayload withSecret:(NSString *)theSecret withHeaders:(NSDictionary *)theHeaders algorithm:(id<JWTAlgorithm>)theAlgorithm __deprecated_and_will_be_removed_in_release_version(JWTVersion_3_0_0);

+ (NSString *)encodePayload:(NSDictionary *)thePayload withSecret:(NSString *)theSecret withHeaders:(NSDictionary *)theHeaders algorithm:(id<JWTAlgorithm>)theAlgorithm withError:(NSError * __autoreleasing *)theError __deprecated_and_will_be_removed_in_release_version(JWTVersion_3_0_0);

//Will be deprecated in later releases
#pragma mark - Decode

/**
 Decodes a JWT and returns the decoded Header and Payload
 @param theMessage The encoded JWT
 @param theSecret The verification key to use for validating the JWT signature
 @param theTrustedClaimsSet The JWTClaimsSet to use for verifying the JWT values
 @param theError Error pointer, if there is an error decoding the message, upon return contains an NSError object that describes the problem.
 @param theAlgorithmName The name of the algorithm to use for verifying the signature. Required, unless skipping verification
 @param theForcedOption BOOL indicating if verifying the JWT signature should be skipped. Should only be used for debugging
 @return A dictionary containing the header and payload dictionaries. Keyed to "header" and "payload", respectively. Or nil if an error occurs.
 */
+ (NSDictionary *)decodeMessage:(NSString *)theMessage withSecret:(NSString *)theSecret withTrustedClaimsSet:(JWTClaimsSet *)theTrustedClaimsSet withError:(NSError *__autoreleasing *)theError withForcedAlgorithmByName:(NSString *)theAlgorithmName withForcedOption:(BOOL)theForcedOption __deprecated_and_will_be_removed_in_release_version(JWTVersion_3_0_0);

/**
 Decodes a JWT and returns the decoded Header and Payload
 @param theMessage The encoded JWT
 @param theSecret The verification key to use for validating the JWT signature
 @param theTrustedClaimsSet The JWTClaimsSet to use for verifying the JWT values
 @param theError Error pointer, if there is an error decoding the message, upon return contains an NSError object that describes the problem.
 @param theAlgorithmName The name of the algorithm to use for verifying the signature. Required.
 @return A dictionary containing the header and payload dictionaries. Keyed to "header" and "payload", respectively. Or nil if an error occurs.
 */
+ (NSDictionary *)decodeMessage:(NSString *)theMessage withSecret:(NSString *)theSecret withTrustedClaimsSet:(JWTClaimsSet *)theTrustedClaimsSet withError:(NSError *__autoreleasing *)theError withForcedAlgorithmByName:(NSString *)theAlgorithmName __deprecated_and_will_be_removed_in_release_version(JWTVersion_3_0_0);

/**
 Decodes a JWT and returns the decoded Header and Payload.

 Uses the JWTAlgorithmHS512 for decoding

 @param theMessage The encoded JWT
 @param theSecret The verification key to use for validating the JWT signature
 @param theTrustedClaimsSet The JWTClaimsSet to use for verifying the JWT values
 @param theError Error pointer, if there is an error decoding the message, upon return contains an NSError object that describes the problem.
 @param theForcedOption BOOL indicating if verifying the JWT signature should be skipped. Should only be used for debugging
 @return A dictionary containing the header and payload dictionaries. Keyed to "header" and "payload", respectively. Or nil if an error occurs.
 */
+ (NSDictionary *)decodeMessage:(NSString *)theMessage withSecret:(NSString *)theSecret withTrustedClaimsSet:(JWTClaimsSet *)theTrustedClaimsSet withError:(NSError *__autoreleasing *)theError withForcedOption:(BOOL)theForcedOption __deprecated_and_will_be_removed_in_release_version(JWTVersion_3_0_0);

/**
 Decodes a JWT and returns the decoded Header and Payload
 @param theMessage The encoded JWT
 @param theSecret The verification key to use for validating the JWT signature
 @param theError Error pointer, if there is an error decoding the message, upon return contains an NSError object that describes the problem.
 @param theAlgorithmName The name of the algorithm to use for verifying the signature. Required, unless skipping verification
 @param skipVerification BOOL indicating if verifying the JWT signature should be skipped. Should only be used for debugging
 @return A dictionary containing the header and payload dictionaries. Keyed to "header" and "payload", respectively. Or nil if an error occurs.
 */
+ (NSDictionary *)decodeMessage:(NSString *)theMessage withSecret:(NSString *)theSecret withError:(NSError *__autoreleasing *)theError withForcedAlgorithmByName:(NSString *)theAlgorithmName skipVerification:(BOOL)skipVerification __deprecated_and_will_be_removed_in_release_version(JWTVersion_3_0_0);

/**
 Decodes a JWT and returns the decoded Header and Payload
 @param theMessage The encoded JWT
 @param theSecret The verification key to use for validating the JWT signature
 @param theError Error pointer, if there is an error decoding the message, upon return contains an NSError object that describes the problem.
 @param theAlgorithmName The name of the algorithm to use for verifying the signature. Required.
 @return A dictionary containing the header and payload dictionaries. Keyed to "header" and "payload", respectively. Or nil if an error occurs.
 */
+ (NSDictionary *)decodeMessage:(NSString *)theMessage withSecret:(NSString *)theSecret withError:(NSError *__autoreleasing *)theError withForcedAlgorithmByName:(NSString *)theAlgorithmName __deprecated_and_will_be_removed_in_release_version(JWTVersion_3_0_0);

/**
 Decodes a JWT and returns the decoded Header and Payload

 Uses the JWTAlgorithmHS512 for decoding

 @param theMessage The encoded JWT
 @param theSecret The verification key to use for validating the JWT signature
 @param theError Error pointer, if there is an error decoding the message, upon return contains an NSError object that describes the problem.
 @param theForcedOption BOOL indicating if verifying the JWT signature should be skipped.
 @return A dictionary containing the header and payload dictionaries. Keyed to "header" and "payload", respectively. Or nil if an error occurs.
 */
+ (NSDictionary *)decodeMessage:(NSString *)theMessage withSecret:(NSString *)theSecret withError:(NSError *__autoreleasing *)theError withForcedOption:(BOOL)theForcedOption __deprecated_and_will_be_removed_in_release_version(JWTVersion_3_0_0);

/**
 Decodes a JWT and returns the decoded Header and Payload.
 Uses the JWTAlgorithmHS512 for decoding
 @param theMessage The encoded JWT
 @param theSecret The verification key to use for validating the JWT signature
 @param theError Error pointer, if there is an error decoding the message, upon return contains an NSError object that describes the problem.
 @return A dictionary containing the header and payload dictionaries. Keyed to "header" and "payload", respectively. Or nil if an error occurs.
 */
+ (NSDictionary *)decodeMessage:(NSString *)theMessage withSecret:(NSString *)theSecret withError:(NSError * __autoreleasing *)theError __deprecated_and_will_be_removed_in_release_version(JWTVersion_3_0_0);

/**
 Decodes a JWT and returns the decoded Header and Payload.
 Uses the JWTAlgorithmHS512 for decoding
 @param theMessage The encoded JWT
 @param theSecret The verification key to use for validating the JWT signature
 @return A dictionary containing the header and payload dictionaries. Keyed to "header" and "payload", respectively. Or nil if an error occurs.
 */
+ (NSDictionary *)decodeMessage:(NSString *)theMessage withSecret:(NSString *)theSecret __deprecated_and_will_be_removed_in_release_version(JWTVersion_3_0_0);

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
@property (copy, nonatomic, readonly) NSArray *jwtDataHolders __available_in_release_version(JWTVersion_3_0_0);

/**
 The verification key to use when encoding/decoding a JWT
 */
@property (copy, nonatomic, readonly) NSString *jwtSecret __first_deprecated_in_release_version(JWTVersion_2_2_0);

/**
 The verification key to use when encoding/decoding a JWT in data form
 */
@property (copy, nonatomic, readonly) NSData *jwtSecretData __first_deprecated_in_release_version(JWTVersion_2_2_0);

/**
 The passphrase for the PKCS12 blob, which represents the certificate containing the private key for the RS algorithms.
 */
@property (copy, nonatomic, readonly) NSString *jwtPrivateKeyCertificatePassphrase __first_deprecated_in_release_version(JWTVersion_2_2_0);

/**
 Contains the error that occured during an operation, or nil if no error occured
 */
@property (copy, nonatomic, readonly) NSError *jwtError;

/**
 The <JWTAlgorithm> to use for encoding a JWT
 */
@property (strong, nonatomic, readonly) id<JWTAlgorithm> jwtAlgorithm __first_deprecated_in_release_version(JWTVersion_2_2_0);

/**
 The algorithm name to use for decoding the JWT. Required unless force decode is true
 */
@property (copy, nonatomic, readonly) NSString *jwtAlgorithmName __first_deprecated_in_release_version(JWTVersion_2_2_0);

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
@property (copy, nonatomic, readonly) JWTBuilder *(^secret)(NSString *secret)__first_deprecated_in_release_version(JWTVersion_2_2_0);

/**
 Sets jwtSecretData and returns the JWTBuilder to allow for method chaining
 */
@property (copy, nonatomic, readonly) JWTBuilder *(^secretData)(NSData *secretData)__first_deprecated_in_release_version(JWTVersion_2_2_0);

/**
 Sets jwtPrivateKeyCertificatePassphrase and returns the JWTBuilder to allow for method chaining
 */
@property (copy, nonatomic, readonly) JWTBuilder *(^privateKeyCertificatePassphrase)(NSString *privateKeyCertificatePassphrase) __first_deprecated_in_release_version(JWTVersion_2_2_0);

/**
 Sets jwtAlgorithm and returns the JWTBuilder to allow for method chaining
 */
@property (copy, nonatomic, readonly) JWTBuilder *(^algorithm)(id<JWTAlgorithm>algorithm)__first_deprecated_in_release_version(JWTVersion_2_2_0);

/**
 Sets jwtAlgorithmName and returns the JWTBuilder to allow for method chaining. See list of names in appropriate headers.
 */
@property (copy, nonatomic, readonly) JWTBuilder *(^algorithmName)(NSString *algorithmName)__first_deprecated_in_release_version(JWTVersion_2_2_0);

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

@property (copy, nonatomic, readonly) JWTBuilder * (^addDataHolder)(JWTAlgorithmBaseDataHolder *dataHolder) __available_in_release_version(JWTVersion_3_0_0);
@property (copy, nonatomic, readonly) JWTBuilder * (^constructDataHolder)(id<JWTAlgorithmDataHolder> (^block)()) __available_in_release_version(JWTVersion_3_0_0);
@end
