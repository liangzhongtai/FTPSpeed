//
//  JUNFTPGetRequest.m
//  JUNFTPDemo
//
//  Created by 王 莉君 on 14-7-10.
//  Copyright (c) 2014年 王 莉君. All rights reserved.
//

#import "JUNFTPGetRequest.h"

#define DOWNLOADPATH [NSHomeDirectory() stringByAppendingPathComponent:@"Documents/download/"] 

void myGetSocketReadCallBack (CFReadStreamRef stream, CFStreamEventType event, void *myPtr);

@interface JUNFTPGetRequest (){
    NSURL* resourceUrl;
    //NSString *directoryStr;
    CFReadStreamRef readStream;
}



@end

@implementation JUNFTPGetRequest

-(id)initWithResource:(NSURL*)url
          toDirectory:(NSString*)directory
        finishedBlock:(FTPGetFinishedBlock)finishedBlock
            failBlock:(FTPGetFailBlock)failBlock
        progressBlock:(FTPGetProgressBlock)progressBlock{
    
	self = [super init];
    
    if(self){
        resourceUrl = url;
		self.directoryStr = directory;
        self.finishedBlock = finishedBlock;
        self.failBlock = failBlock;
        self.progressBlock = progressBlock;
        
	}
	return self;
}

+(JUNFTPGetRequest*)requestWithResource:(NSURL*)url
                            toDirectory:(NSString*)directory{
    return [[self alloc] initWithResource:url toDirectory:directory finishedBlock:nil failBlock:nil progressBlock:nil];
}

+(JUNFTPGetRequest *)requestWithResource:(NSURL*)url
                             toDirectory:(NSString*)directory
                           finishedBlock:(FTPGetFinishedBlock)finishedBlock
                               failBlock:(FTPGetFailBlock)failBlock
                           progressBlock:(FTPGetProgressBlock)progressBlock{
    return [[self alloc] initWithResource:url toDirectory:directory finishedBlock:finishedBlock failBlock:failBlock progressBlock:progressBlock];
}

-(void)start:(FTPSpeed *)plugin andUN:(NSString *)username andPW:(NSString *)password{
    NSFileManager *fileManager = [NSFileManager defaultManager];
    
    //下载前，先删除旧的本地文件
    NSLog(@"下载-----1");
    [self removeFTPSpeedFile];
    NSLog(@"下载-----2");
    
    if (![fileManager fileExistsAtPath:DOWNLOADPATH]){
        [fileManager createDirectoryAtPath: DOWNLOADPATH withIntermediateDirectories:YES attributes:nil error:nil];
    }
    NSString *path = [DOWNLOADPATH stringByAppendingPathComponent:self.directoryStr];
    // NSStringEncoding enc = CFStringConvertEncodingToNSStringEncoding(kCFStringEncodingISOLatin1);
    // path = [NSString stringWithCString:[path UTF8String] encoding:enc];
    NSURL *url = [NSURL fileURLWithPath:path];
    
    NSLog(@"下载存储地址:url=%@",url);
    NSLog(@"下载地址:resourceUrl=%@",resourceUrl.absoluteURL);
    if (self.fileStream == nil) {
        self.fileStream = CFWriteStreamCreateWithFile(kCFAllocatorDefault, (__bridge CFURLRef)(url));
        if (!CFWriteStreamOpen(self.fileStream)) {
            //CFStreamError myErr = CFWriteStreamGetError(myWriteStream); // An error has occurred.
            NSLog(@"FTP下载测试文件写入异常");
            [plugin faileWithMessage:@[[NSNumber numberWithInteger:DOWNLOAD],[NSNumber numberWithInteger:TESTING_ERROR], @"FTP下载测试文件写入异常"]];
            return;
        }
    }
    
    readStream = CFReadStreamCreateWithFTPURL(NULL, (__bridge CFURLRef)resourceUrl);
    CFReadStreamSetProperty(readStream, kCFStreamPropertyFTPFetchResourceInfo, kCFBooleanTrue);

    if(CFReadStreamSetProperty(readStream ,  kCFStreamPropertyFTPUserName,  (__bridge CFTypeRef)username)&&
       CFReadStreamSetProperty(readStream ,  kCFStreamPropertyFTPPassword,  (__bridge CFTypeRef)password)){
        CFStreamClientContext clientContext;
        clientContext.version = 0;
        clientContext.info = CFBridgingRetain(self) ;
        clientContext.retain = nil;
        clientContext.release = nil;
        clientContext.copyDescription = nil;
    
        //初始化测速变量
       self.start = [[NSDate date] timeIntervalSince1970]*1000;
       self.preTime = self.start;
       self.nowLength = 0;
       self.preLength = 0;
       self.speedMax = 0;
       self.speedAver = 0;
       self.totalTime = 0;
       self.totalSize = 0;
    
        if (CFReadStreamSetClient (readStream,
                                   kCFStreamEventOpenCompleted |
                                   kCFStreamEventHasBytesAvailable |
                                   kCFStreamEventCanAcceptBytes |
                                   kCFStreamEventErrorOccurred |
                                   kCFStreamEventEndEncountered,
                                   myGetSocketReadCallBack,
                                   &clientContext ) ){
            NSLog(@"Set read callBack Succeeded");
            CFReadStreamScheduleWithRunLoop(readStream,
                                            CFRunLoopGetCurrent(),
                                            kCFRunLoopCommonModes);
        } else {
            NSLog(@"Set read callBack Failed");
            [plugin faileWithMessage:@[[NSNumber numberWithInteger:DOWNLOAD],[NSNumber numberWithInteger:TESTING_ERROR],@"FTP下载测速回调设置失败"]];
        }
        
        BOOL success = CFReadStreamOpen(readStream);
        if (!success) {
            printf("stream open fail\n");
            [plugin faileWithMessage:@[[NSNumber numberWithInteger:DOWNLOAD],[NSNumber numberWithInteger:TESTING_ERROR],@"FTP下载测速流获取失败"]];
            return;
        }
    }else{
        [plugin faileWithMessage:@[@"FTP用户名或密码错误!"]];
    }
}

