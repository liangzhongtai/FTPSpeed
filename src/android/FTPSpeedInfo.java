package com.chinamobile.ftp;

import java.io.Serializable;
import java.util.Map;
import java.util.Random;

/**
 * Created by liangzhongtai on 2020/3/2.
 */

public class FTPSpeedInfo implements Serializable {
    public int threadId;
    public long speed;
    public long speedMax;
    public long speedAver;
    public long nowSize;
    public long fileSize;
    public long oriSize;
    public float progress;
    public long totalTime;
    public String filePath;
    public String oriPath;
    public boolean finish;
    public int threadCount;

    public FTPSpeedInfo() {

    }

    public static FTPSpeedInfo formatInfos(boolean download, Map<Integer, FTPSpeedInfo> infos,
                                           long speedMaxDownload, long speedMaxUp) {
        FTPSpeedInfo total = new FTPSpeedInfo();
        long speed = 0;
        long speedMax = 0;
        long speedAver = 0;
        float progress = 0;
        long totalTime = 0;
        long oriSize = 0;
        int threadCount = 0;
        for (Integer key: infos.keySet()) {
            FTPSpeedInfo bean = infos.get(key);
            if (bean != null) {
                speed += bean.speed;
                speedMax += bean.speedMax;
                speedAver += bean.speedAver;
                progress += bean.progress;
                totalTime += bean.totalTime;
                oriSize = bean.oriSize;
                threadCount = bean.threadCount;
            }
        }
        total.speed = speed;
        total.speedMax = speedMax;
        if (!download && speedMax > speedMaxDownload) {
            total.speed = total.speed / threadCount;
            if (speedMaxUp == 0 || speedMaxUp < (total.speedMax / threadCount)) {
                total.speedMax = total.speedMax / threadCount;
            } else {
                total.speedMax = speedMaxUp;
            }
        }
        total.speedAver = speedAver;
        total.progress = progress/4;
        total.totalTime = totalTime;
        total.oriSize = oriSize;
        if (total.speedAver > total.speedMax) {
            total.speedMax = total.speedAver * (11 + new Random().nextInt(10)) / 10;
        }
        return total;
    }
}
