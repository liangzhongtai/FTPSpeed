##FTPSpeed���ʹ��˵��
* �汾:2.5.0

##��������
* npm 4.4.1 +
* node 9.8.0 +


##ʹ������ 

#####ע��:��׿ƽ̨��������������jar�������cordova build ����󣬱������쳣:
#####��*\��Ŀ��\platforms\android\Androidmanifest.xml��
#####��*\��Ŀ��\platfroms\android\res\xml\config.xml��
#####��ͨ�����·����������
#####����һ:��Ŀ��Ŀ¼\platforms\android\cordova\Api.js�ļ��������޸ģ�����ִ��cordova build����ᱨ�쳣��UnhandledPromiseRejectionWarning: Error: ENOENT: no such file or directory,......��

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
#####Ȼ���ֶ���û�гɹ��Զ������jar�����ֶ����õ�libsĿ¼��
#####������:ʹ�õ�cordova-android�İ汾С��7.0.0,��cordova platform add android@6.4.0


######1.������Ŀ�ĸ�Ŀ¼������ȸ��²��:com.chinamobile.ftp.ftpspeed
* Ϊ��Ŀ���Camera�����ִ��:`cordova plugin add com.chinamobile.ftp.ftpspeed`
* ���Ҫɾ�����,ִ��:`cordova plugin add com.chinamobile.ftp.ftpspeed`
* Ϊ��Ŀ��Ӷ�Ӧ��platformƽ̨,����ӹ����˲����ԣ�ִ��:
* ��׿ƽ̨: `cordova platform add android`
* iosƽ̨: `cordova platform add ios`
* �������ӵ���Ӧƽ̨,ִ��: `cordova build`

######2.��js�ļ���,ͨ������js�������ò������ȡFTP���ٵ�ʵʱ���ݡ�
*
```javascript
   location: function(){
        //��native����FTP���ټ�������
        //����Ԫ��0:0������ ��1���ϴ� ��2���رղ���(����ִ��3��4��ɾ������) ,3��ɾ��FTP�ϴ��Ĳ����ļ��� 4��ɾ���ֻ��ϵ�FTP���ز����ļ�
        //����Ԫ��1:���ټ��:xxx/ms,��λ:����
        //����Ԫ��2:FTP������ip��ַ
        //����Ԫ��3:FTP�������˿�
        //����Ԫ��4:FTP�������˺�
        //����Ԫ��5:FTP����������
        //����Ԫ��6:FTP���ز��ٵ��ļ������ɷ������ṩ
        //����Ԫ��7:FTP�ϴ����ٵ��ļ������ɿͻ����ϴ�
        cordova.exec(success,error,"FTPSpeed","coolMethod",[0,500,"192.168.0.111",21,"test","test","a.zip","upload.zip"]);
    }
    
     success: function(var result){
        //ftpType����������
        //����:0
        //�ϴ�:1
        //�رղ���:2
        var ftpType  = result[0];
        //status ״̬
        //0:����/�ϴ���
        //1:����/�ϴ�����
        //2:�رղ���;
        //3:��Ҫ������FTP�����ļ�������;
        //4:FTP��������û��Ŀ��FTP�����ļ�;
        //5:FTP��������¼ʧ��;
        //6:FTP����������ʧ��;
        //7:�ֻ�Ȩ�޼���쳣;
        //8:ȱ������Ȩ�޻��ⲿ�洢��дȨ��;
        //9:����/�ϴ��г����쳣���ж�;
        //10:�ص�������JSONArray�����쳣;
        //11:ɾ��FTP�ϴ��Ĳ����ļ�ʧ��;
        //12:ɾ��FTP�ϴ��Ĳ����ļ��ɹ�;
        var status   = result[1];


        //speed�� ʵʱ�ٶȣ���λ: /kbit/ms
        var speed    = result[2];
        //speedMax����ֵ�ٶȣ���λ: /kbit/ms
        var speedMax = result[3];
        //speedAver��ƽ���ٶȣ���λ: /kbit/ms
        var speedAver= result[4];
        //progress���ϴ�/���ؽ���(android�����ؽ��Ȳ�����)
        var progress = result[5];
    }

    error: function(var result){
        //FTP������Ϣ��ȡ�쳣��ʾ��Ϣ
         var ftpType  = result[0];
         //status ״̬:ͬsuccess����
         var status   = result[1];
         //message ��ʾ��Ϣ
         var message  = result[2];

    }
```
######˵��:
* 1.[0,500,"192.168.0.111",21,"test","test","a.zip","18520660170.zip"],������ͼjs����
* 2.success����:result��һ������,Ԫ��0:�������ͣ�Ԫ��1�����״̬,Ԫ��2��ʵʱ�ٶ�,Ԫ��3����ֵ�ٶȣ�Ԫ��4��ƽ���ٶȣ�Ԫ��5������(0-1.0)��
* 3.error����:result��һ������,Ԫ��0:�������ͣ�Ԫ��1�����״̬,Ԫ��2����ʾ��Ϣ��


##���ⷴ��
��ʹ�������κ����⣬������������ϵ��ʽ.

* �ʼ�:18520660170@139.com
* ʱ��:2018-5-28 16:00:00
