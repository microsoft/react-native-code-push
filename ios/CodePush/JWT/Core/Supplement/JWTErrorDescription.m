//
//  JWTErrorDescription.m
//  JWT
//
//  Created by Lobanov Dmitry on 27.11.16.
//  Copyright © 2016 JWTIO. All rights reserved.
//

#import "JWTErrorDescription.h"
NSString *JWTErrorDomain = @"io.jwt";
@implementation JWTErrorDescription
+ (NSDictionary *)userDescriptionsAndCodes {
    static NSDictionary *userDescriptionsAndCodes = nil;
    return userDescriptionsAndCodes ?: (userDescriptionsAndCodes = @{
        @(JWTInvalidFormatError): @"Invalid format! Try to check your encoding algorithm. Maybe you put too many dots as delimiters?",
        @(JWTUnsupportedAlgorithmError): @"Unsupported algorithm! You could implement it by yourself",
        @(JWTAlgorithmNameMismatchError) : @"Algorithm doesn't match name in header.",
        @(JWTInvalidSignatureError): @"Invalid signature! It seems that signed part of jwt mismatch generated part by algorithm provided in header.",
        @(JWTNoPayloadError): @"No payload! Hey, forget payload?",
        @(JWTNoHeaderError): @"No header! Hmm",
        @(JWTEncodingHeaderError): @"It seems that header encoding failed",
        @(JWTEncodingPayloadError): @"It seems that payload encoding failed",
        @(JWTEncodingSigningError): @"It seems that signing output corrupted. Make sure signing worked (e.g. we may have issues extracting the key from the PKCS12 bundle if passphrase is incorrect).",
        @(JWTClaimsSetVerificationFailed): @"It seems that claims verification failed",
        @(JWTInvalidSegmentSerializationError): @"It seems that json serialization failed for segment",
        @(JWTUnspecifiedAlgorithmError): @"Unspecified algorithm! You must explicitly choose an algorithm to decode with.",
        @(JWTBlacklistedAlgorithmError): @"Algorithm in blacklist? Try to check whitelist parameter",
        @(JWTDecodingHeaderError): @"Error decoding the JWT Header segment.",
        @(JWTDecodingPayloadError): @"Error decoding the JWT Payload segment.",
        @(JWTDecodingHoldersChainEmptyError) : @"Error decoding the JWT algorithm and data holders chain is empty!"
    }, userDescriptionsAndCodes);
}

+ (NSDictionary *)errorDescriptionsAndCodes {
    static NSDictionary *errorDescriptionsAndCodes = nil;
    return errorDescriptionsAndCodes ?: (errorDescriptionsAndCodes = @{
        @(JWTInvalidFormatError): @"JWTInvalidFormatError",
        @(JWTUnsupportedAlgorithmError): @"JWTUnsupportedAlgorithmError",
        @(JWTAlgorithmNameMismatchError) :@"JWTAlgorithmNameMismatchError",
        @(JWTInvalidSignatureError): @"JWTInvalidSignatureError",
        @(JWTNoPayloadError): @"JWTNoPayloadError",
        @(JWTNoHeaderError): @"JWTNoHeaderError",
        @(JWTEncodingHeaderError): @"JWTEncodingHeaderError",
        @(JWTEncodingPayloadError): @"JWTEncodingPayloadError",
        @(JWTEncodingSigningError): @"JWTEncodingSigningError",
        @(JWTClaimsSetVerificationFailed): @"JWTClaimsSetVerificationFailed",
        @(JWTInvalidSegmentSerializationError): @"JWTInvalidSegmentSerializationError",
        @(JWTUnspecifiedAlgorithmError): @"JWTUnspecifiedAlgorithmError",
        @(JWTBlacklistedAlgorithmError): @"JWTBlacklistedAlgorithmError",
        @(JWTDecodingHeaderError): @"JWTDecodingHeaderError",
        @(JWTDecodingPayloadError): @"JWTDecodingPayloadError",
        @(JWTDecodingHoldersChainEmptyError) :@"JWTDecodingHoldersChainEmptyError"
    }, errorDescriptionsAndCodes);
}

+ (NSString *)userDescriptionForCode:(JWTError)code {
    NSString *resultString = [self userDescriptionsAndCodes][@(code)];
    return resultString ?: @"Unexpected error";
}

+ (NSString *)errorDescriptionForCode:(JWTError)code {
    NSString *resultString = [self errorDescriptionsAndCodes][@(code)];
    return resultString ?: @"JWTUnexpectedError";
}

+ (NSError *)errorWithCode:(JWTError)code {
    return [self errorWithCode:code withUserDescription:[self userDescriptionForCode:code] withErrorDescription:[self errorDescriptionForCode:code]];
}

+ (NSError *)errorWithCode:(NSInteger)code withUserDescription:(NSString *)userDescription withErrorDescription:(NSString *)errorDescription {
    return [NSError errorWithDomain:JWTErrorDomain code:code userInfo:@{NSLocalizedDescriptionKey: userDescription, @"errorDescription": errorDescription}];
}
@end
