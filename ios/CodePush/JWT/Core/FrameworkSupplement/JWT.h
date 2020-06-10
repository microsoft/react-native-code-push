//
//  JWT.h
//  JWT
//
//  Created by Lobanov Dmitry on 23.10.16.
//  Copyright © 2016 Karma. All rights reserved.
//

#import <Foundation/Foundation.h>

//! Project version number for JWT.
FOUNDATION_EXPORT double JWTVersionNumber;

//! Project version string for JWT.
FOUNDATION_EXPORT const unsigned char JWTVersionString[];

// In this header, you should import all the public headers of your framework using statements like #import <PublicHeader.h>

// Coding
#import <JWTCoding.h>
#import <JWTCoding+ResultTypes.h>
#import <JWTCoding+VersionOne.h>
#import <JWTCoding+VersionTwo.h>
#import <JWTCoding+VersionThree.h>

// Algorithms
#import <JWTAlgorithm.h>
#import <JWTRSAlgorithm.h>
#import <JWTAlgorithmFactory.h>
#import <JWTAlgorithmNone.h>
#import <JWTAlgorithmHSBase.h>
#import <JWTAlgorithmRSBase.h>

// Holders
#import <JWTAlgorithmDataHolder.h>
#import <JWTAlgorithmDataHolderChain.h>

// Claims
#import <JWTClaimsSet.h>
#import <JWTClaim.h>
#import <JWTClaimsSetSerializer.h>
#import <JWTClaimsSetVerifier.h>

// Supplement
#import <JWTDeprecations.h>
#import <JWTBase64Coder.h>
#import <JWTErrorDescription.h>

// Crypto
#import <JWTCryptoKey.h>
#import <JWTCryptoKeyExtractor.h>
#import <JWTCryptoSecurity.h>
