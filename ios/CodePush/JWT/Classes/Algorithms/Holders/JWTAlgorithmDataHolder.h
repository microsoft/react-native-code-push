//
//  JWTAlgorithmDataHolder.h
//  JWT
//
//  Created by Lobanov Dmitry on 31.08.16.
//  Copyright © 2016 Karma. All rights reserved.
//

#import <Foundation/Foundation.h>
#import "JWTAlgorithm.h"
#import "JWTDeprecations.h"

// TODO: available in 3.0
// All methods with secret as NSString in algorithms will be deprecated or removed.

@protocol JWTAlgorithmDataHolder <NSObject>
/**
 The verification key to use when encoding/decoding a JWT in data form
 */
@property (copy, nonatomic, readwrite) NSData *currentSecretData;

/**
 The <JWTAlgorithm> to use for encoding a JWT
 */
@property (strong, nonatomic, readwrite) id <JWTAlgorithm> currentAlgorithm;
@end

@interface JWTAlgorithmBaseDataHolder : NSObject <JWTAlgorithmDataHolder>

#pragma mark - Getters
/**
 The verification key to use when encoding/decoding a JWT
 */
@property (copy, nonatomic, readonly) NSString *currentSecret;

/**
 The algorithm name to use for decoding the JWT. Required unless force decode is true
 */
@property (copy, nonatomic, readonly) NSString *currentAlgorithmName;

#pragma mark - Setters
/**
 Sets jwtSecret and returns the JWTAlgorithmBaseDataHolder to allow for method chaining
 */
@property (copy, nonatomic, readonly) JWTAlgorithmBaseDataHolder *(^secret)(NSString *secret);

/**
 Sets jwtSecretData and returns the JWTAlgorithmBaseDataHolder to allow for method chaining
 */
@property (copy, nonatomic, readonly) JWTAlgorithmBaseDataHolder *(^secretData)(NSData *secretData);

/**
 Sets jwtAlgorithm and returns the JWTAlgorithmBaseDataHolder to allow for method chaining
 */
@property (copy, nonatomic, readonly) JWTAlgorithmBaseDataHolder *(^algorithm)(id<JWTAlgorithm>algorithm);

/**
 Sets jwtAlgorithmName and returns the JWTAlgorithmBaseDataHolder to allow for method chaining. See list of names in appropriate headers.
 */
@property (copy, nonatomic, readonly) JWTAlgorithmBaseDataHolder *(^algorithmName)(NSString *algorithmName);

@end

@interface JWTAlgorithmHSFamilyDataHolder : JWTAlgorithmBaseDataHolder
@end

@interface JWTAlgorithmRSFamilyDataHolder : JWTAlgorithmBaseDataHolder
#pragma mark - Getters
/**
 The passphrase for the PKCS12 blob, which represents the certificate containing the private key for the RS algorithms.
 */
@property (copy, nonatomic, readonly) NSString *currentPrivateKeyCertificatePassphrase;

#pragma mark - Setters
/**
 Sets jwtPrivateKeyCertificatePassphrase and returns the JWTAlgorithmRSFamilyDataHolder to allow for method chaining
 */
@property (copy, nonatomic, readonly) JWTAlgorithmRSFamilyDataHolder *(^privateKeyCertificatePassphrase)(NSString *privateKeyCertificatePassphrase);
@end
