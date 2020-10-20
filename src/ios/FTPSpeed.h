//
//  FTPSpeed.h
//  GoodJob
//
//  Created by 梁仲太 on 2018/5/25.
//

#import <Foundation/Foundation.h>
#import <Cordova/CDVPlugin.h>
#import <Cordova/CDV.h>
#import "sys/dirent.h"


//ftpType
static NSInteger const DOWNLOAD        = 0;
static NSInteger const UPLOAD          = 1;
static NSInteger const CLOSE           = 2;
static NSInteger const DELETE_FTP_FILE = 3;
static NSInteger const DELETE_SD_FILE  = 4;

//status
static NSInteger const TESTING             = 0;
static NSInteger const FINISH              = 1;
static NSInteger const BREAK_OFF           = 2;
static NSInteger const DOWNLOAD_FIRST      = 3;
static NSInteger const FTP_NO_FILE         = 4;
static NSInteger const LOGIN_FAILE         = 5;
static NSInteger const CONNECT_FAILE       = 6;
static NSInteger const PERMISSION_ERROR    = 7;
static NSInteger const PERMISSION_LESS     = 8;
static NSInteger const TESTING_ERROR       = 9;
static NSInteger const JSONARRAY_ERROR     = 10;
static NSInteger const DELETE_FTP_FILE_FAILE = 11;
static NSInteger const DELETE_FTP_FILE_SUCCESS = 12;
static NSInteger const DELETE_SD_FILE_SUCCESS = 13;

@interface FTPSpeed : CDVPlugin

-(void)coolMethod:(CDVInvokedUrlCommand *)command;

-(void)startWork;
-(void)upLoad;
-(void)downLoad;
-(void)close;
-(void)deleteFTPFile;
-(void)deleteSDFile;
-(void)successWithMessage:(NSArray *)messages;
-(void)faileWithMessage:(NSArray *)message;


@end
