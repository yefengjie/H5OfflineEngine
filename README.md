# Normal Mode
## H5 Publish
### H5 file path rules
scheme :// host / app-name / project-name / project-version / file_name.* ? query_parameters
### auto release process
package project -> generate zip file -> publish offline package -> auto generate config file
#### zip file name rules
app-name\_project-name\_project-version.zip
#### H5 config file json
```Java
{
   "appName": "xxApp",
   "projects": [
      {
         "name": "xxProject",
         "version": "1.0.0",
         "zipPath": "http://xxx/xxApp_xxProject_1.0.0.zip"
      },
      {
         "name": "xxProject2",
         "version": "2.0.0",
         "zipPath": "http://xxx/xxApp_xxProject2_2.0.0.zip"
      }
   ]
}
```
## Native Use
### auto update process
app launch -> fetch H5 config -> diff local config file, check if need update -> background download -> unzip to special path
#### offline files path rule
h5offline/projectName/projectVersion/files.*
### intercept resources
WebViewClient -> shouldInterceptRequest -> check project,version,files -> replace resources with offline files
### package to Android apk
put h5 offline zip to android assets -> unzip to special path when app launch


# Increment Update Mode
todo...

# Test
open h5demo fold, and sh server.sh to setup a simple h5 website