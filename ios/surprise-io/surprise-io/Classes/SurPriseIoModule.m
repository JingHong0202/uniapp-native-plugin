//
//  SurPriseIo.m
//  suprise-io
//
//  Created by surprise on 2022/7/29.
//


#import "SurPriseIoModule.h"
#import <Photos/Photos.h>
#import <PhotosUI/PhotosUI.h>
//#import <MobileCoreServices/MobileCoreServices.h>
//#import <UniformTypeIdentifiers/UniformTypeIdentifiers.h>
#import "PDRCoreApp.h"
#import "PDRCoreAppManager.h"
#import "PDRCoreAppInfo.h"


@implementation SurPriseIoModule
// 插件初始化
- (instancetype)init {
    self.selected = [NSMutableArray new];
    //    PHOptions.synchronous = YES;
    //    PHOptions.resizeMode = PHImageRequestOptionsResizeModeFast;
    //                PHOptions.deliveryMode = PHImageRequestOptionsDeliveryModeFastFormat;
    //    CGSize size = CGSizeMake(200.0, 200.0);
    //    PHImageContentMode *contentMode = PHImageContentModeAspectFit;
    self.options = @{
        @"targetSize": @[@100.00,@100.00],
        @"contentMode": @(0),
        @"resize": @(0)
    };
    return self;
}

// 按 Identifiers 返回对应相册内容
UNI_EXPORT_METHOD(@selector(queryAlbumsByIdentifiers:callback:))

-(void) queryAlbumsByIdentifiers:(NSArray<NSString *> *)localIdentifiers callback:(UniModuleKeepAliveCallback)callback {
    
    if (callback == nil) {
        return;
    }
    
    [self checkPHPhotoAuth: ^ {
        
        NSMutableArray *result = [NSMutableArray new];
        PHFetchResult<PHAssetCollection *> *query = [PHAssetCollection fetchAssetCollectionsWithLocalIdentifiers:localIdentifiers options:nil];
        
        [query enumerateObjectsUsingBlock:^(PHAssetCollection * _Nonnull obj, NSUInteger idx, BOOL * _Nonnull stop) {
            
            PHFetchResult<PHAsset *> *collcetion = [PHAsset fetchAssetsInAssetCollection:obj options:nil];
            NSMutableArray *assetes = [self parseAssets:collcetion];
            [result addObject:assetes];
            
        }];
        
        callback(result, NO);
        
    }];
}


// 返回所有相册
UNI_EXPORT_METHOD(@selector(queryAllAlbum:callback:))

-(void) queryAllAlbum:(NSDictionary *)json callback:(UniModuleKeepAliveCallback)callback {
    
    if (callback == nil) {
        return;
    }
    
    [self checkPHPhotoAuth:^{
        
        //  获取所有本地所有相册（系统+用户）
        NSMutableArray *allAlbums = [NSMutableArray new];
        PHFetchOptions *options = [PHFetchOptions new];
        PHFetchResult *topLevelUserCollections = [PHAssetCollection fetchTopLevelUserCollectionsWithOptions:options];
        PHAssetCollectionSubtype subType = PHAssetCollectionSubtypeAlbumRegular;
        PHFetchResult *smartAlbumsResult = [PHAssetCollection fetchAssetCollectionsWithType:PHAssetCollectionTypeSmartAlbum
                                                                                    subtype:subType
                                                                                    options:options];
        // user
        if (json[@"type"] == nil || [json[@"type"] isEqualToString:@"user"]) {
            
            for (PHAssetCollection *album in topLevelUserCollections) {
                
                PHFetchResult<PHAsset *> *collcetion = [PHAsset fetchAssetsInAssetCollection:album options:nil];
                NSMutableDictionary *item = [NSMutableDictionary dictionaryWithDictionary:@{
                    @"title": album.localizedTitle,
                    @"count": @((int)collcetion.count),
                    @"localIdentifier": album.localIdentifier,
                }];
                NSDictionary *content;
                if (json[@"wrapCtx"] != nil) {
                    content = @{
                        @"content": [self parseAssets:collcetion]
                    };
                }
                
                [item addEntriesFromDictionary:content];
                [allAlbums addObject:item];
                
            }
        }
        
        // system
        if (json[@"type"] == nil || [json[@"type"] isEqualToString:@"system"]) {
            for (PHAssetCollection *album in smartAlbumsResult) {
                
                PHFetchResult<PHAsset *> *collcetion = [PHAsset fetchAssetsInAssetCollection:album options:nil];
                NSMutableDictionary *item = [NSMutableDictionary dictionaryWithDictionary:@{
                    @"title": album.localizedTitle,
                    @"count": @((int)collcetion.count),
                    @"localIdentifier": album.localIdentifier,
                }];
                NSDictionary *content;
                if (json[@"wrapCtx"] != nil) {
                    content = @{
                        @"content": [self parseAssets:collcetion]
                    };
                }
                
                [item addEntriesFromDictionary:content];
                [allAlbums addObject:item];
                
            }
        }
        
        callback(allAlbums, NO);
    }];
}



