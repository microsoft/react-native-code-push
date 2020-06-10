//
//  JWTCoding+VersionThree.m
//  JWT
//
//  Created by Lobanov Dmitry on 27.11.16.
//  Copyright © 2016 JWTIO. All rights reserved.
//

#import "JWTCoding+VersionThree.h"
#import "JWTAlgorithmDataHolderChain.h"
#import "JWTRSAlgorithm.h"
#import "JWTCoding+ResultTypes.h"
#import "JWTAlgorithmFactory.h"
#import "JWTErrorDescription.h"
#import "JWTBase64Coder.h"
#import "JWTClaimsSetSerializer.h"
#import "JWTClaimsSetVerifier.h"

@implementation JWT (VersionThree)
+ (JWTEncodingBuilder *)encodeWithHolders:(NSArray *)holders {
    return [JWTEncodingBuilder createWithHolders:holders];
}
+ (JWTEncodingBuilder *)encodeWithChain:(JWTAlgorithmDataHolderChain *)chain {
    return [JWTEncodingBuilder createWithChain:chain];
}
+ (JWTDecodingBuilder *)decodeWithHolders:(NSArray *)holders {
    return [JWTDecodingBuilder createWithHolders:holders];
}
+ (JWTDecodingBuilder *)decodeWithChain:(JWTAlgorithmDataHolderChain *)chain {
    return [JWTDecodingBuilder createWithChain:chain];
}
@end

@interface JWTCodingBuilder ()
#pragma mark - Internal
@property (strong, nonatomic, readwrite) JWTAlgorithmDataHolderChain *internalChain;
@property (copy, nonatomic, readwrite) NSNumber *internalOptions;

#pragma mark - Fluent
@property (copy, nonatomic, readwrite) JWTCodingBuilder *(^chain)(JWTAlgorithmDataHolderChain *chain);
@property (copy, nonatomic, readwrite) JWTCodingBuilder *(^constructChain)(JWTAlgorithmDataHolderChain *(^block)());
@property (copy, nonatomic, readwrite) JWTCodingBuilder *(^modifyChain)(JWTAlgorithmDataHolderChain *(^block)(JWTAlgorithmDataHolderChain * chain));
@property (copy, nonatomic, readwrite) JWTCodingBuilder *(^options)(NSNumber *options);
@property (copy, nonatomic, readwrite) JWTCodingBuilder *(^addHolder)(id<JWTAlgorithmDataHolderProtocol> holder);
@property (copy, nonatomic, readwrite) JWTCodingBuilder *(^constructHolder)(id<JWTAlgorithmDataHolderProtocol>(^block)(id<JWTAlgorithmDataHolderProtocol> holder));

@end
@interface JWTCodingBuilder (Fluent_Setup)
- (instancetype)chain:(JWTAlgorithmDataHolderChain *)chain;
- (instancetype)options:(NSNumber *)options;
- (instancetype)addHolder:(id<JWTAlgorithmDataHolderProtocol>)holder;
- (void)setupFluent;
@end

@implementation JWTCodingBuilder (Fluent_Setup)
- (instancetype)chain:(JWTAlgorithmDataHolderChain *)chain {
    self.internalChain = chain;
    return self;
}
- (instancetype)options:(NSNumber *)options {
    self.internalOptions = options;
    return self;
}
- (instancetype)addHolder:(id<JWTAlgorithmDataHolderProtocol>)holder {
    self.internalChain = [self.internalChain chainByAppendingHolder:holder];
    return self;
}
- (void)setupFluent {
    __weak typeof(self) weakSelf = self;
    self.chain = ^(JWTAlgorithmDataHolderChain *chain) {
        return [weakSelf chain:chain];
    };
    
    self.constructChain = ^(JWTAlgorithmDataHolderChain *(^block)()) {
        if (block) {
            JWTAlgorithmDataHolderChain *chain = block();
            return [weakSelf chain:chain];
        }
        return weakSelf;
    };
    
    self.modifyChain = ^(JWTAlgorithmDataHolderChain *(^block)(JWTAlgorithmDataHolderChain *chain)) {
        if (block) {
            JWTAlgorithmDataHolderChain *chain = block(weakSelf.internalChain);
            return [weakSelf chain:chain];
        }
        return weakSelf;
    };


    self.options = ^(NSNumber *options) {
        return [weakSelf options:options];
    };
    
    self.addHolder = ^(id<JWTAlgorithmDataHolderProtocol> holder) {
        return [weakSelf addHolder:holder];
    };
    
    self.constructHolder = ^(id<JWTAlgorithmDataHolderProtocol> (^block)(id<JWTAlgorithmDataHolderProtocol> holder)) {
        if (block) {
            [weakSelf addHolder:block([JWTAlgorithmBaseDataHolder new])];
        }
        return weakSelf;
    };
}
@end

