package com.chinamobile.ftp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;


import com.umeng.commonsdk.debug.E;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PermissionHelper;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by liangzhongtai on 2018/5/21.
 */

public class FTPSpeed extends CordovaPlugin {
    public final static String TAG = "FTPSpeed_Plugin";
    public final static int RESULTCODE_PERMISSION = 100;
    public final static int RESULTCODE_FTPSPEED_PROVIDER = 200;
    //ftpType
    public final static int DOWNLOAD        = 0;
    public final static int UPLOAD          = 1;
    public final static int CLOSE           = 2;
    public final static int DELETE_FTP_FILE = 3;
    public final static int DELETE_SD_FILE  = 4;

    //status
    public final static int TESTING         = 0;
    public final static int FINISH          = 1;
    public final static int BREAK_OFF       = 2;
    public final static int DOWNLOAD_FIRST  = 3;
    public final static int FTP_NO_FILE     = 4;
    public final static int LOGIN_FAILE     = 5;
    public final static int CONNECT_FAILE   = 6;
    public final static int PERMISSION_ERROR = 7;
    public final static int PERMISSION_LESS = 8;
    public final static int TESTING_ERROR   = 9;
    public final static int JSONARRAY_ERROR = 10;
    public final static int DELETE_FTP_FILE_FAILE  = 11;
    public final static int DELETE_FTP_FILE_SUCCESS= 12;
    public final static int DELETE_SD_FILE_SUCCESS = 13;
    public final static int CLOSE_ERROR = 14;

    public CordovaInterface cordova;
    public CordovaWebView webView;
    public boolean first = true;
    public int ftpType;
    public int interval = 100;
    public static Map<Integer, FTPSpeedInfo> infoMap;
    public static AtomicLong preAllTime;
    public static AtomicLong speedMaxDownload;
    public static AtomicLong speedMaxUp;
    private CallbackContext callbackContext;

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        this.cordova = cordova;
        this.webView = webView;
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        this.callbackContext = callbackContext;
        Log.d(TAG,"FTPSpeed");
        if (args != null) {
            Log.d(TAG, "args=" + args.toString());
        }
        if ("coolMethod".equals(action)) {
            ftpType = args.getInt(0);
            Log.d(TAG,"ftpType=" + ftpType);
            if (args.length() > 1) {
                interval = args.getInt(1);
            }
            if (args.length() > 2) {
                FTPSpeedUtil.getInstance().ftpIp = args.getString(2);
                FTPSpeedUtil.getInstance().ftpPort = args.getInt(3);
                FTPSpeedUtil.getInstance().ftpUN = args.getString(4);
                FTPSpeedUtil.getInstance().ftpPW = args.getString(5);
                FTPSpeedUtil.getInstance().ftpFileName = args.getString(6);
                FTPSpeedUtil.getInstance().sdFileName = args.getString(7);
                if (args.length() > 8) {
                    FTPSpeedUtil.getInstance().downloadFtpPath = args.getString(8);
                }
                if (args.length() > 9) {
                    FTPSpeedUtil.getInstance().uploadFtpPath = args.getString(9);
                }
                FTPSpeedUtil.getInstance().threadCount = 0;
                if (args.length() > 10) {
                    FTPSpeedUtil.getInstance().threadCount = args.getInt(10);
                }
                if (ftpType == DOWNLOAD) {
                    FTPSpeedUtil.getInstance().ftpOriFileName = FTPSpeedUtil.getInstance().ftpFileName;
                }
            }
            //权限
            try {
                if (!PermissionHelper.hasPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) ||
                    !PermissionHelper.hasPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    PermissionHelper.requestPermissions(this, RESULTCODE_PERMISSION, new String[]{
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                    });
                } else {
                    startWork(callbackContext);
                }
            } catch (Exception e) {
                //权限异常
                sendFTPSpeedFaile(ftpType,PERMISSION_ERROR,"FTP测速功能异常", callbackContext);
                return true;
            }

