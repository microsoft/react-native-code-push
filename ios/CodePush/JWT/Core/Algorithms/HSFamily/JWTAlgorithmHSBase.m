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

NSString *const JWTAlgorithmNameHS256 = @"HS256";
NSString *const JWTAlgorithmNameHS384 = @"HS384";
NSString *const JWTAlgorithmNameHS512 = @"HS512";

@interface JWTAlgorithmHSBase () @end

@implementation JWTAlgorithmHSBase

- (size_t)ccSHANumberDigestLength {
    @throw [[NSException alloc] initWithName:NSInternalInconsistencyException reason:@"ccSHANumberDigestLength property should be overriden" userInfo:nil];
}

- (uint32_t)ccHmacAlgSHANumber {
    @throw [[NSException alloc] initWithName:NSInternalInconsistencyException reason:@"ccHmacAlgSHANumber property should be overriden" userInfo:nil];
}

- (NSData *)signHash:(NSData *)hash key:(NSData *)key error:(NSError *__autoreleasing *)error {
    size_t amount = self.ccSHANumberDigestLength;
    size_t fullSize = amount * sizeof(unsigned char);
    unsigned char* cHMAC = malloc(fullSize);
    CCHmacAlgorithm ccAlg = self.ccHmacAlgSHANumber;
    
    CCHmac(ccAlg, key.bytes, key.length, hash.bytes, hash.length, cHMAC);
    
    NSData *result = [[NSData alloc] initWithBytes:cHMAC length:fullSize];
    free(cHMAC);
    return result;
}

- (BOOL)verifyHash:(NSData *)hash signature:(NSData *)signature key:(NSData *)key error:(NSError *__autoreleasing *)error {
    NSData *expectedSignatureData = [self signHash:hash key:key error:error];
    return [expectedSignatureData isEqualToData:signature];
}

- (NSString *)name;
{
    return @"HSBase";
}

- (NSData *)encodePayload:(NSString *)theString withSecret:(NSString *)theSecret;
{
    NSData *inputData = [theString dataUsingEncoding:NSUTF8StringEncoding];
    NSData *secretData = [theSecret dataUsingEncoding:NSUTF8StringEncoding];
    //[JWTBase64Coder dataWithBase64UrlEncodedString:theSecret];
    return [self encodePayloadData:inputData withSecret:secretData];
//    const char *cString = [theString cStringUsingEncoding:NSUTF8StringEncoding];
//    const char *cSecret = [theSecret cStringUsingEncoding:NSUTF8StringEncoding];
//    
//    size_t amount = self.ccSHANumberDigestLength;
//    size_t fullSize = amount * sizeof(unsigned char);
//    unsigned char* cHMAC = malloc(fullSize);
//    CCHmacAlgorithm ccAlg = self.ccHmacAlgSHANumber;
//
//    CCHmac(ccAlg, cSecret, strlen(cSecret), cString, strlen(cString), cHMAC);
//    
//    NSData *returnData = [[NSData alloc] initWithBytes:cHMAC length:fullSize];
//    free(cHMAC);
//    return returnData;
}

- (NSData *)encodePayloadData:(NSData *)theStringData withSecret:(NSData *)theSecretData
{
    return [self signHash:theStringData key:theSecretData error:nil];
//    size_t amount = self.ccSHANumberDigestLength;
//    size_t fullSize = amount * sizeof(unsigned char);
//    unsigned char* cHMAC = malloc(fullSize);
//    CCHmacAlgorithm ccAlg = self.ccHmacAlgSHANumber;
//    
//    CCHmac(ccAlg, theSecretData.bytes, [theSecretData length], theStringData.bytes, [theStringData length], cHMAC);
//    
//    NSData *returnData = [[NSData alloc] initWithBytes:cHMAC length:fullSize];
//    free(cHMAC);
//    return returnData;
}

- (BOOL)verifySignedInput:(NSString *)input withSignature:(NSString *)signature verificationKey:(NSString *)verificationKey
{
    NSData *verificationKeyData = [verificationKey dataUsingEncoding:NSUTF8StringEncoding];
    return [self verifySignedInput:input withSignature:signature verificationKeyData:verificationKeyData];
//    NSData *expectedSignatureData = [self encodePayload:input withSecret:verificationKey];
//    NSString *expectedSignature = [JWTBase64Coder base64UrlEncodedStringWithData:expectedSignatureData];
//    
//    return [expectedSignature isEqualToString:signature];
}

- (BOOL)verifySignedInput:(NSString *)input withSignature:(NSString *)signature verificationKeyData:(NSData *)verificationKeyData {
    NSData *signatureData = [JWTBase64Coder dataWithBase64UrlEncodedString:signature];
    NSData *inputData = [input dataUsingEncoding:NSUTF8StringEncoding];//[JWTBase64Coder dataWithBase64UrlEncodedString:input];
    return [self verifyHash:inputData signature:signatureData key:verificationKeyData error:nil ];
//    const char *cString = [input cStringUsingEncoding:NSUTF8StringEncoding];
//    NSData *inputData = [NSData dataWithBytes:cString length:strlen(cString)];
//    
//    NSData *expectedSignatureData = [self encodePayloadData:inputData withSecret:verificationKeyData];
//    NSString *expectedSignature = [JWTBase64Coder base64UrlEncodedStringWithData:expectedSignatureData];
//    
//    return [expectedSignature isEqualToString:signature];
}

@end

@interface JWTAlgorithmHSFamilyMember : JWTAlgorithmHSBase @end
@implementation JWTAlgorithmHSFamilyMember @end

@interface JWTAlgorithmHS256 : JWTAlgorithmHSBase @end
@interface JWTAlgorithmHS384 : JWTAlgorithmHSBase @end
@interface JWTAlgorithmHS512 : JWTAlgorithmHSBase @end

@implementation JWTAlgorithmHS256

- (size_t)ccSHANumberDigestLength {
    return CC_SHA256_DIGEST_LENGTH;
}

- (uint32_t)ccHmacAlgSHANumber {
    return kCCHmacAlgSHA256;
}

- (NSString *)name {
    return @"HS256";
}

@end

@implementation JWTAlgorithmHS384

- (size_t)ccSHANumberDigestLength {
    return CC_SHA384_DIGEST_LENGTH;
}

- (uint32_t)ccHmacAlgSHANumber {
    return kCCHmacAlgSHA384;
}

- (NSString *)name {
    return @"HS384";
}

@end

@implementation JWTAlgorithmHS512

- (size_t)ccSHANumberDigestLength {
    return CC_SHA512_DIGEST_LENGTH;
}

- (uint32_t)ccHmacAlgSHANumber {
    return kCCHmacAlgSHA512;
}

- (NSString *)name {
    return @"HS512";
}

@end

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
