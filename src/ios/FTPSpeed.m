//
//  FTPSpeed.m
//  GoodJob
//
//  Created by 梁仲太 on 2018/5/25.
//

#import "FTPSpeed.h"
#import <CFNetwork/CFNetwork.h>
#import "JUNFTPPutRequest.h"
#import "JUNFTPGetRequest.h"
#import "JUNFTPDeleteRequest.h"
#import "FTPManager.h"
#import "FYFtpRequest.h"
#import "YHHFtpRequest.h"

@interface FTPSpeed()<NSStreamDelegate, FTPManagerDelegate>

@property(nonatomic,copy)NSString *callbackId;
@property(nonatomic,assign)NSInteger ftpType;
@property(nonatomic,assign)NSInteger interval;
@property(nonatomic,strong)NSString *ftpIP;
@property(nonatomic,assign)NSInteger ftpPort;
@property(nonatomic,strong)NSString *ftpUN;
@property(nonatomic,strong)NSString *ftpPW;
@property(nonatomic,strong)NSString *ftpFileName;
@property(nonatomic,strong)NSString *sdFileName;
@property(nonatomic,strong)NSString *ftpOriFileName;

@property(nonatomic,strong)JUNFTPPutRequest *putRequest;
@property(nonatomic,strong)JUNFTPGetRequest *getRequest;
@property(nonatomic,strong)JUNFTPDeleteRequest *deleteRequest;
@property(nonatomic,strong)FMServer *server;
@property(nonatomic,strong)FTPManager *manager;
@property(nonatomic,strong)NSTimer* progTimer;
@property(nonatomic,assign)BOOL succeeded;

@property(nonatomic,assign)BOOL uploadStart;
@property(nonatomic,assign)BOOL downloadStart;

@property(nonatomic,strong)FYFtpRequest *ftpRequest;
@property(nonatomic,assign)long start;
@property(nonatomic,assign)long preTime;
@property(nonatomic,assign)long nowLength;
@property(nonatomic,assign)long preLength;
@property(nonatomic,assign)long speedMax;
@property(nonatomic,assign)long speedAver;
@property(nonatomic,assign)long totalTime;
@property(nonatomic,assign)long totalSize;

@property(nonatomic,strong)YHHFtpRequest *yhhRequest;

@end

@implementation FTPSpeed

-(void)coolMethod:(CDVInvokedUrlCommand *)command{
    NSLog(@"ftpspeed_cool");
    self.callbackId = command.callbackId;
    self.ftpType = [command.arguments[0] integerValue];
    self.interval = 500;
    NSLog(@"arguments.count=%ld",command.arguments.count);
    if (command.arguments.count > 1) {
        self.interval = [command.arguments[1] integerValue];
    }
    if (command.arguments.count > 2) {
        self.ftpIP    = command.arguments[2];
        self.ftpPort = [command.arguments[3] integerValue];
        self.ftpUN  = command.arguments[4];
        self.ftpPW  = command.arguments[5];
        self.ftpFileName = command.arguments[6];
        self.sdFileName  = command.arguments[7];
    }
//     self.ftpIP    = @"218.204.180.234";
//     self.ftpPort = 6021;
//     self.ftpUN  = @"ftp5g";
//     self.ftpPW  = @"ODVjNTIxM2QyMmQ2";
//     self.ftpFileName = @"read/a12.zip";
//     self.sdFileName  = command.arguments[7];
    [self startWork];
}

-(void)startWork{
    if (self.ftpType == UPLOAD) {
        NSLog(@"ftp_上传");
        // [self upLoad];
        // [self upLoadInit];
        // [self upLoadFile];
        [self uploadCommond];
        // [self uploadSession];
    } else if (self.ftpType == DOWNLOAD) {
        NSLog(@"ftp_下载");
        self.ftpOriFileName = self.ftpFileName;
        // [self downLoad];
        // [self downLoadInit];
        // [self downLoadFile];
        [self downLoadComond];
        
    } else if (self.ftpType == CLOSE) {
        NSLog(@"ftp_暂停");
        [self close];
    } else if (self.ftpType == DELETE_FTP_FILE) {
        NSLog(@"ftp_删除");
        [self deleteFTPFile];
    } else if (self.ftpType == DELETE_SD_FILE) {
        [self deleteSDFile];
    }
}

