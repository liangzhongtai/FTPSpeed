//
//  JUNFTPDeleteRequest.h
//  JUNFTPDemo
//
//  Created by 王 莉君 on 14-7-10.
//  Copyright (c) 2014年 王 莉君. All rights reserved.
//

#import <Foundation/Foundation.h>

typedef void (^FTPDeleteFinishedBlock)(void);
typedef void (^FTPDeleteFailBlock)(void);

@interface JUNFTPDeleteRequest : NSObject

@property (nonatomic, strong) FTPDeleteFinishedBlock finishedBlock;
@property (nonatomic, strong) FTPDeleteFailBlock failBlock;

+(JUNFTPDeleteRequest*)requestWithDirectory:(NSString*)directory;

+(JUNFTPDeleteRequest *)requestWithDirectory:(NSString*)directory
                                      finishedBlock:(FTPDeleteFinishedBlock)finishedBlock
                                          failBlock:(FTPDeleteFailBlock)failBlock;

-(void)start;

@end
