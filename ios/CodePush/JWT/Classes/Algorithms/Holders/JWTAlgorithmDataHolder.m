//
//  JWTAlgorithmDataHolder.m
//  JWT
//
//  Created by Lobanov Dmitry on 31.08.16.
//  Copyright © 2016 Karma. All rights reserved.
//

#import "JWTAlgorithmDataHolder.h"
#import "JWTAlgorithmFactory.h"
@interface JWTAlgorithmBaseDataHolder()
// not needed by algorithm adoption.
// @property (copy, nonatomic, readwrite) NSData *currentSecretData;
// @property (strong, nonatomic, readwrite) id <JWTAlgorithm> currentAlgorithm;

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

@end

@implementation JWTAlgorithmBaseDataHolder
#pragma mark - Convertions
- (NSData *)dataFromString:(NSString *)string {
    return [string dataUsingEncoding:NSUTF8StringEncoding];
}

- (NSString *)stringFromData:(NSData *)data {
    return [[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding];
}

#pragma mark - Fluent
- (instancetype)secretData:(NSData *)secretData {
    self.currentSecretData = secretData;
    return self;
}

- (instancetype)secret:(NSString *)secret {
    self.currentSecretData = [self dataFromString:secret];
    return self;
}

- (instancetype)algorithm:(id<JWTAlgorithm>)algorithm {
    self.currentAlgorithm = algorithm;
    return self;
}

- (instancetype)algorithmName:(NSString *)algorithmName {
    self.currentAlgorithm = [JWTAlgorithmFactory algorithmByName:algorithmName];
    return self;
}

#pragma mark - Custom Getters
- (NSString *)currentAlgorithmName {
    return [self.algorithm name];
}

- (NSString *)currentSecret {
    return [self stringFromData:self.currentSecretData];
}

- (instancetype)init {
    self = [super init];
    if (self) {
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
    }
    return self;
}

@end

@interface JWTAlgorithmRSFamilyDataHolder()
#pragma mark - Getters
@property (copy, nonatomic, readwrite) NSString *currentPrivateKeyCertificatePassphrase;

#pragma mark - Setters
@property (copy, nonatomic, readwrite) JWTAlgorithmRSFamilyDataHolder *(^privateKeyCertificatePassphrase)(NSString *privateKeyCertificatePassphrase);
@end

@implementation JWTAlgorithmRSFamilyDataHolder
- (instancetype)privateKeyCertificatePassphrase:(NSString *)passphrase {
    self.currentPrivateKeyCertificatePassphrase = passphrase;
    return self;
}

- (instancetype)init {
    self = [super init];
    if (self) {
        __weak typeof(self) weakSelf = self;
        self.privateKeyCertificatePassphrase = ^(NSString *privateKeyCertificatePassphrase) {
            return [weakSelf privateKeyCertificatePassphrase:privateKeyCertificatePassphrase];
        };
    }
    return self;
}

@end