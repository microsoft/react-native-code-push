//
//  JWTCryptoKey.h
//  JWT
//
//  Created by Lobanov Dmitry on 04.02.17.
//  Copyright © 2017 JWTIO. All rights reserved.
//

#import <Foundation/Foundation.h>
#import <Security/Security.h>

@protocol JWTCryptoKeyProtocol <NSObject>
@property (copy, nonatomic, readonly) NSString *tag;
@property (assign, nonatomic, readonly) SecKeyRef key;
@property (copy, nonatomic, readonly) NSData *rawKey;
@end

@interface JWTCryptoKeyBuilder : NSObject
@property (assign, nonatomic, readonly) NSString *keyType;
- (instancetype)keyTypeRSA;
- (instancetype)keyTypeEC;
@end

@interface JWTCryptoKey : NSObject<JWTCryptoKeyProtocol>
- (instancetype)initWithData:(NSData *)data parameters:(NSDictionary *)parameters error:(NSError *__autoreleasing*)error; //NS_DESIGNATED_INITIALIZER
- (instancetype)initWithBase64String:(NSString *)base64String parameters:(NSDictionary *)parameters error:(NSError *__autoreleasing*)error;
- (instancetype)initWithPemEncoded:(NSString *)encoded parameters:(NSDictionary *)parameters error:(NSError *__autoreleasing*)error;
- (instancetype)initWithPemAtURL:(NSURL *)url parameters:(NSDictionary *)parameters error:(NSError *__autoreleasing*)error;
@end

@interface JWTCryptoKey (Parameters)
+ (NSString *)parametersKeyBuilder;
@end

@interface JWTCryptoKeyPublic : JWTCryptoKey
- (instancetype)initWithCertificateData:(NSData *)certificateData parameters:(NSDictionary *)parameters error:(NSError *__autoreleasing*)error; //NS_DESIGNATED_INITIALIZER;
- (instancetype)initWithCertificateBase64String:(NSString *)certificateString parameters:(NSDictionary *)parameters error:(NSError *__autoreleasing*)error;
@end

@interface JWTCryptoKeyPrivate : JWTCryptoKey
- (instancetype)initWithP12Data:(NSData *)p12Data withPassphrase:(NSString *)passphrase parameters:(NSDictionary *)parameters error:(NSError *__autoreleasing*)error; //NS_DESIGNATED_INITIALIZER;
- (instancetype)initWithP12AtURL:(NSURL *)url withPassphrase:(NSString *)passphrase parameters:(NSDictionary *)parameters error:(NSError *__autoreleasing*)error;
@end