            return true;
        }
        return super.execute(action, args, callbackContext);
    }

    @Override
    public Bundle onSaveInstanceState() {
        return super.onSaveInstanceState();
    }

    @Override
    public void onRestoreStateForActivityResult(Bundle state, CallbackContext callbackContext) {
        this.callbackContext = callbackContext;
    }

    @Override
    public void onRequestPermissionResult(int requestCode, String[] permissions,
                                          int[] grantResults) throws JSONException {
        for (int r : grantResults) {
            if (r == PackageManager.PERMISSION_DENIED) {
                sendFTPSpeedFaile(ftpType,PERMISSION_LESS,"缺少权限,无法打开FTP测速功能",
                        callbackContext);
                return;
            }
        }
        switch (requestCode) {
            case RESULTCODE_PERMISSION:
               startWork(callbackContext);
                break;
            default:
                break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == RESULTCODE_FTPSPEED_PROVIDER) {
            startWork(callbackContext);
        }
    }

    @Override
    public void onResume(boolean multitasking) {
        super.onResume(multitasking);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        FTPSpeedUtil.getInstance().deleteSdFile(false);
        if(FTPSpeedUtil.getInstance().sdFileName!=null) {
            FTPSpeedUtil.getInstance().ftpFileName = FTPSpeedUtil.getInstance().sdFileName;
            FTPSpeedUtil.getInstance().deleteFTPFile(false);
        }
        FTPSpeedUtil.getInstance().removeFTPSpeedListener();
        FTPSingleTaskManager.getInstance().close(true);
    }

    private void startWork(CallbackContext callbackContext) {
        if (ftpType == CLOSE) {
            FTPSpeedUtil.getInstance().removeFTPSpeedListener();
            FTPSingleTaskManager.getInstance().close(true);
            sendFTPSpeedMessage(ftpType, BREAK_OFF, 0, 0, 0, 0,
                    0, 0, callbackContext);
            return;
        } else if (ftpType == DELETE_SD_FILE) {
            FTPSpeedUtil.getInstance().deleteSdFile(true);
            return;
        } else {
            // 多线程3
            preAllTime = new AtomicLong();
            speedMaxDownload = new AtomicLong();
            speedMaxUp = new AtomicLong();
            if (FTPSpeedUtil.getInstance().threadCount > 1 && ftpType == DOWNLOAD) {
                infoMap = new HashMap<>();
                speedMaxDownload.set(0);
                for (int i = 0; i < FTPSpeedUtil.getInstance().threadCount; i++) {
                    infoMap.put(i, null);
                }
                FTPSingleTaskManager.getInstance().login(true,
                        FTPSpeedUtil.getInstance().ftpIp,
                        FTPSpeedUtil.getInstance().ftpPort,
                        FTPSpeedUtil.getInstance().ftpUN,
                        FTPSpeedUtil.getInstance().ftpPW,
                        FTPSpeedUtil.getInstance().ftpFileName,
                        FTPSpeedUtil.getInstance().sdFileName,
                        FTPSpeedUtil.getInstance().threadCount,
                        interval,
                        callbackContext, listener);
                return;
            } else if (FTPSpeedUtil.getInstance().threadCount > 1 && ftpType == UPLOAD) {
                infoMap = new HashMap<>();
                speedMaxUp.set(0);
                for (int i = 0; i < FTPSpeedUtil.getInstance().threadCount; i++) {
                    infoMap.put(i, null);
                }
                FTPSingleTaskManager.getInstance().login(false,
                        FTPSpeedUtil.getInstance().ftpIp,
                        FTPSpeedUtil.getInstance().ftpPort,
                        FTPSpeedUtil.getInstance().ftpUN,
                        FTPSpeedUtil.getInstance().ftpPW,
                        FTPSpeedUtil.getInstance().ftpFileName,
                        FTPSpeedUtil.getInstance().sdFileName,
                        FTPSpeedUtil.getInstance().threadCount,
                        interval,
                        callbackContext, listener);
                return;
            }

            // 单线程1
            FTPSpeedUtil.getInstance().interval = interval;
            if (FTPSpeedUtil.getInstance().listener == null) {
                FTPSpeedUtil.getInstance().listener = new FTPSpeedListener() {
                    @Override
                    public void sendFTPSpeedMessage(int ftpType,int status, long speed, long speedMax,
                                                    long speedAver, float progress, long totalTime,
                                                    long totalSize, CallbackContext callbackContext) {
                        FTPSpeed.this.sendFTPSpeedMessage(ftpType, status, speed, speedMax,
                                speedAver, progress, totalTime, totalSize, callbackContext);
                    }

                    @Override
                    public void sendFTPSpeedError(int ftpType,int status,String message,
                                                  CallbackContext callbackContext) {
                        FTPSpeed.this.sendFTPSpeedFaile(ftpType, status, message, callbackContext);
                    }
                };
            }
            try {
                if (ftpType == UPLOAD) {
                    FTPSpeedUtil.getInstance().upload(callbackContext);
                } else if (ftpType == DOWNLOAD) {
                    FTPSpeedUtil.getInstance().download(callbackContext);
                } else if (ftpType == DELETE_FTP_FILE) {
                    FTPSpeedUtil.getInstance().deleteFTPFile(true);
                }
            } catch (Exception e) {
                e.printStackTrace();
                sendFTPSpeedFaile(ftpType, BREAK_OFF, "FTP测试中断", callbackContext);
            }
            first = false;
        }
    }

    public void sendFTPSpeedMessage(int ftpType,int status,long speed,long speedMax,long speedAver,
                                    float progress,long totalTime,long totalSize, CallbackContext callbackContext) {
        PluginResult pluginResult = null;
        JSONArray array = new JSONArray();
        try {
            array.put(0,ftpType);
            array.put(1,status);
            array.put(2,speed);
            array.put(3,speedMax);
            array.put(4,speedAver);
            array.put(5,progress);
            array.put(6,totalTime);
            array.put(7,totalSize);
            // Log.d(TAG,"success_array="+array+"_status="+status);
            pluginResult = new PluginResult(PluginResult.Status.OK,array);
        } catch (JSONException e) {
            e.printStackTrace();
            sendFTPSpeedFaile(ftpType,JSONARRAY_ERROR,"JSONARRAY构建异常", callbackContext);
        }
        if (pluginResult == null) {
            return;
        }
        pluginResult.setKeepCallback(true);
        callbackContext.sendPluginResult(pluginResult);
    }

    public void sendFTPSpeedFaile(int ftpType,int status,String message, CallbackContext callbackContext){
        PluginResult pluginResult;
        JSONArray array = new JSONArray();
        Log.d(TAG, message);
        try {
            array.put(0, ftpType);
            array.put(1, status);
            array.put(2, message);
            // Log.d(TAG,"faile_array="+array+"_status="+status);
        }catch (Exception e){
            e.printStackTrace();
        }
        pluginResult = new PluginResult(PluginResult.Status.ERROR,array);
        pluginResult.setKeepCallback(true);
        callbackContext.sendPluginResult(pluginResult);
    }

    public interface FTPSpeedListener{
        void sendFTPSpeedMessage(int ftpType, int status, long speed, long speedMax, long speedAver,
                                 float progress, long totalTime, long totalSize, CallbackContext callbackContext);
        void sendFTPSpeedError(int ftpType, int status, String message, CallbackContext callbackContext);
    }

    private void sendMessage(int ftpType, int status, long speed, long speedMax, long speedAver,
                             float progress, long totalTime, long totalSize, CallbackContext callbackContext){
         sendFTPSpeedMessage(ftpType, status, speed, speedMax,
                    speedAver, progress, totalTime, totalSize, callbackContext);
    }


    private void sendMessageEror(int ftpType, int status, String message, CallbackContext callbackContext){
        sendFTPSpeedFaile(ftpType, status, message, callbackContext);
    }

    private FTPSingleTaskListener listener = new FTPSingleTaskListener() {
        @Override
        public void loginfail(boolean download, String message, CallbackContext callBack) {
            sendMessageEror(download ? DOWNLOAD : UPLOAD, LOGIN_FAILE ,message, callBack);
        }

        @Override
        public void fileExitNo(boolean download, String message, CallbackContext callBack) {
            sendMessageEror(download ? DOWNLOAD : UPLOAD, FTP_NO_FILE, message, callBack);
        }

        @Override
        public void fileError(boolean download, String message, CallbackContext callBack) {
            sendMessageEror(download ? DOWNLOAD : UPLOAD, TESTING_ERROR ,message, callBack);
        }

        @Override
        public void fileClose(boolean download, String message, CallbackContext callBack) {
            sendMessageEror(download ? DOWNLOAD : UPLOAD, CLOSE_ERROR ,message, callBack);
        }

        @Override
        public void uploadStart(CallbackContext callBack) {
            preAllTime.set(System.currentTimeMillis());
        }

        @Override
        public void downloadStart(CallbackContext callBack) {
            preAllTime.set(System.currentTimeMillis());
        }

        @Override
        public synchronized void uploading(String message, int threadId, FTPSpeedInfo info, int status,
                              CallbackContext callback) {
            infoMap.put(threadId, info);
            // 计算速度
            long nowTime = System.currentTimeMillis();
            long costTime = nowTime - preAllTime.get();
            if (costTime < interval && !info.finish) {
                return;
            }
            preAllTime.set(nowTime);
            FTPSpeedInfo total = FTPSpeedInfo.formatInfos(false, infoMap, speedMaxDownload.get(), speedMaxUp.get());
            speedMaxUp.set(total.speedMax);
            sendFTPSpeedMessage(UPLOAD, TESTING, total.speed, total.speedMax, total.speedAver,
                    total.progress, total.totalTime, info.oriSize, callback);
        }

        @Override
        public synchronized void downloading(String message, int threadId, FTPSpeedInfo info, int status,
                                CallbackContext callback) {
            infoMap.put(threadId, info);
            // 计算速度
            long nowTime = System.currentTimeMillis();
            long costTime = nowTime - preAllTime.get();
            if (costTime < interval && !info.finish) {
                return;
            }
            preAllTime.set(nowTime);
            FTPSpeedInfo total = FTPSpeedInfo.formatInfos(true, infoMap, speedMaxDownload.get(), speedMaxUp.get());
            speedMaxDownload.set(total.speedMax);
            sendFTPSpeedMessage(DOWNLOAD, TESTING, total.speed, total.speedMax, total.speedAver,
                    total.progress, 0, 0, callback);
        }

        @Override
        public synchronized void uploadFinish(String message, int threadId, FTPSpeedInfo info,
                                              int status, CallbackContext callback) {
            infoMap.put(threadId, info);
            Log.d(TAG + "--", "上传_结束threadId=" + threadId + "_均速=" + info.speedAver
                    + "_耗时=" + info.totalTime + "_大小=" + info.fileSize);
            // 判断是否全部完成
            boolean finish = true;
            for (Integer key: infoMap.keySet()) {
                FTPSpeedInfo bean = infoMap.get(key);
                if (finish && bean != null && bean.nowSize == bean.fileSize) {
                    finish = true;
                } else {
                    finish = false;
                }
            }
            if (!finish) {
                return;
            }
            sendMessage(UPLOAD, status, 0, 0, 0, 1.0f,
                    0, 0, callback);
        }

        @Override
        public synchronized void downloadFinish(String message, int threadId, FTPSpeedInfo info,
                                                int status, CallbackContext callback) {
            infoMap.put(threadId, info);
            Log.d(TAG + "--", "下载_结束threadId=" + threadId + "_均速=" + info.speedAver
                    + "_耗时=" + info.totalTime + "_大小=" + info.fileSize);
            // 判断是否全部完成
            boolean finish = true;
            for (Integer key: infoMap.keySet()) {
                FTPSpeedInfo bean = infoMap.get(key);
                if (finish && bean != null && bean.nowSize == bean.fileSize) {
                    finish = true;
                } else {
                    finish = false;
                }
            }
            if (!finish) {
                return;
            }
            // 合并小文件路径
            String[] paths = new String[infoMap.size()];
            String oriFilePath = info.oriPath;
            for (Integer key: infoMap.keySet()) {
                paths[key] = infoMap.get(key).filePath;

            }
            Log.d(TAG + "--", "小文件路径_paths=" + Arrays.toString(paths));
            // 小文件合并成一个文件
            FTPSingleTaskManager.mergeFiles(paths, oriFilePath);
            // 删除小文件
            FTPSingleTaskManager.removeFiles(paths);
            sendMessage(DOWNLOAD, status, 0, 0, 0, 1.0f,
                    0, 0, callback);
        }
    };
}
