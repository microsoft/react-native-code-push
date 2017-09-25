//
//  JWTCoding+VersionOne.h
//  JWT
//
//  Created by Lobanov Dmitry on 27.11.16.
//  Copyright © 2016 JWTIO. All rights reserved.
//

#import <JWT/JWTCoding.h>

@protocol JWTAlgorithm;
@class JWTClaimsSet;

@interface JWT (VersionOne)
#pragma mark - Encode
+ (NSString *)encodeClaimsSet:(JWTClaimsSet *)theClaimsSet withSecret:(NSString *)theSecret;
+ (NSString *)encodeClaimsSet:(JWTClaimsSet *)theClaimsSet withSecret:(NSString *)theSecret algorithm:(id<JWTAlgorithm>)theAlgorithm;

+ (NSString *)encodePayload:(NSDictionary *)thePayload withSecret:(NSString *)theSecret;
+ (NSString *)encodePayload:(NSDictionary *)thePayload withSecret:(NSString *)theSecret algorithm:(id<JWTAlgorithm>)theAlgorithm;

+ (NSString *)encodePayload:(NSDictionary *)thePayload withSecret:(NSString *)theSecret withHeaders:(NSDictionary *)theHeaders algorithm:(id<JWTAlgorithm>)theAlgorithm;

+ (NSString *)encodePayload:(NSDictionary *)thePayload withSecret:(NSString *)theSecret withHeaders:(NSDictionary *)theHeaders algorithm:(id<JWTAlgorithm>)theAlgorithm withError:(NSError * __autoreleasing *)theError;

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
+ (NSDictionary *)decodeMessage:(NSString *)theMessage withSecret:(NSString *)theSecret withTrustedClaimsSet:(JWTClaimsSet *)theTrustedClaimsSet withError:(NSError *__autoreleasing *)theError withForcedAlgorithmByName:(NSString *)theAlgorithmName withForcedOption:(BOOL)theForcedOption;

/**
 Decodes a JWT and returns the decoded Header and Payload
 @param theMessage The encoded JWT
 @param theSecret The verification key to use for validating the JWT signature
 @param theTrustedClaimsSet The JWTClaimsSet to use for verifying the JWT values
 @param theError Error pointer, if there is an error decoding the message, upon return contains an NSError object that describes the problem.
 @param theAlgorithmName The name of the algorithm to use for verifying the signature. Required.
 @return A dictionary containing the header and payload dictionaries. Keyed to "header" and "payload", respectively. Or nil if an error occurs.
 */
+ (NSDictionary *)decodeMessage:(NSString *)theMessage withSecret:(NSString *)theSecret withTrustedClaimsSet:(JWTClaimsSet *)theTrustedClaimsSet withError:(NSError *__autoreleasing *)theError withForcedAlgorithmByName:(NSString *)theAlgorithmName;

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
+ (NSDictionary *)decodeMessage:(NSString *)theMessage withSecret:(NSString *)theSecret withTrustedClaimsSet:(JWTClaimsSet *)theTrustedClaimsSet withError:(NSError *__autoreleasing *)theError withForcedOption:(BOOL)theForcedOption;

/**
 Decodes a JWT and returns the decoded Header and Payload
 @param theMessage The encoded JWT
 @param theSecret The verification key to use for validating the JWT signature
 @param theError Error pointer, if there is an error decoding the message, upon return contains an NSError object that describes the problem.
 @param theAlgorithmName The name of the algorithm to use for verifying the signature. Required, unless skipping verification
 @param skipVerification BOOL indicating if verifying the JWT signature should be skipped. Should only be used for debugging
 @return A dictionary containing the header and payload dictionaries. Keyed to "header" and "payload", respectively. Or nil if an error occurs.
 */
+ (NSDictionary *)decodeMessage:(NSString *)theMessage withSecret:(NSString *)theSecret withError:(NSError *__autoreleasing *)theError withForcedAlgorithmByName:(NSString *)theAlgorithmName skipVerification:(BOOL)skipVerification;

/**
 Decodes a JWT and returns the decoded Header and Payload
 @param theMessage The encoded JWT
 @param theSecret The verification key to use for validating the JWT signature
 @param theError Error pointer, if there is an error decoding the message, upon return contains an NSError object that describes the problem.
 @param theAlgorithmName The name of the algorithm to use for verifying the signature. Required.
 @return A dictionary containing the header and payload dictionaries. Keyed to "header" and "payload", respectively. Or nil if an error occurs.
 */
+ (NSDictionary *)decodeMessage:(NSString *)theMessage withSecret:(NSString *)theSecret withError:(NSError *__autoreleasing *)theError withForcedAlgorithmByName:(NSString *)theAlgorithmName;

/**
 Decodes a JWT and returns the decoded Header and Payload

 Uses the JWTAlgorithmHS512 for decoding

 @param theMessage The encoded JWT
 @param theSecret The verification key to use for validating the JWT signature
 @param theError Error pointer, if there is an error decoding the message, upon return contains an NSError object that describes the problem.
 @param theForcedOption BOOL indicating if verifying the JWT signature should be skipped.
 @return A dictionary containing the header and payload dictionaries. Keyed to "header" and "payload", respectively. Or nil if an error occurs.
 */
+ (NSDictionary *)decodeMessage:(NSString *)theMessage withSecret:(NSString *)theSecret withError:(NSError *__autoreleasing *)theError withForcedOption:(BOOL)theForcedOption;

/**
 Decodes a JWT and returns the decoded Header and Payload.
 Uses the JWTAlgorithmHS512 for decoding
 @param theMessage The encoded JWT
 @param theSecret The verification key to use for validating the JWT signature
 @param theError Error pointer, if there is an error decoding the message, upon return contains an NSError object that describes the problem.
 @return A dictionary containing the header and payload dictionaries. Keyed to "header" and "payload", respectively. Or nil if an error occurs.
 */
+ (NSDictionary *)decodeMessage:(NSString *)theMessage withSecret:(NSString *)theSecret withError:(NSError * __autoreleasing *)theError;

/**
 Decodes a JWT and returns the decoded Header and Payload.
 Uses the JWTAlgorithmHS512 for decoding
 @param theMessage The encoded JWT
 @param theSecret The verification key to use for validating the JWT signature
 @return A dictionary containing the header and payload dictionaries. Keyed to "header" and "payload", respectively. Or nil if an error occurs.
 */
+ (NSDictionary *)decodeMessage:(NSString *)theMessage withSecret:(NSString *)theSecret;

@end