@implementation JWTCodingBuilder
#pragma mark - Getters
// Chain always exists
- (JWTAlgorithmDataHolderChain *)internalChain {
    return _internalChain ?: (_internalChain = [JWTAlgorithmDataHolderChain new], _internalChain);
}
#pragma mark - Create
- (instancetype)initWithChain:(JWTAlgorithmDataHolderChain *)chain {
    if (self = [super init]) {
        self.internalChain = chain;
        [self setupFluent];
    }
    return self;
}
+ (instancetype)createWithHolders:(NSArray *)items {
    return [self createWithChain:[[JWTAlgorithmDataHolderChain alloc] initWithHolders:items]];
}
+ (instancetype)createWithChain:(JWTAlgorithmDataHolderChain *)chain {
    return [[self alloc] initWithChain:chain];
}
+ (instancetype)createWithEmptyChain {
    return [self createWithChain:nil];
}
@end

@implementation JWTCodingBuilder (Sugar)
- (instancetype)and {
    return self;
}
- (instancetype)with {
    return self;
}
@end

@interface JWTEncodingBuilder ()
#pragma mark - Internal
@property (copy, nonatomic, readwrite) NSDictionary *internalPayload;
@property (copy, nonatomic, readwrite) NSDictionary *internalHeaders;
@property (strong, nonatomic, readwrite) JWTClaimsSet *internalClaimsSet;
@property (copy, nonatomic, readwrite) NSDictionary *internalMixingClaimsPayload;

#pragma mark - Fluent
@property (copy, nonatomic, readwrite) JWTEncodingBuilder *(^payload)(NSDictionary *payload);
@property (copy, nonatomic, readwrite) JWTEncodingBuilder *(^headers)(NSDictionary *headers);
@property (copy, nonatomic, readwrite) JWTEncodingBuilder *(^claimsSet)(JWTClaimsSet *claimsSet);
@end

@interface JWTEncodingBuilder (Fluent_Setup)
- (instancetype)payload:(NSDictionary *)payload;
- (instancetype)headers:(NSDictionary *)headers;
- (instancetype)claimsSet:(JWTClaimsSet *)claimsSet;
@end

@implementation JWTEncodingBuilder (Fluent_Setup)

- (instancetype)payload:(NSDictionary *)payload {
    self.internalPayload = payload;
    return self;
}
- (instancetype)headers:(NSDictionary *)headers {
    self.internalHeaders = headers;
    return self;
}

- (instancetype)claimsSet:(JWTClaimsSet *)claimsSet {
    self.internalClaimsSet = claimsSet;
    return self;
}

- (void)setupFluent {
    [super setupFluent];
    __weak typeof(self) weakSelf = self;
    self.payload = ^(NSDictionary *payload) {
        return [weakSelf payload:payload];
    };
    self.headers = ^(NSDictionary *headers) {
        return [weakSelf headers:headers];
    };
    self.claimsSet = ^(JWTClaimsSet *claimsSet) {
        return [weakSelf claimsSet:claimsSet];
    };
}

@end