-(void)uploadSession {
    NSString *uploadPath = [NSString stringWithFormat:@"ftp://%@:%ld/%@",self.ftpIP,self.ftpPort, self.sdFileName];
    NSURL * url_upload = [NSURL URLWithString:uploadPath];
    NSMutableURLRequest *request = [[NSMutableURLRequest alloc] initWithURL:url_upload];
    [request setHTTPMethod:@"PUT"];
    NSString *parent = [NSHomeDirectory() stringByAppendingPathComponent: @"Documents/download/"];
    NSString *filePath = [parent stringByAppendingPathComponent: self.sdFileName];
    NSURL * docsDirURL = [NSURL fileURLWithPath:filePath];
  
    NSURLProtectionSpace * protectionSpace = [[NSURLProtectionSpace alloc] initWithHost:url_upload.host port:[url_upload.port integerValue] protocol:url_upload.scheme realm:nil authenticationMethod:nil];

    NSURLCredential * cred = [NSURLCredential
                            credentialWithUser:self.ftpUN
                            password:self.ftpPW
                            persistence:NSURLCredentialPersistenceForSession];
  
    NSURLCredentialStorage * cred_storage = [[NSURLCredentialStorage alloc] init];
    [cred_storage setCredential:cred forProtectionSpace:protectionSpace];
    NSURLSessionConfiguration * sessionConfig = [NSURLSessionConfiguration defaultSessionConfiguration];
    sessionConfig.URLCredentialStorage = cred_storage;
    sessionConfig.timeoutIntervalForRequest = 60.0;
    sessionConfig.timeoutIntervalForResource = 60.0;
    sessionConfig.allowsCellularAccess = YES;
    sessionConfig.HTTPMaximumConnectionsPerHost = 1;
    NSURLSession * upLoadSession = [NSURLSession sessionWithConfiguration:sessionConfig delegate:self delegateQueue:nil];
    NSURLSessionUploadTask * uploadTask = [upLoadSession uploadTaskWithRequest:request fromFile:docsDirURL];
    [uploadTask resume];
}


-(void)uploadCommond {
    self.yhhRequest = [[YHHFtpRequest alloc] init];
    self.yhhRequest.ftpUser = self.ftpUN;
    self.yhhRequest.ftpPassword = self.ftpPW;
    self.yhhRequest.serverIP = self.ftpIP;
    self.yhhRequest.serversPath = [NSString stringWithFormat:@"ftp://%@:%ld/%@",self.ftpIP, self.ftpPort, self.sdFileName];
    NSString *parent = [NSHomeDirectory() stringByAppendingPathComponent: @"Documents/download/"];
    NSString *path = [parent stringByAppendingPathComponent: self.sdFileName];
    self.yhhRequest.localPath = path;
    if (![[NSFileManager defaultManager] fileExistsAtPath: parent]) {
        [[NSFileManager defaultManager] createDirectoryAtPath: parent withIntermediateDirectories:YES attributes:nil error:nil];
        NSLog(@"------------上传开始路径不存在, 创建");
    }
    [self.yhhRequest login];
    self.start = [[NSDate date] timeIntervalSince1970] * 1000;
    self.preTime = self.start;
    self.nowLength = 0;
    self.preLength = 0;
    self.speedMax = 0;
    self.speedAver = 0;
    self.totalTime = 0;
    self.totalSize = 0;
    [self.yhhRequest upload:NO progress:^(Float32 percent, NSUInteger finishSize) {
        self.nowLength = finishSize;
        self.totalSize = [self.yhhRequest getTotalSize];
        long nowTime = [[NSDate date] timeIntervalSince1970] * 1000;
        if((nowTime - self.preTime) >= self.interval){
            if ((nowTime - self.preTime) == 0 || (nowTime - self.start) == 0 || self.totalSize == 0) {
                
            } else {
                long speed = (self.nowLength - self.preLength) * 8 / (nowTime - self.preTime);
                self.speedMax = self.speedMax > speed ? self.speedMax : speed;
                self.speedAver = self.nowLength * 8 / (nowTime - self.start);
                CGFloat progress = finishSize / self.totalSize;
                [self successWithMessage:@[[NSNumber numberWithInteger:UPLOAD],[NSNumber numberWithInteger:TESTING],[NSNumber numberWithLong:speed],[NSNumber numberWithLong: self.speedMax],[NSNumber numberWithLong: self.speedAver],[NSNumber numberWithFloat: progress],[NSNumber numberWithLong: nowTime - self.start],[NSNumber numberWithLongLong: finishSize]]];
                // NSLog(@"speed=%ld",speed);
                // NSLog(@"speedMax=%ld",self.speedMax);
                // NSLog(@"speedAver=%ld",self.speedAver);
                self.preTime = nowTime;
                self.preLength = self.nowLength;
            }
        }
        NSLog(@"上传进度percent=%f——total=%ld", percent, self.totalSize);
    } complete:^(id respond, NSError *error) {
        if (error == nil) {
            self.totalTime = [[NSDate date] timeIntervalSince1970]*1000 - self.start;
            [self successWithMessage:@[[NSNumber numberWithInteger:UPLOAD],[NSNumber numberWithInteger:FINISH],[NSNumber numberWithLong:0],[NSNumber numberWithLong:0],[NSNumber numberWithLong:0],[NSNumber numberWithFloat:100],[NSNumber numberWithLong: self.totalTime],[NSNumber numberWithLongLong: self.totalSize]]];
        } else {
            [self faileWithMessage:@[[NSNumber numberWithInteger:UPLOAD],[NSNumber numberWithInteger:TESTING_ERROR],[NSString stringWithFormat:@"%@%@",@"FTP上传测试异常", error.description]]];
        }
        NSLog(@"上传异常error=%@", error);
    }];
}

