//
//  JWTAlgorithmRSBase.m
//  JWT
//
//  Created by Lobanov Dmitry on 13.03.16.
//  Copyright © 2016 Karma. All rights reserved.
//

#import "JWTAlgorithmRSBase.h"
#import "JWTBase64Coder.h"
#import <CommonCrypto/CommonCrypto.h>

/*
*    * Possible inheritence *
*
*
*             RSBase (Public + Create-category)
*            /      \
*           /        \
*  RSBaseMac          RSBaseIOS
*           \  ifdef /
*            \      /
*         RSFamilyMember
*                |
*         RSFamilyMemberMutable
*
*/
/*
    TODO: rename RSBaseTest into RSFamilyMemberMutable
*/

NSString *const JWTAlgorithmNameRS256 = @"RS256";
NSString *const JWTAlgorithmNameRS384 = @"RS384";
NSString *const JWTAlgorithmNameRS512 = @"RS512";

@interface JWTAlgorithmRSBase()

@end

@implementation JWTAlgorithmRSBase

@synthesize privateKeyCertificatePassphrase;

#pragma mark - Override
// TODO: put assurance that algorithm was properly overriden?
- (size_t)ccSHANumberDigestLength {
    @throw [[NSException alloc] initWithName:NSInternalInconsistencyException reason:@"ccSHANumberDigestLength property should be overriden" userInfo:nil];
}

- (uint32_t)secPaddingPKCS1SHANumber {
    @throw [[NSException alloc] initWithName:NSInternalInconsistencyException reason:@"secPaddingPKCS1SHANumber property should be overriden" userInfo:nil];
}

- (unsigned char *)CC_SHANumberWithData:(const void *)data withLength:(CC_LONG)len withHashBytes:(unsigned char *)hashBytes {
    return nil;
}

- (NSString *)name {
    return @"RSBase";
}

#pragma mark - JWTAlgorithm
- (NSData *)encodePayload:(NSString *)theString withSecret:(NSString *)theSecret {
    return [self encodePayloadData:[theString dataUsingEncoding:NSUTF8StringEncoding] withSecret:[JWTBase64Coder dataWithBase64UrlEncodedString:theSecret]];
}

- (NSData *)encodePayloadData:(NSData *)theStringData withSecret:(NSData *)theSecretData {
    SecIdentityRef identity = nil;
    SecTrustRef trust = nil;
    __extractIdentityAndTrust((__bridge CFDataRef)theSecretData, &identity, &trust, (__bridge CFStringRef) self.privateKeyCertificatePassphrase);
    if (identity && trust) {
        SecKeyRef privateKey;
        SecIdentityCopyPrivateKey(identity, &privateKey);
        NSData *result = [self PKCSSignBytesSHANumberwithRSA:theStringData withPrivateKey:privateKey];
        
        if (identity) {
            CFRelease(identity);
        }
        
        if (trust) {
            CFRelease(trust);
        }
        
        if (privateKey) {
            CFRelease(privateKey);
        }
        
        return result;
        
    } else {
        return nil;
    }
}

- (BOOL)verifySignedInput:(NSString *)input withSignature:(NSString *)signature verificationKey:(NSString *)verificationKey {
    NSData *certificateData = [JWTBase64Coder dataWithBase64UrlEncodedString:verificationKey];
    return [self verifySignedInput:input
                     withSignature:signature
               verificationKeyData:certificateData];
}

- (BOOL)verifySignedInput:(NSString *)input withSignature:(NSString *)signature verificationKeyData:(NSData *)verificationKeyData {
    NSData *signedData = [input dataUsingEncoding:NSUTF8StringEncoding];
    NSData *signatureData = [JWTBase64Coder dataWithBase64UrlEncodedString:signature];
    SecKeyRef publicKey = [self publicKeyFromCertificate:verificationKeyData];
    BOOL signatureOk = [self PKCSVerifyBytesSHANumberWithRSA:signedData witSignature:signatureData withPublicKey:publicKey];
    (CFRelease(publicKey));
    return signatureOk;
}

