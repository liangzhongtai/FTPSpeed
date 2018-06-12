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

@interface FTPSpeed()<NSStreamDelegate>

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

@property(nonatomic,assign)BOOL uploadStart;
@property(nonatomic,assign)BOOL downloadStart;

@end

@implementation FTPSpeed

-(void)coolMethod:(CDVInvokedUrlCommand *)command{
    //NSLog(@"ftpspeed_cool");
    self.callbackId = command.callbackId;
    self.ftpType = [command.arguments[0] integerValue];
    self.interval = 500;
    //NSLog(@"arguments.count=%ld",command.arguments.count);
    if(command.arguments.count>1){
        self.interval = [command.arguments[1] integerValue];
    }
    if(command.arguments.count>2){
        self.ftpIP    = command.arguments[2];
        self.ftpPort = [command.arguments[3] integerValue];
        self.ftpUN  = command.arguments[4];
        self.ftpPW  = command.arguments[5];
        self.ftpFileName = command.arguments[6];
        self.sdFileName  = command.arguments[7];
    }
    //NSLog(@"ftpType=%ld",self.ftpType);
    //NSLog(@"self.ftpFileName=%@",self.ftpFileName);
    //NSLog(@"self.sdFileName=%@",self.sdFileName);
    [self startWork];
}

-(void)startWork{
    if(self.ftpType == UPLOAD){
        //NSLog(@"ftp_上传");
        [self upLoad];
    }else if(self.ftpType == DOWNLOAD){
        //NSLog(@"ftp_下载");
        self.ftpOriFileName = self.ftpFileName;
        [self downLoad];
    }else if(self.ftpType == CLOSE){
        //NSLog(@"ftp_暂停");
        [self close];
    }else if(self.ftpType == DELETE_FTP_FILE){
        //NSLog(@"ftp_删除");
        [self deleteFTPFile];
    }else if(self.ftpType == DELETE_SD_FILE){
        [self deleteSDFile];
    }
}


-(void)upLoad{
    
    if(![JUNFTPPutRequest checkFTPSpeedFile:self.sdFileName]){
        //NSLog(@"请先下载FTP测试文件");
        [self faileWithMessage:@[[NSNumber numberWithInteger:self.ftpType],[NSNumber numberWithInteger:FTP_NO_FILE],@"请先下载FTP测试文件"]];
    }else if(self.uploadStart){
        //NSLog(@"正在上传测速");
        [self faileWithMessage:@[[NSNumber numberWithInteger:self.ftpType],[NSNumber numberWithInteger:TESTING],@"正在上传测速"]];
    }else if(self.putRequest == nil){
        self.uploadStart = YES;
         NSString *url = [NSString stringWithFormat:@"ftp://%@:%@@%@/%@",self.ftpUN,self.ftpPW,self.ftpIP,self.sdFileName];
        self.putRequest = [JUNFTPPutRequest requestWithResource:self.sdFileName toDirectory:[NSURL URLWithString:url] finishedBlock:^{
            //NSLog(@"get finish\n");
            [self faileWithMessage:@[[NSNumber numberWithInteger:self.ftpType],[NSNumber numberWithInteger:FINISH],@"FTP上传测试结束"]];
            self.uploadStart = NO;
            
        }failBlock:^{
            //NSLog(@"get fail\n");
            [self faileWithMessage:@[[NSNumber numberWithInteger:self.ftpType],[NSNumber numberWithInteger:TESTING_ERROR],@"FTP上传测试异常"]];
            self.uploadStart = NO;
            
        }progressBlock:^(float progress,long speed,long speedMax,long speedAver){
            //NSLog(@"get ---%f\n",progress);
            [self successWithMessage:@[[NSNumber numberWithInteger:UPLOAD],[NSNumber numberWithInteger:TESTING],[NSNumber numberWithLong:speed]
                                       ,[NSNumber numberWithLong:speedMax],[NSNumber numberWithLong:speedAver]]];
        }];
        self.putRequest.interval = self.interval;
        [self.putRequest start:self andUN:self.ftpUN andPW:self.ftpPW andFTPPath:url];
    }
}

