package com.chinamobile.ftp;

import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;


import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;

/**
 * Created by liangzhongtai on 2018/5/25.
 */

public class FTPSpeedUtil {
    private volatile static FTPSpeedUtil uniqueInstance;
    private FTPClient ftpClient;
    public FTPSpeed.FTPSpeedListener listener;
    public int ftpType;
    public int interval = 500;
    //public String sdFileName;
    public boolean uploadStart;
    public boolean downloadStart;

    public String ftpIp;
    public int ftpPort;
    public String ftpUN;
    public String ftpPW;
    public String ftpFileName;
    public String sdFileName;
    public String ftpOriFileName;

    private FTPSpeedUtil() {
        getFTPSpeed();
    }

    public static FTPSpeedUtil getInstance() {
        if (uniqueInstance == null) {
            synchronized (FTPSpeedUtil.class) {
                if (uniqueInstance == null) {
                    uniqueInstance = new FTPSpeedUtil();
                }
            }
        }
        return uniqueInstance;
    }


    private void getFTPSpeed() {
        ftpClient = new FTPClient();
    }


    protected void upload() {
        if(!checkFTPSpeedFile()){
            listener.sendFTPSpeedError(FTPSpeed.UPLOAD,FTPSpeed.DOWNLOAD_FIRST,"请先下载FTP测试文件");
            return;
        }else if(uploadStart){
            listener.sendFTPSpeedError(FTPSpeed.UPLOAD,FTPSpeed.TESTING,"正在进行FTP上传测试");
            return;
        }
        ftpType = FTPSpeed.UPLOAD;
        uploadStart = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (connect()) {
                    uploadFile();
                }else{
                    uploadStart = false;
                }
            }
        }).start();
    }


    protected void download() {
        ftpType = FTPSpeed.DOWNLOAD;
        if(downloadStart){
            listener.sendFTPSpeedError(FTPSpeed.UPLOAD,FTPSpeed.TESTING,"正在进行下载FTP测试");
            return;
        }
        downloadStart = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                //test.mainTest(null);
                if (connect()) {
                    downloadFile();
                }else{
                    downloadStart = false;
                }
            }
        }).start();
    }


    private boolean checkFTPSpeedFile() {
        if(TextUtils.isEmpty(sdFileName))return false;
        String fileName = sdFileName;
        String localPath = Environment.getExternalStorageDirectory() + "/"+ fileName;
        File localFile = new File(localPath);

        return localFile.exists();
    }

    //连接FTP服务器
    public synchronized boolean connect(){
        boolean bool = false;
        try {
            if (ftpClient.isConnected()) {
                ftpClient.disconnect();
            }
            ftpClient.setDataTimeout(60000);
            ftpClient.connect(ftpIp, ftpPort);
            int reply = ftpClient.getReplyCode();
            if (FTPReply.isPositiveCompletion(reply)) {
                if (ftpClient.login(ftpUN, ftpPW)) {
                    bool = true;
                }else{
                    sendMessage(ftpType,FTPSpeed.LOGIN_FAILE);
                }
            }else{
                ftpClient.disconnect();
                //if(listener!=null)
                //listener.sendFTPSpeedError(ftpType,FTPSpeed.LOGIN_FAILE,"FTP服务器登录异常_reply="+reply);
                sendMessage(ftpType,FTPSpeed.LOGIN_FAILE);
            }
        } catch (Exception e) {
            e.printStackTrace();
            //if(listener!=null)
            //listener.sendFTPSpeedError(ftpType,FTPSpeed.CONNECT_FAILE,"FTP服务器连接异常_error="+e.toString());
            sendMessage(ftpType,FTPSpeed.CONNECT_FAILE);
        }
        return bool;
    }


    //上传测试
    public synchronized void uploadFile() {
        RandomAccessFile raf = null;
        OutputStream output = null;
        boolean error = false;
        try {
            String localFilePath = Environment.getExternalStorageDirectory() + "/"+sdFileName;
            File file = new File(localFilePath);
            if (!file.exists()) {
                sendMessage(ftpType,FTPSpeed.FTP_NO_FILE);
                //if(listener!=null)
                //listener.sendFTPSpeedError(ftpType,FTPSpeed.FTP_NO_FILE,"FTP服务器上没有目标测试文件");
                uploadStart = false;
                error = true;
                return;
            }
            String fileName = file.getName();
            long serverSize = 0;
            raf = new RandomAccessFile(file, "rw");
            ftpClient.enterLocalPassiveMode();
            //设置文件格式为二进制
            ftpClient.setControlEncoding("UTF-8");
            ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE);
            ftpClient.setRestartOffset(serverSize);
            //ftpClient.changeWorkingDirectory("/");
            raf.seek(serverSize);
            long start = System.currentTimeMillis();
            output = ftpClient.appendFileStream(fileName);
            byte[] b = new byte[1024];
            int length = 0;
            float localsize = file.length();
            localsize = localsize<=0?1000000000:localsize;
            long preTime = start;
            long nowLength = length;
            long preLength = length;
            long speedMax = 0;
            while ((length = raf.read(b)) != -1) {
                if(ftpType==FTPSpeed.CLOSE)break;
                nowLength += length;
                long nowTime = System.currentTimeMillis();
                if(nowTime-preTime>=interval){
                    long speed = (nowLength-preLength)*8/(nowTime-preTime);
                    speedMax = speedMax>speed?speedMax:speed;
                    sendMessage(ftpType,FTPSpeed.TESTING,speed,speedMax,nowLength*8/(nowTime-start),nowLength/localsize);
                    preTime = nowTime;
                    preLength = nowLength;
                }
                output.write(b, 0, length);
            }
        }catch (Exception e) {
            e.printStackTrace();
            sendMessage(FTPSpeed.CLOSE,FTPSpeed.TESTING_ERROR,0,0,0,0);
            error = true;
        } finally {
            uploadStart = false;
            if(!error){
                sendMessage(ftpType,FTPSpeed.FINISH,0,0,0,1);
            }
            try {
                if(output!=null){
                    output.flush();
                    output.close();
                }
                if(raf!=null)raf.close();
                if(sdFileName!=null&&!sdFileName.equals(ftpOriFileName))
                ftpClient.deleteFile(sdFileName);
                ftpClient.logout();
            }catch (Exception e){
                sendMessage(ftpType,FTPSpeed.TESTING_ERROR,0,0,0,0);
            }
        }
    }

    protected void downloadFile() {
        OutputStream out = null;
        InputStream input = null;
        boolean error = false;
        try {
            String fileName = sdFileName;
            String localPath = Environment.getExternalStorageDirectory() + "/"+ fileName;
            String ftpPath = ftpFileName;
            float localSize = 0;
            File localFile = new File(localPath);
            if(localFile.exists()){
                localFile.delete();
            }
            localFile.createNewFile();
            long start = System.currentTimeMillis();
            ftpClient.enterLocalPassiveMode();
            //设置文件格式为二进制
            ftpClient.setControlEncoding("UTF-8");
            ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE);
            //ftpClient.changeWorkingDirectory("/");

            //检测FTP服务器上的FTP测试文件是否存在
            FTPFile[] files = ftpClient.listFiles(ftpFileName);
            if(files!=null&&files.length>0){
                localSize = files[0].getSize();
            }

            out = new FileOutputStream(localFile);
            input = ftpClient.retrieveFileStream(ftpPath);
            Log.d(FTPSpeed.TAG,"intput="+input);

            if(input == null) {
                error = true;
                return;
            }
            byte[] b = new byte[1024];
            int length = 0;

            localSize = localSize<=0?1000000000:localSize;
            Log.d(FTPSpeed.TAG,"localSize="+localSize);
            long preTime = start;
            long nowLength = length;
            long preLength = length;
            long speedMax = 0;
            while ((length = input.read(b)) != -1) {
                if(ftpType==FTPSpeed.CLOSE)break;
                nowLength += length;
                long nowTime = System.currentTimeMillis();
                if(nowTime-preTime>=interval){
                    long speed = (nowLength-preLength)*8/(nowTime-preTime);
                    speedMax = speedMax>speed?speedMax:speed;
                    sendMessage(ftpType,FTPSpeed.TESTING,speed,speedMax,nowLength*8/(nowTime-start),nowLength/localSize);
                    preTime = nowTime;
                    preLength = nowLength;
                }
                out.write(b, 0, length);
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendMessage(ftpType,FTPSpeed.TESTING_ERROR,0,0,0,0);
            error = true;
        } finally {
            downloadStart = false;
            if(!error){
                sendMessage(ftpType,FTPSpeed.FINISH,0,0,0,1);
            }
            try {
                if(out!=null){
                    out.flush();
                    out.close();
                }
                if(input!=null)input.close();
                ftpClient.logout();
            }catch (Exception e){
                sendMessage(ftpType,FTPSpeed.TESTING_ERROR,0,0,0,0);
            }
        }
    }

    private void sendMessage(int ftpType,int status,long speed,long speedMax,long speedAver,float progress){
        Bundle bundle = new Bundle();
        //TODO
       /* try {
            if (ftpClient.completePendingCommand()) {
                bundle.putBoolean(ftpType==FTPSpeed.DOWNLOAD?"isDownload":"isUpload", true);
            } else {
                bundle.putBoolean(ftpType==FTPSpeed.DOWNLOAD?"isDownload":"isUpload", false);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }*/
        bundle.putInt("ftpType", ftpType);
        bundle.putInt("status",  status);
        bundle.putLong("speed",  speed);
        bundle.putLong("speedMax",  speedMax);
        bundle.putLong("speedAver", speedAver);
        bundle.putFloat("progress", progress);
        Message msg = new Message();
        msg.setData(bundle);
        if(ftpType==FTPSpeed.DOWNLOAD) {
            mDownloadHandler.sendMessage(msg);
        }else if(ftpType==FTPSpeed.UPLOAD){
            mUploadHandler.sendMessage(msg);
        }
    }

    private void sendMessage(int ftpType,int status){
        Bundle bundle = new Bundle();
        bundle.putInt("ftpType", ftpType);
        bundle.putInt("status",  status);
        Message msg = new Message();
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }




    public boolean createDirectory(String path) throws Exception {
        boolean bool = false;
        String directory = path.substring(0, path.lastIndexOf("/") + 1);
        int start = 0;
        int end = 0;
        if (directory.startsWith("/")) {
            start = 1;
        }
        end = directory.indexOf("/", start);
        while (true) {
            String subDirectory = directory.substring(start, end);
            if (!ftpClient.changeWorkingDirectory(subDirectory)) {
                ftpClient.makeDirectory(subDirectory);
                ftpClient.changeWorkingDirectory(subDirectory);
                bool = true;
            }
            start = end + 1;
            end = directory.indexOf("/", start);
            if (end == -1) {
                break;
            }
        }
        return bool;
    }

    public void removeFTPSpeedListener() {
        ftpType = FTPSpeed.CLOSE;
        try {
            if(ftpClient!=null) {
                ftpClient.logout();
                if(ftpClient.isConnected())
                ftpClient.disconnect();
            }

            if(mDownloadHandler!=null)mDownloadHandler.removeCallbacksAndMessages(null);
            if(mUploadHandler!=null)mUploadHandler.removeCallbacksAndMessages(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void deleteFTPFile(final boolean message){
        if(ftpFileName==null||ftpFileName.equals(ftpOriFileName))return;
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (connect()) {
                    boolean error = false;
                    try {
                        ftpClient.deleteFile(ftpFileName);
                    } catch (Exception e) {
                        error = true;
                        if (message)
                            sendMessage(FTPSpeed.DELETE_FTP_FILE, FTPSpeed.DELETE_FTP_FILE_FAILE);
                    } finally {
                        if (!error && message) {
                            sendMessage(FTPSpeed.DELETE_FTP_FILE, FTPSpeed.DELETE_FTP_FILE_SUCCESS);
                        }
                    }
                }
            }
        }).start();
    }

    public void deleteSdFile(boolean message){
        new File(Environment.getExternalStorageDirectory(),sdFileName).delete();
        if(message)
        sendMessage(FTPSpeed.DELETE_SD_FILE,FTPSpeed.DELETE_SD_FILE_SUCCESS);
    }



    private Handler mUploadHandler = new Handler(){
        public void dispatchMessage(Message msg) {
            Bundle bundle = msg.getData();
            int ftpType   = bundle.getInt("ftpType");
            int status    = bundle.getInt("status");
            long speed    = bundle.getLong("speed");
            long speedMax = bundle.getLong("speedMax");
            long speedAver= bundle.getLong("speedAver");
            float progress= bundle.getFloat("progress");
            Log.d(FTPSpeed.TAG,"progress="+progress);
            //Log.d(FTPSpeed.TAG,"speed="+speed);
            //Log.d(FTPSpeed.TAG,"speedMax="+speedMax);
            //Log.d(FTPSpeed.TAG,"speedAver="+speedAver);
            if(listener==null)return;
            if(status == FTPSpeed.FTP_NO_FILE){
                listener.sendFTPSpeedError(ftpType,status,"FTP服务器上没有目标测试文件");
                return;
            }else if(status == FTPSpeed.TESTING_ERROR){
                listener.sendFTPSpeedError(ftpType,status,"FTP下载测试异常已关闭");
                return;
            }

            if(status==FTPSpeed.FINISH){
                listener.sendFTPSpeedMessage(ftpType,status,0,0,0,progress);
            }else{
                listener.sendFTPSpeedMessage(FTPSpeed.UPLOAD,FTPSpeed.TESTING,speed,speedMax,speedAver,progress);
            }

        }
    };

    private Handler mDownloadHandler = new Handler(){
        public void dispatchMessage(Message msg) {
            Bundle bundle = msg.getData();
            int ftpType   = bundle.getInt("ftpType");
            int status    = bundle.getInt("status");
            long speed    = bundle.getLong("speed");
            long speedMax = bundle.getLong("speedMax");
            long speedAver= bundle.getLong("speedAver");
            float progress= bundle.getFloat("progress");
            Log.d(FTPSpeed.TAG,"progress="+progress);
            //Log.d(FTPSpeed.TAG,"speed="+speed);
            //Log.d(FTPSpeed.TAG,"speedMax="+speedMax);
            //Log.d(FTPSpeed.TAG,"speedAver="+speedAver);
            if(listener==null)return;
            if(status == FTPSpeed.FTP_NO_FILE){
                listener.sendFTPSpeedError(ftpType,status,"FTP服务器上没有目标测试文件");
                return;
            }else if(status == FTPSpeed.TESTING_ERROR){
                listener.sendFTPSpeedError(ftpType,status,"FTP下载测试异常已关闭");
                return;
            }

            if(status==FTPSpeed.FINISH){
                listener.sendFTPSpeedMessage(ftpType,status,0,0,0,progress);
            }else {
                listener.sendFTPSpeedMessage(FTPSpeed.DOWNLOAD, FTPSpeed.TESTING,speed, speedMax, speedAver, progress);
            }
        }
    };

    private Handler mHandler = new Handler(){
        public void dispatchMessage(Message msg) {
            Bundle bundle = msg.getData();
            int ftpType   = bundle.getInt("ftpType");
            int status    = bundle.getInt("status");
            if(status == FTPSpeed.DELETE_FTP_FILE_FAILE){
                listener.sendFTPSpeedError(ftpType,status,"FTP服务器上没有目标测试文件");
            }else if(status == FTPSpeed.DELETE_FTP_FILE_SUCCESS||status == FTPSpeed.DELETE_SD_FILE_SUCCESS){
                listener.sendFTPSpeedMessage(ftpType,status,0,0,0,0);
            }else if(status == FTPSpeed.LOGIN_FAILE){
                listener.sendFTPSpeedError(ftpType,status,"FTP服务器登录失败");
            }else if(status == FTPSpeed.CONNECT_FAILE){
                listener.sendFTPSpeedError(ftpType,status,"FTP服务器连接失败");
            }
        }
    };
}