//- (void)photoLibraryDidChange:(PHChange *)changeInstance {
//    [[PHPhotoLibrary sharedPhotoLibrary] unregisterChangeObserver:self];
//};


// 返回所有相册资源
UNI_EXPORT_METHOD(@selector(queryAlbumAssets:callback:))

-(void) queryAlbumAssets:(NSDictionary *)json callback:(UniModuleKeepAliveCallback)callback {
    //    [[PHPhotoLibrary sharedPhotoLibrary] registerChangeObserver:self];
    //    [[PHPhotoLibrary sharedPhotoLibrary] presentLimitedLibraryPickerFromViewController: [self dc_findCurrentShowingViewController]];
    //    PHPhotoLibraryPreventAutomaticLimitedAccessAlert
    if (callback == nil) {
        return;
    }
    [self checkPHPhotoAuth:^{
        //        [self clearAssetsDirectory];
        int *type = [json[@"type"] intValue];
        // 获取指定类型的所有资源
        PHFetchResult<PHAsset*> *all = [PHAsset fetchAssetsWithMediaType:type options:nil];
        NSMutableArray *allAssets = [self parseAssets:all];
        callback(allAssets, false);
    }];
}

// 打开相册选择器
UNI_EXPORT_METHOD(@selector(openMediaSelector:callback:))

-(void) openMediaSelector:(NSDictionary *)json callback:(UniModuleKeepAliveCallback)callback {
    [self checkPHPhotoAuth:^{
        if (callback == nil) {
            return;
        }
        
        self.callback = callback;
        UIViewController  *cv = [self dc_findCurrentShowingViewController];
        if (@available(iOS 14.0, *)) {
            //   PHPickerViewController ios14 以上
            PHPickerConfiguration *config = [[PHPickerConfiguration alloc] init];
            config.selectionLimit = json[@"limit"] != nil ? [json[@"limit"] intValue]  : 1;
            config.filter = [PHPickerFilter anyFilterMatchingSubfilters:@[PHPickerFilter.imagesFilter, PHPickerFilter.videosFilter]];
            PHPickerViewController *pickerViewController = [[PHPickerViewController alloc] initWithConfiguration:config];
            pickerViewController.modalPresentationStyle = UIModalPresentationFullScreen;
            pickerViewController.delegate = self;
            [cv presentViewController:pickerViewController animated:YES completion:nil];
        } else {
            //   UIImagePickerController ios14以下
            UIImagePickerController *pickerCtr = [[UIImagePickerController alloc] init];
            pickerCtr.modalPresentationStyle = UIModalPresentationFullScreen;
            pickerCtr.mediaTypes = @[@"public.image", @"public.movie"];
            pickerCtr.allowsEditing = json[@"isEdit"] != nil;
            pickerCtr.delegate = self;
            [cv presentViewController:pickerCtr animated:YES completion:nil];
        }
    }];
}

#pragma mark -UIImagePickerController Delegate

- (void)imagePickerController:(UIImagePickerController *)picker didFinishPickingMediaWithInfo:(NSDictionary<UIImagePickerControllerInfoKey, id> *)info {
    //    UIViewController  *cv = [self dc_findCurrentShowingViewController];
    //    [cv dismissViewControllerAnimated:YES completion:nil];
    [picker dismissViewControllerAnimated:YES completion:nil];
    self.callback(@{
        @"url": [[info objectForKey:@"UIImagePickerControllerMediaType"] isEqualToString:@"public.image"]  ? [[info objectForKey:@"UIImagePickerControllerImageURL"] absoluteString]
        : [[info objectForKey:@"UIImagePickerControllerMediaURL"] absoluteString]}, NO);
}
- (void)imagePickerControllerDidCancel:(UIImagePickerController *)picker {
    [picker dismissViewControllerAnimated:YES completion:nil];
}

