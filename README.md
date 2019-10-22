H5OfflineEngine is a library for Android developers to open H5 pages in offline mode
# Offline Advantages
#### Save about 20 times the flow
package all h5 static resource into 7z file, save a lot of flow.
in my test, this will save about 90 times the flow
#### Without network and faster
#### Easy to use
#### Online,offline all support
this will allow some resource from local, the others from server
# Normal Mode
## How To Use In Android
### gradle
implementation 'com.hzy:libp7zip:1.6.0'   
implementation 'com.yefeng:h5_offline_engine:1.1.0'
### init
```Java
H5OfflineEngine.init(
                this, // context
                "innerH5OfflineZips", // inner zip assets path
                true // show debug log
            )
```
### intercept request
```Java
// replace resource in shouldInterceptRequest in webViewClient
web.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView?,
                url: String?
            ): WebResourceResponse? {
                if (!url.isNullOrEmpty()) {
                    return H5OfflineEngine.interceptResource(Uri.parse(url), applicationContext)
                }
                return super.shouldInterceptRequest(view, url)
            }

            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP &&
                    null != request
                ) {
                    return H5OfflineEngine.interceptResource(request.url, applicationContext)
                }
                return super.shouldInterceptRequest(view, request)
            }
        }
```
### download offline files
```Java
	H5OfflineEngine.download(context, downloadUrlList)
```
### clear cache
```Java
	H5OfflineEngine.clear(context)
```


## H5 Publish
### H5 file path rules
scheme :// host / app-name / project-name / project-version / file_name.* ? query_parameters

example:  http://localhost:8080/demoApp/demoProject/1.0.0/offline.html

### auto release process
package project -> generate 7z file -> publish offline package -> auto generate config file
### zip file rules
package all remote path to zip  
examples:  
localhost..8080/demoApp/demoProject/1.0.0/files.*  
localhost/demoApp/demoProject/1.0.0/files.*
#### zip file name rules
host\_app-name\_project-name\_project-version.7z  
if port is not 80  
host..port\_app-name\_project-name\_project-version.7z 

example:  
localhost\_demoApp\_demoProject\_1.0.0.7z  
localhost..8080\_demoApp\_demoProject\_1.1.0.7z
#### H5 config file json
```Java
[
	"http://localhost:8080/zip/localhost..8080_demoApp_demoProject_1.1.0.7z",
	"http://localhost:8080/zip/path/patch-localhost..8080_demoApp_demoProject_1.1.0.7z"
]
```
## Native Use
### auto update process
app launch -> fetch H5 config -> check if need download -> background download -> unzip to special path -> delete if has old version dirs
#### offline files path rule
h5OfflineEngine/localhost..8080/appName/projectName/projectVersion/files.*
### intercept resources
WebViewClient -> shouldInterceptRequest -> check if has offline files -> replace resources with offline files
### package to Android apk
put h5 offline zip to android assets -> unzip to special path when app launch


# Increment Update Mode
### 1. H5 package patch 7z files
file name start with patch-  
file name example: patch-localhost..8080_demoApp_demoProject_1.1.0.7z
### 2. add patch 7z files to config
```Java
[
	"http://localhost:8080/zip/localhost..8080_demoApp_demoProject_1.1.0.7z",
	"http://localhost:8080/zip/path/patch-localhost..8080_demoApp_demoProject_1.1.0.7z"
]
```
### 3. native download and unzip

# Test
open h5demo fold, and sh server.sh to setup a simple h5 website
