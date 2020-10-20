package com.chinamobile.ftp;

import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.cordova.CallbackContext;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


/**
 * Created by liangzhongtai on 2020/2/4.
 */

public class FTPSingleTask {
    public long startL;
    public long endL;
    public long fileLength;
    public int threadId;
    public String ftpIp;
    public int ftpPort;
    public String ftpUN;
    public String ftpPW;
    public String fileName;
    public String sdFileName;
    public String charSet;
    public int interval;
    public int threadCount;
    // public RandomAccessFile randomAccessFile;
    public File downFile;
    public File upFile;
    public FTPClient client;
    public boolean isCloseAdvance;
    public FTPSingleTaskListener listener;
    public CallbackContext callBack;

    public FTPSingleTask(int threadId, long startL, long endL, long fileLength) {
        this.threadId = threadId;
        this.startL = startL;
        this.endL = endL;
        this.fileLength = fileLength;
        isCloseAdvance = false;
    }

    /**
     * 下载
     * */
    public void startDownload() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (client != null && client.isConnected()) {
                    try {
                        client.logout();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                client = new FTPClient();
                boolean error = false;
                try {
                    client.setDataTimeout(60000);
                    client.connect(ftpIp, ftpPort);
                    client.login(ftpUN, ftpPW);
                    int reply = client.getReplyCode();
                    if (!FTPReply.isPositiveCompletion(reply)) {
                        sendMessage(HANDLER_LOGIN_FAIL, true, "无法连接到ftp服务器，错误码为：" + reply);
                        try {
                            client.disconnect();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        Log.d(FTPSpeed.TAG, "无法连接到ftp服务器，错误码为：" + reply);
                        return;
                    }
                } catch (Exception e) {
                    sendMessage(HANDLER_LOGIN_FAIL, true, "无法连接到ftp服务器，IO异常");
                    e.printStackTrace();
                    return;
                }
                InputStream is = null;
                FTPSpeedInfo info = new FTPSpeedInfo();
                try {
                    client.enterLocalPassiveMode();    //设置被动模式
                    client.setFileType(FTP.BINARY_FILE_TYPE);//设置文件传输模式
                    client.setRestartOffset(startL);//设置恢复下载的位置
                    client.changeWorkingDirectory("/");
                    client.allocate(1024);
                    is = client.retrieveFileStream(new String(fileName.getBytes(charSet), "ISO-8859-1"));
                    Log.d(FTPSpeed.TAG, "下载5_startL=" + startL);
                    Log.d(FTPSpeed.TAG, "下载5_endL=" + endL);
                    Log.d(FTPSpeed.TAG, "下载5_fileName=" + fileName);
                    Log.d(FTPSpeed.TAG, "下载6_fileName=" + is);

                    downFile = getFile("", sdFileName + threadId);
                    if (downFile.exists()) {
                        downFile.delete();
                        downFile.createNewFile();
                    }
                    FileOutputStream output = new FileOutputStream(downFile);
                    byte[] buffer = new byte[1024];
                    int len;
                    long startTime = System.currentTimeMillis();
                    long currentL = startL;
                    long preTime = startTime;
                    long preLength = startL;
                    long speedMax = 0;
                    long speedAver = 0;

                    sendMessage(HANDLER_DOWNLOAD_START);

                    info.threadId = threadId;
                    info.filePath = downFile.getPath();
                    info.fileSize = endL - startL;
                    info.oriPath = Environment.getExternalStorageDirectory() + "/" + sdFileName;
                    info.oriSize = fileLength;
                    info.threadCount = threadCount;
                    long preSpeed = 0;
                    while ((len = is.read(buffer)) != -1 && client != null && !isCloseAdvance) {
                        //如果该条线程读取的数据长度大于所分配的区间长度，则只能读到区间的最大长度
                        if (currentL + len >= endL) {
                            len = (int) (endL - currentL);
                            output.write(buffer, 0, len);
                        } else {
                            output.write(buffer, 0, len);
                        }
                        currentL = currentL + len;
                        long nowTime = System.currentTimeMillis();
                        if (nowTime - preTime > interval) {
                            long speed = (currentL - preLength) * 8 / (nowTime - preTime);
                            speedMax = speedMax > speed ? speedMax : speed;
                            speedAver = (currentL -startL) * 8 / (nowTime - startTime);
                            info.speed = speed;
                            info.speedMax = speedMax;
                            info.speedAver = speedAver;
                            info.nowSize = currentL - startL;
                            info.progress = (currentL - startL) / info.fileSize;
                            sendMessage(HANDLER_DOWNLOADING, true, FTPSpeed.TESTING,
                                    threadId, info, "");
                            preTime = nowTime;
                            preLength = currentL;
                            preSpeed = speed;
                        }
                        if (currentL>= endL) {
                            long speed = preSpeed;
                            speedMax = speedMax > speed ? speedMax : speed;
                            speedAver = (currentL - startL) * 8 / (nowTime - startTime);
                            info.speed = speed;
                            info.speedMax = speedMax;
                            info.speedAver = speedAver;

                            info.progress = 1.0f;
                            info.totalTime = nowTime - startTime;
                            info.finish = true;
                            sendMessage(HANDLER_DOWNLOADING, true, FTPSpeed.TESTING,
                                    threadId, info, "");
                            break;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    error = true;
                    if (client != null) {
                        sendMessage(HANDLER_FILE_ERROR, true, "下载文件异常");
                    }
                } finally {
                    if (!error && client != null) {
                        sendMessage(HANDLER_DOWNLOAD_FINISH, true, FTPSpeed.FINISH,
                                threadId, info, "下载完成");
                    }
                    if (is != null) {
                        try {
                            is.close();
                            if (!isCloseAdvance) {
                                close(false);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            // sendMessage(HANDLER_FILE_CLOSE, true, "下载文件关闭异常");
                        }
                    }
                    try {
                        if (client != null && client.isConnected()) {
                            client.logout();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    /**
     * 上传
     * */
    public void startUpload() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean error = false;
                try {
                    if (client != null && client.isConnected()) {
                        try {
                            client.logout();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    client = new FTPClient();
                    client.setDataTimeout(60000);
                    client.connect(ftpIp, ftpPort);
                    client.login(ftpUN, ftpPW);
                    int reply = client.getReplyCode();
                    if (!FTPReply.isPositiveCompletion(reply)) {
                        sendMessage(HANDLER_LOGIN_FAIL, false, "无法连接到ftp服务器，错误码为：" + reply);
                        try {
                            client.disconnect();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        Log.d(FTPSpeed.TAG, "无法连接到ftp服务器，错误码为：" + reply);
                        return;
                    }
                } catch (Exception e) {
                    sendMessage(HANDLER_LOGIN_FAIL, false, "无法连接到ftp服务器，IO异常");
                    e.printStackTrace();
                    return;
                }
                OutputStream output = null;
                FTPSpeedInfo info = new FTPSpeedInfo();
                try {
                    client.enterLocalPassiveMode();    //设置被动模式
                    client.setFileType(FTP.BINARY_FILE_TYPE);//设置文件传输模式
                    client.setRestartOffset(startL);//设置恢复下载的位置
                    client.changeWorkingDirectory("/");
                    client.allocate(1024);

                    upFile = getFile("", sdFileName + threadId);
                    Log.d(FTPSpeed.TAG, "-----------fileName=" + (sdFileName + threadId)+"----upFile.length=" + upFile.length());
                    FileInputStream input = new FileInputStream(upFile);
                    String fileName = sdFileName + threadId;
                    output = client.appendFileStream(fileName);
                    byte[] buffer = new byte[1024];
                    int len;
                    long startTime = System.currentTimeMillis();
                    long currentL = startL;
                    long preTime = startTime;
                    long preLength = startL;
                    long speedMax = 0;
                    long speedAver = 0;
                    sendMessage(HANDLER_UPLOAD_START);
                    Log.d(FTPSpeed.TAG, "----------currentL=" + currentL + "------startL=" + startL + "-----endL=" + endL);
                    info.threadId = threadId;
                    info.filePath = upFile.getPath();
                    info.fileSize = endL - startL;
                    info.oriPath = Environment.getExternalStorageDirectory() + "/" + sdFileName;
                    info.oriSize = fileLength;
                    info.threadCount = threadCount;
                    long preSpeed = 0;
                    while ((len = input.read(buffer)) != -1 && client != null && !isCloseAdvance) {
                        //如果该条线程读取的数据长度大于所分配的区间长度，则只能读到区间的最大长度
                        if (currentL + len >= endL) {
                            len = (int) (endL - currentL);
                            output.write(buffer, 0, len);
                        } else {
                            output.write(buffer, 0, len);
                        }
                        currentL = currentL + len;
                        long nowTime = System.currentTimeMillis();
                        if (nowTime - preTime > interval) {
                            long speed = (currentL - preLength) * 8 / (nowTime - preTime);
                            speedMax = speedMax > speed ? speedMax : speed;
                            speedAver = (currentL - startL) * 8 / (nowTime - startTime);
                            info.speed = speed;
                            info.speedMax = speedMax;
                            info.speedAver = speedAver;
                            info.nowSize = currentL - startL;
                            info.progress = info.nowSize / info.fileSize;
                            sendMessage(HANDLER_UPLOADING, false, FTPSpeed.TESTING,
                                    threadId, info, "");
                            preTime = nowTime;
                            preLength = currentL;
                            preSpeed = speed;
                        }
                        if (currentL >= endL) {
                            long speed = preSpeed;
                            speedMax = speedMax > speed ? speedMax : speed;
                            speedAver = (currentL - startL) * 8 / (nowTime - startTime);
                            info.speed = speed;
                            info.speedMax = speedMax;
                            info.speedAver = speedAver;
                            info.progress = 1.0f;
                            info.totalTime = nowTime - startTime;
                            info.finish = true;
                            sendMessage(HANDLER_UPLOADING, false, FTPSpeed.TESTING,
                                    threadId, info, "");
                            break;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    error = true;
                    if (client != null) {
                        sendMessage(HANDLER_FILE_ERROR, false, "上传文件异常");
                    }
                } finally {
                    if (!error && client != null) {
                        sendMessage(HANDLER_UPLOAD_FINISH, false, FTPSpeed.FINISH,
                                threadId, info, "上传完成");
                    }
                    if (output != null) {
                        try {
                            output.close();
                            if (!isCloseAdvance) {
                                close(false);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            // sendMessage(HANDLER_FILE_CLOSE, false, "上传文件关闭异常");
                        }
                    }
                    try {
                        if (client != null && client.isConnected()) {
                            client.logout();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    private static File getFile(String dirName, String fileName) {
        String root = Environment.getExternalStorageDirectory() + "/";
        File file = new File(TextUtils.isEmpty(dirName) ? root : (root + "/" + dirName), fileName);
        return file;
    }

    public void close(boolean advance) {
        Log.d(FTPSpeed.TAG, "关闭FTP任务-------------------");
        try {
            isCloseAdvance = advance;
            if (mHandler != null) {
                mHandler.removeCallbacksAndMessages(null);
            }
            if (client != null && client.isConnected()) {
                client.logout();
            }
            client = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendMessage(int handleType){
        sendMessage(handleType, false, 0, threadId, new FTPSpeedInfo(), "");
    }

    private void sendMessage(int handleType, boolean download, String message){
        sendMessage(handleType, download, -1, threadId, new FTPSpeedInfo(), message);
    }

    private void sendMessage(int handleType, boolean download, int status, int threadId,
                             FTPSpeedInfo info, String message){
        Bundle bundle = new Bundle();
        bundle.putInt("handleType", handleType);
        bundle.putBoolean("download", download);
        bundle.putInt("status",  status);
        bundle.putInt("threadId",  threadId);
        bundle.putSerializable("info",  info);
        bundle.putString("message", message);
        Message msg = new Message();
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }

    private final static int HANDLER_DOWNLOAD_FINISH    = 10;
    private final static int HANDLER_UPLOAD_FINISH      = 9;
    private final static int HANDLER_DOWNLOADING        = 8;
    private final static int HANDLER_UPLOADING          = 7;
    private final static int HANDLER_DOWNLOAD_START     = 6;
    private final static int HANDLER_UPLOAD_START       = 5;
    private final static int HANDLER_FILE_CLOSE         = 4;
    private final static int HANDLER_FILE_ERROR         = 3;
    private final static int HANDLER_FILE_EXITNO        = 2;
    private final static int HANDLER_LOGIN_FAIL         = 1;

    private Handler mHandler = new Handler() {
        @Override
        public void dispatchMessage(Message msg) {
            Bundle bundle     = msg.getData();
            int handleType    = bundle.getInt("handleType");
            boolean download  = bundle.getBoolean("download");
            int status        = bundle.getInt("status");
            String message    = bundle.getString("message");
            if(handleType == HANDLER_LOGIN_FAIL) {
                listener.loginfail(download, message, callBack);
            } else if (handleType == HANDLER_FILE_EXITNO) {
                listener.fileExitNo(download, message, callBack);
            } else if (handleType == HANDLER_FILE_ERROR) {
                listener.fileError(download, message, callBack);
            } else if (handleType == HANDLER_FILE_CLOSE) {
                listener.fileClose(download, message, callBack);
            } else if (handleType == HANDLER_UPLOAD_START) {
                listener.uploadStart(callBack);
            } else if (handleType == HANDLER_DOWNLOAD_START) {
                listener.downloadStart(callBack);
            } else if (handleType == HANDLER_UPLOADING) {
                if (isCloseAdvance) {
                    return;
                }
                int threadId      = bundle.getInt("threadId");
                FTPSpeedInfo info = (FTPSpeedInfo) bundle.getSerializable("info");
                listener.uploading(message, threadId, info, status, callBack);
            } else if (handleType == HANDLER_DOWNLOADING) {
                if (isCloseAdvance) {
                    return;
                }
                int threadId      = bundle.getInt("threadId");
                FTPSpeedInfo info = (FTPSpeedInfo) bundle.getSerializable("info");
                listener.downloading(message, threadId, info, status, callBack);
            } else if (handleType == HANDLER_UPLOAD_FINISH) {
                Log.d(FTPSpeed.TAG, "是否提前关闭上传------isCloseAdvance=" + isCloseAdvance);
                if (isCloseAdvance) {
                    return;
                }
                int threadId      = bundle.getInt("threadId");
                FTPSpeedInfo info = (FTPSpeedInfo) bundle.getSerializable("info");
                info.nowSize = endL - startL;
                listener.uploadFinish(message, threadId, info, status, callBack);
            } else if (handleType == HANDLER_DOWNLOAD_FINISH) {
                Log.d(FTPSpeed.TAG, "是否提前关闭下载------isCloseAdvance=" + isCloseAdvance);
                if (isCloseAdvance) {
                    return;
                }
                int threadId      = bundle.getInt("threadId");
                FTPSpeedInfo info = (FTPSpeedInfo) bundle.getSerializable("info");
                info.nowSize = endL - startL;
                listener.downloadFinish(message,  threadId, info , status, callBack);
            }
        }
    };
}
