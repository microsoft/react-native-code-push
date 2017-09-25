//
//  JWTDecriptedViewController.h
//  JWTDesktop
//
//  Created by Lobanov Dmitry on 25.09.16.
//  Copyright © 2016 JWT. All rights reserved.
//

#import <Cocoa/Cocoa.h>
#import <JWT/JWT.h>

@interface JWTDecriptedViewController : NSViewController

@property (strong, nonatomic, readwrite) JWTBuilder *builder;

@end
