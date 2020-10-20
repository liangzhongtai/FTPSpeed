//
//  JUNFTPPutRequest.h
//  JUNFTPDemo
//
//  Created by 王 莉君 on 14-7-10.
//  Copyright (c) 2014年 王 莉君. All rights reserved.
//

#import <Foundation/Foundation.h>
#import "FTPSpeed.h"

typedef void (^FTPPutFinishedBlock)(long totalTime,long totalSize);
typedef void (^FTPPutFailBlock)(void);
typedef void (^FTPPutProgressBlock)(float progress,long speed,long speedMax,long speedAver,long totalTime,long totalSize);

@interface JUNFTPPutRequest : NSObject

@property (nonatomic, strong) FTPPutFinishedBlock finishedBlock;
@property (nonatomic, strong) FTPPutFailBlock failBlock;
@property (nonatomic, strong) FTPPutProgressBlock progressBlock;

@property (nonatomic, assign) unsigned long long bytesTotal;
@property (nonatomic, assign) unsigned long long bytesUploaded;
@property (nonatomic, assign) CFReadStreamRef fileStream;
@property (nonatomic, assign) CFWriteStreamRef myWriteStream;
@property (nonatomic,strong) NSString *ftpPath;
@property (nonatomic,assign) NSInteger interval;

@property (nonatomic,assign) long start;// = [[NSDate date] timeIntervalSince1970]*1000;
@property (nonatomic,assign) long preTime;// = start;
@property (nonatomic,assign) long nowLength;// = 0;
@property (nonatomic,assign) long preLength;// = 0;
@property (nonatomic,assign) long speedMax;// = 0;
@property (nonatomic,assign) long speedAver;// = 0;
@property (nonatomic,assign) long totalTime;// = 0;
@property (nonatomic,assign) long totalSize;// = 0;

+(JUNFTPPutRequest*)requestWithResource:(NSString*)resource
                            toDirectory:(NSURL*)url;

+(JUNFTPPutRequest *)requestWithResource:(NSString*)resource
                             toDirectory:(NSURL*)url
                           finishedBlock:(FTPPutFinishedBlock)finishedBlock
                               failBlock:(FTPPutFailBlock)failBlock
                           progressBlock:(FTPPutProgressBlock)progressBlock;
+(BOOL)checkFTPSpeedFile:(NSString *)fileName;

-(void)start:(FTPSpeed *)plugin andUN:(NSString *)username andPW:(NSString *)password andFTPPath:(NSString *)ftpPath;
-(void)stop;
-(void)removeFTPSpeedFile;
@end
