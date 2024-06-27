//
//  RiseUtils.swift
//  CodePush
//
//  Created by Kosy on 27/06/2024.
//  Copyright © 2024 Microsoft. All rights reserved.
//

import Foundation
import Zip

@objc
public class RiseUtils : NSObject {
    
    @objc(unzipAtPath: destination:error:)
    public class func unzipAtPath(_ folder: String, destination: String) throws {
   
        return try Zip.unzipFile(URL(string: folder)!, destination: URL(string: destination)!, overwrite: false, password: nil)
    }
}
