//
//  ViewController.swift
//  JWTDesktopSwift
//
//  Created by Lobanov Dmitry on 01.10.16.
//  Copyright © 2016 JWTIO. All rights reserved.
//

import Cocoa
import JWT
import SnapKit

enum SignatureValidationType : Int {
    case Unknown
    case Valid
    case Invalid
    var color : NSColor {
        switch self {
        case .Unknown:
            return NSColor.darkGray
        case .Valid:
            return NSColor(calibratedRed: 0, green: 185/255.0, blue: 241/255.0, alpha: 1.0)
        case .Invalid:
            return NSColor.red
        }
    }
    var title : String {
        switch self {
        case .Unknown:
            return "Signature Unknown"
        case .Valid:
            return "Signature Valid"
        case .Invalid:
            return "Signature Invalid"
        }
    }
}

// MARK - Supply JWT Methods
extension ViewController {
    var availableAlgorithms : [JWTAlgorithm] {
        return JWTAlgorithmFactory.algorithms() as! [JWTAlgorithm]
    }
    var availableAlgorithmsNames : [String] {
        return self.availableAlgorithms.map {$0.name}
    }
    
    var chosenAlgorithmName : String {
        return self.algorithmPopUpButton.selectedItem?.title ?? ""
    }
    
    var chosenSecretData : Data? {
        let secret = self.chosenSecret;
    
        let isBase64Encoded = self.isBase64EncodedSecret;
        guard let result = Data(base64Encoded: secret), isBase64Encoded else {
            return nil
        }
        
        return result
    }
    
    var chosenSecret : String {
        return self.secretTextField.stringValue
    }
    
    var isBase64EncodedSecret : Bool {
        return self.secretIsBase64EncodedCheckButton.integerValue == 1
    }
    
    func JWT(token: String, skipVerification: Bool) -> [AnyHashable : Any]? {
        print("JWT ENCODED TOKEN \(token)")
        let algorithmName = self.chosenAlgorithmName
        print("JWT Algorithm NAME \(algorithmName)")
        let builder = JWTBuilder.decodeMessage(token).algorithmName(algorithmName)!.options(skipVerification as NSNumber)
        if (algorithmName != JWTAlgorithmNameNone) {
            if let secretData = self.chosenSecretData, isBase64EncodedSecret {
                _ = builder?.secretData(secretData)
            }
            else {
                self.secretIsBase64EncodedCheckButton.state = 0
                _ = builder?.secret(self.chosenSecret)
            }
        }
        
        self.builder = builder
        
        guard let decoded = builder?.decode else {
            print("JWT ERROR \(String(describing: builder?.jwtError))")
            return nil
        }
        
        print("JWT DICTIONARY \(decoded)")
        return decoded
    }
}

// Refresh UI
extension ViewController {
    //    #pragma mark - Refresh UI
    func refreshUI() {
        
        let textStorage = self.encodedTextView.textStorage!;
        let string = textStorage.string;
        let range = NSMakeRange(0, string.characters.count);
        if let attributedString = self.encodedAttributedString(text: string) {
            self.encodedTextView.undoManager?.beginUndoGrouping()
            textStorage.replaceCharacters(in: range, with: attributedString)
            self.encodedTextView.undoManager?.endUndoGrouping()
        }

        let jwtVerified = self.JWT(token: string, skipVerification: false) != nil
        self.signatureReactOnVerifiedToken(verified: jwtVerified)

        self.decriptedViewController.builder = self.builder
    }
}

// MARK - Encoding
extension ViewController {
    
}

// MARK - Actions
extension ViewController {
    func popUpButtonValueChanged(sender : AnyClass) {
        self.refreshUI()
    }
    
    func checkBoxState(sender : AnyClass) {
        self.refreshUI()
    }
    func signatureReactOnVerifiedToken(verified: Bool) {
        let type = verified ? SignatureValidationType.Valid : SignatureValidationType.Invalid
        self.signatureValidation = type
    }
}

extension ViewController : NSTextFieldDelegate {
    override func controlTextDidChange(_ obj: Notification) {
        if (obj.name == NSNotification.Name.NSControlTextDidChange) {
            let textField = obj.object as! NSTextField
            if textField == self.secretTextField {
                self.refreshUI()
            }
        }
    }
}

// MARK - Encoding Heplers
extension Array {
    func safeObject(index: Array.Index) -> Element? {
        return index >= self.count ? nil : self[index]
    }
}

