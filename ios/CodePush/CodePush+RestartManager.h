#import "CodePush.h"

@interface CodePush (RestartManager)

-(void) allowRestart;
-(void) disallowRestart;
-(void) clearPendingRestart;
-(void) restartApplication:(BOOL)onlyIfUpdateIsPending;

@end