// 上传文件
-(void)upLoadFile {
    self.ftpRequest = [[FYFtpRequest alloc] initFTPClientWithUserName:self.ftpUN userPassword:self.ftpPW serverIp:self.ftpIP serverHost:self.ftpPort];
    NSStringEncoding enc = CFStringConvertEncodingToNSStringEncoding(kCFStringEncodingISOLatin1);
    NSString *ftpFileName = [NSString stringWithCString:[self.ftpFileName UTF8String] encoding:enc];
    NSString *remotePath = ftpFileName;
    NSString *locaPath = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES)[0];
    locaPath = [locaPath stringByAppendingPathComponent:[remotePath lastPathComponent]];
    self.start = [[NSDate date] timeIntervalSince1970] * 1000;
    self.preTime = self.start;
    self.nowLength = 0;
    self.preLength = 0;
    self.speedMax = 0;
    self.speedAver = 0;
    self.totalTime = 0;
    self.totalSize = 0;
    [self.ftpRequest uploadFileToRemoteRelativePath:remotePath withLocalPath:locaPath progress:^(long long totalSize, long long finishedSize) {
        self.nowLength = finishedSize;
        long nowTime = [[NSDate date] timeIntervalSince1970] * 1000;
        if((nowTime - self.preTime) >= self.interval){
            if ((nowTime - self.preTime) == 0 || (nowTime - self.start) == 0 || totalSize == 0) {
                
            } else {
                long speed = (self.nowLength - self.preLength) * 8 / (nowTime-self.preTime);
                self.speedMax = self.speedMax > speed ? self.speedMax : speed;
                self.speedAver = self.nowLength * 8 / (nowTime - self.start);
                CGFloat progress = finishedSize / totalSize;
                [self successWithMessage:@[[NSNumber numberWithInteger:UPLOAD],[NSNumber numberWithInteger:TESTING],[NSNumber numberWithLong:speed],[NSNumber numberWithLong: self.speedMax],[NSNumber numberWithLong: self.speedAver],[NSNumber numberWithFloat: progress],[NSNumber numberWithLong: nowTime - self.start],[NSNumber numberWithLongLong: finishedSize]]];
                // NSLog(@"speed=%ld",speed);
                // NSLog(@"speedMax=%ld",self.speedMax);
                // NSLog(@"speedAver=%ld",self.speedAver);
                self.preTime = nowTime;
                self.preLength = self.nowLength;
            }
        }
        
    } sucess:^(__unsafe_unretained Class resultClass, id result) {
        // NSLog(@"------------上传完成");
        self.totalTime = [[NSDate date] timeIntervalSince1970]*1000 - self.start;
        [self successWithMessage:@[[NSNumber numberWithInteger:UPLOAD],[NSNumber numberWithInteger:FINISH],[NSNumber numberWithLong:0],[NSNumber numberWithLong:0],[NSNumber numberWithLong:0],[NSNumber numberWithFloat:100],[NSNumber numberWithLong: self.totalTime],[NSNumber numberWithLongLong: self.totalSize]]];
    } fail:^(NSString *errorDescription) {
        // NSLog(@"------------上传失败");
        [self faileWithMessage:@[[NSNumber numberWithInteger:UPLOAD],[NSNumber numberWithInteger:TESTING_ERROR],[NSString stringWithFormat:@"%@%@",@"FTP上传测试异常", errorDescription]]];
    }];
}

-(void)upLoadInit {
    NSLog(@"------------上传初始化1");
    // 配置FTP服务器信息
    NSString *url = self.ftpIP;
    self.server = [FMServer serverWithDestination:url username:self.ftpUN password:self.ftpPW];
    // 初始化定时器
    self.progTimer = [NSTimer scheduledTimerWithTimeInterval:0.1 target:self selector:@selector(uploadProgress) userInfo:nil repeats:YES];
    // 激活定时器
    [self.progTimer fire];
    // 调用开始上传文件的方法
    [self performSelectorInBackground:@selector(uploadStarted) withObject:nil];
    NSLog(@"------------上传初始化2");
}

