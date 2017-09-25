//
//  JWTAlgorithm.h
//  JWT
//
//  Created by Klaas Pieter Annema on 31-05-13.
//  Copyright (c) 2013 Karma. All rights reserved.
//

#import <Foundation/Foundation.h>
#import "JWTDeprecations.h"

@protocol JWTAlgorithm <NSObject>

@required
/**
 Signs data using provided secret data.
 @param hash The data to sign.
 @param key The secret to use for signing.
 @param error The inout error.
 */
- (NSData *)signHash:(NSData *)hash key:(NSData *)key error:(NSError *__autoreleasing*)error;
/**
 Verifies data using.
 @param hash The data to sign.
 @param signature The secret to use for signing.
 @param error The inout error.
 */
- (BOOL)verifyHash:(NSData *)hash signature:(NSData *)signature key:(NSData *)key error:(NSError *__autoreleasing*)error;

//@required

@property (nonatomic, readonly, copy) NSString *name;

/**
 Encodes and encrypts the provided payload using the provided secret key
 @param theString The string to encode
 @param theSecret The secret to use for encryption
 @return An NSData object containing the encrypted payload, or nil if something went wrong.
 */
- (NSData *)encodePayload:(NSString *)theString withSecret:(NSString *)theSecret __deprecated_and_will_be_removed_in_release_version(JWTVersion_3_0_0);

/**
 Verifies the provided signature using the signed input and verification key
 @param input The header and payload encoded string
 @param signature The JWT provided signature
 @param verificationKey The key to use for verifying the signature
 @return YES if the provided signature is valid, NO otherwise
 */
- (BOOL)verifySignedInput:(NSString *)input withSignature:(NSString *)signature verificationKey:(NSString *)verificationKey __deprecated_and_will_be_removed_in_release_version(JWTVersion_3_0_0);

@optional

/**
 Encodes and encrypts the provided payload using the provided secret key
 @param theStringData The data to encode
 @param theSecretData The secret data to use for encryption
 @return An NSData object containing the encrypted payload, or nil if something went wrong.
 */
- (NSData *)encodePayloadData:(NSData *)theStringData withSecret:(NSData *)theSecretData;

/**
 Verifies the provided signature using the signed input and verification key (as data)
 @param input The header and payload encoded string
 @param signature The JWT provided signature
 @param verificationKeyData The key data to use for verifying the signature
 @return YES if the provided signature is valid, NO otherwise
 */
- (BOOL)verifySignedInput:(NSString *)input withSignature:(NSString *)signature verificationKeyData:(NSData *)verificationKeyData;
@end
