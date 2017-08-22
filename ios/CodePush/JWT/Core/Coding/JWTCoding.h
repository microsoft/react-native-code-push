//
//  JWT.h
//  JWT
//
//  Created by Klaas Pieter Annema on 31-05-13.
//  Copyright (c) 2013 Karma. All rights reserved.
//

#import <Foundation/Foundation.h>
/**
 @discussion JWT is a general interface for decoding and encoding.
 Now it is to complex and fat to support.
 Possible solution: split interface into several pieces.
 
 JWT_1_0 -> JWT with plain old functions.
 JWT_2_0 -> JWT with builder usage.
 JWT_3_0 -> JWT with splitted apart algorithm data and payload data.
 */
@interface JWT : NSObject @end

typedef NS_OPTIONS(NSInteger, JWTCodingDecodingOptions) {
    JWTCodingDecodingOptionsNone = 0,
    JWTCodingDecodingOptionsSkipVerification = 1
};
