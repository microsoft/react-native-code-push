using CodePush.ReactNative;
using ReactNative;
using ReactNative.Modules.Core;
using ReactNative.Shell;
using System;
using System.Collections.Generic;

namespace CodePushDemoApp.Wpf
{
    internal class AppReactPage : ReactPage
    {
        public override string MainComponentName
        {
            get
            {
                return "CodePushDemoApp";
            }
        }

        private CodePushReactPackage codePushReactPackage = null;
        public override string JavaScriptBundleFile
        {
            get
            {
                codePushReactPackage = new CodePushReactPackage("deployment-key-here", this);

#if BUNDLE
                    return codePushReactPackage.GetJavaScriptBundleFile();
#else
                    return null;
#endif
            }
        }


        public override List<IReactPackage> Packages
        {
            get
            {
                return new List<IReactPackage>
                {
                    new MainReactPackage(),
                    codePushReactPackage
                };
            }
        }

        public override bool UseDeveloperSupport
        {
            get
            {
#if !BUNDLE || DEBUG
                return true;
#else
                return false;
#endif
            }
        }
    }
}
