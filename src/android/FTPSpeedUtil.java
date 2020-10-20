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
import org.apache.cordova.CallbackContext;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;

/**
 * Created by liangzhongtai on 2018/5/25.
 */

public class  FTPSpeedUtil {
    // 公网服务器
    // 多线程下载，上传
    // 1G文件
    private volatile static FTPSpeedUtil uniqueInstance;
    private FTPClient ftpClient;
    public FTPSpeed.FTPSpeedListener listener;
    public int ftpType;
    public int interval = 100;
    //public String sdFileName;
    public boolean uploadStart;
    public boolean downloadStart;

    public String ftpIp;
    public int ftpPort;
    public String ftpUN;
    public String ftpPW;
    public String uploadFtpPath;
    public String downloadFtpPath;
    public String ftpFileName;
    public String sdFileName;
    public String ftpOriFileName;
    public int threadCount;
    public CallbackContext callbackContext;

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


    protected void upload(CallbackContext callbackContext) {
        this.callbackContext = callbackContext;
        if (!checkFTPSpeedFile()) {
            listener.sendFTPSpeedError(FTPSpeed.UPLOAD, FTPSpeed.DOWNLOAD_FIRST,
                    "请先下载FTP测试文件", callbackContext);
            return;
        } else if (uploadStart) {
            listener.sendFTPSpeedError(FTPSpeed.UPLOAD, FTPSpeed.TESTING,
                    "正在进行FTP上传测试", callbackContext);
            return;
        }
        ftpType = FTPSpeed.UPLOAD;
        uploadStart = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (connect()) {
                    uploadFile(callbackContext);
                }else{
                    uploadStart = false;
                }
            }
        }).start();
    }

    protected void download(CallbackContext callbackContext) {
        this.callbackContext = callbackContext;
        ftpType = FTPSpeed.DOWNLOAD;
        if(downloadStart){
            listener.sendFTPSpeedError(FTPSpeed.UPLOAD, FTPSpeed.TESTING,
                    "正在进行下载FTP测试", callbackContext);
            return;
        }
        downloadStart = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                //test.mainTest(null);
                if (connect()) {
                    downloadFile(callbackContext);
                }else{
                    downloadStart = false;
                }
            }
        }).start();
    }


    private boolean checkFTPSpeedFile() {
        if (TextUtils.isEmpty(sdFileName)) {
            return false;
        }
        String fileName = sdFileName;
        String localPath = Environment.getExternalStorageDirectory() + "/"+ fileName;
        File localFile = new File(localPath);

        return localFile.exists();
    }

    //连接FTP服务器
    public synchronized boolean connect(){
        boolean bool = false;
        try {
            try {
                if (ftpClient != null && ftpClient.isConnected()) {
                    Log.d(FTPSpeed.TAG, "ftpClient已经处于链接，先断开链接");
                    ftpClient.logout();
                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.d(FTPSpeed.TAG, "登录前，登出ftpe=" + e.toString());
            }
            ftpClient = new FTPClient();
            ftpClient.setDataTimeout(60000);
            ftpClient.connect(ftpIp, ftpPort);
            ftpClient.login(ftpUN, ftpPW);
            int reply = ftpClient.getReplyCode();
            if (!FTPReply.isPositiveCompletion(reply)) {
                sendMessage(ftpType, FTPSpeed.LOGIN_FAILE);
                try {
                    ftpClient.disconnect();
                }  catch (Exception e) {
                    Log.d(FTPSpeed.TAG, "登录失败，e=" + e.toString());
                    e.printStackTrace();
                }
            } else {
                bool = true;
            }

        } catch (Exception e) {
            e.printStackTrace();
            Log.d(FTPSpeed.TAG, "连接失败，e=" + e.toString());
            sendMessage(ftpType, FTPSpeed.CONNECT_FAILE);
        }
        return bool;
    }


    //上传测试
    public synchronized void uploadFile(CallbackContext callbackContext) {
        RandomAccessFile raf = null;
        OutputStream output = null;
        boolean error = false;
        long start = System.currentTimeMillis();
        long nowLength = 0;
        try {
            String localFilePath = Environment.getExternalStorageDirectory() + "/"+sdFileName;
            File file = new File(localFilePath);
            if (!file.exists()) {
                sendMessage(ftpType,FTPSpeed.FTP_NO_FILE);
                uploadStart = false;
                error = true;
                return;
            }
            String fileName = file.getName();
            // fileName = new String(fileName.getBytes("UTF-8"), "ISO-8859-1");
            long serverSize = 0;
            raf = new RandomAccessFile(file, "rw");
            // TODO
            ftpClient.enterLocalPassiveMode();
            //设置文件格式为二进制
            ftpClient.setControlEncoding("UTF-8");
            ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE);
            ftpClient.setRestartOffset(serverSize);
            if (TextUtils.isEmpty(uploadFtpPath)) {
                ftpClient.changeWorkingDirectory("/");
            } else {
                ftpClient.changeWorkingDirectory(uploadFtpPath);
            }
            Log.d(FTPSpeed.TAG, "上传文件_fileName=" + fileName);
            Log.d(FTPSpeed.TAG, "上传文件大小_sice=" + file.length());
            ftpClient.changeWorkingDirectory("/");
            raf.seek(serverSize);

            output = ftpClient.appendFileStream(fileName);
            byte[] b = new byte[1024];
            int length = 0;
            float localsize = file.length();
            localsize = localsize <= 0 ? 1000000000 : localsize;
            long preTime = start;
            long preLength = length;
            long speedMax = 0;
            while ((length = raf.read(b)) != -1) {
                Log.d(FTPSpeed.TAG, "上传文件写入_length=" + length);
                if (ftpType == FTPSpeed.CLOSE) {
                    break;
                }
                nowLength += length;
                long nowTime = System.currentTimeMillis();
                if (nowTime-preTime >= interval) {
                    long speed = (nowLength - preLength) * 8 / (nowTime - preTime);
                    speedMax = speedMax > speed ? speedMax:speed;
                    sendMessage(ftpType, FTPSpeed.TESTING, speed, speedMax,
                            nowLength * 8/ (nowTime - start),
                            nowLength / localsize,
                            nowTime - start, nowLength);
                    preTime = nowTime;
                    preLength = nowLength;
                }
                output.write(b, 0, length);
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendMessage(ftpType, FTPSpeed.CLOSE,0,0,0,
                    0,0,0);
            error = true;
        } finally {
            uploadStart = false;
            if (!error) {
                sendMessage(ftpType, FTPSpeed.FINISH,0,0,0,1,
                        System.currentTimeMillis()-start,nowLength);
            }
            try {
                if (output != null) {
                    output.flush();
                    output.close();
                }
                if (raf != null) {
                    raf.close();
                }
                if (sdFileName != null && !sdFileName.equals(ftpOriFileName)) {
                    ftpClient.deleteFile(sdFileName);
                }
                if (ftpClient.isConnected()) {
                    Log.d(FTPSpeed.TAG, "上传结束，关闭链接");
                    ftpClient.logout();
                }
            } catch (Exception e){
                Log.d(FTPSpeed.TAG, "上传结束e=" + e.toString());
                // sendMessage(ftpType, FTPSpeed.TESTING_ERROR, 0, 0, 0, 0, 0, 0);
            }
        }
    }

    protected void downloadFile(CallbackContext callbackContext) {
        OutputStream out = null;
        InputStream input = null;
        boolean error = false;
        long start = System.currentTimeMillis();
        long nowLength = 0;
        try {
            String fileName = sdFileName;
            String localPath = Environment.getExternalStorageDirectory() + "/"+ fileName;
            String ftpPath = ftpFileName;
            ftpFileName = new String(ftpFileName.getBytes("UTF-8"), "ISO-8859-1");
            float localSize = 0;
            File localFile = new File(localPath);
            if (localFile.exists()) {
                localFile.delete();
            }
            localFile.createNewFile();
            // TODO
            ftpClient.enterLocalPassiveMode();
            //设置文件格式为二进制
            ftpClient.setControlEncoding("UTF-8");
            ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE);
            if (TextUtils.isEmpty(downloadFtpPath)) {
                ftpClient.changeWorkingDirectory("/");
            } else {
                ftpClient.changeWorkingDirectory(downloadFtpPath);
            }
            Log.d(FTPSpeed.TAG, "下载路径_downloadFtpPath=" + downloadFtpPath);
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
                sendMessage(ftpType, FTPSpeed.FTP_NO_FILE, 0, 0, 0,
                        0, 0, 0);
                return;
            }
            byte[] b = new byte[1024];
            int length = 0;

            localSize = localSize<=0?1000000000:localSize;
            Log.d(FTPSpeed.TAG,"localSize="+localSize);
            long preTime = start;
            long preLength = length;
            long speedMax = 0;
            while ((length = input.read(b)) != -1) {
                if (ftpType == FTPSpeed.CLOSE) {
                    break;
                }
                nowLength += length;
                long nowTime = System.currentTimeMillis();
                if (nowTime - preTime >= interval){
                    long speed = (nowLength - preLength) * 8/(nowTime - preTime);
                    speedMax = speedMax > speed ? speedMax : speed;
                    sendMessage(ftpType, FTPSpeed.TESTING, speed, speedMax,
                            nowLength * 8 /(nowTime - start),
                            nowLength / localSize,
                            nowTime - start, nowLength);
                    preTime = nowTime;
                    preLength = nowLength;
                }
                out.write(b, 0, length);
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendMessage(ftpType, FTPSpeed.TESTING_ERROR,0,0,0,
                    0,0,0);
            error = true;
        } finally {
            downloadStart = false;
            if (!error) {
                sendMessage(ftpType, FTPSpeed.FINISH, 0, 0, 0,
                        1,System.currentTimeMillis()-start,nowLength);
            }
            try {
                if (out != null) {
                    out.flush();
                    out.close();
                }
                if (input!=null) {
                    input.close();
                }
                if (ftpClient.isConnected()) {
                    Log.d(FTPSpeed.TAG, "下载结束，关闭链接");
                    ftpClient.logout();
                }
            } catch (Exception e) {
                Log.d(FTPSpeed.TAG, "下载结束e=" + e.toString());
                // sendMessage(ftpType,FTPSpeed.TESTING_ERROR,0,0,0, 0,0,0);
            }
        }
    }

    private void sendMessage(int ftpType, int status, long speed, long speedMax, long speedAver,
                             float progress, long totalTime, long totalSize){
        Bundle bundle = new Bundle();
        bundle.putInt("ftpType", ftpType);
        bundle.putInt("status",  status);
        bundle.putLong("speed",  speed);
        bundle.putLong("speedMax",  speedMax);
        bundle.putLong("speedAver", speedAver);
        bundle.putFloat("progress", progress);
        bundle.putLong("time", totalTime);
        bundle.putLong("size", totalSize);
        Message msg = new Message();
        msg.setData(bundle);
        if(ftpType == FTPSpeed.DOWNLOAD) {
            mDownloadHandler.sendMessage(msg);
        }else if(ftpType == FTPSpeed.UPLOAD){
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
        downloadStart = false;
        uploadStart = false;
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
        }
        try {
            if (mDownloadHandler != null) {
                mDownloadHandler.removeCallbacksAndMessages(null);
            }
            if (mUploadHandler != null) {
                mUploadHandler.removeCallbacksAndMessages(null);
            }
            if (ftpClient != null && ftpClient.isConnected()) {
                Log.d(FTPSpeed.TAG, "ftp连接中，removeFTPSpeedListener");
                ftpClient.logout();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(FTPSpeed.TAG, "removeFTPSpeedListener，e=" + e.toString());
        }
    }

    public void deleteFTPFile(final boolean message) {
        if (ftpFileName == null || ftpFileName.equals(ftpOriFileName)) {
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (connect()) {
                    boolean error = false;
                    try {
                        ftpClient.deleteFile(ftpFileName);
                    } catch (Exception e) {
                        error = true;
                        if (message) {
                            sendMessage(FTPSpeed.DELETE_FTP_FILE, FTPSpeed.DELETE_FTP_FILE_FAILE);
                        }
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
        if (message) {
            sendMessage(FTPSpeed.DELETE_SD_FILE, FTPSpeed.DELETE_SD_FILE_SUCCESS);
        }
    }


    private Handler mUploadHandler = new Handler(){
        @Override
        public void dispatchMessage(Message msg) {
            Bundle bundle = msg.getData();
            int ftpType   = bundle.getInt("ftpType");
            int status    = bundle.getInt("status");
            long speed    = bundle.getLong("speed");
            long speedMax = bundle.getLong("speedMax");
            long speedAver= bundle.getLong("speedAver");
            float progress= bundle.getFloat("progress");
            long totalTime= bundle.getLong("time");
            long totalSize= bundle.getLong("size");
            Log.d(FTPSpeed.TAG,"progress="+progress);
            if (listener==null) {
                return;
            }
            if(status == FTPSpeed.FTP_NO_FILE){
                listener.sendFTPSpeedError(ftpType, status,
                        "FTP服务器上没有目标测试文件", callbackContext);
                return;
            }else if(status == FTPSpeed.TESTING_ERROR){
                listener.sendFTPSpeedError(ftpType, status,
                        "FTP下载测试异常已关闭", callbackContext);
                return;
            }

            if(status==FTPSpeed.FINISH){
                listener.sendFTPSpeedMessage(ftpType, status,0,0,0,
                        progress, totalTime, totalSize, callbackContext);
            }else{
                listener.sendFTPSpeedMessage(FTPSpeed.UPLOAD, FTPSpeed.TESTING, speed, speedMax,
                        speedAver, progress, totalTime, totalSize, callbackContext);
            }

        }
    };

    private Handler mDownloadHandler = new Handler(){
        @Override
        public void dispatchMessage(Message msg) {
            Bundle bundle = msg.getData();
            int ftpType   = bundle.getInt("ftpType");
            int status    = bundle.getInt("status");
            long speed    = bundle.getLong("speed");
            long speedMax = bundle.getLong("speedMax");
            long speedAver= bundle.getLong("speedAver");
            float progress= bundle.getFloat("progress");
            long totalTime= bundle.getLong("time");
            long totalSize= bundle.getLong("size");
            Log.d(FTPSpeed.TAG,"progress="+progress);
            //Log.d(FTPSpeed.TAG,"speed="+speed);
            //Log.d(FTPSpeed.TAG,"speedMax="+speedMax);
            //Log.d(FTPSpeed.TAG,"speedAver="+speedAver);
            if (listener==null) {
                return;
            }
            if (status == FTPSpeed.FTP_NO_FILE) {
                listener.sendFTPSpeedError(ftpType, status,
                        "FTP服务器上没有目标测试文件", callbackContext);
                return;
            } else if(status == FTPSpeed.TESTING_ERROR) {
                listener.sendFTPSpeedError(ftpType, status,
                        "FTP下载测试异常已关闭", callbackContext);
                return;
            }

            if (status == FTPSpeed.FINISH) {
                listener.sendFTPSpeedMessage(ftpType, status, 0,0, 0,
                        progress, totalTime, totalSize, callbackContext);
            } else {
                listener.sendFTPSpeedMessage(FTPSpeed.DOWNLOAD, FTPSpeed.TESTING, speed, speedMax,
                        speedAver, progress, totalTime, totalSize, callbackContext);
            }
        }
    };

    private Handler mHandler = new Handler(){
        @Override
        public void dispatchMessage(Message msg) {
            Bundle bundle = msg.getData();
            int ftpType   = bundle.getInt("ftpType");
            int status    = bundle.getInt("status");
            if (listener == null) {
                return;
            }
            if(status == FTPSpeed.DELETE_FTP_FILE_FAILE) {
                listener.sendFTPSpeedError(ftpType, status,
                        "FTP服务器上没有目标测试文件", callbackContext);
            } else if (status == FTPSpeed.DELETE_FTP_FILE_SUCCESS||
                    status == FTPSpeed.DELETE_SD_FILE_SUCCESS) {
                listener.sendFTPSpeedMessage(ftpType, status, 0, 0, 0,
                        0,0,0, callbackContext);
            } else if (status == FTPSpeed.LOGIN_FAILE) {
                listener.sendFTPSpeedError(ftpType, status,
                        "FTP服务器登录失败", callbackContext);
            } else if (status == FTPSpeed.CONNECT_FAILE) {
                listener.sendFTPSpeedError(ftpType, status,
                        "FTP服务器连接失败", callbackContext);
            }
        }
    };
}
