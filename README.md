##FTPSpeed插件使用说明
* 版本:2.5.0

##环境配置
* npm 4.4.1 +
* node 9.8.0 +


##使用流程 

#####注意:安卓平台，插件加入第三方jar包后，如果cordova build 命令后，报以下异常:
#####‘*\项目名\platforms\android\Androidmanifest.xml’
#####‘*\项目名\platfroms\android\res\xml\config.xml’
#####请通过以下方法解决问题
#####方法一:项目根目录\platforms\android\cordova\Api.js文件作以下修改，否则执行cordova build命令会报异常：UnhandledPromiseRejectionWarning: Error: ENOENT: no such file or directory,......；

```javascript
this.locations = {
    root: self.root,
    www: path.join(self.root, 'assets/www'),
    res: path.join(self.root, 'res'),
    platformWww: path.join(self.root, 'platform_www'),
    configXml: path.join(self.root, 'app/src/main/res/xml/config.xml'),
    defaultConfigXml: path.join(self.root, 'cordova/defaults.xml'),
    strings: path.join(self.root, 'app/src/main/res/values/strings.xml'),
    manifest: path.join(self.root, 'app/src/main/AndroidManifest.xml'),
    build: path.join(self.root, 'build'),
    javaSrc: path.join(self.root, 'app/src/main/java/'),
    // NOTE: Due to platformApi spec we need to return relative paths here
    cordovaJs: 'bin/templates/project/assets/www/cordova.js',
    cordovaJsSrc: 'cordova-js-src'
};
```
#####然后手动将没有成功自动导入的jar包，手动放置到libs目录下
#####方法二:使用的cordova-android的版本小于7.0.0,如cordova platform add android@6.4.0


######1.进入项目的根目录，添加热更新插件:com.chinamobile.ftp.ftpspeed
* 为项目添加Camera插件，执行:`cordova plugin add com.chinamobile.ftp.ftpspeed`
* 如果要删除插件,执行:`cordova plugin add com.chinamobile.ftp.ftpspeed`
* 为项目添加对应的platform平台,已添加过，此步忽略，执行:
* 安卓平台: `cordova platform add android`
* ios平台: `cordova platform add ios`
* 将插件添加到对应平台,执行: `cordova build`

######2.在js文件中,通过以下js方法调用插件，获取FTP测速的实时数据。
*
```javascript
   location: function(){
        //向native发出FTP测速监听请求
        //参数元素0:0：下载 ，1：上传 ，2：关闭测速(不会执行3，4的删除操作) ,3：删除FTP上传的测试文件， 4：删除手机上的FTP下载测试文件
        //参数元素1:测速间隔:xxx/ms,单位:毫秒
        //参数元素2:FTP服务器ip地址
        //参数元素3:FTP服务器端口
        //参数元素4:FTP服务器账号
        //参数元素5:FTP服务器密码
        //参数元素6:FTP下载测速的文件名，由服务器提供
        //参数元素7:FTP上传测速的文件名，由客户端上传
        cordova.exec(success,error,"FTPSpeed","coolMethod",[0,500,"192.168.0.111",21,"test","test","a.zip","upload.zip"]);
    }
    
     success: function(var result){
        //ftpType，测试类型
        //下载:0
        //上传:1
        //关闭测速:2
        var ftpType  = result[0];
        //status 状态
        //0:下载/上传中
        //1:下载/上传结束
        //2:关闭测速;
        //3:需要先下载FTP测试文件到本地;
        //4:FTP服务器上没有目标FTP测试文件;
        //5:FTP服务器登录失败;
        //6:FTP服务器连接失败;
        //7:手机权限检测异常;
        //8:缺少网络权限或外部存储读写权限;
        //9:下载/上传中出现异常而中断;
        //10:回调函数的JSONArray构建异常;
        //11:删除FTP上传的测试文件失败;
        //12:删除FTP上传的测试文件成功;
        var status   = result[1];


        //speed， 实时速度，单位: /kbit/ms
        var speed    = result[2];
        //speedMax，峰值速度，单位: /kbit/ms
        var speedMax = result[3];
        //speedAver，平均速度，单位: /kbit/ms
        var speedAver= result[4];
        //progress，上传/下载进度(android端下载进度不可用)
        var progress = result[5];
    }

    error: function(var result){
        //FTP测速信息获取异常提示信息
         var ftpType  = result[0];
         //status 状态:同success函数
         var status   = result[1];
         //message 提示信息
         var message  = result[2];

    }
```
######说明:
* 1.[0,500,"192.168.0.111",21,"test","test","a.zip","18520660170.zip"],参照上图js代码
* 2.success函数:result是一个数组,元素0:测试类型，元素1：结果状态,元素2：实时速度,元素3：峰值速度，元素4：平均速度，元素5：进度(0-1.0)，
* 3.error函数:result是一个数组,元素0:测试类型，元素1：结果状态,元素2：提示信息，


##问题反馈
在使用中有任何问题，可以用以下联系方式.

* 邮件:18520660170@139.com
* 时间:2018-5-28 16:00:00
