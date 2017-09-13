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
#import "JWTBase64Coder.h"

// TODO: available in 3.0
// All methods with secret as NSString in algorithms will be deprecated or removed.

@protocol JWTAlgorithmDataHolderProtocol <NSObject, NSCopying>
/**
 The verification key to use when encoding/decoding a JWT in data form
 */
@property (copy, nonatomic, readwrite) NSData *internalSecretData;

/**
 The <JWTAlgorithm> to use for encoding a JWT
 */
@property (strong, nonatomic, readwrite) id <JWTAlgorithm> internalAlgorithm;

/**
 The <JWTStringCoder__Protocol> string coder. It converts data to string and vice versa.
 */
@property (strong, nonatomic, readwrite) id <JWTStringCoder__Protocol> internalStringCoder;
@end

@interface JWTAlgorithmBaseDataHolder : NSObject <JWTAlgorithmDataHolderProtocol>

#pragma mark - Getters
/**
 The verification key to use when encoding/decoding a JWT
 */
@property (copy, nonatomic, readonly) NSString *internalSecret;

/**
 The algorithm name to use for decoding the JWT. Required unless force decode is true
 */
@property (copy, nonatomic, readonly) NSString *internalAlgorithmName;

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

/**
 Sets stringCoder and returns the JWTAlgorithmBaseDataHolder to allow for method chaining. See list of names in appropriate headers.
 */
@property (copy, nonatomic, readonly) JWTAlgorithmBaseDataHolder *(^stringCoder)(id<JWTStringCoder__Protocol> stringCoder);
@end

@protocol JWTAlgorithmDataHolderCreateProtocol <NSObject>

+ (instancetype)createWithAlgorithm256;
+ (instancetype)createWithAlgorithm384;
+ (instancetype)createWithAlgorithm512;

@end

@interface JWTAlgorithmNoneDataHolder : JWTAlgorithmBaseDataHolder @end
@interface JWTAlgorithmHSFamilyDataHolder : JWTAlgorithmBaseDataHolder <JWTAlgorithmDataHolderCreateProtocol>
@end
@protocol JWTCryptoKeyProtocol;
@interface JWTAlgorithmRSFamilyDataHolder : JWTAlgorithmBaseDataHolder <JWTAlgorithmDataHolderCreateProtocol>
#pragma mark - Getters
/**
 The passphrase for the PKCS12 blob, which represents the certificate containing the private key for the RS algorithms.
 */
@property (copy, nonatomic, readonly) NSString *internalPrivateKeyCertificatePassphrase;
@property (copy, nonatomic, readonly) NSString *internalKeyExtractorType;
@property (strong, nonatomic, readonly) id<JWTCryptoKeyProtocol> internalSignKey;
@property (strong, nonatomic, readonly) id<JWTCryptoKeyProtocol> internalVerifyKey;
#pragma mark - Setters
/**
 Sets jwtPrivateKeyCertificatePassphrase and returns the JWTAlgorithmRSFamilyDataHolder to allow for method chaining
 */
@property (copy, nonatomic, readonly) JWTAlgorithmRSFamilyDataHolder *(^privateKeyCertificatePassphrase)(NSString *privateKeyCertificatePassphrase);
@property (copy, nonatomic, readonly) JWTAlgorithmRSFamilyDataHolder *(^keyExtractorType)(NSString *keyExtractorType);
@property (copy, nonatomic, readonly) JWTAlgorithmRSFamilyDataHolder *(^signKey)(id<JWTCryptoKeyProtocol> signKey);
@property (copy, nonatomic, readonly) JWTAlgorithmRSFamilyDataHolder *(^verifyKey)(id<JWTCryptoKeyProtocol> verifyKey);
@end
