package com.chinamobile.ftp;

import org.apache.cordova.CallbackContext;

/**
 * Created by liangzhongtai on 2020/2/4.
 */

public interface FTPSingleTaskListener {
    /**
     * 登录过程中失败
     */
    void loginfail(boolean download, String message, CallbackContext callBack);
    /**
     * 文件不存在
     */
    void fileExitNo(boolean download, String message, CallbackContext callBack);

    /**
     * 获取文件出错
     * */
    void fileError(boolean download, String message, CallbackContext callBack);

    /**
     * 关闭文件出错
     * */
    void fileClose(boolean download, String message, CallbackContext callBack);

    /**
     * 开始上传
     * */
    void uploadStart(CallbackContext callBack);

    /**
     * 开始下载
     * */
    void downloadStart(CallbackContext callBack);

    /**
     * 上传中
     * */
    void uploading(String message, int threadId, FTPSpeedInfo info, int status,
                   CallbackContext callback);

    /**
     * 下载中
     * */
    void downloading(String message, int threadId, FTPSpeedInfo info, int status,
                      CallbackContext callback);

    /**
     * 上传完成
     * */
    void uploadFinish(String message, int threadId, FTPSpeedInfo info, int status,
                       CallbackContext callback);

    /**
     * 下载完成
     * */
    void downloadFinish(String message, int threadId, FTPSpeedInfo info, int status,
                        CallbackContext callback);
}
