#import "CodePush+RestartManager.h"
#import "CodePush.h"

@implementation CodePush (RestartManager)

-(void)dequeueItemFromRestartQueueAndRestart
{
    if ([[self restartQueue] count] > 0)
    {
        CPLog(@"Executing pending restarts");
        BOOL queuedObject = (BOOL)[[self restartQueue] objectAtIndex:0];
        [[self restartQueue] removeObjectAtIndex:0];

        [self restartApplication:queuedObject];
    }
    return;
}

-(void) allowRestart
{
    CPLog(@"Re-allowing restarts");
    [self setRestartAllowed:YES];
    [self dequeueItemFromRestartQueueAndRestart];
}

-(void) disallowRestart
{
    CPLog(@"Disallowing restarts");
    [self setRestartAllowed:NO];
}

-(void) clearPendingRestart
{
    [[self restartQueue] removeAllObjects];
}

-(void) restartApplication:(BOOL)onlyIfUpdateIsPending
{
    if ([self restartInProgress])
    {
        CPLog(@"Restart request queued until the current restart is completed");
        [[self restartQueue] addObject:[NSNumber numberWithBool:onlyIfUpdateIsPending]];
    }
    else if (![self restartAllowed]) {
        CPLog(@"Restart request queued until restarts are re-allowed");
        [[self restartQueue] addObject:[NSNumber numberWithBool:onlyIfUpdateIsPending]];
    }
    else
    {
        [self setRestartInProgress:YES];
        if ([self restartApp:onlyIfUpdateIsPending]){
            // The app has already restarted, so there is no need to
            // process the remaining queued restarts.
            CPLog(@"Restarting app");
            return;
        }
        [self setRestartInProgress:NO];
        [self dequeueItemFromRestartQueueAndRestart];
    }
}

@end