-(void)uploadStarted {
    NSLog(@"------------上传开始1");
    // 初始化FTPManager
    self.manager = [[FTPManager alloc] init];
    // 设置代理（非必须）
    self.manager.delegate = self;
    // 可以指定FTP端口
    self.server.port = (int)self.ftpPort;
    self.start = [[NSDate date] timeIntervalSince1970] * 1000;
    self.preTime = self.start;
    self.nowLength = 0;
    self.preLength = 0;
    self.speedMax = 0;
    self.speedAver = 0;
    self.totalTime = 0;
    self.totalSize = 0;
    //创建上传文件
    // UIImage *img = [UIImage imageNamed:@"abc"];
    // NSData * data = UIImagePNGRepresentation(img);
    // 开始上传并记录结果
    // self.succeeded = [self.manager uploadData:data withFileName:@"20173160007.png" toServer:self.server];
    
    NSString *parent = [NSHomeDirectory() stringByAppendingPathComponent:@"Documents/download/"];
    NSString *path = [parent stringByAppendingPathComponent:self.ftpFileName];
    //    NSString *path = @"/Users/lxf/Desktop/1114.txt";
    NSURL *fileUrl = [NSURL URLWithString:path];
    self.succeeded = [self.manager uploadFile:fileUrl toServer:self.server];
    
    [self performSelectorOnMainThread:@selector(uploadFinished) withObject:nil waitUntilDone:NO];
    NSLog(@"------------上传开始2");
}

-(void)uploadProgress {
    if (!self.manager) {
        return;
    }
    NSNumber* progress = [self.manager.progress objectForKey:kFMProcessInfoProgress];
    float percent = progress.floatValue; //0.0f ≤ p ≤ 1.0f
    NSLog(@"------------上传进度%f-------%lld", percent, self.manager.totalSize);
    self.nowLength = percent * self.manager.totalSize;
    long nowTime = [[NSDate date] timeIntervalSince1970] * 1000;
    if((nowTime - self.preTime) >= self.interval){
        if ((nowTime - self.preTime) == 0 || (nowTime - self.start) == 0 || self.manager.totalSize == 0) {
        } else {
            long speed = (self.nowLength - self.preLength) * 8 / (nowTime-self.preTime);
            self.speedMax = self.speedMax > speed ? self.speedMax : speed;
            self.speedAver = self.nowLength * 8 / (nowTime - self.start);
            CGFloat progress = self.nowLength / self.manager.totalSize;
            [self successWithMessage:@[[NSNumber numberWithInteger:UPLOAD],[NSNumber numberWithInteger:TESTING],[NSNumber numberWithLong:speed],[NSNumber numberWithLong: self.speedMax],[NSNumber numberWithLong: self.speedAver],[NSNumber numberWithFloat: progress],[NSNumber numberWithLong: nowTime - self.start],[NSNumber numberWithLongLong: self.nowLength]]];
            self.preTime = nowTime;
            self.preLength = self.nowLength;
        }
    }
}

-(void)uploadFinished {
    NSLog(@"------------上传结束");
    [self successWithMessage:@[[NSNumber numberWithInteger:UPLOAD],[NSNumber numberWithInteger:FINISH],[NSNumber numberWithLong:0],[NSNumber numberWithLong:0],[NSNumber numberWithLong:0],[NSNumber numberWithFloat:100],[NSNumber numberWithLong: self.totalTime],[NSNumber numberWithLongLong: self.totalSize]]];
    [self.progTimer invalidate];
    self.progTimer = nil;
    self.server = nil;
    self.manager = nil;
}

-(void)upLoad{
    if (![JUNFTPPutRequest checkFTPSpeedFile:self.sdFileName]) {
        NSLog(@"请先下载FTP测试文件");
        [self faileWithMessage:@[[NSNumber numberWithInteger:self.ftpType],[NSNumber numberWithInteger:FTP_NO_FILE],@"请先下载FTP测试文件"]];
    } else if (self.uploadStart) {
        NSLog(@"正在上传测速");
        [self faileWithMessage:@[[NSNumber numberWithInteger:self.ftpType],[NSNumber numberWithInteger:TESTING],@"正在上传测速"]];
    } else {
        self.uploadStart = YES;
        // NSString *url = [NSString stringWithFormat:@"ftp://%@:%@@%@:%ld/%@",self.ftpUN,self.ftpPW,self.ftpIP,self.ftpPort,self.sdFileName];
        NSString *url = [NSString stringWithFormat:@"ftp://%@:%ld/%@",self.ftpIP,self.ftpPort,self.sdFileName];
        if (self.putRequest == nil) {
            self.putRequest = [JUNFTPPutRequest requestWithResource:self.sdFileName toDirectory:[NSURL URLWithString:url] finishedBlock:^(long totalTime,long totalSize){
                NSLog(@"get finish\n");

                [self successWithMessage:@[[NSNumber numberWithInteger:UPLOAD],[NSNumber numberWithInteger:FINISH],[NSNumber numberWithLong:0],[NSNumber numberWithLong:0],[NSNumber numberWithLong:0],[NSNumber numberWithFloat:100],[NSNumber numberWithLong:totalTime],[NSNumber numberWithLong:totalSize]]];
                self.uploadStart = NO;
                
            }failBlock:^{
                NSLog(@"get fail\n");
                [self faileWithMessage:@[[NSNumber numberWithInteger:UPLOAD],[NSNumber numberWithInteger:TESTING_ERROR],@"FTP上传测试异常"]];
                self.uploadStart = NO;
                
            }progressBlock:^(float progress,long speed,long speedMax,long speedAver,long totalTime,long totalSize){
                NSLog(@"get ---%f\n",progress);
                [self successWithMessage:@[[NSNumber numberWithInteger:UPLOAD],[NSNumber numberWithInteger:TESTING],[NSNumber numberWithLong:speed]
                                           ,[NSNumber numberWithLong:speedMax],[NSNumber numberWithLong:speedAver],[NSNumber numberWithFloat:progress],[NSNumber numberWithLong:totalTime],[NSNumber numberWithLong:totalSize]]];
            }];
        }
        self.putRequest.interval = self.interval;
        [self.putRequest start:self andUN:self.ftpUN andPW:self.ftpPW andFTPPath:url];
    }
    
}