#pragma mark -PHPickerViewController Delegate

- (void)picker:(PHPickerViewController *)picker didFinishPicking:(NSArray<PHPickerResult *> *)results {
    NSMutableArray *callBackResults = [NSMutableArray new];
    if (results.count >= 1) {
        [results enumerateObjectsUsingBlock:^(PHPickerResult * _Nonnull obj, NSUInteger idx, BOOL * _Nonnull stop) {
            PHFetchResult<PHAsset *> *assets = [[PHAsset fetchAssetsWithBurstIdentifier:[obj assetIdentifier] options:nil] firstObject];
            PHAssetResource *resource = [[PHAssetResource assetResourcesForAsset:assets] firstObject];
            NSString *url = [[resource valueForKey:@"privateFileURL"] absoluteString];
            NSString *fileName = resource.originalFilename;
            NSNumber *fileSize = [resource valueForKey:@"fileSize"];
            NSNumber *width = [resource valueForKey:@"pixelWidth"];
            NSNumber *height = [resource valueForKey:@"pixelHeight"];
            
            [callBackResults addObject:@{
                @"url": url,
                @"fileName": fileName,
                @"fileSize": fileSize,
                @"width": width,
                @"height": height
            }];
        }];
    }
    self.callback(callBackResults, NO);
    [picker dismissViewControllerAnimated:YES completion:nil];
}

// 打开系统文件选择器
UNI_EXPORT_METHOD(@selector(openDocumentBrowser:callback:))

-(void) openDocumentBrowser:( NSDictionary *)json callback:(UniModuleKeepAliveCallback)callback {
    if (callback == nil) {
        return;
    }
    self.callback = callback;
    //    NSArray *documentTypes = @[];
    //    [documentTypes arrayByAddingObjectsFromArray:];
    UIDocumentBrowserViewController *documentBrowserViewController;
    if (@available(iOS 14.0, *) && [json[@"notUTT"] boolValue] == nil) {
        documentBrowserViewController = [[UIDocumentBrowserViewController alloc] initForOpeningContentTypes:json[@"types"]];
    } else if (@available(iOS 11.0, *)) {
        documentBrowserViewController = [[UIDocumentBrowserViewController alloc]  initForOpeningFilesWithContentTypes:json[@"types"]];
    } else {
        self.callback(@{
            @"msg": @"该api不支持 iOS11 以下的版本"
        }, NO);
    }
    
    UIViewController  *cv = [self dc_findCurrentShowingViewController];
    documentBrowserViewController.delegate = self;
    //    documentBrowserViewController.modalPresentationStyle = UIModalPresentationFormSheet;
    
    UIBarButtonItem *bar = [[UIBarButtonItem alloc] init];
    bar.title = @"取消";
    bar.target = self;
    bar.action = @selector(back);
    documentBrowserViewController.allowsDocumentCreation = NO;
    documentBrowserViewController.additionalTrailingNavigationBarButtonItems = @[bar];
    if (@available(iOS 13.0, *)) {
        documentBrowserViewController.shouldShowFileExtensions = YES;
    }
    [cv presentViewController:documentBrowserViewController animated:YES completion:^ {
        documentBrowserViewController.allowsPickingMultipleItems = [json[@"multiple"] boolValue] != nil;
    }];
}

// 打开系统文件选择器
UNI_EXPORT_METHOD(@selector(openDocumentPicker:callback:))

