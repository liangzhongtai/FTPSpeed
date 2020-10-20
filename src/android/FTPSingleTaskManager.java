package com.chinamobile.ftp;

import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.widget.EditText;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.cordova.CallbackContext;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.HashMap;

/**
 * Created by liangzhongtai on 2020/2/4.
 */

public class FTPSingleTaskManager {
    private volatile static FTPSingleTaskManager uniqueInstance;
    private HashMap<Integer, FTPSingleTask> taskMap;

    private FTPSingleTaskManager() {
    }

    public static FTPSingleTaskManager getInstance() {
        if (uniqueInstance == null) {
            synchronized (FTPSingleTaskManager.class) {
                if (uniqueInstance == null) {
                    uniqueInstance = new FTPSingleTaskManager();
                }
            }
        }
        return uniqueInstance;
    }

    public void login(boolean download, String ftpIp, int ftpPort, String ftpUN, String ftpPW,
                      String fileName, String sdFileName, int threadCount, int interval,
                      CallbackContext callBack, FTPSingleTaskListener listener) {
        FTPClient client = new FTPClient();
        try {
            client.setDataTimeout(60000);
            client.connect(ftpIp, ftpPort);
            client.login(ftpUN, ftpPW);
            int reply = client.getReplyCode();
            if (!FTPReply.isPositiveCompletion(reply)) {
                listener.loginfail(download, "无法连接到ftp服务器，错误码为：" + reply, callBack);
                try {
                    client.disconnect();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Log.d(FTPSpeed.TAG, "无法连接到ftp服务器，错误码为：" + reply);
                return;
            }
            taskMap = new HashMap<>();
            if (download) {
                Log.d(FTPSpeed.TAG, "下载0");
                download(client, ftpIp, ftpPort, ftpUN, ftpPW, fileName, sdFileName, threadCount,
                        interval, callBack, listener);
            } else {
                upload(ftpIp, ftpPort, ftpUN, ftpPW, fileName, sdFileName, threadCount, interval,
                        callBack, listener);
            }
        } catch (Exception e) {
            e.printStackTrace();
            listener.loginfail(download, "无法连接到ftp服务器，IO异常", callBack);
        } finally {
            try {
                if (client.isConnected()) {
                    client.logout();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void download(FTPClient client, String ftpIp, int ftpPort, String ftpUN, String ftpPW,
                         String fileName, String sdFileName, int threadCount, int interval,
                         CallbackContext callBack, FTPSingleTaskListener listener) {
            try {
                // 删除本地文件
                String localPath = Environment.getExternalStorageDirectory() + "/"+ sdFileName;
                File localFile = new File(localPath);
                if(localFile.exists()){
                    localFile.delete();
                }
                localFile.createNewFile();
                Log.d(FTPSpeed.TAG, "下载1_fileName=" + fileName + "_sdFileName=" + sdFileName);
                // 获取文件信息
                String charSet = "UTF-8";
                client.enterLocalPassiveMode();    // 设置被动模式
                client.setFileType(FTP.BINARY_FILE_TYPE);// 设置文件传输模式
                FTPFile[] files = client.listFiles(new String(fileName.getBytes(charSet), "ISO-8859-1"));
                if (files == null || files.length == 0) {
                    listener.fileExitNo(true, "FTP服务器的下载文件不存在", callBack);
                    return;
                }
                FTPFile file = files[0];
                long fileLength = file.getSize();
                // 返回文件大小


                // Properties pro = CommonUtil.loadConfig(mConfigFile);
                int blockSize = (int) (fileLength / threadCount);
                int[] recordL = new int[threadCount];
                for (int i = 0; i < threadCount; i++) {
                    recordL[i] = -1;
                }
                int rl = 0;
                for (int i = 0; i < threadCount; i++) {
                    long startL = i * blockSize, endL = (i + 1) * blockSize;
                    recordL[rl] = i;
                    rl++;
                    //最后一个线程的结束位置即为文件的总长度
                    if (i == (threadCount - 1)) {
                        endL = fileLength;
                    }
                    //创建分段线程
                    FTPSingleTask task = createSingThreadTask(i, startL, endL, fileLength);
                    task.ftpIp = ftpIp;
                    task.ftpPort = ftpPort;
                    task.ftpUN = ftpUN;
                    task.ftpPW = ftpPW;
                    task.fileName = fileName;
                    task.sdFileName = sdFileName;
                    task.charSet = charSet;
                    task.interval = interval;
                    task.callBack = callBack;
                    task.listener = listener;
                    task.threadCount = threadCount;
                    taskMap.put(i, task);
                }
                client.logout();
                client.disconnect();
                Log.d(FTPSpeed.TAG, "下载2");
                startSingleTask(recordL, true);
            } catch (IOException e) {
                e.printStackTrace();
                listener.fileExitNo(true, "FTP服务器的下载文件不存在", callBack);
            } finally {
                try {
                    if (client.isConnected()) {
                        client.disconnect();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
    }

    private FTPSingleTask createSingThreadTask(int id, long startL, long endL, long fileLength) {
        FTPSingleTask task = new FTPSingleTask(id, startL, endL, fileLength);
        return task;
    }

    private void startSingleTask(int[] recordL, boolean download) {
        for (int i = 0; i < recordL.length; i++) {
            if (download) {
                taskMap.get(recordL[i]).startDownload();
            } else {
                taskMap.get(recordL[i]).startUpload();
            }
        }
    }

    public void upload(String ftpIp, int ftpPort, String ftpUN, String ftpPW,
                       String fileName, String sdFileName, int threadCount, int interval,
                       CallbackContext callBack, FTPSingleTaskListener listener) {
                try {
                    String charSet = "UTF-8";
                    String localFilePath = Environment.getExternalStorageDirectory() + "/" + sdFileName;
                    Log.d(FTPSpeed.TAG, "上传1_fileName=" + fileName + "_sdFileName=" + sdFileName);
                    File file = new File(localFilePath);
                    if (!file.exists() || file.length() == 0) {
                        Log.d(FTPSpeed.TAG, "上传1_合并文件不存在，尝试合并");
                        // 尝试合并已下载的文件
                        String[] paths = new String[threadCount];
                        for (int i = 0; i < paths.length; i++) {
                            paths[i] = localFilePath + i;
                        }
                        mergeFiles(paths, localFilePath);
                        file = new File(localFilePath);
                        if (!file.exists() || file.length() == 0) {
                            Log.d(FTPSpeed.TAG, "上传1_尝试合并失败，上传终止");
                            listener.fileExitNo(false, "手机本地的上传文件不存在", callBack);
                            return;
                        }
                    }

                    Log.d(FTPSpeed.TAG, "上传2");
                    long fileLength = file.length();
                    int blockSize = (int) (fileLength / threadCount);
                    // 切割文件
                    splitFiles(localFilePath, sdFileName, blockSize);
                    int[] recordL = new int[threadCount];
                    for (int i = 0; i < threadCount; i++) {
                        recordL[i] = -1;
                    }
                    int rl = 0;
                    for (int i = 0; i < threadCount; i++) {
                        long startL = i * blockSize, endL = (i + 1) * blockSize;
                        recordL[rl] = i;
                        rl++;
                        //最后一个线程的结束位置即为文件的总长度
                        if (i == (threadCount - 1)) {
                            endL = fileLength;
                        }
                        //创建分段线程
                        FTPSingleTask task = createSingThreadTask(i, startL, endL, fileLength);
                        task.ftpIp = ftpIp;
                        task.ftpPort = ftpPort;
                        task.ftpUN = ftpUN;
                        task.ftpPW = ftpPW;
                        task.fileName = fileName;
                        task.sdFileName = sdFileName;
                        task.charSet = charSet;
                        task.interval = interval;
                        task.callBack = callBack;
                        task.listener = listener;
                        task.threadCount = threadCount;
                        taskMap.put(i, task);
                    }
                    Log.d(FTPSpeed.TAG, "上传3");
                    startSingleTask(recordL, false);
                } catch (Exception e) {
                    e.printStackTrace();
                    listener.fileExitNo(false, "手机本地的上传文件不存在", callBack);
                }
    }

    public void close(boolean advance) {
        try {
            if (taskMap == null) {
                return;
            }
            for (int i = 0; i < taskMap.size(); i++) {
                if (taskMap.get(i) != null) {
                    taskMap.get(i).close(advance);
                }
            }
            taskMap = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 合并多个文件文件
     * */
    public static boolean mergeFiles(String[] fpaths, String resultPath) {
        if (fpaths == null || fpaths.length < 1 || TextUtils.isEmpty(resultPath)) {
            return false;
        }
        if (fpaths.length == 1) {
            return new File(fpaths[0]).renameTo(new File(resultPath));
        }

        File[] files = new File[fpaths.length];
        for (int i = 0; i < fpaths.length; i ++) {
            files[i] = new File(fpaths[i]);
            if (TextUtils.isEmpty(fpaths[i]) /*|| !files[i].exists() || !files[i].isFile()*/) {
                return false;
            }
        }

        File resultFile = new File(resultPath);
        if (resultFile.exists()) {
            resultFile.delete();
        }

        try {
            resultFile.createNewFile();
            FileChannel resultFileChannel = new FileOutputStream(resultFile, true).getChannel();
            for (int i = 0; i < fpaths.length; i ++) {
                FileChannel blk = new FileInputStream(files[i]).getChannel();
                resultFileChannel.transferFrom(blk, resultFileChannel.size(), blk.size());
                blk.close();
            }
            resultFileChannel.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        for (int i = 0; i < fpaths.length; i ++) {
            files[i].delete();
        }

        return true;
    }

    /**
     * 分隔多个文件
     * */
    public static void splitFiles(String path, String fileName, long size){
        try {
            File file = new File(path);
            if (!file.exists() || (!file.isFile())) {
                Log.d(FTPSpeed.TAG, path + "_文件不存在");
            }
            // 获得被分割文件父文件，将来被分割成的小文件便存在这个目录下
            File parentFile = file.getParentFile();
            // 取得文件的大小
            long fileLength = file.length();
            Log.d(FTPSpeed.TAG, "文件大小：" + fileLength + "_分割大小=" + size);
            if (size <= 0) {
                size = fileLength / 2;
            }
            // 取得被分割后的小文件的数目
            int num = (fileLength % size != 0) ? (int) (fileLength / size + 1)
                    : (int) (fileLength / size);
            // 输入文件流，即被分割的文件
            FileInputStream is = new FileInputStream(file);

            // 根据要分割的数目输出文件
            for (int i = 0; i < num; i++) {
                // 对于前num – 1个小文件，大小都为指定的size
                File outFile = new File(parentFile, fileName + i);
                // 构建小文件的输出流
                FileOutputStream out = new FileOutputStream(outFile);
                byte[] buffer = new byte[1024];
                // 读输入文件流的开始和结束下标
                long startL = i * size;
                long endL = (i + 1) * size;
                endL = endL > fileLength ? fileLength : endL;
                int len;
                long currentL = startL;
                while ((len = is.read(buffer)) != -1 ) {
                    if (currentL + len >= endL) {
                        len = (int) (endL - currentL);
                        out.write(buffer, 0, len);
                        break;
                    } else {
                        out.write(buffer, 0, len);
                    }
                    currentL = currentL + len;
                }
                out.close();
            }
            is.close();
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(FTPSpeed.TAG, e.toString());
        }
    }

    /**
     * 删除多个文件
     * */
    public static boolean removeFiles(String[] fpaths) {
        for (int i  = 0; i < fpaths.length; i++) {
            File file = new File(fpaths[i]);
            if (file.exists()) {
                file.delete();
            }
        }
        return true;
    }
}
