//
//  JWTCryptoSecurity.m
//  JWT
//
//  Created by Lobanov Dmitry on 04.02.17.
//  Copyright © 2017 JWTIO. All rights reserved.
//

#import "JWTCryptoSecurity.h"
@interface JWTMemoryLayout : NSObject
+ (NSString *)typeUInt8;
+ (NSString *)typeCUnsignedChar;
- (instancetype)initWithType:(NSString *)type;
+ (instancetype)createWithType:(NSString *)type;
@property (copy, nonatomic, readwrite) NSString *type;
@property (assign, nonatomic, readonly) NSInteger size;
@end
@implementation JWTMemoryLayout
+ (NSString *)typeUInt8 {
    return NSStringFromSelector(_cmd);
}
+ (NSString *)typeCUnsignedChar {
    return [self typeUInt8];
}
- (instancetype)initWithType:(NSString *)type {
    self = [super init];
    if (self) {
        self.type = type;
    }
    return self;
}
+ (instancetype)createWithType:(NSString *)type {
    return [[self alloc] initWithType:type];
}
+ (NSDictionary *)sizesAndTypes {
    static NSDictionary *sizesAndTypes = nil;
    return sizesAndTypes ?: (sizesAndTypes = @{
        [self typeUInt8] : @(1) // or 8?
    }, sizesAndTypes);
}
- (NSInteger)size {
    return [[self.class sizesAndTypes][self.type] integerValue];
}
@end
@implementation JWTCryptoSecurity
+ (NSDictionary *)dictionaryByCombiningDictionaries:(NSArray *)dictionaries {
    NSMutableDictionary *result = [@{} mutableCopy];
    for (NSDictionary *dictionary in dictionaries) {
        [result addEntriesFromDictionary:dictionary];
    }
    return [result copy];
}
+ (NSString *)keyTypeRSA {
    return (__bridge NSString *)kSecAttrKeyTypeRSA;
}
+ (NSString *)keyTypeEC {
//    extern const CFStringRef kSecAttrKeyTypeEC
//    __OSX_AVAILABLE_STARTING(__MAC_10_9, __IPHONE_4_0);
//    extern const CFStringRef kSecAttrKeyTypeECSECPrimeRandom
//    __OSX_AVAILABLE_STARTING(__MAC_10_12, __IPHONE_10_0);
    return (__bridge NSString *)kSecAttrKeyTypeEC;
}
+ (SecKeyRef)addKeyWithData:(NSData *)data asPublic:(BOOL)public tag:(NSString *)tag type:(NSString *)type error:(NSError *__autoreleasing*)error; {
    NSString *keyClass = (__bridge NSString *)(public ? kSecAttrKeyClassPublic : kSecAttrKeyClassPrivate);
    NSInteger sizeInBits = data.length * [JWTMemoryLayout createWithType:[JWTMemoryLayout typeUInt8]].size;
    NSDictionary *attributes = @{
        (__bridge NSString*)kSecAttrKeyType : type,
        (__bridge NSString*)kSecAttrKeyClass : keyClass,
        (__bridge NSString*)kSecAttrKeySizeInBits : @(sizeInBits)
    };
    
    if (SecKeyCreateWithData != NULL) {
        CFErrorRef createError = NULL;
        SecKeyRef key = SecKeyCreateWithData((__bridge CFDataRef)data, (__bridge CFDictionaryRef)attributes, &createError);
        if (error && createError != nil) {
            *error = (__bridge NSError*)createError;
        }
        return key;
    }
    // oh... not avaialbe API :/
    else {

        CFTypeRef result = NULL;
        NSData *tagData = [tag dataUsingEncoding:NSUTF8StringEncoding];
        NSDictionary *commonAttributes = @{
            (__bridge NSString*)kSecClass: (__bridge NSString*)kSecClassKey,
            (__bridge NSString*)kSecAttrApplicationTag: tagData,
            (__bridge NSString*)kSecAttrAccessible: (__bridge NSString*)kSecAttrAccessibleWhenUnlocked
        };


        NSDictionary *addItemAttributes = @{
           (__bridge NSString*)kSecValueData: data,
           (__bridge NSString*)kSecReturnPersistentRef: @(YES),
        };

        OSStatus addItemStatus = SecItemAdd((__bridge CFDictionaryRef)[self dictionaryByCombiningDictionaries:@[attributes, commonAttributes, addItemAttributes]], &result);
        if (addItemStatus != errSecSuccess && addItemStatus != errSecDuplicateItem) {
            // add item error
            // not duplicate and not added to keychain.
            return NULL;
        }

        NSDictionary *copyAttributes = @{
                (__bridge NSString*)kSecReturnRef: @(YES),
        };

        CFTypeRef key = NULL;
        // TODO: Add error handling later.
        OSStatus copyItemStatus = errSecSuccess;
        SecItemCopyMatching((__bridge CFDictionaryRef)[self dictionaryByCombiningDictionaries:@[attributes, commonAttributes, copyAttributes]], &key);
        if (key == NULL) {
            // copy item error
        }
        return (SecKeyRef)key;
    }

    return NULL;
}
+ (SecKeyRef)addKeyWithData:(NSData *)data asPublic:(BOOL)public tag:(NSString *)tag error:(NSError *__autoreleasing*)error; {
    return [self addKeyWithData:data asPublic:public tag:tag type:[self keyTypeRSA] error:error];
}

