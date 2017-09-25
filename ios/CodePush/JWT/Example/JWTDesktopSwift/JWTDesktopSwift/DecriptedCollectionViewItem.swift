//
//  DecriptedCollectionViewItem.swift
//  JWTDesktopSwift
//
//  Created by Lobanov Dmitry on 01.10.16.
//  Copyright © 2016 JWTIO. All rights reserved.
//

import Cocoa

class DecriptedCollectionViewItem: NSCollectionViewItem {

    @IBOutlet var textView: NSTextView!
    
    override func viewDidLoad() {
        super.viewDidLoad()
        // Do view setup here.
    }
    
    func update(text: String) {
        self.textView.string = text
    }
    
    func update(textColor: NSColor) {
        self.textView.textColor = textColor
    }
}
