//
//  ViewController.swift
//  CarthageCompatibility
//
//  Created by Lobanov Dmitry on 18.10.16.
//  Copyright © 2016 JWTIO. All rights reserved.
//

import UIKit
import JWT

class ViewController: UIViewController {

    override func viewDidLoad() {
        super.viewDidLoad()
        _ = JWT.decodeMessage("").options(0 as NSNumber)
        // Do any additional setup after loading the view, typically from a nib.
    }

    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
        // Dispose of any resources that can be recreated.
    }


}