#pragma mark - Private ( Override-part depends on platform )
- (BOOL)PKCSVerifyBytesSHANumberWithRSA:(NSData *)plainData witSignature:(NSData *)signature withPublicKey:(SecKeyRef) publicKey {
    return NO;
}

- (NSData *)PKCSSignBytesSHANumberwithRSA:(NSData *)plainData withPrivateKey:(SecKeyRef)privateKey {
    return nil;
}

#pragma mark - Private Helpers
- (SecKeyRef)publicKeyFromCertificate:(NSData *)certificateData {
    SecCertificateRef certificate = SecCertificateCreateWithData(NULL, (__bridge CFDataRef)certificateData);
    SecPolicyRef secPolicy = SecPolicyCreateBasicX509();
    SecTrustRef trust;
    SecTrustCreateWithCertificates(certificate, secPolicy, &trust);
    SecTrustResultType resultType;
    SecTrustEvaluate(trust, &resultType);
    SecKeyRef publicKey = SecTrustCopyPublicKey(trust);
    (CFRelease(trust));
    (CFRelease(secPolicy));
    (CFRelease(certificate));
    return publicKey;
}

OSStatus __extractIdentityAndTrust(CFDataRef inPKCS12Data,
        SecIdentityRef *outIdentity,
        SecTrustRef *outTrust,
        CFStringRef keyPassword) {
    OSStatus securityError = errSecSuccess;


    const void *keys[] =   { kSecImportExportPassphrase };
    const void *values[] = { keyPassword };
    CFDictionaryRef optionsDictionary = NULL;

    /* Create a dictionary containing the passphrase if one
       was specified.  Otherwise, create an empty dictionary. */
    optionsDictionary = CFDictionaryCreate(
            NULL, keys,
            values, (keyPassword ? 1 : 0),
            NULL, NULL);  // 1

    CFArrayRef items = NULL;
    securityError = SecPKCS12Import(inPKCS12Data,
            optionsDictionary,
            &items);                    // 2


    //
    if (securityError == 0) {                                   // 3
        CFDictionaryRef myIdentityAndTrust = CFArrayGetValueAtIndex (items, 0);
        const void *tempIdentity = NULL;
        tempIdentity = CFDictionaryGetValue (myIdentityAndTrust,
                kSecImportItemIdentity);
        CFRetain(tempIdentity);
        *outIdentity = (SecIdentityRef)tempIdentity;
        const void *tempTrust = NULL;
        tempTrust = CFDictionaryGetValue (myIdentityAndTrust, kSecImportItemTrust);

        CFRetain(tempTrust);
        *outTrust = (SecTrustRef)tempTrust;
    }

    if (optionsDictionary)                                      // 4
        CFRelease(optionsDictionary);

    if (items)
        CFRelease(items);

    return securityError;
}

@end

#if TARGET_OS_MAC && TARGET_OS_IPHONE
@interface JWTAlgorithmRSBaseIOS : JWTAlgorithmRSBase @end
@implementation JWTAlgorithmRSBaseIOS
- (BOOL)PKCSVerifyBytesSHANumberWithRSA:(NSData *)plainData witSignature:(NSData *)signature withPublicKey:(SecKeyRef) publicKey {
    size_t signedHashBytesSize = SecKeyGetBlockSize(publicKey);
    const void* signedHashBytes = [signature bytes];
    
    size_t hashBytesSize = self.ccSHANumberDigestLength;
    uint8_t* hashBytes = malloc(hashBytesSize);
    if (![self CC_SHANumberWithData:[plainData bytes] withLength:(CC_LONG)[plainData length] withHashBytes:hashBytes]) {
        return false;
    }
    
    OSStatus status = SecKeyRawVerify(publicKey,
                                      self.secPaddingPKCS1SHANumber,
                                      hashBytes,
                                      hashBytesSize,
                                      signedHashBytes,
                                      signedHashBytesSize);
    
    return status == errSecSuccess;
}

