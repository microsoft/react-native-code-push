//
//  JWTTokenTextTypeDescription.h
//  JWTDesktop
//
//  Created by Lobanov Dmitry on 25.09.16.
//  Copyright © 2016 JWT. All rights reserved.
//

#import <Foundation/Foundation.h>
#import <AppKit/AppKit.h>

typedef NS_ENUM(NSInteger, JWTTokenTextType) {
    JWTTokenTextTypeDefault, // dot text color
    JWTTokenTextTypeHeader,
    JWTTokenTextTypePayload,
    JWTTokenTextTypeSignature
};

@interface JWTTokenTextTypeDescription : NSObject
- (NSColor *)tokenTextColorForType:(JWTTokenTextType)type;
@end
