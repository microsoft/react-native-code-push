//
//  JWTBase64Coder.m
//  Pods
//
//  Created by Lobanov Dmitry on 05.10.16.
//
//

#import "JWTBase64Coder.h"

@interface JWTBase64Coder (ConditionLinking)
+ (BOOL)isBase64AddtionsAvailable;
@end

@implementation JWTBase64Coder (ConditionLinking)
+ (BOOL)isBase64AddtionsAvailable {
    return [[NSData class] respondsToSelector:@selector(dataWithBase64UrlEncodedString:)];
}
@end

#if __has_include("MF_Base64Additions.h")
#import <Base64/MF_Base64Additions.h>
#endif

@implementation JWTBase64Coder

+ (NSString *)base64UrlEncodedStringWithData:(NSData *)data {
    if ([self isBase64AddtionsAvailable] && [data respondsToSelector:@selector(base64UrlEncodedString)]) {
        return [data performSelector:@selector(base64UrlEncodedString)];
    }
    else {
        return [data base64EncodedStringWithOptions:0];
    }
}

+ (NSData *)dataWithBase64UrlEncodedString:(NSString *)urlEncodedString {
    if ([self isBase64AddtionsAvailable] && [[NSData class] respondsToSelector:@selector(dataWithBase64UrlEncodedString:)]) {
        return [[NSData class] performSelector:@selector(dataWithBase64UrlEncodedString:) withObject:urlEncodedString];
    }
    else {
        return [[NSData alloc] initWithBase64EncodedString:urlEncodedString options:0];
    }
}

@end
