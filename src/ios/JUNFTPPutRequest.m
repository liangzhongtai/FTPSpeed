//
//  JUNFTPPutRequest.m
//  JUNFTPDemo
//
//  Created by 王 莉君 on 14-7-10.
//  Copyright (c) 2014年 王 莉君. All rights reserved.
//

#import "JUNFTPPutRequest.h"
#import "JUNFTPDeleteRequest.h"

#define UPLOADPATH [NSHomeDirectory() stringByAppendingPathComponent:@"Documents/download/"]

void myPutSocketWriteCallBack (CFWriteStreamRef stream, CFStreamEventType event, void *myPtr);

@interface JUNFTPPutRequest (){
    NSURL *directoryUrl;
    NSString *resoureceStr;
    
}



@end

@implementation JUNFTPPutRequest

-(id)initWithResource:(NSString*)resourece
          toDirectory:(NSURL*)url
        finishedBlock:(FTPPutFinishedBlock)finishedBlock
            failBlock:(FTPPutFailBlock)failBlock
        progressBlock:(FTPPutProgressBlock)progressBlock{
    
	self = [super init];
    
    if(self){
        resoureceStr = resourece;
		directoryUrl = url;
        self.finishedBlock = finishedBlock;
        self.failBlock = failBlock;
        self.progressBlock = progressBlock;
        //self.ftpPath = [NSString stringWithFormat:@"%@/%@",url,resoureceStr];
        
	}
	return self;
}

+(JUNFTPPutRequest*)requestWithResource:(NSString*)resourece
                            toDirectory:(NSURL*)url{
    return [[self alloc] initWithResource:resourece toDirectory:url finishedBlock:nil failBlock:nil progressBlock:nil];
}

+(JUNFTPPutRequest *)requestWithResource:(NSString*)resourece
                             toDirectory:(NSURL*)url
                           finishedBlock:(FTPPutFinishedBlock)finishedBlock
                               failBlock:(FTPPutFailBlock)failBlock
                           progressBlock:(FTPPutProgressBlock)progressBlock{
    return [[self alloc] initWithResource:resourece toDirectory:url finishedBlock:finishedBlock failBlock:failBlock progressBlock:progressBlock];
}

-(void)start:(FTPSpeed *)plugin andUN:(NSString *)username andPW:(NSString *)password andFTPPath:(NSString *)ftpPath{
    NSFileManager *fileManager = [NSFileManager defaultManager];
    
    if (![fileManager fileExistsAtPath:UPLOADPATH]){
        [fileManager createDirectoryAtPath: UPLOADPATH withIntermediateDirectories:YES attributes:nil error:nil];
        
    }
    NSString *path = [UPLOADPATH stringByAppendingPathComponent:resoureceStr];
    self.bytesTotal = [[[NSFileManager defaultManager] attributesOfItemAtPath:path error:nil] fileSize];
    NSURL *url = [NSURL fileURLWithPath:path];
    
    NSLog(@"上传存储地址:url=%@",url);
    NSLog(@"上传地址:resourceUrl=%@",directoryUrl.absoluteURL);
    NSLog(@"上传地址:path=%@",path);
    
    if (self.fileStream == nil) {
        self.fileStream = CFReadStreamCreateWithFile(kCFAllocatorDefault, (__bridge CFURLRef)(url));
        if (!CFReadStreamOpen(self.fileStream)) {
            NSLog(@"FTP上传测试文件读取异常");
            [plugin faileWithMessage:@[[NSNumber numberWithInteger:UPLOAD],[NSNumber numberWithInteger:TESTING_ERROR],@"FTP上传测试文件读取异常"]];
            return;
        }
    }
    
    //初始化测速变量
    self.start = [[NSDate date] timeIntervalSince1970]*1000;
    self.preTime = self.start;
    self.nowLength = 0;
    self.preLength = 0;
    self.speedMax = 0;
    self.speedAver = 0;
    self.totalTime = 0;
    self.totalSize = 0;
    
    self.ftpPath = ftpPath;
    
    self.myWriteStream = CFWriteStreamCreateWithFTPURL(NULL, (__bridge CFURLRef)directoryUrl);
    CFWriteStreamSetProperty(self.myWriteStream, kCFStreamPropertyFTPFetchResourceInfo, kCFBooleanTrue);
    
    if(CFWriteStreamSetProperty(self.myWriteStream ,  kCFStreamPropertyFTPUserName,  (__bridge CFTypeRef)username)&&
       CFWriteStreamSetProperty(self.myWriteStream ,  kCFStreamPropertyFTPPassword,  (__bridge CFTypeRef)password)){
        CFStreamClientContext clientContext;
        clientContext.version = 0;
        clientContext.info = CFBridgingRetain(self) ;
        clientContext.retain = nil;
        clientContext.release = nil;
        clientContext.copyDescription = nil;
        if (CFWriteStreamSetClient (self.myWriteStream,
                                    kCFStreamEventOpenCompleted |
                                    kCFStreamEventHasBytesAvailable |
                                    kCFStreamEventCanAcceptBytes |
                                    kCFStreamEventErrorOccurred |
                                    kCFStreamEventEndEncountered,
                                    myPutSocketWriteCallBack,
                                    &clientContext ) ){
            //NSLog(@"Set write callBack Succeeded");
            CFWriteStreamScheduleWithRunLoop(self.myWriteStream,
                                             CFRunLoopGetCurrent(),
                                             kCFRunLoopCommonModes);
        }else{
            //NSLog(@"Set write callBack Failed");
            [plugin faileWithMessage:@[[NSNumber numberWithInteger:UPLOAD],[NSNumber numberWithInteger:TESTING_ERROR],@"FTP上传测速回调设置失败"]];
        }
        
        BOOL success = CFWriteStreamOpen(self.myWriteStream);
        if (!success) {
            //NSLog(@"stream open fail\n");
            [plugin faileWithMessage:@[[NSNumber numberWithInteger:UPLOAD],[NSNumber numberWithInteger:TESTING_ERROR],@"FTP上传测速流获取失败"]];
            return;
        }
    }else{
        [plugin faileWithMessage:@[@"FTP用户名或密码错误"]];
    }
}

