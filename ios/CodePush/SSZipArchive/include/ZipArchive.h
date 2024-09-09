//
//  ZipArchive.h
//  ZipArchive
//
//  Created by Serhii Mumriak on 12/1/15.
//

#import <Foundation/Foundation.h>

//! Project version number for ZipArchive.
FOUNDATION_EXPORT double ZipArchiveVersionNumber;

//! Project version string for ZipArchive.
FOUNDATION_EXPORT const unsigned char ZipArchiveVersionString[];

// In this header, you should import all the public headers of your framework using statements like #import <SSZipArchive.h>

// This is to account for the many different ways this library gets imported.
#if __has_include(<SSZipArchive/SSZipArchive.h>)
#import <SSZipArchive/SSZipArchive.h>
#elif __has_include("../SSZipArchive.h")
#import "../SSZipArchive.h"
#else
#import "SSZipArchive.h"
#endif
