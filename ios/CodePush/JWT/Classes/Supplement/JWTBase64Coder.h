//
//  JWTBase64Coder.h
//  Pods
//
//  Created by Lobanov Dmitry on 05.10.16.
//
//

#import <Foundation/Foundation.h>

@interface JWTBase64Coder : NSObject
+ (NSString *)base64UrlEncodedStringWithData:(NSData *)data;
+ (NSData *)dataWithBase64UrlEncodedString:(NSString *)urlEncodedString;
@end
