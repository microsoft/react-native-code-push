#import "CodePush.h"

@implementation CodePushDownloadHandler {
    // Header chars used to determine if the file is a zip.
    char _header[4];
    
    dispatch_queue_t _operationQueue;
    NSOutputStream *_outputFileStream;
    
    // State for tracking download progress.
    long long _expectedContentLength;
    long long _receivedContentLength;
    
    // Notification callbacks
    void (^_doneCallback)(BOOL);
    void (^_failCallback)(NSError *err);
    void (^_progressCallback)(long long, long long);    
}

#pragma mark - Public methods

- (instancetype)init:(NSString *)downloadFilePath
      operationQueue:(dispatch_queue_t)operationQueue
    progressCallback:(void (^)(long long, long long))progressCallback
        doneCallback:(void (^)(BOOL))doneCallback
        failCallback:(void (^)(NSError *err))failCallback
{
    _operationQueue = operationQueue;
    _outputFileStream = [NSOutputStream outputStreamToFileAtPath:downloadFilePath
                                                          append:NO];

    _doneCallback = doneCallback;
    _failCallback = failCallback;
    _progressCallback = progressCallback;    
    
    return self;
}

-(void)download:(NSString *)url
{
    NSURLRequest *request = [NSURLRequest requestWithURL:[NSURL URLWithString:url]
                                             cachePolicy:NSURLRequestUseProtocolCachePolicy
                                         timeoutInterval:60.0];
                                         
    NSURLConnection *connection = [[NSURLConnection alloc] initWithRequest:request
                                                                  delegate:self
                                                          startImmediately:NO];
                                                    
    // Ensure that the download is run on the same GCD
    // queue as the CodePush native module      
    NSOperationQueue *delegateQueue = [NSOperationQueue new];
    delegateQueue.underlyingQueue = _operationQueue;
    [connection setDelegateQueue:delegateQueue];
    [connection start];
}

#pragma mark - NSURLConnectionDelegate Methods

- (NSCachedURLResponse *)connection:(NSURLConnection *)connection
                  willCacheResponse:(NSCachedURLResponse *)cachedResponse
{
    // Return nil to indicate not necessary to store a cached response for this connection
    return nil;
}

- (void)connection:(NSURLConnection *)connection didReceiveResponse:(NSURLResponse *)response
{
    _expectedContentLength = response.expectedContentLength;
    [_outputFileStream open];
}

- (void)connection:(NSURLConnection *)connection didReceiveData:(NSData *)data
{
    if (_receivedContentLength < 4) {
        for (int i = 0; i < [data length]; i++) {
            int headerOffset = (int)_receivedContentLength + i;
            if (headerOffset >= 4) {
                break;
            }

            const char *bytes = [data bytes];
            _header[headerOffset] = bytes[i];
        }
    }

    _receivedContentLength = _receivedContentLength + [data length];

    NSInteger bytesLeft = [data length];

    do {
        NSInteger bytesWritten = [_outputFileStream write:[data bytes]
                                                     maxLength:bytesLeft];
        if (bytesWritten == -1) {
            break;
        }

        bytesLeft -= bytesWritten;
    } while (bytesLeft > 0);

    _progressCallback(_expectedContentLength, _receivedContentLength);

    // bytesLeft should not be negative.
    assert(bytesLeft >= 0);

    if (bytesLeft) {
        [_outputFileStream close];
        [connection cancel];
        _failCallback([_outputFileStream streamError]);
    }
}

- (void)connection:(NSURLConnection *)connection didFailWithError:(NSError *)error
{
    [_outputFileStream close];
    _failCallback(error);
}

- (void)connectionDidFinishLoading:(NSURLConnection *)connection
{
    // expectedContentLength might be -1 when NSURLConnection don't know the length(e.g. response encode with gzip)
    if (_expectedContentLength > 0) {
        // We should have received all of the bytes if this is called.
        assert(_receivedContentLength == _expectedContentLength);
    }

    [_outputFileStream close];
    BOOL isZip = _header[0] == 'P' && _header[1] == 'K' && _header[2] == 3 && _header[3] == 4;
    _doneCallback(isZip);
}

@end