- (NSData *)PKCSSignBytesSHANumberwithRSA:(NSData *)plainData withPrivateKey:(SecKeyRef)privateKey {
    size_t signedHashBytesSize = SecKeyGetBlockSize(privateKey);
    uint8_t* signedHashBytes = malloc(signedHashBytesSize);
    memset(signedHashBytes, 0x0, signedHashBytesSize);
    
    size_t hashBytesSize = self.ccSHANumberDigestLength;
    uint8_t* hashBytes = malloc(hashBytesSize);
    
    // ([plainData bytes], (CC_LONG)[plainData length], hashBytes)
    unsigned char *str = [self CC_SHANumberWithData:[plainData bytes] withLength:(CC_LONG)[plainData length] withHashBytes:hashBytes];
    
    if (!str) {
        return nil;
    }
    
    SecKeyRawSign(privateKey,
                  self.secPaddingPKCS1SHANumber,
                  hashBytes,
                  hashBytesSize,
                  signedHashBytes,
                  &signedHashBytesSize);
    
    NSData* signedHash = [NSData dataWithBytes:signedHashBytes
                                        length:(NSUInteger)signedHashBytesSize];
    
    if (hashBytes)
        free(hashBytes);
    if (signedHashBytes)
        free(signedHashBytes);
    
    return signedHash;
}
@end
#endif

#if TARGET_OS_MAC && !TARGET_OS_IPHONE
@interface JWTAlgorithmRSBaseMac : JWTAlgorithmRSBase
@end

@implementation JWTAlgorithmRSBaseMac
- (NSData *)executeTransform:(SecTransformRef)transform withInput:(NSData *)input withDigestType:(CFStringRef)type withDigestLength:(NSNumber *)length withFalseResult:(CFTypeRef)falseResultRef {
    CFErrorRef errorRef = NULL;
    
    CFTypeRef resultRef = NULL;
    NSData *resultData = nil;
    
    
    BOOL success = transform != NULL;
    
    if (success) {
        // setup digest type
        success = SecTransformSetAttribute(transform, kSecDigestTypeAttribute, kSecDigestSHA2, &errorRef);
    }
    
    if (success) {
        // digest length
        success = SecTransformSetAttribute(transform, kSecDigestLengthAttribute, (__bridge CFNumberRef)length, &errorRef);
    }
    
    if (success) {
        // set input
        success = SecTransformSetAttribute(transform, kSecTransformInputAttributeName, (__bridge CFDataRef)input, &errorRef);
    }
    
    if (success) {
        // execute
        resultRef = SecTransformExecute(transform, &errorRef);
        success = (resultRef != falseResultRef);
    }
    
    BOOL positiveResult = success; // resultRef != falseResultRef
    
    // error
    if (errorRef != NULL) {
        NSLog(@"%@ error: %@", self.debugDescription, (__bridge NSError *)errorRef);
    }
    else {
        if (positiveResult) {
            resultData = (__bridge NSData *)resultRef;
        }
    }
    
    // release
    if (transform != NULL) {
        CFRelease(transform);
    }
    
    if (errorRef != NULL) {
        CFRelease(errorRef);
    }
    
    if (resultRef != NULL) {
        CFRelease(resultRef);
    }
    
    return resultData;
}
- (BOOL)PKCSVerifyBytesSHANumberWithRSA:(NSData *)plainData witSignature:(NSData *)signature withPublicKey:(SecKeyRef) publicKey {
    
    size_t signedHashBytesSize = SecKeyGetBlockSize(publicKey);
    //const void* signedHashBytes = [signature bytes];
    
    size_t hashBytesSize = self.ccSHANumberDigestLength;
    uint8_t* hashBytes = malloc(hashBytesSize);
    if (![self CC_SHANumberWithData:[plainData bytes] withLength:(CC_LONG)[plainData length] withHashBytes:hashBytes]) {
        return false;
    }
    
    // verify for iOS
//    OSStatus status = SecKeyRawVerify(publicKey,
//                                      self.secPaddingPKCS1SHANumber,
//                                      hashBytes,
//                                      hashBytesSize,
//                                      signedHashBytes,
//                                      signedHashBytesSize);
//    return status == errSecSuccess;
    
    CFErrorRef errorRef = NULL;
    SecTransformRef transform = SecVerifyTransformCreate(publicKey, (__bridge CFDataRef)signature, &errorRef);
    
    // verification. false result is kCFBooleanFalse
    BOOL result = [self executeTransform:transform withInput:plainData withDigestType:kSecDigestSHA2 withDigestLength:@(signedHashBytesSize) withFalseResult:kCFBooleanFalse] != nil;
    
    if (errorRef != NULL) {
        CFRelease(errorRef);
    }
    
    return result;
}