// 指令下载
-(void)downLoadComond {
    self.yhhRequest = [[YHHFtpRequest alloc] init];
    self.yhhRequest.ftpUser = self.ftpUN;
    self.yhhRequest.ftpPassword = self.ftpPW;
    self.yhhRequest.serverIP = self.ftpIP;
    self.yhhRequest.serversPath = [NSString stringWithFormat:@"ftp://%@:%ld/%@",self.ftpIP,self.ftpPort,self.ftpFileName];
    NSString *parent = [NSHomeDirectory() stringByAppendingPathComponent: @"Documents/download/"];
    NSString *path = [parent stringByAppendingPathComponent: self.sdFileName];
    self.yhhRequest.localPath = path;
    if (![[NSFileManager defaultManager] fileExistsAtPath: parent]) {
        [[NSFileManager defaultManager] createDirectoryAtPath: parent withIntermediateDirectories:YES attributes:nil error:nil];
        NSLog(@"------------下载开始路径不存在, 创建");
    }
    [self.yhhRequest login];
    self.start = [[NSDate date] timeIntervalSince1970] * 1000;
    self.preTime = self.start;
    self.nowLength = 0;
    self.preLength = 0;
    self.speedMax = 0;
    self.speedAver = 0;
    self.totalTime = 0;
    self.totalSize = 0;
    [self.yhhRequest download:NO progress:^(Float32 percent, NSUInteger finishSize) {
        self.nowLength = finishSize;
        self.totalSize = [self.yhhRequest getTotalSize];
        long nowTime = [[NSDate date] timeIntervalSince1970] * 1000;
        if((nowTime - self.preTime) >= self.interval){
            if ((nowTime - self.preTime) == 0 || (nowTime - self.start) == 0 || self.totalSize == 0) {
                
            } else {
                long speed = (self.nowLength - self.preLength) * 8 / (nowTime - self.preTime);
                self.speedMax = self.speedMax > speed ? self.speedMax : speed;
                self.speedAver = self.nowLength * 8 / (nowTime - self.start);
                CGFloat progress = finishSize / self.totalSize;
                [self successWithMessage:@[[NSNumber numberWithInteger:DOWNLOAD],[NSNumber numberWithInteger:TESTING],[NSNumber numberWithLong:speed],[NSNumber numberWithLong: self.speedMax],[NSNumber numberWithLong: self.speedAver],[NSNumber numberWithFloat: progress],[NSNumber numberWithLong: nowTime - self.start],[NSNumber numberWithLongLong: finishSize]]];
                // NSLog(@"speed=%ld",speed);
                // NSLog(@"speedMax=%ld",self.speedMax);
                // NSLog(@"speedAver=%ld",self.speedAver);
                self.preTime = nowTime;
                self.preLength = self.nowLength;
            }
        }
        NSLog(@"下载进度percent=%f", percent);
    } complete:^(id respond, NSError *error) {
        if (error == nil) {
            self.totalTime = [[NSDate date] timeIntervalSince1970]*1000 - self.start;
            [self successWithMessage:@[[NSNumber numberWithInteger:DOWNLOAD],[NSNumber numberWithInteger:FINISH],[NSNumber numberWithLong:0],[NSNumber numberWithLong:0],[NSNumber numberWithLong:0],[NSNumber numberWithFloat:100],[NSNumber numberWithLong: self.totalTime],[NSNumber numberWithLongLong: self.totalSize]]];
            NSLog(@"下载完成");
        } else {
            [self faileWithMessage:@[[NSNumber numberWithInteger:DOWNLOAD],[NSNumber numberWithInteger:TESTING_ERROR],[NSString stringWithFormat:@"%@%@",@"FTP下载测试异常", error.description]]];
        }
        NSLog(@"下载异常error=%@", error);
    }];
}

