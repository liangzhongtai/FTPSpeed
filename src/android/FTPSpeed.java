package com.chinamobile.ftp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PermissionHelper;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;

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

    public CordovaInterface cordova;
    public CordovaWebView webView;
    public boolean first = true;
    public int ftpType;
    public int interval = 500;


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
        if ("coolMethod".equals(action)) {
            ftpType = args.getInt(0);
            if(args.length()>1)interval = args.getInt(1);
            if(args.length()>2){
                FTPSpeedUtil.getInstance().ftpIp = args.getString(2);
                FTPSpeedUtil.getInstance().ftpPort = args.getInt(3);
                FTPSpeedUtil.getInstance().ftpUN = args.getString(4);
                FTPSpeedUtil.getInstance().ftpPW = args.getString(5);
                FTPSpeedUtil.getInstance().ftpFileName = args.getString(6);
                FTPSpeedUtil.getInstance().sdFileName = args.getString(7);
                if(ftpType == DOWNLOAD){
                    FTPSpeedUtil.getInstance().ftpOriFileName = FTPSpeedUtil.getInstance().ftpFileName;
                }
            }
            //权限
            try {
                if (!PermissionHelper.hasPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                        || !PermissionHelper.hasPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    PermissionHelper.requestPermissions(this, RESULTCODE_PERMISSION, new String[]{
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                    });
                } else {
                    startWork();
                }
            }catch (Exception e){
                //权限异常
                sendFTPSpeedFaile(ftpType,PERMISSION_ERROR,"FTP测速功能异常");
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

    public void onRestoreStateForActivityResult(Bundle state, CallbackContext callbackContext) {
        this.callbackContext = callbackContext;
    }

    @Override
    public void onRequestPermissionResult(int requestCode, String[] permissions,
                                          int[] grantResults) throws JSONException {
        for (int r : grantResults) {
            if (r == PackageManager.PERMISSION_DENIED) {
                sendFTPSpeedFaile(ftpType,PERMISSION_LESS,"缺少权限,无法打开FTP测速功能");
                return;
            }
        }
        switch (requestCode) {
            case RESULTCODE_PERMISSION:
               startWork();
                break;
            default:
                break;
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == RESULTCODE_FTPSPEED_PROVIDER) {
            startWork();
        }
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
    }

    private void startWork() {
        //Log.d(TAG,"showFTPSpeed="+showFTPSpeed);
        if(ftpType==CLOSE) {
            FTPSpeedUtil.getInstance().removeFTPSpeedListener();
            sendFTPSpeedMessage(ftpType,BREAK_OFF,0,0,0,0);

        }else if(ftpType==DELETE_SD_FILE){
            FTPSpeedUtil.getInstance().deleteSdFile(true);

        }else{
            FTPSpeedUtil.getInstance().interval = interval;

            if(FTPSpeedUtil.getInstance().listener==null){
                FTPSpeedUtil.getInstance().listener =new FTPSpeedListener() {
                    @Override
                    public void sendFTPSpeedMessage(int ftpType,int status, long speed, long speedMax, long speedAver,float progress) {
                        FTPSpeed.this.sendFTPSpeedMessage(ftpType,status,speed,speedMax,speedAver,progress);
                    }

                    @Override
                    public void sendFTPSpeedError(int ftpType,int status,String message) {
                        FTPSpeed.this.sendFTPSpeedFaile(ftpType,status,message);
                    }
                };
            }

            if(ftpType==UPLOAD){
                FTPSpeedUtil.getInstance().upload();
            }else if(ftpType==DOWNLOAD){
                FTPSpeedUtil.getInstance().download();
            }else if(ftpType==DELETE_FTP_FILE){
                FTPSpeedUtil.getInstance().deleteFTPFile(true);
            }
            first = false;
        }
    }

    public void sendFTPSpeedMessage(int ftpType,int status,long speed,long speedMax,long speedAver,float progress) {
        PluginResult pluginResult = null;
        JSONArray array = new JSONArray();
        try {
            array.put(0,ftpType);
            array.put(1,status);
            array.put(2,speed);
            array.put(3,speedMax);
            array.put(4,speedAver);
            array.put(5,progress);
            Log.d(TAG,"success_array="+array+"_status="+status);
            pluginResult = new PluginResult(PluginResult.Status.OK,array);
        } catch (JSONException e) {
            e.printStackTrace();
            sendFTPSpeedFaile(ftpType,JSONARRAY_ERROR,"JSONARRAY构建异常");
        }
        if(pluginResult==null){
            return;
        }
        pluginResult.setKeepCallback(true);
        callbackContext.sendPluginResult(pluginResult);
    }

    public void sendFTPSpeedFaile(int ftpType,int status,String message){
        PluginResult pluginResult;
        JSONArray array = new JSONArray();
        Log.d(TAG,message);
        try {
            array.put(0, ftpType);
            array.put(1, status);
            array.put(2, message);
            Log.d(TAG,"faile_array="+array+"_status="+status);
        }catch (Exception e){
            e.printStackTrace();
        }
        pluginResult = new PluginResult(PluginResult.Status.ERROR,array);
        pluginResult.setKeepCallback(true);
        callbackContext.sendPluginResult(pluginResult);
    }

    public interface FTPSpeedListener{
        void sendFTPSpeedMessage(int ftpType, int status, long speed, long speedMax, long speedAver, float progress);
        void sendFTPSpeedError(int ftpType, int status, String message);
    }
}
