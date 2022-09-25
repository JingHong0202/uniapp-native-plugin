//
//  SurPriseIo.h
//  suprise-io
//
//  Created by surprise on 2022/7/29.
//

#import <Foundation/Foundation.h>

#import "DCUniModule.h"

NS_ASSUME_NONNULL_BEGIN

@interface SurPriseIoModule : DCUniModule

@property UniModuleKeepAliveCallback callback;
@property NSMutableArray<NSURL *>* selected;
@property NSDictionary *options;


@end

NS_ASSUME_NONNULL_END