-(void) openDocumentPicker:(NSDictionary *)json callback:(UniModuleKeepAliveCallback)callback {
    if (callback == nil) {
        return;
    }
    self.callback = callback;
    
    UIDocumentPickerViewController *documentPickerViewController;
    //    if (@available(iOS 14.0, *) && [json[@"notUTT"] boolValue] == nil) {
    //        documentPickerViewController = [[UIDocumentPickerViewController alloc] initForOpeningContentTypes:json[@"types"]];
    //    }
    
    if (@available(iOS 8.0, *)) {
        documentPickerViewController = [[UIDocumentPickerViewController alloc]  initWithDocumentTypes:json[@"types"] inMode:UIDocumentPickerModeOpen];
    } else {
        self.callback(@{
            @"msg": @"该api不支持 iOS8 以下的版本"
        }, NO);
    }
    
    UIViewController  *cv = [self dc_findCurrentShowingViewController];
    documentPickerViewController.delegate = self;
    documentPickerViewController.modalPresentationStyle = UIModalPresentationFormSheet;
    if (@available(iOS 13.0, *)) {
        documentPickerViewController.shouldShowFileExtensions = YES;
    }
    if (@available(iOS 11.0,*)) {
        documentPickerViewController.allowsMultipleSelection = [json[@"multiple"] boolValue] != nil;
    }
    [cv presentViewController:documentPickerViewController animated:YES completion:nil];
}



#pragma mark - DocumentPicker Delegate

- (void)documentPicker:(UIDocumentPickerViewController *)controller didPickDocumentsAtURLs:(NSArray <NSURL *>*)documentURLs {
    NSMutableArray *result = [NSMutableArray new];
    [documentURLs enumerateObjectsUsingBlock:^(NSURL * _Nonnull obj, NSUInteger idx, BOOL * _Nonnull stop) {
        NSFileManager *file = [NSFileManager defaultManager];
        if ([file fileExistsAtPath:obj.path] == nil) {
            [obj startAccessingSecurityScopedResource];
        }
        NSDictionary *fileAttributes = [file fileAttributesAtPath:[obj path] traverseLink:YES];
        [result addObject:@{
            @"fileSize": [fileAttributes objectForKey:NSFileSize],
            @"url": obj.absoluteString
        }];
        [self.selected  addObject:obj];
        NSSet *set = [NSSet setWithArray:self.selected];
        self.selected = [NSMutableArray new];
        [self.selected addObjectsFromArray:[set allObjects]];
    }];
    self.callback(result, NO);
}
- (void)documentPicker:(UIDocumentPickerViewController *)controller didPickDocumentAtURL:(NSURL *)url {
    NSDictionary *result;
    NSFileManager *file = [NSFileManager defaultManager];
    if ([file fileExistsAtPath:url] == nil) {
        [url startAccessingSecurityScopedResource];
    }
    NSDictionary *fileAttributes = [file fileAttributesAtPath:[url path] traverseLink:YES];
    result = @{
        @"fileSize": [fileAttributes objectForKey:NSFileSize],
        @"url": url.absoluteString
    };
    [self.selected  addObject:url];
    NSSet *set = [NSSet setWithArray:self.selected];
    self.selected = [NSMutableArray new];
    [self.selected addObjectsFromArray:[set allObjects]];
    self.callback(result, NO);
}
- (void)documentPickerWasCancelled:(UIDocumentPickerViewController *)controller {
    self.callback(@{
        @"message": @"cancel"
    },  NO);
}


#pragma mark - DocumentBrowser Delegate

- (void)documentBrowser:(UIDocumentBrowserViewController *)controller didPickDocumentsAtURLs:(NSArray <NSURL *> *)documentURLs {
    NSMutableArray *result = [NSMutableArray new];
    [documentURLs enumerateObjectsUsingBlock:^(NSURL * _Nonnull obj, NSUInteger idx, BOOL * _Nonnull stop) {
        NSFileManager *file = [NSFileManager defaultManager];
        if ([file fileExistsAtPath:obj.path] == nil) {
            [obj startAccessingSecurityScopedResource];
        }
        NSDictionary *fileAttributes = [file fileAttributesAtPath:[obj path] traverseLink:YES];
        [result addObject:@{
            @"fileSize": [fileAttributes objectForKey:NSFileSize],
            @"url": obj.absoluteString
        }];
        [self.selected  addObject:obj];
        NSSet *set = [NSSet setWithArray:self.selected];
        self.selected = [NSMutableArray new];
        [self.selected addObjectsFromArray:[set allObjects]];
    }];
    self.callback(result, NO);
    UIViewController  *cv = [self dc_findCurrentShowingViewController];
    [cv dismissViewControllerAnimated:YES completion:nil];
}


