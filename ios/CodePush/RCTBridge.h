#import <Foundation/Foundation.h>

@interface RCTBridge () // RN私有类 ，这里暴露他的接口

- (void)executeSourceCode:(NSData *)sourceCode sync:(BOOL)sync;

@end
 
