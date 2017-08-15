//
//  JWTAlgorithmHSBase.m
//  JWT
//
//  Created by Lobanov Dmitry on 13.03.16.
//  Copyright © 2016 Karma. All rights reserved.
//

#import "JWTAlgorithmHSBase.h"
#import "JWTBase64Coder.h"
#import <CommonCrypto/CommonCrypto.h>
#import <CommonCrypto/CommonHMAC.h>
#import "JWTAlgorithmHS256.h"
#import "JWTAlgorithmHS384.h"
#import "JWTAlgorithmHS512.h"

NSString *const JWTAlgorithmNameHS256 = @"HS256";
NSString *const JWTAlgorithmNameHS384 = @"HS384";
NSString *const JWTAlgorithmNameHS512 = @"HS512";

// TODO(3.0):
// 1. hide algorithms as it was done in RSBase.
// 2. remove remain headers.
@interface JWTAlgorithmHSBase ()

@end

@implementation JWTAlgorithmHSBase

- (size_t)ccSHANumberDigestLength {
    @throw [[NSException alloc] initWithName:NSInternalInconsistencyException reason:@"ccSHANumberDigestLength property should be overriden" userInfo:nil];
}

- (uint32_t)ccHmacAlgSHANumber {
    @throw [[NSException alloc] initWithName:NSInternalInconsistencyException reason:@"ccHmacAlgSHANumber property should be overriden" userInfo:nil];
}

- (NSString *)name;
{
    return @"HSBase";
}

- (NSData *)encodePayload:(NSString *)theString withSecret:(NSString *)theSecret;
{
    const char *cString = [theString cStringUsingEncoding:NSUTF8StringEncoding];
    const char *cSecret = [theSecret cStringUsingEncoding:NSUTF8StringEncoding];
    
    size_t amount = self.ccSHANumberDigestLength;
    size_t fullSize = amount * sizeof(unsigned char);
    unsigned char* cHMAC = malloc(fullSize);
    CCHmacAlgorithm ccAlg = self.ccHmacAlgSHANumber;

    CCHmac(ccAlg, cSecret, strlen(cSecret), cString, strlen(cString), cHMAC);
    
    NSData *returnData = [[NSData alloc] initWithBytes:cHMAC length:fullSize];
    free(cHMAC);
    return returnData;
}

- (NSData *)encodePayloadData:(NSData *)theStringData withSecret:(NSData *)theSecretData
{
    size_t amount = self.ccSHANumberDigestLength;
    size_t fullSize = amount * sizeof(unsigned char);
    unsigned char* cHMAC = malloc(fullSize);
    CCHmacAlgorithm ccAlg = self.ccHmacAlgSHANumber;
    
    CCHmac(ccAlg, theSecretData.bytes, [theSecretData length], theStringData.bytes, [theStringData length], cHMAC);
    
    NSData *returnData = [[NSData alloc] initWithBytes:cHMAC length:fullSize];
    free(cHMAC);
    return returnData;
}

- (BOOL)verifySignedInput:(NSString *)input withSignature:(NSString *)signature verificationKey:(NSString *)verificationKey
{
    NSData *expectedSignatureData = [self encodePayload:input withSecret:verificationKey];
    NSString *expectedSignature = [JWTBase64Coder base64UrlEncodedStringWithData:expectedSignatureData];
    
    return [expectedSignature isEqualToString:signature];
}

- (BOOL)verifySignedInput:(NSString *)input withSignature:(NSString *)signature verificationKeyData:(NSData *)verificationKeyData {
    const char *cString = [input cStringUsingEncoding:NSUTF8StringEncoding];
    NSData *inputData = [NSData dataWithBytes:cString length:strlen(cString)];
    
    NSData *expectedSignatureData = [self encodePayloadData:inputData withSecret:verificationKeyData];
    NSString *expectedSignature = [JWTBase64Coder base64UrlEncodedStringWithData:expectedSignatureData];
    
    return [expectedSignature isEqualToString:signature];
}

@end

@interface JWTAlgorithmHSFamilyMember : JWTAlgorithmHSBase @end
@implementation JWTAlgorithmHSFamilyMember @end

/* JWTAlgorithmHS256 : JWTAlgorithmHSFamilyMember and others */

@interface JWTAlgorithmHSFamilyMemberMutable : JWTAlgorithmHSFamilyMember
@property (assign, nonatomic, readwrite) size_t ccSHANumberDigestLength;
@property (assign, nonatomic, readwrite) uint32_t ccHmacAlgSHANumber;
@property (copy, nonatomic, readwrite) NSString *name;
@end

@implementation JWTAlgorithmHSFamilyMemberMutable

@synthesize ccSHANumberDigestLength = _ccSHANumberDigestLength;
@synthesize ccHmacAlgSHANumber = _ccHmacAlgSHANumber;
@synthesize name = _name;

- (size_t)ccSHANumberDigestLength {
    return _ccSHANumberDigestLength;
}

- (uint32_t)ccHmacAlgSHANumber {
    return _ccHmacAlgSHANumber;
}

@end

@implementation JWTAlgorithmHSBase (Create)

+ (instancetype)algorithm256 {
    return [JWTAlgorithmHS256 new];
}

+ (instancetype)algorithm384 {
    return [JWTAlgorithmHS384 new];
}

+ (instancetype)algorithm512 {
    return [JWTAlgorithmHS512 new];
}

@end
