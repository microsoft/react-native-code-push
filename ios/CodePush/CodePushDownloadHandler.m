#import "CodePush.h"

@implementation CodePushDownloadHandler {
    // Header chars used to determine if the file is a zip.
    char _header[4];
}

- (id)init:(NSString *)downloadFilePath
operationQueue:(dispatch_queue_t)operationQueue
progressCallback:(void (^)(long long, long long))progressCallback
doneCallback:(void (^)(BOOL))doneCallback
failCallback:(void (^)(NSError *err))failCallback {
    self.outputFileStream = [NSOutputStream outputStreamToFileAtPath:downloadFilePath
                                                              append:NO];
    self.receivedContentLength = 0;
    self.operationQueue = operationQueue;
    self.progressCallback = progressCallback;
    self.doneCallback = doneCallback;
    self.failCallback = failCallback;
    return self;
}

- (void)download:(NSString *)url {
    self.downloadUrl = url;
    NSURLRequest *request = [NSURLRequest requestWithURL:[NSURL URLWithString:url]
                                             cachePolicy:NSURLRequestUseProtocolCachePolicy
                                         timeoutInterval:60.0];
    NSURLConnection *connection = [[NSURLConnection alloc] initWithRequest:request
                                                                  delegate:self
                                                          startImmediately:NO];
    if ([NSOperationQueue instancesRespondToSelector:@selector(setUnderlyingQueue:)]) {
        NSOperationQueue *delegateQueue = [NSOperationQueue new];
        delegateQueue.underlyingQueue = self.operationQueue;
        [connection setDelegateQueue:delegateQueue];
    } else {
        [connection scheduleInRunLoop:[NSRunLoop mainRunLoop]
                              forMode:NSDefaultRunLoopMode];
    }

    [connection start];
}

#pragma mark NSURLConnection Delegate Methods

- (NSCachedURLResponse *)connection:(NSURLConnection *)connection
                  willCacheResponse:(NSCachedURLResponse*)cachedResponse {
    // Return nil to indicate not necessary to store a cached response for this connection
    return nil;
}

- (void)connection:(NSURLConnection *)connection didReceiveResponse:(NSURLResponse *)response {
    if ([response isKindOfClass:[NSHTTPURLResponse class]]) {
        NSInteger statusCode = [(NSHTTPURLResponse *)response statusCode];
        if (statusCode >= 400) {
            [self.outputFileStream close];
            [connection cancel];
            NSError *err = [CodePushErrorUtils errorWithMessage:[NSString stringWithFormat: @"Received %ld response from %@", (long)statusCode, self.downloadUrl]];
            self.failCallback(err);
            return;
        }
    }
    
    self.expectedContentLength = response.expectedContentLength;
    [self.outputFileStream open];
}

- (void)connection:(NSURLConnection *)connection didReceiveData:(NSData *)data {
    if (self.receivedContentLength < 4) {
        for (int i = 0; i < [data length]; i++) {
            int headerOffset = (int)self.receivedContentLength + i;
            if (headerOffset >= 4) {
                break;
            }

            const char *bytes = [data bytes];
            _header[headerOffset] = bytes[i];
        }
    }

    self.receivedContentLength = self.receivedContentLength + [data length];

    NSInteger bytesLeft = [data length];

    do {
        NSInteger bytesWritten = [self.outputFileStream write:[data bytes]
                                                     maxLength:bytesLeft];
        if (bytesWritten == -1) {
            break;
        }

        bytesLeft -= bytesWritten;
    } while (bytesLeft > 0);

    self.progressCallback(self.expectedContentLength, self.receivedContentLength);

    // bytesLeft should not be negative.
    assert(bytesLeft >= 0);

    if (bytesLeft) {
        [self.outputFileStream close];
        [connection cancel];
        self.failCallback([self.outputFileStream streamError]);
    }
}

- (void)connection:(NSURLConnection*)connection didFailWithError:(NSError*)error
{
    [self.outputFileStream close];
    self.failCallback(error);
}

- (void)connectionDidFinishLoading:(NSURLConnection *)connection {
    [self.outputFileStream close];
    if (self.receivedContentLength < 1) {
        NSError *err = [CodePushErrorUtils errorWithMessage:[NSString stringWithFormat:@"Received empty response from %@", self.downloadUrl]];
        self.failCallback(err);
        return;
    }
    
    // expectedContentLength might be -1 when NSURLConnection don't know the length(e.g. response encode with gzip)
    if (self.expectedContentLength > 0) {
        // We should have received all of the bytes if this is called.
        assert(self.receivedContentLength == self.expectedContentLength);
    }

    BOOL isZip = _header[0] == 'P' && _header[1] == 'K' && _header[2] == 3 && _header[3] == 4;
    self.doneCallback(isZip);
}

@end
