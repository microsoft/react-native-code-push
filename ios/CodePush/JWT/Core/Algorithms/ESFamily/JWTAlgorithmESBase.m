//
//  JWTAlgorithmESBase.m
//  Pods
//
//  Created by Lobanov Dmitry on 12.02.17.
//
//

#import "JWTAlgorithmESBase.h"
#import <CommonCrypto/CommonCryptor.h>
@interface JWTAlgorithmESBase ()

@end
@implementation JWTAlgorithmESBase
@synthesize keyExtractorType;
@synthesize signKey;
@synthesize verifyKey;
@end

// thanks! https://github.com/soyersoyer/SwCrypt
@interface JWTAlgorithmESBase (ImportKeys)
- (void)importKey;
//importKey(publicKey, format: .importKeyBinary, keyType: .keyPublic)
@end
@implementation JWTAlgorithmESBase (ImportKeys)
- (void)importKey {
    return;
}
@end

@implementation JWTAlgorithmESBase (JWTAsymmetricKeysAlgorithm)
- (NSString *)name {
    return @"ESBase";
}
- (NSData *)signHash:(NSData *)hash key:(NSData *)key error:(NSError *__autoreleasing *)error {
    return nil;
}
- (BOOL)verifyHash:(NSData *)hash signature:(NSData *)signature key:(NSData *)key error:(NSError *__autoreleasing *)error {
    return NO;
}
@end