-(void)stop{
    if(self.fileStream!=nil){
        CFWriteStreamClose(self.fileStream);
        CFRelease(self.fileStream);
    }
    self.fileStream = nil;

    if(readStream!=nil){
        CFReadStreamUnscheduleFromRunLoop(readStream, CFRunLoopGetCurrent(), kCFRunLoopCommonModes);
        CFReadStreamClose(readStream);
        CFRelease(readStream);
    }
    readStream = nil;
    
}

-(void)removeFTPSpeedFile{
    //关闭测速功能,删除本地文件
    NSFileManager *fileManager = [NSFileManager defaultManager];
    if ([fileManager fileExistsAtPath:DOWNLOADPATH]){
         NSLog(@"删除本地文件1");
        NSString *path = [DOWNLOADPATH stringByAppendingPathComponent:self.directoryStr];
        [fileManager removeItemAtPath:path error:nil];
        NSLog(@"删除本地文件2");
    }
}

@end

#define BUFSIZE 32768
void myGetSocketReadCallBack (CFReadStreamRef stream, CFStreamEventType event, void *myPtr){
    JUNFTPGetRequest* request = (__bridge JUNFTPGetRequest *)myPtr;
    CFNumberRef       cfSize;
    UInt64            size;
    
    long start = [[NSDate date] timeIntervalSince1970]*1000;
    long totalSize = 0;
    NSString *msg = @"";
    NSLog(@"-------------------------------stream=%@", stream);
    switch(event){
        case kCFStreamEventOpenCompleted:
            cfSize = CFReadStreamCopyProperty(stream, kCFStreamPropertyFTPResourceSize);
            if (cfSize) {
                if (CFNumberGetValue(cfSize, kCFNumberLongLongType, &size)) {
                    printf("File size is %llu\n", size);
                    request.bytesTotal = size;
                }
                msg = @"size is good";
                CFRelease(cfSize);
            } else {
                printf("File size is unknown.\n");
                msg = @"size is 0";
            }
        
            break;
        case kCFStreamEventHasBytesAvailable:{
            UInt8 recvBuffer[BUFSIZE];
            CFIndex bytesRead = CFReadStreamRead(stream, recvBuffer, BUFSIZE);
            //printf("bytesRead:%ld\n",bytesRead);
            if (bytesRead > 0){
                NSInteger   bytesOffset = 0;
                do{
                    CFIndex bytesWritten = CFWriteStreamWrite(request.fileStream, &recvBuffer[bytesOffset], bytesRead-bytesOffset );
                 
                    if (bytesWritten > 0) {
                        bytesOffset += bytesWritten;
                        request.bytesDownloaded +=bytesWritten;

                        request.nowLength += bytesWritten;
                        long nowTime = [[NSDate date] timeIntervalSince1970]*1000;
                        if((nowTime-request.preTime)>=request.interval){
                            if ((nowTime-request.preTime) == 0 || (nowTime-request.start) == 0 || request.bytesTotal == 0) {
                                msg = [NSString stringWithFormat:@"%@%@", msg, @"(nowTime-request.preTime) == 0 || (nowTime-request.start) == 0 || request.bytesTotal == 0)"];
                            }
                            long speed = (request.nowLength-request.preLength)*8/(nowTime-request.preTime);
                            request.speedMax = request.speedMax>speed?request.speedMax:speed;
                            request.speedAver = request.nowLength*8/(nowTime-request.start);
                            request.progressBlock((float)request.bytesDownloaded/(float)request.bytesTotal,speed,request.speedMax,request.speedAver,nowTime-start,request.nowLength);
                            
                            //NSLog(@"speed=%ld",speed);
                            //NSLog(@"speedMax=%ld",request.speedMax);
                            //NSLog(@"speedAver=%ld",request.speedAver);
                            request.preTime = nowTime;
                            request.preLength = request.nowLength;
                        }
                    }else if (bytesWritten == 0){
                        break;
                    }else{
                        request.failBlock([NSString stringWithFormat:@"%@%@", msg, @"bytesWritten < 0"]);
                        return;
                    }
                }while ((bytesRead-bytesOffset)>0);
            }else if(bytesRead == 0){
                totalSize = request.nowLength;
                request.finishedBlock([[NSDate date] timeIntervalSince1970]*1000-start,totalSize);
                [request stop];
            }else{
                request.failBlock([NSString stringWithFormat:@"%@%@", msg, @"bytesRead < 0"]);
            }
        }
            break;
        case kCFStreamEventErrorOccurred:{
            CFStreamError error = CFReadStreamGetError(stream);
            //NSLog(@"kCFStreamEventErrorOccurred-%d\n",error.error);
            [request stop];
            request.failBlock([NSString stringWithFormat:@"%@_error=%d_domin=%ld",msg,error.error, error.domain]);
        }
           break;
        case kCFStreamEventEndEncountered:
            //NSLog(@"request finished\n");
            request.finishedBlock([[NSDate date] timeIntervalSince1970]*1000-start,totalSize);
            [request stop];
            break;
        default:
            break;
    }
}