#pragma mark - 获得当前活动窗口的根视图

- (UIViewController *)dc_findCurrentShowingViewController {
    UIViewController *vc = [UIApplication sharedApplication].keyWindow.rootViewController;
    UIViewController *currentShowingVC = [self findCurrentShowingViewControllerFrom:vc];
    return currentShowingVC;
}

- (UIViewController *)findCurrentShowingViewControllerFrom:(UIViewController *)vc {
    // 递归方法 Recursive method
    UIViewController *currentShowingVC;
    if ([vc presentedViewController]) {
        // 当前视图是被presented出来的
        UIViewController *nextRootVC = [vc presentedViewController];
        currentShowingVC = [self findCurrentShowingViewControllerFrom:nextRootVC];
        
    } else if ([vc isKindOfClass:[UITabBarController class]]) {
        // 根视图为UITabBarController
        UIViewController *nextRootVC = [(UITabBarController *)vc selectedViewController];
        currentShowingVC = [self findCurrentShowingViewControllerFrom:nextRootVC];
        
    } else if ([vc isKindOfClass:[UINavigationController class]]){
        // 根视图为UINavigationController
        UIViewController *nextRootVC = [(UINavigationController *)vc visibleViewController];
        currentShowingVC = [self findCurrentShowingViewControllerFrom:nextRootVC];
        
    } else {
        // 根视图为非导航类
        currentShowingVC = vc;
    }
    
    return currentShowingVC;
}

#pragma mark - Common

// 设置 options
UNI_EXPORT_METHOD_SYNC(@selector(setSettings:))
- (void) setSettings:(NSDictionary *) json {
    self.options = json;
    return;
}

- (NSMutableArray<NSDictionary*>*) parseAssets:(PHFetchResult<PHAsset*>*) all {
    // 获取路径信息
    PDRCoreAppInfo *appinfo = [PDRCore Instance].appManager.getMainAppInfo;
    // 创建文件管理类&检查是否有assets目录，没有则创建
    NSFileManager *fileManager = [NSFileManager defaultManager];
    // NSString *caches = NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, YES)[0];
    NSString *assetsDir = [appinfo.documentPath stringByAppendingString:@"/assets/"];
    if (![fileManager fileExistsAtPath:assetsDir]) {
        if (![fileManager createDirectoryAtPath:assetsDir withIntermediateDirectories:YES attributes:nil error:nil]) {
            return @{
                @"message": @"临时文件存储目录创建失败"
            };
        }
    }
    
    NSMutableArray *allAssets = [NSMutableArray new];
    for (PHAsset *sub in all) {
        PHAssetResource *resource = [[PHAssetResource assetResourcesForAsset:sub] firstObject];
        NSURL *originUrl = [resource valueForKey:@"privateFileURL"];
        NSString *fileName = resource.originalFilename;
        NSNumber *fileSize = [resource valueForKey:@"fileSize"];
        NSNumber *width = [resource valueForKey:@"pixelWidth"];
        NSNumber *height = [resource valueForKey:@"pixelHeight"];
        NSString *localIdentifier = resource.assetLocalIdentifier;
        NSString __block *url = [assetsDir stringByAppendingString:[localIdentifier stringByReplacingOccurrencesOfString:@"/" withString:@"-"]];
        NSMutableDictionary __block *exteral = [NSMutableDictionary new];
        if (![fileManager fileExistsAtPath:url]) {
            
            if (resource.type == 1) {
                PHImageRequestOptions *PHOptions = [[PHImageRequestOptions alloc] init];
                PHOptions.synchronous = YES;
                PHOptions.resizeMode = PHImageRequestOptionsResizeModeFast;
                //                PHOptions.deliveryMode = PHImageRequestOptionsDeliveryModeFastFormat;
                CGSize size = CGSizeMake([[self.options[@"targetSize"] firstObject] doubleValue], [[self.options[@"targetSize"] lastObject] doubleValue]);
                PHImageContentMode contentMode = [self.options[@"contentMode"] intValue];
                
                //                if (![resource.uniformTypeIdentifier isEqualToString:@"public.jpeg"] && ![resource.uniformTypeIdentifier isEqualToString:@"public.png"]) {
                [[PHImageManager defaultManager] requestImageForAsset:sub targetSize:size contentMode:contentMode options:PHOptions resultHandler:^(UIImage * _Nullable imageData, NSDictionary * _Nullable info) {
                    //                    UIImage *imageUI = [[UIImage new] initWithData:imageData];
                    NSData *toJPEG = UIImageJPEGRepresentation(imageData, 1);
                    if (![fileManager createFileAtPath:url contents:toJPEG attributes:nil]) {
                        url = @"";
                    }
                }];
                
                
                //                } else {
                //   NSError *error;
                //                    if (![fileManager copyItemAtPath:originUrl toPath:url error:nil]) {
                //                        url = @"";
                //                    }
                
                //                }
                
            } else if (resource.type == 2) {
                NSDictionary *videoInfo = [self getVideoThumbnail:originUrl];
                if (![fileManager createFileAtPath:url contents:videoInfo[@"data"] attributes:nil]) {
                    url = @"";
                } else {
                    [exteral addEntriesFromDictionary:@{
                        @"duration": videoInfo[@"duration"]
                    }];
                }
            }
            
        }
        [exteral addEntriesFromDictionary:@{
            @"originUrl": [originUrl absoluteString],
            @"url": url,
            @"fileName": fileName,
            @"fileSize": fileSize,
            @"width": width,
            @"height": height,
            @"localIdentifier": localIdentifier
        }];
        [allAssets addObject:exteral];
    }
    
    
    
    return allAssets;
}

