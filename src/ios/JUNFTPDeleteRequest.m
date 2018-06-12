//
//  JUNFTPDeleteRequest.m
//  JUNFTPDemo
//
//  Created by 王 莉君 on 14-7-10.
//  Copyright (c) 2014年 王 莉君. All rights reserved.
//

#import "JUNFTPDeleteRequest.h"

@interface JUNFTPDeleteRequest (){
    NSString *directoryStr;
    
}

@end

@implementation JUNFTPDeleteRequest

+(JUNFTPDeleteRequest*)requestWithDirectory:(NSString*)directory{
    return [[self alloc] initWithDirectory:directory finishedBlock:nil failBlock:nil];
}

+(JUNFTPDeleteRequest *)requestWithDirectory:(NSString*)directory
                               finishedBlock:(FTPDeleteFinishedBlock)finishedBlock
                                   failBlock:(FTPDeleteFailBlock)failBlock{
    return [[self alloc] initWithDirectory:directory finishedBlock:finishedBlock failBlock:failBlock];
}

-(id)initWithDirectory:(NSString*)directory
         finishedBlock:(FTPDeleteFinishedBlock)finishedBlock
             failBlock:(FTPDeleteFailBlock)failBlock{
    
	self = [super init];
    
    if(self){
		directoryStr = directory;
        self.finishedBlock = finishedBlock;
        self.failBlock = failBlock;
        
	}
	return self;
}

-(void)start{
    SInt32 status = 0;
	NSURL* url = [NSURL URLWithString:directoryStr];
	BOOL success = CFURLDestroyResource((__bridge CFURLRef)url, &status);
	if(success) {
		self.finishedBlock();
	} else {
		self.failBlock();
	}
}

@end