-(void)stop{
    if(self.myWriteStream != nil){
        CFWriteStreamUnscheduleFromRunLoop(self.myWriteStream, CFRunLoopGetCurrent(), kCFRunLoopCommonModes);
        CFWriteStreamClose(self.myWriteStream);
        CFRelease(self.myWriteStream);
    }
    self.myWriteStream = nil;

    if(self.fileStream != nil){
        CFReadStreamClose(self.fileStream);
        CFRelease(self.fileStream);
    }
    self.fileStream = nil;
    
    //上传结束，删除ftp服务器上的文件
    JUNFTPDeleteRequest *deleteRequest = [JUNFTPDeleteRequest requestWithDirectory:self.ftpPath finishedBlock:^{
        //NSLog(@"delete success\n");
         //[plugin faileWithMessage:@[[NSNumber numberWithInteger:DELETE_FTP_FILE],[NSNumber numberWithInteger:DELETE_FTP_FILE_SUCCESS],@"FTP上传测试文件删除成功"]];
    }failBlock:^{
        //NSLog(@"delete fail\n");
         //[plugin faileWithMessage:@[[NSNumber numberWithInteger:DELETE_FTP_FILE],[NSNumber numberWithInteger:DELETE_FTP_FILE_FAILE],@"FTP上传测试文件删除失败"]];
    }];
    [deleteRequest start];
}

+(BOOL)checkFTPSpeedFile:(NSString *)fileName{
    NSFileManager *fileManager = [NSFileManager defaultManager];
    if (![fileManager fileExistsAtPath:UPLOADPATH]){
        [fileManager createDirectoryAtPath: UPLOADPATH withIntermediateDirectories:YES attributes:nil error:nil];
    }
    NSString *path = [UPLOADPATH stringByAppendingPathComponent:fileName];
    long long bytesTotal = [[[NSFileManager defaultManager] attributesOfItemAtPath:path error:nil] fileSize];
    //NSLog(@"bytesToal=%lld",bytesTotal);
    return bytesTotal > 100;
}

@end

#define BUFSIZE 32768
void myPutSocketWriteCallBack (CFWriteStreamRef stream, CFStreamEventType event, void *myPtr){
    JUNFTPPutRequest* request = (__bridge JUNFTPPutRequest *)myPtr;
    long start = [[NSDate date] timeIntervalSince1970]*1000;
    long totalSize = 0;
    switch(event){
        case NSStreamEventHasSpaceAvailable:{
            UInt8 recvBuffer[BUFSIZE];
            
            CFIndex bytesRead = CFReadStreamRead(request.fileStream, recvBuffer, BUFSIZE);
            
            //NSLog(@"bytesRead:%ld\n",bytesRead);
            if (bytesRead > 0){
                NSInteger   bytesOffset = 0;
                do{
                    CFIndex bytesWritten = CFWriteStreamWrite(request.myWriteStream, &recvBuffer[bytesOffset], bytesRead-bytesOffset );
                    if (bytesWritten > 0) {
                        bytesOffset += bytesWritten;
                        request.bytesUploaded +=bytesWritten;
                        
                        request.nowLength += bytesWritten;
                        long nowTime = [[NSDate date] timeIntervalSince1970]*1000;
                        if(nowTime-request.preTime>=request.interval){
                            long speed = (request.nowLength-request.preLength)*8/(nowTime-request.preTime);
                            request.speedMax = request.speedMax>speed?request.speedMax:speed;
                            request.speedAver = request.nowLength*8/(nowTime-request.start);
                            request.progressBlock((float)request.bytesUploaded/(float)request.bytesTotal,speed,request.speedMax,request.speedAver,nowTime-start,request.nowLength);
                            
                            //NSLog(@"speed=%ld",speed);
                            //NSLog(@"speedMax=%ld",request.speedMax);
                            //NSLog(@"speedAver=%ld",request.speedAver);
                            
                            request.preTime = nowTime;
                            request.preLength = request.nowLength;
                        }
                    }else if (bytesWritten == 0){
                        break;
                    }else{
                        request.failBlock();
                        return;
                    }
                }while ((bytesRead-bytesOffset)>0);
            }else if(bytesRead == 0){
                totalSize = request.nowLength;
                request.finishedBlock([[NSDate date] timeIntervalSince1970]*1000-start,totalSize);
                [request stop];
            }else{
                request.failBlock();
            }
        }
            break;
        case kCFStreamEventErrorOccurred:{
            CFStreamError error = CFWriteStreamGetError(stream);
            //NSLog(@"kCFStreamEventErrorOccurred-%d\n",(int)error.error);
            [request stop];
            request.failBlock();
        }
            break;
        case kCFStreamEventEndEncountered:
            //NSLog(@"upload finished\n");
            request.finishedBlock([[NSDate date] timeIntervalSince1970]*1000-start,totalSize);
            [request stop];
            break;
        default:
            break;
    }
}
