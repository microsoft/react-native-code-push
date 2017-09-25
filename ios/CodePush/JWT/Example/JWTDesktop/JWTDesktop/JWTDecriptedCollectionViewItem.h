//
//  JWTDecriptedCollectionViewItem.h
//  JWTDesktop
//
//  Created by Lobanov Dmitry on 25.09.16.
//  Copyright © 2016 JWT. All rights reserved.
//

#import <Cocoa/Cocoa.h>

@interface JWTDecriptedCollectionViewItem : NSCollectionViewItem

- (void)updateWithText:(NSString *)text;
- (void)updateWithTextColor:(NSColor *)color;
@end