// 获取视频封面
UNI_EXPORT_METHOD(@selector(getVideoThumbnail:callback:))

- (void) getVideoThumbnail:(NSDictionary *) json callback:(UniModuleKeepAliveCallback)callback {
    if (callback == nil) {
        return;
    }
    if (json == nil || json[@"path"] == nil || json[@"localIdentifier"] == nil) {
        callback(@{
            @"msg": @"缺少必要参数"
        },NO);
        return;
    }
    AVURLAsset *videoAsset = [[AVURLAsset alloc] initWithURL:json[@"path"] options:nil];
    AVAssetImageGenerator *assetGen = [[AVAssetImageGenerator alloc] initWithAsset:videoAsset];
    assetGen.appliesPreferredTrackTransform = YES;
    CMTime time = CMTimeMakeWithSeconds(0.0, 600);
    NSError*error =nil;
    CMTime actualTime;
    CGImageRef image = [assetGen copyCGImageAtTime:time actualTime:&actualTime error:&error];
    UIImage *videoImage = [[UIImage alloc] initWithCGImage:image];
    NSData *toJPEG = UIImageJPEGRepresentation(videoImage, 0);
    
    // 获取路径信息
    PDRCoreAppInfo *appinfo = [PDRCore Instance].appManager.getMainAppInfo;
    // 创建文件管理类&检查是否有assets目录，没有则创建
    NSFileManager *fileManager = [NSFileManager defaultManager];
    // NSString *caches = NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, YES)[0];
    NSString *assetsDir = [appinfo.documentPath stringByAppendingString:@"/assets/"];
    if (![fileManager fileExistsAtPath:assetsDir]) {
        if (![fileManager createDirectoryAtPath:assetsDir withIntermediateDirectories:YES attributes:nil error:nil]) {
            callback(@{
                @"message": @"临时文件存储目录创建失败"
            }, NO);
            return;
        }
    }
    NSString *savePath = [assetsDir stringByAppendingString:[json[@"localIdentifier"] stringByReplacingOccurrencesOfString:@"/" withString:@"-"]];
    if ([fileManager createFileAtPath:savePath contents:toJPEG attributes:nil]) {
        callback(@{
            @"url": savePath
        },NO);
    } else {
        callback(@{
            @"msg": @"save fail"
        },NO);
    }
}