+ (SecKeyRef)keyByTag:(NSString *)tag error:(NSError *__autoreleasing*)error; {
    return NULL;
}

+ (void)removeKeyByTag:(NSString *)tag error:(NSError *__autoreleasing*)error; {
    NSData *tagData = [tag dataUsingEncoding:NSUTF8StringEncoding];
    if (tagData == nil) {
        // tell that nothing to remove.        
        return;
    }
    NSDictionary *removeAttributes = @{
            (__bridge NSString*)kSecClass: (__bridge NSString*)kSecClassKey,
            (__bridge NSString*)kSecAttrKeyType: (__bridge NSString*)kSecAttrKeyTypeRSA,
            (__bridge NSString*)kSecAttrApplicationTag: tagData,
    };
    SecItemDelete((__bridge CFDictionaryRef)removeAttributes);
}
@end

@implementation JWTCryptoSecurity (Certificates)
+ (OSStatus)extractIdentityAndTrustFromPKCS12:(CFDataRef)inPKCS12Data password:(CFStringRef)password identity:(SecIdentityRef *)outIdentity trust:(SecTrustRef *)outTrust {

    OSStatus securityError = errSecSuccess;


    const void *keys[] =   { kSecImportExportPassphrase };
    const void *values[] = { password };
    CFDictionaryRef optionsDictionary = NULL;

    /* Create a dictionary containing the passphrase if one
     was specified.  Otherwise, create an empty dictionary. */
    optionsDictionary = CFDictionaryCreate(
                                           NULL, keys,
                                           values, (password ? 1 : 0),
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

+ (SecKeyRef)publicKeyFromCertificate:(NSData *)certificateData {
    SecCertificateRef certificate = SecCertificateCreateWithData(NULL, (__bridge CFDataRef)certificateData);
    if (certificate != NULL) {
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
    return NULL;
}
@end

@implementation JWTCryptoSecurity (Pem)
+ (NSString *)certificateFromPemFileContent:(NSString *)content {
    NSRegularExpression *expression = [[NSRegularExpression alloc] initWithPattern:@"-----BEGIN CERTIFICATE-----(.+?)-----END CERTIFICATE-----" options:NSRegularExpressionDotMatchesLineSeparators error:nil];
    return [self itemsFromPemFileContent:content byRegex:expression].firstObject;
}
+ (NSString *)keyFromPemFileContent:(NSString *)content {
    NSRegularExpression *expression = [[NSRegularExpression alloc] initWithPattern:@"-----BEGIN(?:[\\w\\s]|)+KEY-----(.+?)-----END(?:[\\w\\s])+KEY-----" options:NSRegularExpressionDotMatchesLineSeparators error:nil];
    return [self itemsFromPemFileContent:content byRegex:expression].firstObject;
}
+ (NSArray *)itemsFromPemFileContent:(NSString *)content byRegex:(NSRegularExpression *)expression {
    NSArray *matches = [expression matchesInString:content options:0 range:NSMakeRange(0, content.length)];
    NSTextCheckingResult *result = matches.firstObject;
    NSArray *resultArray = @[];
    
    if (result) {
        for (NSUInteger i = 1; i < result.numberOfRanges; ++i) {
            NSString *extractedString = [content substringWithRange:[result rangeAtIndex:i]];
            resultArray = [resultArray arrayByAddingObject:extractedString];
        }
    }
    return resultArray;
}
+ (NSString *)certificateFromPemFileWithName:(NSString *)name {
    NSRegularExpression *expression = [[NSRegularExpression alloc] initWithPattern:@"-----BEGIN CERTIFICATE-----(.+?)-----END CERTIFICATE-----" options:NSRegularExpressionDotMatchesLineSeparators error:nil];
    return [self itemsFromPemFileWithName:name byRegex:expression].firstObject;
}
+ (NSString *)keyFromPemFileWithName:(NSString *)name {
    NSRegularExpression *expression = [[NSRegularExpression alloc] initWithPattern:@"-----BEGIN(?:[\\w\\s]|)+KEY-----(.+?)-----END(?:[\\w\\s])+KEY-----" options:NSRegularExpressionDotMatchesLineSeparators error:nil];
    return [self itemsFromPemFileWithName:name byRegex:expression].firstObject;
}
+ (NSArray *)itemsFromPemFileWithName:(NSString *)name byRegex:(NSRegularExpression *)expression {
    NSURL *fileURL = [[NSBundle bundleForClass:self.class] URLForResource:name withExtension:@"pem"];
    NSError *error = nil;
    NSString *fileContent = [NSString stringWithContentsOfURL:fileURL encoding:NSUTF8StringEncoding error:&error];
    
    if (error) {
        NSLog(@"%@ error: %@", self.debugDescription, error);
        return nil;
    }
    
    return [self itemsFromPemFileContent:fileContent byRegex:expression];
}
+ (NSString *)stringByRemovingPemHeadersFromString:(NSString *)string {
    NSArray *lines = [string componentsSeparatedByCharactersInSet:[NSCharacterSet newlineCharacterSet]];
    NSArray *linesWithoutHeaders = [lines filteredArrayUsingPredicate:[NSPredicate predicateWithBlock:^BOOL(NSString *_Nullable evaluatedObject, NSDictionary<NSString *,id> * _Nullable bindings) {
        return !([evaluatedObject hasPrefix:@"-----BEGIN"] || [evaluatedObject hasPrefix:@"-----END"]);
    }]];
    return [linesWithoutHeaders componentsJoinedByString:@""];
}
@end

@implementation JWTCryptoSecurity (PublicKey)
/**
 This method strips the x509 from a provided ASN.1 DER public key.
 If the key doesn't contain a header, the DER data is returned as is.

 Supported formats are:

 Headerless:
 SEQUENCE
    INTEGER (1024 or 2048 bit) -- modulo
    INTEGER -- public exponent

 With x509 header:
 SEQUENCE
    SEQUENCE
        OBJECT IDENTIFIER 1.2.840.113549.1.1.1
        NULL
    BIT STRING
        SEQUENCE
        INTEGER (1024 or 2048 bit) -- modulo
        INTEGER -- public exponent

 Example of headerless key:
 https://lapo.it/asn1js/#3082010A0282010100C1A0DFA367FBC2A5FD6ED5A071E02A4B0617E19C6B5AD11BB61192E78D212F10A7620084A3CED660894134D4E475BAD7786FA1D40878683FD1B7A1AD9C0542B7A666457A270159DAC40CE25B2EAE7CCD807D31AE725CA394F90FBB5C5BA500545B99C545A9FE08EFF00A5F23457633E1DB84ED5E908EF748A90F8DFCCAFF319CB0334705EA012AF15AA090D17A9330159C9AFC9275C610BB9B7C61317876DC7386C723885C100F774C19830F475AD1E9A9925F9CA9A69CE0181A214DF2EB75FD13E6A546B8C8ED699E33A8521242B7E42711066AEC22D25DD45D56F94D3170D6F2C25164D2DACED31C73963BA885ADCB706F40866B8266433ED5161DC50E4B3B0203010001

 Example of key with X509 header (notice the additional ASN.1 sequence):
 https://lapo.it/asn1js/#30819F300D06092A864886F70D010101050003818D0030818902818100D0674615A252ED3D75D2A3073A0A8A445F3188FD3BEB8BA8584F7299E391BDEC3427F287327414174997D147DD8CA62647427D73C9DA5504E0A3EED5274A1D50A1237D688486FADB8B82061675ABFA5E55B624095DB8790C6DBCAE83D6A8588C9A6635D7CF257ED1EDE18F04217D37908FD0CBB86B2C58D5F762E6207FF7B92D0203010001
 */
//static func stripPublicKeyHeader(keyData: Data) throws -> Data {
//    let count = keyData.count / MemoryLayout<CUnsignedChar>.size
//
//    guard count > 0 else {
//        throw SwiftyRSAError(message: "Provided public key is empty")
//    }
//
//    var byteArray = [UInt8](repeating: 0, count: count)
//    (keyData as NSData).getBytes(&byteArray, length: keyData.count)
//
//    var index = 0
//    guard byteArray[index] == 0x30 else {
//        throw SwiftyRSAError(message: "Provided key doesn't have a valid ASN.1 structure (first byte should be 0x30 == SEQUENCE)")
//    }
//
//    index += 1
//    if byteArray[index] > 0x80 {
//        index += Int(byteArray[index]) - 0x80 + 1
//    } else {
//        index += 1
//    }
//
//    // If current byte marks an integer (0x02), it means the key doesn't have a X509 header and just
//    // contains its modulo & public exponent. In this case, we can just return the provided DER data as is.
//    if Int(byteArray[index]) == 0x02 {
//        return keyData
//    }
//
//    // Now that we've excluded the possibility of headerless key, we're looking for a valid X509 header sequence.
//    // It should look like this:
//    // 0x30, 0x0d, 0x06, 0x09, 0x2a, 0x86, 0x48, 0x86, 0xf7, 0x0d, 0x01, 0x01, 0x01, 0x05, 0x00
//    guard Int(byteArray[index]) == 0x30 else {
//        throw SwiftyRSAError(message: "Provided key doesn't have a valid X509 header")
//    }
//
//    index += 15
//    if byteArray[index] != 0x03 {
//        throw SwiftyRSAError(message: "Invalid byte at index \(index - 1) (\(byteArray[index - 1])) for public key header")
//    }
//
//    index += 1
//    if byteArray[index] > 0x80 {
//        index += Int(byteArray[index]) - 0x80 + 1
//    } else {
//        index += 1
//    }
//
//    guard byteArray[index] == 0 else {
//        throw SwiftyRSAError(message: "Invalid byte at index \(index - 1) (\(byteArray[index - 1])) for public key header")
//    }
//
//    index += 1
//
//    let strippedKeyBytes = [UInt8](byteArray[index...keyData.count - 1])
//    let data = Data(bytes: UnsafePointer<UInt8>(strippedKeyBytes), count: keyData.count - index)
//
//    return data
//}
typedef NS_ENUM(NSInteger, JWTPublicHeaderStrippingError) {
    JWTPublicHeaderStrippingError_KeyIsEmpty = -200,
    JWTPublicHeaderStrippingError_Invalid_ASN1_Structure,
    JWTPublicHeaderStrippingError_Invalid_X509_Header,
    JWTPublicHeaderStrippingError_Invalid_Byte_At_Index,
};
+ (NSDictionary *)publicHeaderStrippingMessagesAndCodes {
    static NSDictionary *publicHeaderStrippingMessagesAndCodes = nil;
    return publicHeaderStrippingMessagesAndCodes ?:
    (publicHeaderStrippingMessagesAndCodes = @{
        @(JWTPublicHeaderStrippingError_KeyIsEmpty) : @"Provided public key is empty",
        @(JWTPublicHeaderStrippingError_Invalid_ASN1_Structure) : @"Provided key doesn't have a valid ASN.1 structure (first byte should be 0x30 == SEQUENCE)",
        @(JWTPublicHeaderStrippingError_Invalid_X509_Header) : @"Provided key doesn't have a valid X509 header",
        @(JWTPublicHeaderStrippingError_Invalid_Byte_At_Index) : @"Invalid byte at index (index - 1) ((bytes[index - 1])) for public key header. Access as error.userInfo[Parameters][index] or error.userInfo[Parameters][byte]"
    },
     publicHeaderStrippingMessagesAndCodes);
}
+ (NSString *)stringForPublicHeaderStrippingErrorCode:(NSInteger)code {
    return [self publicHeaderStrippingMessagesAndCodes][@(code)] ?: @"Unknown Public Header Stripping Error";
}
+ (NSError*)publicHeaderStrippingErrorForCode:(NSInteger)code parameters:(NSDictionary *)parameters {
    return [NSError errorWithDomain:@"io.jwt.crypto.stripping_public_header" code:code userInfo:@{
                                                                                                  @"Parameters" : parameters ?: [NSNull null],NSLocalizedDescriptionKey: [self stringForPublicHeaderStrippingErrorCode:code]
                                                                                                  }];
}
+ (NSError*)publicHeaderStrippingErrorForCode:(NSInteger)code {
    return [self publicHeaderStrippingErrorForCode:code parameters:nil];
}
+ (NSData *)dataByRemovingPublicKeyHeader:(NSData *)data error:(NSError *__autoreleasing *)error {
    NSError *currentError = nil;
    NSData *currentData = [data copy];
    //    let count = keyData.count / MemoryLayout<CUnsignedChar>.size
    //
    //    guard count > 0 else {
    //        throw SwiftyRSAError(message: "Provided public key is empty")
    //    }
    NSInteger countOfBytes = currentData.length / [JWTMemoryLayout createWithType:[JWTMemoryLayout typeUInt8]].size;
    if (countOfBytes == 0) {
        //        throw SwiftyRSAError(message: "Provided public key is empty")
        currentError = [self publicHeaderStrippingErrorForCode:JWTPublicHeaderStrippingError_KeyIsEmpty];
        if (error) {
            *error = currentError;
        }
        return nil;
    }
    //    var byteArray = [UInt8](repeating: 0, count: count)
    //    (keyData as NSData).getBytes(&byteArray, length: keyData.count)
//    UInt8 *bytes = (UInt8*)malloc(countOfBytes);
    UInt8 *bytes = (UInt8 *)[currentData bytes];
//    memcpy(bytes, [currentData bytes], countOfBytes);

    //    var index = 0
    //    guard byteArray[index] == 0x30 else {
    //        throw SwiftyRSAError(message: "Provided key doesn't have a valid ASN.1 structure (first byte should be 0x30 == SEQUENCE)")
    //    }

    UInt8 index = 0;
    if (bytes[index] != 0x30) {
        //        throw SwiftyRSAError(message: "Provided key doesn't have a valid ASN.1 structure (first byte should be 0x30 == SEQUENCE)")
        currentError = [self publicHeaderStrippingErrorForCode:JWTPublicHeaderStrippingError_Invalid_ASN1_Structure];
        if (error) {
            *error = currentError;
        }
        return nil;
    }

    //    index += 1
    //    if byteArray[index] > 0x80 {
    //        index += Int(byteArray[index]) - 0x80 + 1
    //    } else {
    //        index += 1
    //    }
    index += 1;
    if (bytes[index] > 0x80) {
        index += (SInt8)bytes[index] - 0x80 + 1;
    }
    else {
        index += 1;
    }

    // Headerless!
    //    // If current byte marks an integer (0x02), it means the key doesn't have a X509 header and just
    //    // contains its modulo & public exponent. In this case, we can just return the provided DER data as is.
    //    if Int(byteArray[index]) == 0x02 {
    //        return keyData
    //    }
    if ((SInt8)bytes[index] == 0x02) {
        return currentData;
    }

    // Has header.

    //    // Now that we've excluded the possibility of headerless key, we're looking for a valid X509 header sequence.
    //    // It should look like this:
    //    // 0x30, 0x0d, 0x06, 0x09, 0x2a, 0x86, 0x48, 0x86, 0xf7, 0x0d, 0x01, 0x01, 0x01, 0x05, 0x00
    //    guard Int(byteArray[index]) == 0x30 else {
    //        throw SwiftyRSAError(message: "Provided key doesn't have a valid X509 header")
    //    }
    if ((SInt8)bytes[index] != 0x30) {
        //        throw SwiftyRSAError(message: "Provided key doesn't have a valid X509 header")
        currentError = [self publicHeaderStrippingErrorForCode:JWTPublicHeaderStrippingError_Invalid_X509_Header];
        if (error) {
            *error = currentError;
        }
        return nil;
    }

    //    index += 15
    //    if byteArray[index] != 0x03 {
    //        throw SwiftyRSAError(message: "Invalid byte at index \(index - 1) (\(byteArray[index - 1])) for public key header")
    //    }
    index += 15;
    if (bytes[index] != 0x03) {
        //        throw SwiftyRSAError(message: "Invalid byte at index \(index - 1) (\(byteArray[index - 1])) for public key header")
        currentError = [self publicHeaderStrippingErrorForCode:JWTPublicHeaderStrippingError_Invalid_Byte_At_Index parameters:@{@"index" : @(index - 1), @"byte": @(bytes[index - 1] ?: 0)}];
        if (error) {
            *error = currentError;
        }
        return nil;
    }
    //    index += 1
    //    if byteArray[index] > 0x80 {
    //        index += Int(byteArray[index]) - 0x80 + 1
    //    } else {
    //        index += 1
    //    }
    index += 1;
    if (bytes[index] > 0x80) {
        index += (SInt8)bytes[index] - 0x80 + 1;
    }
    else {
        index += 1;
    }
    //    guard byteArray[index] == 0 else {
    //        throw SwiftyRSAError(message: "Invalid byte at index \(index - 1) (\(byteArray[index - 1])) for public key header")
    //    }
    if (bytes[index] != 0) {
        //        throw SwiftyRSAError(message: "Invalid byte at index \(index - 1) (\(byteArray[index - 1])) for public key header")
        currentError = [self publicHeaderStrippingErrorForCode:JWTPublicHeaderStrippingError_Invalid_Byte_At_Index parameters:@{@"index" : @(index - 1), @"byte": @(bytes[index - 1] ?: 0)}];
        if (error) {
            *error = currentError;
        }
        return nil;
    }
    //    index += 1
    //
    //    let strippedKeyBytes = [UInt8](byteArray[index...keyData.count - 1])
    //    let data = Data(bytes: UnsafePointer<UInt8>(strippedKeyBytes), count: keyData.count - index)
    index += 1;
    NSInteger countOfStrippedBytes = currentData.length - index;
    UInt8 *strippedBytes = (UInt8 *)(bytes + index);
    NSData *resultData = [[NSData alloc] initWithBytes:strippedBytes length:countOfStrippedBytes];
    //    return data
    return resultData;
}
@end