// Array issues
extension ViewController {
    func attributesJoinedBy(_ attributes: [NSAttributedString], by: NSAttributedString) -> NSAttributedString? {
        var array = attributes
        if let first = array.first {
            array.removeFirst()
            return array.reduce(first, { (result, string) -> NSAttributedString in
                let mutableResult = NSMutableAttributedString(attributedString: result)
                mutableResult.append(by)
                mutableResult.append(string)
                return mutableResult
            })
        }
        return nil
    }
}

//extension Sequence where Iterator.Element == NSAttributedString {
//    func componentsJoined(by: Iterator.Element) -> Iterator.Element? {
//        let array = self
//        if let first = array.first(where:  {_ in true}) {
//            let subarray = array.dropFirst()
//            return (Self(subarray) as! Self).reduce(first, {
//                (result, part) -> Iterator.Element in
//                let mutableResult = NSMutableAttributedString(attributedString: result)
//                mutableResult.append(by)
//                mutableResult.append(part)
//                return mutableResult
//            })
//        }
//        return nil
//    }
//}

// Encoding Customization
extension ViewController {
        //    #pragma mark - Encoding Customization
    func textPart(parts: [String], type: TokenTextType) -> String? {
        var result: String? = nil
        switch type {
        case .Header:
            result = parts.safeObject(index: 0)
        case .Payload:
            result = parts.safeObject(index: 1)
        case .Signature:
            if (parts.count > 2) {
                result = (Array(parts[2..<parts.count]) as NSArray).componentsJoined(by: ".")
            }
        default: result = nil
        }
        return result
    }
    
    func encodedTextAttributes(type: TokenTextType) -> [String: Any] {
        var attributes = self.defaultEncodedTextAttributes
        attributes[NSForegroundColorAttributeName] = type.color
        return attributes
    }
    
    var defaultEncodedTextAttributes: [String:Any] {
        return [
            NSFontAttributeName : NSFont.boldSystemFont(ofSize: 22)
        ] as [String: Any]
    }
    
    func encodedAttributedString(text: String) -> NSAttributedString? {
        let parts = (text as NSString).components(separatedBy: ".")
        // next step, determine text color!
        // add missing dots.
        // restore them like this:
        // color text if you can
        var resultParts: [NSAttributedString] = [];
        for typeNumber in TokenTextType.Header.rawValue ... TokenTextType.Signature.rawValue {
            let type = TokenTextType(rawValue: typeNumber)
            
            if let currentPart = self.textPart(parts: parts, type: type!) {
                let attributes = self.encodedTextAttributes(type: type!)
                let currentAttributedString = NSAttributedString(string: currentPart, attributes: attributes)
                resultParts.append(currentAttributedString)
            }
        }
        
        let attributes = self.encodedTextAttributes(type: .Default)
        
        
        let dot = NSAttributedString(string: ".", attributes: attributes)
        let result = self.attributesJoinedBy(resultParts, by: dot)
        return result


    }
}

// MARK - EncodingTextViewDelegate
//extension ViewController : NSTextViewDelegate {
//    func textView(_ textView: NSTextView, shouldChangeTextIn affectedCharRange: NSRange, replacementString: String?) -> Bool {
//        if (textView == self.encodedTextView) {
//            if let textStore = textView.textStorage {
//                textStore.replaceCharacters(in: affectedCharRange, with: replacementString!)
//            }
//            self.refreshUI()
//            return false
//        }
//        return false
//    }
//}

extension ViewController : NSTextViewDelegate {
    func textDidChange(_ notification: Notification) {
        self.refreshUI()
    }
}

class ViewController: NSViewController {
    override init?(nibName nibNameOrNil: String?, bundle nibBundleOrNil: Bundle?) {
        super.init(nibName: nibNameOrNil, bundle: nibBundleOrNil)
    }
    
    required init?(coder: NSCoder) {
        super.init(coder: coder)
    }
    
    // Properties - Outlets
    @IBOutlet weak var algorithmLabel : NSTextField!
    @IBOutlet weak var algorithmPopUpButton : NSPopUpButton!
    
    @IBOutlet weak var secretLabel : NSTextField!
    @IBOutlet weak var secretTextField : NSTextField!
    @IBOutlet weak var secretIsBase64EncodedCheckButton : NSButton!
    
    @IBOutlet weak var encodedTextView : NSTextView!
    @IBOutlet weak var decriptedView : NSView!
    var decriptedViewController : DecriptedViewController!
    