@implementation JWTEncodingBuilder
#pragma mark - Getters
- (NSDictionary *)internalMixingClaimsPayload {
    NSMutableDictionary *dictionary = [@{} mutableCopy];
    if (_internalPayload) {
        [dictionary addEntriesFromDictionary:_internalPayload];
    }
    
    if (_internalClaimsSet) {
        [dictionary addEntriesFromDictionary:[JWTClaimsSetSerializer dictionaryWithClaimsSet:_internalClaimsSet]];
    }
    
    return dictionary;
}

#pragma mark - Create
+ (instancetype)encodePayload:(NSDictionary *)payload {
    return ((JWTEncodingBuilder *)[self createWithEmptyChain]).payload(payload);
}

+ (instancetype)encodeClaimsSet:(JWTClaimsSet *)claimsSet {
    return ((JWTEncodingBuilder *)[self createWithEmptyChain]).claimsSet(claimsSet);
}

@end

@implementation JWTEncodingBuilder (Coding)
- (JWTCodingResultType *)encode {
    
    NSDictionary *headers = self.internalHeaders;
    NSDictionary *payload = self.internalMixingClaimsPayload;
    
    NSString *encodedMessage = nil;
    NSError *error = nil;
    
    NSArray *holders = self.internalChain.holders;
    // ERROR: HOLDERS ARE EMPTY.
    if (holders.count == 0) {
        error = [JWTErrorDescription errorWithCode:JWTDecodingHoldersChainEmptyError];
    }
    
    for (id <JWTAlgorithmDataHolderProtocol>holder in holders) {
        id <JWTAlgorithm>algorithm = holder.internalAlgorithm;
        NSData *secretData = holder.internalSecretData;
        encodedMessage = [self encodeWithAlgorithm:algorithm withHeaders:headers withPayload:payload withSecretData:secretData withError:&error];
        if (encodedMessage && (error == nil)) {
            break;
        }
    }
    
    JWTCodingResultType *result = nil;
    
    if (error) {
        result = [[JWTCodingResultType alloc] initWithErrorResult:[[JWTCodingResultTypeError alloc] initWithError:error]];
    }
    else if (encodedMessage) {
        result = [[JWTCodingResultType alloc] initWithSuccessResult:[[JWTCodingResultTypeSuccess alloc] initWithEncoded:encodedMessage]];
    }
    else {
        NSLog(@"%@ something went wrong! result is nil!", self.debugDescription);
    }
    
    return result;
}
- (NSString *)encodeWithAlgorithm:(id<JWTAlgorithm>)theAlgorithm withHeaders:(NSDictionary *)theHeaders withPayload:(NSDictionary *)thePayload withSecretData:(NSData *)theSecretData withError:(NSError *__autoreleasing *)theError {
    // do it!
    
    if (!theAlgorithm) {
        if (theError) {
            *theError = [JWTErrorDescription errorWithCode:JWTUnspecifiedAlgorithmError];
        }
        return nil;
    }

    NSString *theAlgorithmName = [theAlgorithm name];
    
    if (!theAlgorithmName) {
        if (theError) {
            *theError = [JWTErrorDescription errorWithCode:JWTUnsupportedAlgorithmError];
        }
        return nil;
    }
    
    NSDictionary *header = @{
                             @"alg": theAlgorithmName,
                             @"typ": @"JWT"
                             };
    NSMutableDictionary *allHeaders = [header mutableCopy];

    if (theHeaders.allKeys.count > 0) {
        [allHeaders addEntriesFromDictionary:theHeaders];
    }

    NSString *headerSegment = [self encodeSegment:[allHeaders copy] withError:nil];
    
    if (!headerSegment) {
        // encode header segment error
        if (theError) {
            *theError = [JWTErrorDescription errorWithCode:JWTEncodingHeaderError];
        }
        return nil;
    }
    
    NSString *payloadSegment = [self encodeSegment:thePayload withError:nil];
    
    if (!payloadSegment) {
        // encode payment segment error
        if (theError) {
            *theError = [JWTErrorDescription errorWithCode:JWTEncodingPayloadError];
        }
        return nil;
    }

    NSString *signingInput = [@[headerSegment, payloadSegment] componentsJoinedByString:@"."];

    NSString *signedOutput = nil;

    // this happens somewhere outside.

    NSError *algorithmError = nil;
    if (theSecretData && [theAlgorithm respondsToSelector:@selector(signHash:key:error:)]) {
          NSData *signedOutputData = [theAlgorithm signHash:[signingInput dataUsingEncoding:NSUTF8StringEncoding] key:theSecretData error:&algorithmError];
        signedOutput = [JWTBase64Coder base64UrlEncodedStringWithData:signedOutputData];
    }
//    if (theSecretData && [theAlgorithm respondsToSelector:@selector(encodePayloadData:withSecret:)]) {
//        // not sure that it is correct.
//        NSData *signedOutputData = [theAlgorithm encodePayloadData:[signingInput dataUsingEncoding:NSUTF8StringEncoding] withSecret:theSecretData];
//        signedOutput = [JWTBase64Coder base64UrlEncodedStringWithData:signedOutputData];
//    }
    // not used now.
//    else {
//        NSData *signedOutputData = [theAlgorithm encodePayload:signingInput withSecret:self.jwtSecret];
//        signedOutput = [JWTBase64Coder base64UrlEncodedStringWithData:signedOutputData];
//    }

    if (algorithmError) {
        // algorithmError
        if (theError) {
            *theError = algorithmError;
        }
        return nil;
    }
    if (!signedOutput) {
        // Make sure signing worked (e.g. we may have issues extracting the key from the PKCS12 bundle if passphrase is incorrect)
        if (theError) {
            *theError = [JWTErrorDescription errorWithCode:JWTEncodingSigningError];
        }
        return nil;
    }
    
    return [@[headerSegment, payloadSegment, signedOutput] componentsJoinedByString:@"."];
}

