//
//  JUNFTPGetRequest.h
//  JUNFTPDemo
//
//  Created by 王 莉君 on 14-7-10.
//  Copyright (c) 2014年 王 莉君. All rights reserved.
//

#import <Foundation/Foundation.h>
#import "FTPSpeed.h"

typedef void (^FTPGetFinishedBlock)(void);
typedef void (^FTPGetFailBlock)(void);
typedef void (^FTPGetProgressBlock)(float progress,long speed,long speedMax,long speedAver);

@interface JUNFTPGetRequest : NSObject

@property (nonatomic, strong) FTPGetFinishedBlock finishedBlock;
@property (nonatomic, strong) FTPGetFailBlock failBlock;
@property (nonatomic, strong) FTPGetProgressBlock progressBlock;
@property (nonatomic, assign) unsigned long long bytesTotal;
@property (nonatomic, assign) unsigned long long bytesDownloaded;
@property (nonatomic, assign) CFWriteStreamRef fileStream;
@property (nonatomic,strong) NSString *ftpFileName;
@property (nonatomic,assign) NSInteger interval;
@property (nonatomic,strong) NSString *directoryStr;

@property (nonatomic,assign) long start;// = [[NSDate date] timeIntervalSince1970]*1000;
@property (nonatomic,assign) long preTime;// = start;
@property (nonatomic,assign) long nowLength;// = 0;
@property (nonatomic,assign) long preLength;// = 0;
@property (nonatomic,assign) long speedMax;// = 0;
@property (nonatomic,assign) long speedAver;// = 0;

+(JUNFTPGetRequest*)requestWithResource:(NSURL*)url
                            toDirectory:(NSString*)directory;

+(JUNFTPGetRequest *)requestWithResource:(NSURL*)url
                             toDirectory:(NSString*)directory
                           finishedBlock:(FTPGetFinishedBlock)finishedBlock
                               failBlock:(FTPGetFailBlock)failBlock
                           progressBlock:(FTPGetProgressBlock)progressBlock;

-(void)start:(FTPSpeed *)plugin andUN:(NSString *)username andPW:(NSString *)password;

-(void)stop;
-(void)removeFTPSpeedFile;
@end
