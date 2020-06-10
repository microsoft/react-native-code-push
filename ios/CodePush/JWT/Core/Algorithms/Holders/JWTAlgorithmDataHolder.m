//
//  JWTAlgorithmDataHolder.m
//  JWT
//
//  Created by Lobanov Dmitry on 31.08.16.
//  Copyright © 2016 Karma. All rights reserved.
//

#import "JWTAlgorithmDataHolder.h"
#import "JWTAlgorithmFactory.h"
#import "JWTAlgorithmNone.h"
#import "JWTRSAlgorithm.h"
#import "JWTAlgorithmHSBase.h"
#import "JWTAlgorithmRSBase.h"
#import "JWTBase64Coder.h"

@interface JWTAlgorithmBaseDataHolder()
// not needed by algorithm adoption.
// @property (copy, nonatomic, readwrite) NSData *internalSecretData;
// @property (strong, nonatomic, readwrite) id <JWTAlgorithm> internalAlgorithm;

#pragma mark - Setters
/**
 Sets jwtSecret and returns the JWTAlgorithmBaseDataHolder to allow for method chaining
 */
@property (copy, nonatomic, readwrite) JWTAlgorithmBaseDataHolder *(^secret)(NSString *secret);

/**
 Sets jwtSecretData and returns the JWTAlgorithmBaseDataHolder to allow for method chaining
 */
@property (copy, nonatomic, readwrite) JWTAlgorithmBaseDataHolder *(^secretData)(NSData *secretData);

/**
 Sets jwtAlgorithm and returns the JWTAlgorithmBaseDataHolder to allow for method chaining
 */
@property (copy, nonatomic, readwrite) JWTAlgorithmBaseDataHolder *(^algorithm)(id<JWTAlgorithm>algorithm);

/**
 Sets jwtAlgorithmName and returns the JWTAlgorithmBaseDataHolder to allow for method chaining. See list of names in appropriate headers.
 */
@property (copy, nonatomic, readwrite) JWTAlgorithmBaseDataHolder *(^algorithmName)(NSString *algorithmName);

/**
 Sets stringCoder and returns the JWTAlgorithmBaseDataHolder to allow for method chaining. See list of names in appropriate headers.
 */
@property (copy, nonatomic, readwrite) JWTAlgorithmBaseDataHolder *(^stringCoder)(id<JWTStringCoder__Protocol> stringCoder);
@end

@interface JWTAlgorithmBaseDataHolder (Convertions)

- (NSData *)dataFromString:(NSString *)string;
- (NSString *)stringFromData:(NSData *)data;

@end

@implementation JWTAlgorithmBaseDataHolder (Convertions)
#pragma mark - Convertions
- (NSData *)dataFromString:(NSString *)string {
    NSData *result = [self.internalStringCoder dataWithString:string];
    
    if (result == nil) {
        // tell about it?!
        NSLog(@"%@ %@ something went wrong. Data is not base64encoded", self.debugDescription, NSStringFromSelector(_cmd));
    }
    
    return result;// ?: [string dataUsingEncoding:NSUTF8StringEncoding];
}