- (NSString *)encodeSegment:(id)theSegment withError:(NSError *__autoreleasing*)error {
    NSData *encodedSegmentData = nil;
    
    if (theSegment) {
        encodedSegmentData = [NSJSONSerialization dataWithJSONObject:theSegment options:0 error:error];
    }
    else {
        // error!
        NSError *generatedError = [JWTErrorDescription errorWithCode:JWTInvalidSegmentSerializationError];
        if (error) {
            *error = generatedError;
        }
        NSLog(@"%@ Could not encode segment: %@", self.class, generatedError.localizedDescription);
        return nil;
    }
    
    NSString *encodedSegment = nil;
    
    if (encodedSegmentData) {
        encodedSegment = [JWTBase64Coder base64UrlEncodedStringWithData:encodedSegmentData];
    }
    
    return encodedSegment;
}

- (JWTCodingResultType *)result {
    return self.encode;
}
@end

@interface JWTDecodingBuilder ()
#pragma mark - Internal
@property (copy, nonatomic, readwrite) NSString *internalMessage;
@property (nonatomic, readwrite) JWTClaimsSet *internalClaimsSet;

#pragma mark - Fluent
@property (copy, nonatomic, readwrite) JWTDecodingBuilder *(^message)(NSString *message);
@property (copy, nonatomic, readwrite) JWTDecodingBuilder *(^claimsSet)(JWTClaimsSet *claimsSet);

@end

@interface JWTDecodingBuilder (Fluent_Setup)
- (instancetype)message:(NSString *)message;
- (instancetype)claimsSet:(JWTClaimsSet *)claimsSet;
@end
@implementation JWTDecodingBuilder (Fluent_Setup)
- (instancetype)message:(NSString *)message {
    self.internalMessage = message;
    return self;
}
- (instancetype)claimsSet:(JWTClaimsSet *)claimsSet {
    self.internalClaimsSet = claimsSet;
    return self;
}
- (void)setupFluent {
    [super setupFluent];
    __weak typeof(self) weakSelf = self;
    self.message = ^(NSString *message) {
        return [weakSelf message:message];
    };
    self.claimsSet = ^(JWTClaimsSet *claimsSet) {
        return [weakSelf claimsSet:claimsSet];
    };
}
@end