-(void)downLoad{
    if(self.downloadStart){
        //NSLog(@"正在测速下载");
        [self faileWithMessage:@[[NSNumber numberWithInteger:self.ftpType],[NSNumber numberWithInteger:TESTING],@"正在下载测速"]];
    }else if(self.getRequest == nil){
        self.downloadStart = YES;
        NSString *url = [NSString stringWithFormat:@"ftp://%@:%@@%@/%@",self.ftpUN,self.ftpPW,self.ftpIP,self.ftpFileName];
        self.getRequest = [JUNFTPGetRequest requestWithResource:[NSURL URLWithString:url] toDirectory:self.sdFileName finishedBlock:^{
            //NSLog(@"get finish\n");
            self.downloadStart = NO;
            [self faileWithMessage:@[[NSNumber numberWithInteger:self.ftpType],[NSNumber numberWithInteger:FINISH],@"FTP下载测试结束"]];
        }failBlock:^{
            //NSLog(@"get fail\n");
            self.downloadStart = NO;
            [self faileWithMessage:@[[NSNumber numberWithInteger:self.ftpType],[NSNumber numberWithInteger:TESTING_ERROR],@"FTP下载测试异常"]];
        }progressBlock:^(float progress,long speed,long speedMax,long speedAver){
            //NSLog(@"get ---%f\n",progress);
            [self successWithMessage:@[[NSNumber numberWithInteger:DOWNLOAD],[NSNumber numberWithInteger:TESTING],[NSNumber numberWithLong:speed],[NSNumber numberWithLong:speedMax],[NSNumber numberWithLong:speedAver],[NSNumber numberWithFloat:progress]]];
        }];
        self.getRequest.interval = self.interval;
        [self.getRequest start:self andUN:self.ftpUN andPW:self.ftpPW];
    }
}

-(void)close{
    if(self.putRequest!=nil){
        [self.putRequest stop];
        self.putRequest = nil;
    }
    if(self.getRequest!=nil){
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
    [self successWithMessage:@[[NSNumber numberWithInteger:self.ftpType],[NSNumber numberWithInteger:BREAK_OFF],[NSNumber numberWithLong:0],[NSNumber numberWithLong:0],[NSNumber numberWithLong:0],[NSNumber numberWithFloat:0.0]]];
}

-(void)deleteFTPFile{
    if(self.ftpFileName == nil||[self.ftpFileName isEqualToString:self.ftpOriFileName]){
        if(self.ftpType!=CLOSE)
        [self faileWithMessage:@[[NSNumber numberWithInteger:DELETE_FTP_FILE],[NSNumber numberWithInteger:DELETE_FTP_FILE_FAILE],@"FTP上传测试文件名有误"]];
        return;
    }
    NSString *url = [NSString stringWithFormat:@"ftp://%@:%@@%@/%@",self.ftpUN,self.ftpPW,self.ftpIP,self.ftpFileName];
    self.deleteRequest = [JUNFTPDeleteRequest requestWithDirectory:url finishedBlock:^{
        if(self.ftpType!=CLOSE)
        [self successWithMessage:@[[NSNumber numberWithInteger:DELETE_FTP_FILE],[NSNumber numberWithInteger:DELETE_FTP_FILE_SUCCESS],[NSNumber numberWithLong:0],[NSNumber numberWithLong:0],[NSNumber numberWithLong:0],[NSNumber numberWithFloat:0.0]]];
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
    [self successWithMessage:@[[NSNumber numberWithInteger:DELETE_SD_FILE],[NSNumber numberWithInteger:DELETE_SD_FILE_SUCCESS],[NSNumber numberWithLong:0],[NSNumber numberWithLong:0],[NSNumber numberWithLong:0],[NSNumber numberWithFloat:0.0]]];
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
