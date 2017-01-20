using ReactNative;
using ReactNative.Modules.Core;
using ReactNative.Shell;
using System.Collections.Generic;
using CodePush.ReactNative;
using System;
using System.Diagnostics;

namespace CodePushDemoApp
{
    class MainPage : ReactPage
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
#if CODE_PUSH
                codePushReactPackage = new CodePushReactPackage("v6chCZ69IGmYpSD7hTubJw0WBY-8VkBuREd8z", this);
#else
                codePushReactPackage = new CodePushReactPackage("http://localhost:8081/index.windows.bundle?platform=windows&dev=true&host=false", this);
#endif
                try
                {
#if (!BUNDLE && !CODE_PUSH)
                    return null;
#else
                    return codePushReactPackage.GetJavaScriptBundleFile();
#endif
                }
                catch (Exception)
                {
                    return null;
                }
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
#if (!BUNDLE && !CODE_PUSH) || DEBUG
                return true;
#else
                return false;
#endif
            }
        }
    }

}