@implementation JWTDecodingBuilder
#pragma mark - Create
+ (instancetype)decodeMessage:(NSString *)message {
    return ((JWTDecodingBuilder *)[self createWithEmptyChain]).message(message);
}
@end

@implementation JWTDecodingBuilder (Coding)
- (JWTCodingResultType *)decode {
    // do!
    // iterate over items in chain!
    // and return if everything ok!
    // or return error!
    NSError *error = nil;
    NSDictionary *decodedDictionary = nil;
    NSString *message = self.internalMessage;
    NSNumber *options = self.internalOptions;
    NSArray *holders = self.internalChain.holders;
    JWTClaimsSet *claimsSet = self.internalClaimsSet;
    
    // ERROR: HOLDERS ARE EMPTY.
    if (holders.count == 0) {
        error = [JWTErrorDescription errorWithCode:JWTDecodingHoldersChainEmptyError];
    }
    
    for (id <JWTAlgorithmDataHolderProtocol>holder in self.internalChain.holders) {
        // try decode!
        id <JWTAlgorithm> algorithm = holder.internalAlgorithm;
        NSData *secretData = holder.internalSecretData;
        // try to retrieve passphrase.
        decodedDictionary = [self decodeMessage:message secretData:secretData algorithm:algorithm options:options error:&error];
        if (decodedDictionary && (error == nil)) {
            break;
        }
    }
    
    // claimsSet verification.
    JWTCodingResultType *result = nil;
    if (error) {
        return [[JWTCodingResultType alloc] initWithErrorResult:[[JWTCodingResultTypeError alloc] initWithError:error]];
    }
    
    if (claimsSet) {
        BOOL claimsVerified = [JWTClaimsSetVerifier verifyClaimsSet:[JWTClaimsSetSerializer claimsSetWithDictionary:decodedDictionary[JWTCodingResultPayload]] withTrustedClaimsSet:claimsSet];
        if (!claimsVerified){
            error = [JWTErrorDescription errorWithCode:JWTClaimsSetVerificationFailed];
            return [[JWTCodingResultType alloc] initWithErrorResult:[[JWTCodingResultTypeError alloc] initWithError:error]];
        }
    }
    
    if (decodedDictionary) {
        NSDictionary *headers = decodedDictionary[JWTCodingResultHeaders];
        NSDictionary *payload = decodedDictionary[JWTCodingResultPayload];
        result = [[JWTCodingResultType alloc] initWithSuccessResult:[[JWTCodingResultTypeSuccess alloc] initWithHeaders:headers withPayload:payload]];
    }
    else {
        NSLog(@"%@ something went wrong! result is nil!", self.debugDescription);
    }
    
    return result;
}