- (NSDictionary *) getVideoThumbnail:(NSString *) path {
    AVURLAsset *videoAsset = [[AVURLAsset alloc] initWithURL:path options:nil];
    AVAssetImageGenerator *assetGen = [[AVAssetImageGenerator alloc] initWithAsset:videoAsset];
    assetGen.appliesPreferredTrackTransform = YES;
    CMTime time = CMTimeMakeWithSeconds(0.0, 600);
    NSError*error =nil;
    CMTime actualTime;
    CGImageRef image = [assetGen copyCGImageAtTime:time actualTime:&actualTime error:&error];
    UIImage *videoImage = [[UIImage alloc] initWithCGImage:image];
    NSData *toJPEG = UIImageJPEGRepresentation(videoImage, 0);
    return  @{
        @"data": toJPEG,
        @"duration": @(videoAsset.duration.value/videoAsset.duration.timescale)
    };
}

// 主动调停止授权方法
UNI_EXPORT_METHOD_SYNC(@selector(stopAccessingSecurityScopedResource))

- (void) stopAccessingSecurityScopedResource {
    if ([self.selected count] == 0) {
        return;
    }
    
    [self.selected enumerateObjectsUsingBlock:^(NSURL * _Nonnull obj, NSUInteger idx, BOOL * _Nonnull stop) {
        [obj stopAccessingSecurityScopedResource];
    }];
    
    [self.selected removeAllObjects];
}


// 清除doc/assets目录下的所有文件
UNI_EXPORT_METHOD_SYNC(@selector(clearAssetsDirectory))

- (void) clearAssetsDirectory {
    PDRCoreAppInfo *appinfo = [PDRCore Instance].appManager.getMainAppInfo;
    NSArray* assetsDir = [[NSFileManager defaultManager] contentsOfDirectoryAtPath:[appinfo.documentPath stringByAppendingString:@"/assets"] error:NULL];
    for (NSString *file in assetsDir) {
        [[NSFileManager defaultManager] removeItemAtPath:[NSString stringWithFormat:@"%@%@", [appinfo.documentPath stringByAppendingString:@"/assets/"], file] error:nil];
    }
}

- (void) back {
    UIViewController  *cv = [self dc_findCurrentShowingViewController];
    [cv dismissViewControllerAnimated:YES completion:nil];
    if (self.callback != nil) {
        self.callback(@{
            @"message": @"cancel"
        },  NO);
    }
}

// 检查当前是否有读取相册的权限
- (void) checkPHPhotoAuth:(void (NS_NOESCAPE ^)())block {
    PHAuthorizationStatus status = [PHPhotoLibrary  authorizationStatus];
    switch (status) {
        case PHAuthorizationStatusRestricted:
        case PHAuthorizationStatusDenied:
        {
            [self showPermissionHelp];
        }
            break;
        case PHAuthorizationStatusAuthorized:
        case PHAuthorizationStatusLimited:
        {
            block();
        }
            break;
        default:
        {
            [PHPhotoLibrary requestAuthorization:^(PHAuthorizationStatus status) {
                [self checkPHPhotoAuth:block];
            }];
        }
    }
}



- (void) showPermissionHelp {
    UIAlertController* alert = [UIAlertController alertControllerWithTitle:@"没有该权限"
                                                                   message:@"该功能需要读取相册权限，请到APP设置里面打开"
                                                            preferredStyle:UIAlertControllerStyleAlert];
    
    UIAlertAction* defaultAction = [UIAlertAction actionWithTitle:@"去设置" style:UIAlertActionStyleDefault
                                                          handler:^(UIAlertAction * action) {
        if (@available(iOS 10.0, *)) {
            [[UIApplication sharedApplication] openURL:[NSURL URLWithString:UIApplicationOpenSettingsURLString] options:@{} completionHandler:nil];
        } else {
            [[UIApplication sharedApplication] openURL:[NSURL URLWithString:UIApplicationOpenSettingsURLString] ];
        }
    }];
    [alert addAction:defaultAction];
    UIAlertAction* cancelAction = [UIAlertAction actionWithTitle:@"知道了" style:UIAlertActionStyleCancel handler:^(UIAlertAction * action) {
        
    }];
    [alert addAction:cancelAction];
    if ([NSThread isMainThread]) {
        [[self dc_findCurrentShowingViewController] presentViewController:alert animated:YES completion:nil];
    } else {
        dispatch_sync(dispatch_get_main_queue(), ^{
            [[self dc_findCurrentShowingViewController] presentViewController:alert animated:YES completion:nil];
        });
    }
}

@end
