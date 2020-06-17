//
//  JWTCryptoKeyExtractor.h
//  JWT
//
//  Created by Lobanov Dmitry on 04.02.17.
//  Copyright © 2017 JWTIO. All rights reserved.
//

#import <Foundation/Foundation.h>
#import <Security/Security.h>

@protocol JWTCryptoKeyProtocol;
@protocol JWTCryptoKeyExtractorProtocol <NSObject>
@optional
- (id<JWTCryptoKeyProtocol>)keyFromString:(NSString *)string parameters:(NSDictionary *)parameters error:(NSError *__autoreleasing*)error;
- (id<JWTCryptoKeyProtocol>)keyFromData:(NSData *)data parameters:(NSDictionary *)parameters error:(NSError *__autoreleasing*)error;
@end

@interface JWTCryptoKeyExtractor : NSObject <JWTCryptoKeyExtractorProtocol>
@property (copy, nonatomic, readonly) NSString *type;
+ (NSString *)type;
+ (NSString *)parametersKeyCertificatePassphrase;
@end

@interface JWTCryptoKeyExtractor (ClassCluster)
+ (instancetype)publicKeyWithCertificate;
+ (instancetype)privateKeyInP12;
+ (instancetype)publicKeyWithPEMBase64;
+ (instancetype)privateKeyWithPEMBase64;
+ (instancetype)createWithType:(NSString *)type;
@end