// 下载文件
-(void)downLoadFile {
    self.ftpRequest = [[FYFtpRequest alloc] initFTPClientWithUserName:self.ftpUN userPassword:self.ftpPW serverIp:self.ftpIP serverHost:self.ftpPort];
    NSStringEncoding enc = CFStringConvertEncodingToNSStringEncoding(kCFStringEncodingISOLatin1);
    NSString *ftpFileName = [NSString stringWithCString:[self.ftpFileName UTF8String] encoding:enc];
    NSString *remotePath = ftpFileName;
    NSString *locaPath = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES)[0];
    locaPath = [locaPath stringByAppendingPathComponent:remotePath];
    self.start = [[NSDate date] timeIntervalSince1970] * 1000;
    self.preTime = self.start;
    self.nowLength = 0;
    self.preLength = 0;
    self.speedMax = 0;
    self.speedAver = 0;
    self.totalTime = 0;
    self.totalSize = 0;
    [self.ftpRequest downloadFileWithRelativePath:remotePath toLocalPath:locaPath progress:^(long long totalSize, long long finishedSize) {
        self.nowLength = finishedSize;
        long nowTime = [[NSDate date] timeIntervalSince1970] * 1000;
        if((nowTime - self.preTime) >= self.interval){
            if ((nowTime - self.preTime) == 0 || (nowTime - self.start) == 0 || totalSize == 0) {
                
            } else {
                long speed = (self.nowLength - self.preLength) * 8 / (nowTime-self.preTime);
                self.speedMax = self.speedMax > speed ? self.speedMax : speed;
                self.speedAver = self.nowLength * 8 / (nowTime - self.start);
                CGFloat progress = finishedSize / totalSize;
                [self successWithMessage:@[[NSNumber numberWithInteger:DOWNLOAD],[NSNumber numberWithInteger:TESTING],[NSNumber numberWithLong:speed],[NSNumber numberWithLong: self.speedMax],[NSNumber numberWithLong: self.speedAver],[NSNumber numberWithFloat: progress],[NSNumber numberWithLong: nowTime - self.start],[NSNumber numberWithLongLong: finishedSize]]];
                // NSLog(@"speed=%ld",speed);
                // NSLog(@"speedMax=%ld",self.speedMax);
                // NSLog(@"speedAver=%ld",self.speedAver);
                self.preTime = nowTime;
                self.preLength = self.nowLength;
            }
        }
        // NSLog(@"------------下载total=%lld___finishedSize=%lld", totalSize, finishedSize);
    } sucess:^(__unsafe_unretained Class resultClass, id result) {
        NSLog(@"------------下载结束");
        self.totalTime = [[NSDate date] timeIntervalSince1970]*1000 - self.start;
        [self successWithMessage:@[[NSNumber numberWithInteger:DOWNLOAD],[NSNumber numberWithInteger:FINISH],[NSNumber numberWithLong:0],[NSNumber numberWithLong:0],[NSNumber numberWithLong:0],[NSNumber numberWithFloat:100],[NSNumber numberWithLong: self.totalTime],[NSNumber numberWithLongLong: self.totalSize]]];
    } fail:^(NSString *errorDescription) {
        NSLog(@"------------下载失败");
        [self faileWithMessage:@[[NSNumber numberWithInteger:DOWNLOAD],[NSNumber numberWithInteger:TESTING_ERROR],[NSString stringWithFormat:@"%@%@",@"FTP下载测试异常", errorDescription]]];
        [self.ftpRequest disconnectFTPServer];
    }];
}

-(void)downLoadInit{
    NSLog(@"------------下载初始化1");
    // 配置FTP服务器信息
    NSString *url = self.ftpIP;
    self.server = [FMServer serverWithDestination:url username:self.ftpUN password:self.ftpPW];
    // 初始化定时器
    self.progTimer = [NSTimer scheduledTimerWithTimeInterval:0.1 target:self selector:@selector(downloadProgress) userInfo:nil repeats:YES];
    // 激活定时器
    [self.progTimer fire];
    // 调用开始上传文件的方法
    [self performSelectorInBackground:@selector(downloadStarted) withObject:nil];
    NSLog(@"------------下载初始化2");
}

-(void)downloadStarted {
    NSLog(@"------------下载开始0");
    // 初始化FTPManager
    self.manager = [[FTPManager alloc] init];
    // 设置代理（非必须）
    self.manager.delegate = self;
    // 可以指定FTP端口
    self.server.port = (int)self.ftpPort;
    self.start = [[NSDate date] timeIntervalSince1970] * 1000;
    self.preTime = self.start;
    self.nowLength = 0;
    self.preLength = 0;
    self.speedMax = 0;
    self.speedAver = 0;
    self.totalTime = 0;
    self.totalSize = 0;
    //创建上传文件
    // UIImage *img = [UIImage imageNamed:@"abc"];
    // NSData * data = UIImagePNGRepresentation(img);
    // 开始上传并记录结果
    // self.succeeded = [self.manager uploadData:data withFileName:@"20173160007.png" toServer:self.server];
    
    NSString *parent = [NSHomeDirectory() stringByAppendingPathComponent: @"Documents/download/"];
    // NSString *path = [parent stringByAppendingPathComponent: self.sdFileName];
    //    NSString *path = @"/Users/lxf/Desktop/1114.txt";
    NSURL *dirUrl = [NSURL URLWithString:parent];
    
    NSStringEncoding enc = CFStringConvertEncodingToNSStringEncoding(kCFStringEncodingISOLatin1);
    self.ftpFileName =  [NSString stringWithCString:[self.ftpFileName UTF8String] encoding:enc];
    NSLog(@"------------下载开始0-1");
    // self.succeeded = [self.manager uploadFile:fileUrl toServer:self.server];
    self.succeeded = [self.manager downloadFile:self.ftpFileName toDirectory:dirUrl fromServer:self.server];
    NSLog(@"------------下载开始0-2");
    [self performSelectorOnMainThread:@selector(downloadFinished) withObject:nil waitUntilDone:NO];
    NSLog(@"------------下载开始2");
}