- (NSData *)PKCSSignBytesSHANumberwithRSA:(NSData *)plainData withPrivateKey:(SecKeyRef)privateKey {
    size_t signedHashBytesSize = SecKeyGetBlockSize(privateKey);
    //uint8_t* signedHashBytes = malloc(signedHashBytesSize);
    //memset(signedHashBytes, 0x0, signedHashBytesSize);
    
    size_t hashBytesSize = self.ccSHANumberDigestLength;
    uint8_t* hashBytes = malloc(hashBytesSize);
    
    /**
     for sha256
     CC_SHANumberWithData() is CC_SHA256()
     self.secPaddingPKCS1SHANumber = kSecPaddingPKCS1SHA256
     self.ccSHANumberDigestLength  = CC_SHA256_DIGEST_LENGTH
     */
    unsigned char *str = [self CC_SHANumberWithData:[plainData bytes] withLength:(CC_LONG)[plainData length] withHashBytes:hashBytes];
    
    if (!str) {
        return nil;
    }
    
    CFErrorRef errorRef = NULL;
    
    SecTransformRef transform = SecSignTransformCreate(privateKey, &errorRef);
    
    /** iOS
    SecKeyRawSign(privateKey,
                  self.secPaddingPKCS1SHANumber,
                  hashBytes,
                  hashBytesSize,
                  signedHashBytes,
                  &signedHashBytesSize);
    
    NSData* signedHash = [NSData dataWithBytes:signedHashBytes
                                        length:(NSUInteger)signedHashBytesSize];
    
     */
    
    NSData *resultData = nil;
    // signing: false result is NULL.
    // it will release error.
    resultData = [self executeTransform:transform withInput:plainData withDigestType:kSecDigestSHA2 withDigestLength:@(signedHashBytesSize) withFalseResult:NULL];
    
    if (errorRef != NULL) {
        CFRelease(errorRef);
    }
    
    return resultData;
}
@end
#endif


// MacOS OR iOS is Base
#if TARGET_OS_MAC && !TARGET_OS_IPHONE
@interface JWTAlgorithmRSFamilyMember : JWTAlgorithmRSBaseMac @end
#else
@interface JWTAlgorithmRSFamilyMember : JWTAlgorithmRSBaseIOS @end
#endif

@interface JWTAlgorithmRS256 : JWTAlgorithmRSFamilyMember @end
@interface JWTAlgorithmRS384 : JWTAlgorithmRSFamilyMember @end
@interface JWTAlgorithmRS512 : JWTAlgorithmRSFamilyMember @end

@implementation JWTAlgorithmRSFamilyMember
- (uint32_t)secPaddingPKCS1SHANumber {
    return 0;
}
@end

@implementation JWTAlgorithmRS256

- (size_t)ccSHANumberDigestLength {
    return CC_SHA256_DIGEST_LENGTH;
}

#if TARGET_OS_MAC && TARGET_OS_IPHONE
- (uint32_t)secPaddingPKCS1SHANumber {
    return kSecPaddingPKCS1SHA256;
}
#endif

