//
//  TokenTextTypeDescription.swift
//  JWTDesktopSwift
//
//  Created by Lobanov Dmitry on 01.10.16.
//  Copyright © 2016 JWTIO. All rights reserved.
//

import Cocoa

enum TokenTextType : Int {
    case Default = 0
    case Header
    case Payload
    case Signature
    var color : NSColor {
        var color = NSColor.black
        switch self {
        case .Default:
            color = NSColor.black
        case .Header:
            color = NSColor.red
        case .Payload:
            color = NSColor.magenta
        case .Signature:
            color = NSColor(calibratedRed: 0, green: 185/255.0, blue: 241/255.0, alpha: 1.0)
        }
        return color
    }
}