-(void)downloadProgress {
    if (!self.manager) {
        return;
    }
    long long totalSize = [[self.manager.serverReadStream propertyForKey:(id)kCFStreamPropertyFTPResourceSize] longLongValue];
    NSLog(@"------------下载大小%lld", totalSize);
    NSNumber* progress = [self.manager.progress objectForKey:kFMProcessInfoProgress];
    float percent = progress.floatValue; //0.0f ≤ p ≤ 1.0f
    NSLog(@"------------下载进度%f", percent);
    self.nowLength = percent * self.manager.totalSize;
    long nowTime = [[NSDate date] timeIntervalSince1970] * 1000;
    if((nowTime - self.preTime) >= self.interval){
        if ((nowTime - self.preTime) == 0 || (nowTime - self.start) == 0 || self.manager.totalSize == 0) {
        } else {
            long speed = (self.nowLength - self.preLength) * 8 / (nowTime-self.preTime);
            self.speedMax = self.speedMax > speed ? self.speedMax : speed;
            self.speedAver = self.nowLength * 8 / (nowTime - self.start);
            CGFloat progress = self.nowLength / self.manager.totalSize;
            [self successWithMessage:@[[NSNumber numberWithInteger:DOWNLOAD],[NSNumber numberWithInteger:TESTING],[NSNumber numberWithLong:speed],[NSNumber numberWithLong: self.speedMax],[NSNumber numberWithLong: self.speedAver],[NSNumber numberWithFloat: progress],[NSNumber numberWithLong: nowTime - self.start],[NSNumber numberWithLongLong: self.nowLength]]];
            self.preTime = nowTime;
            self.preLength = self.nowLength;
        }
    }
}

-(void)downloadFinished {
    NSLog(@"------------下载结束");
    [self successWithMessage:@[[NSNumber numberWithInteger:UPLOAD],[NSNumber numberWithInteger:FINISH],[NSNumber numberWithLong:0],[NSNumber numberWithLong:0],[NSNumber numberWithLong:0],[NSNumber numberWithFloat:100],[NSNumber numberWithLong: self.totalTime],[NSNumber numberWithLongLong: self.totalSize]]];
    [self.progTimer invalidate];
    self.progTimer = nil;
    self.server = nil;
    self.manager = nil;
}

-(void)downLoad{
    if(self.downloadStart){
        NSLog(@"正在测速下载");
        [self faileWithMessage:@[[NSNumber numberWithInteger:self.ftpType],[NSNumber numberWithInteger:TESTING],@"正在下载测速"]];
    } else {
        self.downloadStart = YES;
        //NSString *url = [NSString stringWithFormat:@"ftp://%@:%@@%@:%ld/%@",self.ftpUN,self.ftpPW,self.ftpIP,self.ftpPort,self.ftpFileName];
        NSStringEncoding enc = CFStringConvertEncodingToNSStringEncoding(kCFStringEncodingISOLatin1);
        self.ftpFileName =  [NSString stringWithCString:[self.ftpFileName UTF8String] encoding:enc];
        NSString *url = [NSString stringWithFormat:@"ftp://%@:%ld/%@",self.ftpIP,self.ftpPort,self.ftpFileName];
        // url = [NSString stringWithCString:[url UTF8String] encoding:enc];
        if(self.getRequest == nil) {
            self.getRequest = [JUNFTPGetRequest requestWithResource:[NSURL URLWithString:url] toDirectory:self.sdFileName finishedBlock:^(long totalTime,long totalSize){
                NSLog(@"get finish\n");
                self.downloadStart = NO;
                [self successWithMessage:@[[NSNumber numberWithInteger:DOWNLOAD],[NSNumber numberWithInteger:FINISH],[NSNumber numberWithLong:0],[NSNumber numberWithLong:0],[NSNumber numberWithLong:0],[NSNumber numberWithFloat:100],[NSNumber numberWithLong:totalTime],[NSNumber numberWithLong:totalSize]]];
            } failBlock:^ (NSString *msg){
                NSLog(@"get fail\n");
                self.downloadStart = NO;
                [self faileWithMessage:@[[NSNumber numberWithInteger:DOWNLOAD],[NSNumber numberWithInteger:TESTING_ERROR],[NSString stringWithFormat:@"%@%@",@"FTP下载测试异常", msg]]];
            } progressBlock:^ (float progress,long speed,long speedMax,long speedAver,long totalTime,long totalSize){
                NSLog(@"get ---%f\n",progress);
                [self successWithMessage:@[[NSNumber numberWithInteger:DOWNLOAD],[NSNumber numberWithInteger:TESTING],[NSNumber numberWithLong:speed],[NSNumber numberWithLong:speedMax],[NSNumber numberWithLong:speedAver],[NSNumber numberWithFloat:progress],[NSNumber numberWithLong:totalTime],[NSNumber numberWithLong:totalSize]]];
            }];
        }
        self.getRequest.interval = self.interval;
        [self.getRequest start:self andUN:self.ftpUN andPW:self.ftpPW];
    }
}