- (NSString *)stringFromData:(NSData *)data {
    NSString *result = [self.internalStringCoder stringWithData:data];
    
    if (result == nil) {
        NSLog(@"%@ %@ something went wrong. String is not base64encoded", self.debugDescription, NSStringFromSelector(_cmd));
    }
    return result ?: [[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding];
}
@end

@interface JWTAlgorithmBaseDataHolder (Fluent)
- (void)setupFluent;
@end

@implementation JWTAlgorithmBaseDataHolder (Fluent)
#pragma mark - Fluent
- (instancetype)secretData:(NSData *)secretData {
    self.internalSecretData = secretData;
    return self;
}

- (instancetype)secret:(NSString *)secret {
    self.internalSecretData = [self dataFromString:secret];
    return self;
}

- (instancetype)algorithm:(id<JWTAlgorithm>)algorithm {
    self.internalAlgorithm = algorithm;
    return self;
}

- (instancetype)algorithmName:(NSString *)algorithmName {
    self.internalAlgorithm = [JWTAlgorithmFactory algorithmByName:algorithmName];
    return self;
}

- (instancetype)stringCoder:(id<JWTStringCoder__Protocol>)stringCoder {
    self.internalStringCoder = stringCoder;
    return self;
}

- (void)setupFluent {
    __weak typeof(self) weakSelf = self;
    self.secret = ^(NSString *secret) {
        return [weakSelf secret:secret];
    };
    
    self.secretData = ^(NSData *secretData) {
        return [weakSelf secretData:secretData];
    };
    
    self.algorithm = ^(id<JWTAlgorithm> algorithm) {
        return [weakSelf algorithm:algorithm];
    };
    
    self.algorithmName = ^(NSString *algorithmName) {
        return [weakSelf algorithmName:algorithmName];
    };

    self.stringCoder = ^(id <JWTStringCoder__Protocol> stringCoder) {
        return [weakSelf stringCoder:stringCoder];
    };
}
@end

@interface JWTAlgorithmBaseDataHolder (Debug)
- (NSDictionary *)debugInformation;
@end
@implementation JWTAlgorithmBaseDataHolder (Debug)
- (NSString *)debugDescription {
    return [[self debugInformation] debugDescription];
}
- (NSDictionary *)debugInformation {
    return @{
             @"algorithmName" : self.internalAlgorithmName ?: @"unknown",
             @"algorithm" : [self.internalAlgorithm debugDescription] ?: @"unknown",
             @"secretData" : [self.internalSecretData debugDescription] ?: @"unknown",
             @"stringCoder" : [self.internalStringCoder debugDescription] ?: @"unknown"
             };
}

@end

@implementation JWTAlgorithmBaseDataHolder
@synthesize internalAlgorithm;
@synthesize internalSecretData;
@synthesize internalStringCoder = _internalStringCoder;

- (id<JWTStringCoder__Protocol>)internalStringCoder {
    return _internalStringCoder ?: [JWTBase64Coder new];
}

#pragma mark - Custom Getters
- (NSString *)internalAlgorithmName {
    return [self.internalAlgorithm name];
}

- (NSString *)internalSecret {
    return [self stringFromData:self.internalSecretData];
}

- (instancetype)init {
    self = [super init];
    if (self) {
        [self setupFluent];
    }
    return self;
}

#pragma mark - Copy
- (id)copyWithZone:(NSZone *)zone {
    JWTAlgorithmBaseDataHolder *holder = [self.class new];
    holder.internalAlgorithm = self.internalAlgorithm;
    holder.internalSecretData = self.internalSecretData;
    holder.internalStringCoder = self.internalStringCoder;
    return holder;
}
@end

@interface JWTAlgorithmBaseDataHolder(Create)
- (instancetype)initWithAlgorithmName:(NSString *)name;
@end

@implementation JWTAlgorithmBaseDataHolder(Create)
- (instancetype)initWithAlgorithmName:(NSString *)name {
    return [self init].algorithmName(name);
}
@end

@implementation JWTAlgorithmNoneDataHolder
- (instancetype)init {
    if (self = [super init]) {
        self.internalAlgorithm = [JWTAlgorithmFactory algorithmByName:JWTAlgorithmNameNone];
        self.internalSecretData = nil;
    }
    return self;
}
@end

@implementation JWTAlgorithmHSFamilyDataHolder
+ (instancetype)createWithAlgorithm256 {
    return [[self alloc] initWithAlgorithmName:JWTAlgorithmNameHS256];
}
+ (instancetype)createWithAlgorithm384 {
    return [[self alloc] initWithAlgorithmName:JWTAlgorithmNameHS384];
}
+ (instancetype)createWithAlgorithm512 {
    return [[self alloc] initWithAlgorithmName:JWTAlgorithmNameHS512];
}
@end

@interface JWTAlgorithmRSFamilyDataHolder()
#pragma mark - Getters
@property (copy, nonatomic, readwrite) NSString *internalPrivateKeyCertificatePassphrase;
@property (copy, nonatomic, readwrite) NSString *internalKeyExtractorType;
@property (strong, nonatomic, readwrite) id<JWTCryptoKeyProtocol> internalSignKey;
@property (strong, nonatomic, readwrite) id<JWTCryptoKeyProtocol> internalVerifyKey;
#pragma mark - Setters
@property (copy, nonatomic, readwrite) JWTAlgorithmRSFamilyDataHolder *(^privateKeyCertificatePassphrase)(NSString *privateKeyCertificatePassphrase);
@property (copy, nonatomic, readwrite) JWTAlgorithmRSFamilyDataHolder *(^keyExtractorType)(NSString *keyExtractorType);
@property (copy, nonatomic, readwrite) JWTAlgorithmRSFamilyDataHolder *(^signKey)(id<JWTCryptoKeyProtocol> signKey);
@property (copy, nonatomic, readwrite) JWTAlgorithmRSFamilyDataHolder *(^verifyKey)(id<JWTCryptoKeyProtocol> verifyKey);
@end

@implementation JWTAlgorithmRSFamilyDataHolder (Debug)
- (NSDictionary *)debugInformation {
    NSDictionary *add = @{@"privateKeyCertificatePassphrase" : self.internalPrivateKeyCertificatePassphrase ?: @"unknown",
                          @"keyExtractorType" : self.internalKeyExtractorType ?: @"unknown",
                          @"signKey" : [self.signKey debugDescription] ?: @"unknown",
                          @"verifyKey" : [self.verifyKey debugDescription] ?: @"unknown"
                          };
    NSMutableDictionary *result = [[super debugInformation] mutableCopy];
    [result addEntriesFromDictionary:add];
    return result;
}
@end

@implementation JWTAlgorithmRSFamilyDataHolder

#pragma mark - Initialization
+ (instancetype)createWithAlgorithm256 {
    return [[self alloc] initWithAlgorithmName:JWTAlgorithmNameRS256];
}

+ (instancetype)createWithAlgorithm384 {
    return [[self alloc] initWithAlgorithmName:JWTAlgorithmNameRS384];
}

+ (instancetype)createWithAlgorithm512 {
    return [[self alloc] initWithAlgorithmName:JWTAlgorithmNameRS512];
}

#pragma mark - Getters
- (id<JWTAlgorithm>)internalAlgorithm {
    id <JWTAlgorithm> algorithm = [super internalAlgorithm];
    if ([algorithm conformsToProtocol:@protocol(JWTRSAlgorithm)]) {
        // copy?
        id<JWTRSAlgorithm>currentAlgorithm = [(id <JWTRSAlgorithm>)algorithm copyWithZone:nil];
        currentAlgorithm.privateKeyCertificatePassphrase = self.internalPrivateKeyCertificatePassphrase;
        currentAlgorithm.keyExtractorType = self.internalKeyExtractorType;
        currentAlgorithm.signKey = self.internalSignKey;
        currentAlgorithm.verifyKey = self.internalVerifyKey;
        algorithm = currentAlgorithm;
    }
    return algorithm;
}

#pragma mark - Setters
- (instancetype)privateKeyCertificatePassphrase:(NSString *)passphrase {
    self.internalPrivateKeyCertificatePassphrase = passphrase;
    return self;
}
- (instancetype)keyExtractorType:(NSString *)type {
    self.internalKeyExtractorType = type;
    return self;
}
- (instancetype)signKey:(id<JWTCryptoKeyProtocol>)key {
    self.internalSignKey = key;
    return self;
}
- (instancetype)verifyKey:(id<JWTCryptoKeyProtocol>)key {
    self.internalVerifyKey = key;
    return self;
}

#pragma mark - Copy
- (id)copyWithZone:(NSZone *)zone {
    JWTAlgorithmRSFamilyDataHolder *holder = [super copyWithZone:zone];
    holder.internalPrivateKeyCertificatePassphrase = self.internalPrivateKeyCertificatePassphrase;
    holder.internalKeyExtractorType = self.internalKeyExtractorType;
    holder.internalSignKey = self.internalSignKey;
    holder.internalVerifyKey = self.internalVerifyKey;
    return holder;
}
@end

@implementation JWTAlgorithmRSFamilyDataHolder (Fluent)
- (void)setupFluent {
    [super setupFluent];
    __weak typeof(self) weakSelf = self;
    self.privateKeyCertificatePassphrase = ^(NSString *privateKeyCertificatePassphrase) {
        return [weakSelf privateKeyCertificatePassphrase:privateKeyCertificatePassphrase];
    };
    self.keyExtractorType = ^(NSString *keyExtractorType) {
        return [weakSelf keyExtractorType:keyExtractorType];
    };
    self.signKey = ^(id<JWTCryptoKeyProtocol> key){
        return [weakSelf signKey:key];
    };
    self.verifyKey = ^(id<JWTCryptoKeyProtocol> key){
        return [weakSelf verifyKey:key];
    };
}
@end
