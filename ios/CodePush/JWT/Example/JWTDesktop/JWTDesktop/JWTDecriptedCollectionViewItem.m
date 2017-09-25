//
//  JWTDecriptedCollectionViewItem.m
//  JWTDesktop
//
//  Created by Lobanov Dmitry on 25.09.16.
//  Copyright © 2016 JWT. All rights reserved.
//

#import "JWTDecriptedCollectionViewItem.h"

@interface JWTDecriptedCollectionViewItem ()
@property (unsafe_unretained) IBOutlet NSTextView *textView;

@end

@implementation JWTDecriptedCollectionViewItem

- (void)updateWithText:(NSString *)text {
    self.textView.string = text;
}

- (void)updateWithTextColor:(NSColor *)color {
    self.textView.textColor = color;
}

- (void)viewDidLoad {
    [super viewDidLoad];
    //    self.textView.font = [NSFont boldSystemFontOfSize:14];
    // Do view setup here.
}

@end
