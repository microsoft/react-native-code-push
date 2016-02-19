//
//  SSZipArchive.h
//  SSZipArchive
//
//  Created by Sam Soffes on 7/21/10.
//  Copyright (c) Sam Soffes 2010-2015. All rights reserved.
//

#ifndef _SSZIPARCHIVE_H
#define _SSZIPARCHIVE_H

#import <Foundation/Foundation.h>
#include "Common.h"

@protocol SSZipArchiveDelegate;

@interface SSZipArchive : NSObject

// Unzip
+ (BOOL)unzipFileAtPath:(NSString *)path toDestination:(NSString *)destination;
+ (BOOL)unzipFileAtPath:(NSString *)path toDestination:(NSString *)destination delegate:(id<SSZipArchiveDelegate>)delegate;

+ (BOOL)unzipFileAtPath:(NSString *)path toDestination:(NSString *)destination overwrite:(BOOL)overwrite password:(NSString *)password error:(NSError * *)error;
+ (BOOL)unzipFileAtPath:(NSString *)path toDestination:(NSString *)destination overwrite:(BOOL)overwrite password:(NSString *)password error:(NSError * *)error delegate:(id<SSZipArchiveDelegate>)delegate;

+ (BOOL)unzipFileAtPath:(NSString *)path
    toDestination:(NSString *)destination
    progressHandler:(void (^)(NSString *entry, unz_file_info zipInfo, long entryNumber, long total))progressHandler
    completionHandler:(void (^)(NSString *path, BOOL succeeded, NSError *error))completionHandler;

+ (BOOL)unzipFileAtPath:(NSString *)path
    toDestination:(NSString *)destination
    overwrite:(BOOL)overwrite
    password:(NSString *)password
    progressHandler:(void (^)(NSString *entry, unz_file_info zipInfo, long entryNumber, long total))progressHandler
    completionHandler:(void (^)(NSString *path, BOOL succeeded, NSError *error))completionHandler;

// Zip

// without password
+ (BOOL)createZipFileAtPath:(NSString *)path withFilesAtPaths:(NSArray *)paths;
+ (BOOL)createZipFileAtPath:(NSString *)path withContentsOfDirectory:(NSString *)directoryPath;
+ (BOOL)createZipFileAtPath:(NSString *)path withContentsOfDirectory:(NSString *)directoryPath keepParentDirectory:(BOOL)keepParentDirector;

// with password, password could be nil
+ (BOOL)createZipFileAtPath:(NSString *)path withFilesAtPaths:(NSArray *)paths withPassword:(NSString *)password;
+ (BOOL)createZipFileAtPath:(NSString *)path withContentsOfDirectory:(NSString *)directoryPath withPassword:(NSString *)password;
+ (BOOL)createZipFileAtPath:(NSString *)path withContentsOfDirectory:(NSString *)directoryPath keepParentDirectory:(BOOL)keepParentDirectory withPassword:(NSString *)password;

- (instancetype)initWithPath:(NSString *)path;
@property (NS_NONATOMIC_IOSONLY, readonly, getter = isOpen) BOOL open;
- (BOOL)writeFile:(NSString *)path withPassword:(NSString *)password;
- (BOOL)writeFileAtPath:(NSString *)path withFileName:(NSString *)fileName withPassword:(NSString *)password;
- (BOOL)writeData:(NSData *)data filename:(NSString *)filename withPassword:(NSString *)password;
@property (NS_NONATOMIC_IOSONLY, readonly, getter = isClosed) BOOL close;

@end

@protocol SSZipArchiveDelegate <NSObject>

@optional

- (void)zipArchiveWillUnzipArchiveAtPath:(NSString *)path zipInfo:(unz_global_info)zipInfo;
- (void)zipArchiveDidUnzipArchiveAtPath:(NSString *)path zipInfo:(unz_global_info)zipInfo unzippedPath:(NSString *)unzippedPath;

- (BOOL)zipArchiveShouldUnzipFileAtIndex:(NSInteger)fileIndex totalFiles:(NSInteger)totalFiles archivePath:(NSString *)archivePath fileInfo:(unz_file_info)fileInfo;
- (void)zipArchiveWillUnzipFileAtIndex:(NSInteger)fileIndex totalFiles:(NSInteger)totalFiles archivePath:(NSString *)archivePath fileInfo:(unz_file_info)fileInfo;
- (void)zipArchiveDidUnzipFileAtIndex:(NSInteger)fileIndex totalFiles:(NSInteger)totalFiles archivePath:(NSString *)archivePath fileInfo:(unz_file_info)fileInfo;
- (void)zipArchiveDidUnzipFileAtIndex:(NSInteger)fileIndex totalFiles:(NSInteger)totalFiles archivePath:(NSString *)archivePath unzippedFilePath:(NSString *)unzippedFilePath;

- (void)zipArchiveProgressEvent:(unsigned long long)loaded total:(unsigned long long)total;
- (void)zipArchiveDidUnzipArchiveFile:(NSString *)zipFile entryPath:(NSString *)entryPath destPath:(NSString *)destPath;

@end

#endif /* _SSZIPARCHIVE_H */
