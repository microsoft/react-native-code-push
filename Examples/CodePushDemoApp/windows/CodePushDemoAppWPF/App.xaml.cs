using System;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Navigation;

namespace CodePushDemoApp
{
    /// <summary>
    /// Interaction logic for App.xaml
    /// </summary>
    public partial class App : Application
    {
        private readonly MainPage _reactPage = new MainPage();
        public App()
        {
        }
        protected override void OnStartup(StartupEventArgs e)
        {
            base.OnStartup(e);
            OnCreate(e.Args);
        }
        private void OnCreate(string[] arguments)
        {
            var shellWindow = Application.Current.MainWindow;
            if (shellWindow == null)
            {
                shellWindow = new Window
                {
                    ShowActivated = true,
                    ShowInTaskbar = true,
                    Title = "CodePushDemoWPF",
                    Height = 768,
                    Width = 1024,
                    WindowStartupLocation = WindowStartupLocation.CenterScreen
                };
                Application.Current.MainWindow = shellWindow;
            }
            if (!shellWindow.IsLoaded)
            {
                shellWindow.Show();
            }
            var rootFrame = shellWindow.Content as Frame;
            if (rootFrame == null)
            {
                _reactPage.OnCreate(arguments);
                rootFrame = new Frame();
                rootFrame.NavigationFailed += OnNavigationFailed;
                shellWindow.Content = rootFrame;
            }
            if (rootFrame.Content == null)
            {
                rootFrame.Content = _reactPage;
            }
            shellWindow.Activate();
        }
        private void OnNavigationFailed(object sender, NavigationFailedEventArgs e)
        {
            throw new Exception("Failed to load Page...");
        }
    }
}
