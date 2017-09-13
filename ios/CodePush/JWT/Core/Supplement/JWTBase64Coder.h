//
//  JWTBase64Coder.h
//  Pods
//
//  Created by Lobanov Dmitry on 05.10.16.
//
//

#import <Foundation/Foundation.h>

@protocol JWTStringCoder__Protocol <NSObject>
- (NSString *)stringWithData:(NSData *)data;
- (NSData *)dataWithString:(NSString *)string;
@end

@interface JWTBase64Coder : NSObject
+ (NSString *)base64UrlEncodedStringWithData:(NSData *)data;
+ (NSData *)dataWithBase64UrlEncodedString:(NSString *)urlEncodedString;
@end

@interface JWTBase64Coder (JWTStringCoder__Protocol) <JWTStringCoder__Protocol> @end


@interface JWTStringCoder__For__Encoding : NSObject
@property (assign, nonatomic, readwrite) NSStringEncoding stringEncoding;
+ (instancetype)utf8Encoding;
@end
@interface JWTStringCoder__For__Encoding (JWTStringCoder__Protocol) <JWTStringCoder__Protocol> @end
