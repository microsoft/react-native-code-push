//
//  JWTTokenTextTypeDescription.m
//  JWTDesktop
//
//  Created by Lobanov Dmitry on 25.09.16.
//  Copyright © 2016 JWT. All rights reserved.
//

#import "JWTTokenTextTypeDescription.h"

@interface JWTTokenTextTypeDescription ()
@property (strong, nonatomic, readwrite) NSDictionary *textColors;
@end
@implementation JWTTokenTextTypeDescription

- (NSDictionary *)tokenTextColors {
    if (!_textColors) {
        _textColors = @{
                        @(JWTTokenTextTypeDefault) : [NSColor blackColor],
                        @(JWTTokenTextTypeHeader) : [NSColor redColor],
                        @(JWTTokenTextTypePayload) : [NSColor magentaColor],
                        @(JWTTokenTextTypeSignature) : [NSColor colorWithRed:0 green:185/255.0f blue:241/255.0f alpha:1.0f]
                        };
    }
    return _textColors;
}

- (NSColor *)tokenTextColorForType:(JWTTokenTextType)type {
    NSColor *defaultValue = [self tokenTextColors][@(JWTTokenTextTypeDefault)];
    return [self tokenTextColors][@(type)] ?: defaultValue;
}

@end