-(void)close {
    if (self.ftpRequest != nil) {
        [self.ftpRequest disconnectFTPServer];
    }
    if (self.manager != nil) {
        [self.manager cancel];
    }
    if (self.yhhRequest != nil) {
        [self.yhhRequest quit];
    }
    if (self.putRequest != nil){
        [self.putRequest stop];
        self.putRequest = nil;
    }
    if (self.getRequest != nil){
        [self.getRequest stop];
        //[self.getRequest removeFTPSpeedFile];
        self.getRequest = nil;
    }//else{
        //[self deleteSDFile];
    //}
    //[self deleteFTPFile];
    self.uploadStart = NO;
    self.downloadStart = NO;
    self.ftpType = CLOSE;
    [self successWithMessage:@[[NSNumber numberWithInteger:self.ftpType],[NSNumber numberWithInteger:BREAK_OFF],[NSNumber numberWithLong:0],[NSNumber numberWithLong:0],[NSNumber numberWithLong:0],[NSNumber numberWithFloat:0.0],[NSNumber numberWithLong:0],[NSNumber numberWithLong:0]]];
}

-(void)deleteFTPFile{
    if(self.ftpFileName == nil||[self.ftpFileName isEqualToString:self.ftpOriFileName]){
        if(self.ftpType!=CLOSE)
        [self faileWithMessage:@[[NSNumber numberWithInteger:DELETE_FTP_FILE],[NSNumber numberWithInteger:DELETE_FTP_FILE_FAILE],@"FTP上传测试文件名有误"]];
        return;
    }
    // NSString *url = [NSString stringWithFormat:@"ftp://%@:%@@%@:%ld/%@",self.ftpUN,self.ftpPW,self.ftpIP,self.ftpPort,self.sdFileName];
    NSString *url = [NSString stringWithFormat:@"ftp://%@:%ld/%@",self.ftpIP,self.ftpPort,self.sdFileName];
    self.deleteRequest = [JUNFTPDeleteRequest requestWithDirectory:url finishedBlock:^{
        if(self.ftpType!=CLOSE)
        [self successWithMessage:@[[NSNumber numberWithInteger:DELETE_FTP_FILE],[NSNumber numberWithInteger:DELETE_FTP_FILE_SUCCESS],[NSNumber numberWithLong:0],[NSNumber numberWithLong:0],[NSNumber numberWithLong:0],[NSNumber numberWithFloat:0.0],[NSNumber numberWithLong:0],[NSNumber numberWithLong:0]]];
    }failBlock:^{
        if(self.ftpType!=CLOSE)
        [self faileWithMessage:@[[NSNumber numberWithInteger:DELETE_FTP_FILE],[NSNumber numberWithInteger:DELETE_FTP_FILE_FAILE],@"FTP上传测试文件删除失败"]];
    }];
    [self.deleteRequest start];
}

-(void)deleteSDFile{
    self.getRequest = [JUNFTPGetRequest new];
    self.getRequest.directoryStr = self.sdFileName;
    [self.getRequest removeFTPSpeedFile];
    self.getRequest = nil;
    if(self.ftpType!=CLOSE)
    [self successWithMessage:@[[NSNumber numberWithInteger:DELETE_SD_FILE],[NSNumber numberWithInteger:DELETE_SD_FILE_SUCCESS],[NSNumber numberWithLong:0],[NSNumber numberWithLong:0],[NSNumber numberWithLong:0],[NSNumber numberWithFloat:0.0],[NSNumber numberWithLong:0],[NSNumber numberWithLong:0]]];
}

-(void)successWithMessage:(NSArray *)messages{
    if(self.callbackId==nil)return;
    CDVPluginResult *result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsArray:messages];
    [result setKeepCallbackAsBool:YES];
    [self.commandDelegate sendPluginResult:result callbackId:self.callbackId];
}

-(void)faileWithMessage:(NSArray *)message{
    if(self.callbackId==nil)return;
    CDVPluginResult *result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsArray:message];
    [result setKeepCallbackAsBool:YES];
    [self.commandDelegate sendPluginResult:result callbackId:self.callbackId];
}

@end
