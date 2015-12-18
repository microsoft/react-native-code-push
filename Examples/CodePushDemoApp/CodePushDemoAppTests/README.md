
Test Cases
---
* CheckForUpdateTests - Tests the functionality of querying for new app updates via the SDK
  * testNoRemotePackage - Checks that when the remote server has no update packages available, CodePushSdk.queryUpdate does not return a new package nor throw an error.
  * testNoRemotePackageWithSameAppVersion - Checks that when the remote server has an update with a different appVersion, the CodePushSdk.queryUpdate does not return a new package nor throw an error.
  * testFirstUpdate - Checks that when there is no current package (for example, the current build is a fresh install from the app store) and the remote server has a new package, CodePushSdk.queryUpdate returns that new package without throwing an error.
  * testNewUpdate - Checks that when the remote server has a new package with a different package hash and same version as the current package, CodePushSdk.queryUpdate returns that new package without throwing an error.
  * testSamePackage - Checks that when the remote server has a package that is identical to the current package, CodePushSdk.queryUpdate does not return a new package nor throw an error.
  
* InstallUpdateTests - Tests the functionality of installing new app updates downloaded from the server via the SDK
  * testDownloadAndInstallUpdate - Queries for a new update, downloads it and then verifies that from the UI that the new update has been installed. 
  
* DownloadProgressTests - Tests the functionality of downloading app updates and tracking the download progress via the SDK
  * testDownloadProgress - Downloads three files of different sizes and verifies that the reported number of bytes tally.