    @IBOutlet weak var signatureStatusLabel : NSTextField!

    // Properties - Data
    var signatureDecorations : NSDictionary = [:]
    var signatureValidation : SignatureValidationType = .Unknown {
        didSet {
            self.signatureStatusLabel.backgroundColor = signatureValidation.color
            self.signatureStatusLabel.stringValue = signatureValidation.title
        }
    }
    
    // Properties - Tests
    var builder : JWTBuilder?;
    
    // MARK - Setup
    
    func setupTop() {
        // top label.
        self.algorithmLabel.stringValue = "Algorithm";
        // pop up button.
        
        self.algorithmPopUpButton.removeAllItems()
        self.algorithmPopUpButton.addItems(withTitles: self.availableAlgorithmsNames)
        self.algorithmPopUpButton.target = self
        self.algorithmPopUpButton.action = #selector(ViewController.popUpButtonValueChanged(sender:))
        
        // secretLabel
        self.secretLabel.stringValue = "Secret"
        
        // secretTextField
        self.secretTextField.placeholderString = "Secret"
        self.secretTextField.delegate = self

        // check button
        self.secretIsBase64EncodedCheckButton.title = "is Base64Encoded Secret"
        self.secretIsBase64EncodedCheckButton.integerValue = 0
        self.secretIsBase64EncodedCheckButton.target = self
        self.secretIsBase64EncodedCheckButton.action = #selector(ViewController.checkBoxState(sender:))
    }
    
    func setupBottom() {
        self.signatureStatusLabel.alignment       = .center
        self.signatureStatusLabel.textColor       = NSColor.white
        self.signatureStatusLabel.drawsBackground = true
        self.signatureValidation = .Unknown
    }
    
    
    func setupEncodingDecodingViews() {
        self.encodedTextView.delegate = self;
    }
    
    func setupDecorations() {
        self.setupTop()
        self.setupBottom()
    }
    
    func setupDecriptedViews() {
        let view = self.decriptedView
        self.decriptedViewController = DecriptedViewController()
        view?.addSubview(self.decriptedViewController.view)
    }
    
    func tryToEncode() {
        let claimsSet = JWTClaimsSet()
        claimsSet.subject = "1234567890"
        let dictionary = [
            "name" : "John Doe",
            "admin": true
        ] as [String : Any]
        let secret = "secret"
        let holder = JWTAlgorithmHSFamilyDataHolder.createWithAlgorithm256().secret(secret)
        let builder = JWTEncodingBuilder.encode(claimsSet).payload(dictionary)?.addHolder(holder)
        let builderResult = builder?.result
        
        if let result = builderResult {
            if let success = result.successResult {
                print("SUCCESS: \(success.encoded)")
            }
            
            if let error = result.errorResult {
                print("ERROR: \(error.error)")
            }
        }
        
        let decodingBuilder = JWTDecodingBuilder.decodeMessage(builderResult?.successResult.encoded).addHolder(holder)
        
        if let result = decodingBuilder?.result {
            if let success = result.successResult {
                print("SUCCESS: \(success.headerAndPayloadDictionary)")
            }
            
            if let error = result.errorResult {
                print("ERROR: \(error.error)")
            }
        }
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()
        self.setupDecorations()
        self.setupEncodingDecodingViews()
        self.setupDecriptedViews()
        self.defaultJWTIOSetup()
        self.refreshUI()
        self.tryToEncode()
    }
    
    func defaultJWTIOSetup() {
        // secret
        self.secretTextField.stringValue = "secret"
        
        // token
        let token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiYWRtaW4iOnRydWV9.TJVA95OrM7E2cBab30RMHrHDcEfxjoYZgeFONFh7HgQ"
        var range = NSRange()
        range.location = 0
        range.length = token.characters.count
        self.encodedTextView.insertText(token, replacementRange: range)
        // algorithm HS256
        if let index = self.availableAlgorithmsNames.index(where: {
            $0 == JWTAlgorithmNameHS256
        }) {
            self.algorithmPopUpButton.selectItem(at: index)
        }
    }
    
    override func viewWillAppear() {
        super.viewWillAppear()
        let view = self.decriptedView
        let subview = self.decriptedViewController.view

        subview.snp.makeConstraints { (maker) in
            maker.edges.equalTo((view?.snp.edges)!)
        }
    }
}