- (unsigned char *)CC_SHANumberWithData:(const void *)data withLength:(CC_LONG)len withHashBytes:(unsigned char *)hashBytes {
    return CC_SHA256(data, len, hashBytes);
}

- (NSString *)name {
    return JWTAlgorithmNameRS256;
}

@end

@implementation JWTAlgorithmRS384

- (size_t)ccSHANumberDigestLength {
    return CC_SHA384_DIGEST_LENGTH;
}

#if TARGET_OS_MAC && TARGET_OS_IPHONE
- (uint32_t)secPaddingPKCS1SHANumber {
    return kSecPaddingPKCS1SHA384;
}
#endif

- (unsigned char *)CC_SHANumberWithData:(const void *)data withLength:(CC_LONG)len withHashBytes:(unsigned char *)hashBytes {
    return CC_SHA384(data, len, hashBytes);
}

- (NSString *)name {
    return JWTAlgorithmNameRS384;
}

@end

@implementation JWTAlgorithmRS512

- (size_t)ccSHANumberDigestLength {
    return CC_SHA512_DIGEST_LENGTH;
}

#if TARGET_OS_MAC && TARGET_OS_IPHONE
- (uint32_t)secPaddingPKCS1SHANumber {
    return kSecPaddingPKCS1SHA512;
}
#endif

- (unsigned char *)CC_SHANumberWithData:(const void *)data withLength:(CC_LONG)len withHashBytes:(unsigned char *)hashBytes {
    return CC_SHA512(data, len, hashBytes);
}

- (NSString *)name {
    return JWTAlgorithmNameRS512;
}

@end


@interface JWTAlgorithmRSFamilyMemberMutable : JWTAlgorithmRSFamilyMember

@property (assign, nonatomic, readwrite) size_t ccSHANumberDigestLength;
@property (assign, nonatomic, readwrite) uint32_t secPaddingPKCS1SHANumber;
@property (copy, nonatomic, readwrite) unsigned char * (^ccShaNumberWithData)(const void *data, CC_LONG len, unsigned char *hashBytes);
@property (copy, nonatomic, readwrite) NSString *name;
@end

@implementation JWTAlgorithmRSFamilyMemberMutable

@synthesize ccSHANumberDigestLength = _ccSHANumberDigestLength;
@synthesize secPaddingPKCS1SHANumber = _secPaddingPKCS1SHANumber;
@synthesize name = _name;

- (size_t)ccSHANumberDigestLength {
    return _ccSHANumberDigestLength;
}

- (uint32_t)secPaddingPKCS1SHANumber {
    return _secPaddingPKCS1SHANumber;
}

- (unsigned char *)CC_SHANumberWithData:(const void *)data withLength:(uint32_t)len withHashBytes:(unsigned char *)hashBytes {
    unsigned char *result = [super CC_SHANumberWithData:data withLength:len withHashBytes:hashBytes];
    if (!result && self.ccShaNumberWithData) {
        result = self.ccShaNumberWithData(data, len, hashBytes);
    }
    return result;
}

@end


@implementation JWTAlgorithmRSBase (Create)

+ (instancetype)algorithm256 {
    return [JWTAlgorithmRS256 new];
}

+ (instancetype)algorithm384 {
    return [JWTAlgorithmRS384 new];
}

+ (instancetype)algorithm512 {
    return [JWTAlgorithmRS512 new];
}

+ (instancetype)mutableAlgorithm {
    JWTAlgorithmRSFamilyMemberMutable *base = [JWTAlgorithmRSFamilyMemberMutable new];
    base.ccSHANumberDigestLength = CC_SHA256_DIGEST_LENGTH;
    
    //set to something ok
    //base.secPaddingPKCS1SHANumber = kSecPaddingPKCS1SHA256;
    base.ccShaNumberWithData = ^unsigned char *(const void *data, CC_LONG len, unsigned char *hashBytes){
        return CC_SHA256(data, len, hashBytes);
    };
    base.name = @"RS256";
    return base;
}

@end