// Maybe later add algorithmName
- (NSDictionary *)decodeMessage:(NSString *)theMessage secretData:(NSData *)theSecretData algorithm:(id<JWTAlgorithm>)theAlgorithm options:(NSNumber *)theOptions error:(NSError *__autoreleasing *)theError {
    
    BOOL skipVerification = [theOptions boolValue];
    NSString *theAlgorithmName = [theAlgorithm name];
    
    NSArray *parts = [theMessage componentsSeparatedByString:@"."];
    
    if (parts.count < 3) {
        // generate error?
        if (theError) {
            *theError = [JWTErrorDescription errorWithCode:JWTInvalidFormatError];
        }
        return nil;
    }
    
    NSString *headerPart = parts[0];
    NSString *payloadPart = parts[1];
    NSString *signedPart = parts[2];
    
    // decode headerPart
    NSError *jsonError = nil;
    NSData *headerData = [JWTBase64Coder dataWithBase64UrlEncodedString:headerPart];
    id headerJSON = [NSJSONSerialization JSONObjectWithData:headerData
                                                    options:0
                                                      error:&jsonError];
    if (jsonError) {
        if (theError) {
            *theError = [JWTErrorDescription errorWithCode:JWTDecodingHeaderError];
        }
        return nil;
    }
    NSDictionary *header = (NSDictionary *)headerJSON;
    if (!header) {
        if (theError) {
            *theError = [JWTErrorDescription errorWithCode:JWTNoHeaderError];
        }
        return nil;
    }
    
    if (!skipVerification) {
        // find algorithm
        
        //It is insecure to trust the header's value for the algorithm, since
        //the signature hasn't been verified yet, so an algorithm must be provided
        if (!theAlgorithmName) {
            if (theError) {
                *theError = [JWTErrorDescription errorWithCode:JWTUnspecifiedAlgorithmError];
            }
            return nil;
        }
        
        NSString *headerAlgorithmName = header[@"alg"];
        
        //If the algorithm in the header doesn't match what's expected, verification fails
        if (![theAlgorithmName isEqualToString:headerAlgorithmName]) {
            if (theError) {
                *theError = [JWTErrorDescription errorWithCode:JWTAlgorithmNameMismatchError];
            }
            return nil;
        }
        
        // A shit logic, but...
        // You should copy algorithm if this algorithm conforms to RSAlgorithm (NSCopying).
        // Now RS Algorithm holds too much. ( All data about keys :/ )
        // Need further investigation.
        id<JWTAlgorithm> algorithm = nil;
        if ([theAlgorithm conformsToProtocol:@protocol(JWTRSAlgorithm)]) {
            algorithm = [(id<JWTRSAlgorithm>)theAlgorithm copyWithZone:nil];
        }
        else {
            algorithm = [JWTAlgorithmFactory algorithmByName:theAlgorithmName];
        }
        
        if (!algorithm) {
            if (theError) {
                *theError = [JWTErrorDescription errorWithCode:JWTUnsupportedAlgorithmError];
            }
            return nil;
        }
        
        // Verify the signed part
        NSString *signingInput = [@[headerPart, payloadPart] componentsJoinedByString:@"."];
        BOOL signatureValid = NO;

        NSError *algorithmError = nil;
        if (theSecretData && [algorithm respondsToSelector:@selector(verifyHash:signature:key:error:)]) {
            signatureValid =
            //[algorithm verifySignedInput:signingInput withSignature:signedPart verificationKeyData:theSecretData];
            [algorithm verifyHash:[signingInput dataUsingEncoding:NSUTF8StringEncoding] signature:[JWTBase64Coder dataWithBase64UrlEncodedString:signedPart] key:theSecretData error:&algorithmError];
        }
//        if (theSecretData && [algorithm respondsToSelector:@selector(verifySignedInput:withSignature:verificationKeyData:)]) {
//            signatureValid = [algorithm verifySignedInput:signingInput withSignature:signedPart verificationKeyData:theSecretData];
//
//            // Not used now.
////        } else {
////            signatureValid = [algorithm verifySignedInput:signingInput withSignature:signedPart verificationKey:theSecret];
//        }
        
        if (algorithmError) {
            if (theError) {
                *theError = algorithmError;
            }
            return nil;
        }
        if (!signatureValid) {
            if (theError) {
                *theError = [JWTErrorDescription errorWithCode:JWTInvalidSignatureError];
            }
            return nil;
        }
    }
    
    // and decode payload
    jsonError = nil;
    NSData *payloadData = [JWTBase64Coder dataWithBase64UrlEncodedString:payloadPart];
    id payloadJSON = [NSJSONSerialization JSONObjectWithData:payloadData
                                                     options:0
                                                       error:&jsonError];
    if (jsonError) {
        if (theError) {
            *theError = [JWTErrorDescription errorWithCode:JWTDecodingPayloadError];
        }
        return nil;
    }
    NSDictionary *payload = (NSDictionary *)payloadJSON;
    
    if (!payload) {
        if (theError) {
            *theError = [JWTErrorDescription errorWithCode:JWTNoPayloadError];
        }
        return nil;
    }
    
    NSDictionary *result = @{
                             JWTCodingResultHeaders : header,
                             JWTCodingResultPayload : payload
                             };
    
    return result;
}

- (JWTCodingResultType *)result {
    return self.decode;
}
@end
