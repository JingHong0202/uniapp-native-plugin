# surprise-io

> 本插件用于 APP 端本地文件选择，可批量返回文件路径或弹出文件选择器 UI 进行自定义筛选

# Support Platform

- Android （5 - 12）
- iOS （11 - 16）

# Usage

Android：

```javascript
const ioPlugin = uni.requireNativePlugin('surprise-io');

// 直接返回文件路径
ioPlugin.queryAsync(
  {
    type: 'video' || 'audio' || 'image' || '为空或其他值则默认全部类型',
    limit: 10, // 返回数量
    page: 1, // 页数
    mime: ['image/png', 'image/jpeg'], // 返回指定 MIMETYPE 类型的文件,传数组可多项
    isHideThumb: false, // 是否禁止返回缩略图 目前只支持 type 值为 image类型时才会生效,其他类型无效
  },
  res => {
    // 返回值在此回调进行接受
    console.log(
      res.list, // 文件列表
      res.count // 当前查询参数所有文件数量
    );
  }
);

// 弹出文件选择器UI
ioPlugin.openFileManager(
  {
    // options 数组类型，用于创建Tab页面，可创建多个Tab
    options: [
      {
        // query 传参格式和 queryAsync 一样
        query: {
          limit: 200,
        },
        title: '全部文件', // Tab 标题
      },
      {
        query: {
          limit: 200,
          type: 'image',
          mime: ['image/png'],
        },
        title: '图片',
      },
    ],
  },
  res => {
    // 返回选择的文件
    console.log(res.length);
  }
);
```

iOS:

```javascript
const ioPlugin = uni.requireNativePlugin('surprise-io');

// 按传入的 Identifiers 返回对应的相册内容
ioPlugin.queryAlbumsByIdentifiers(
  [
    // ... Identifiers
  ],
  res => {}
);

// 获取手机相册（系统相册、用户相册）
ioPlugin.queryAllAlbum(
  {
    type: 'user' || 'system' || '空则默认全部',
  },
  res => {}
);

// 返回指定的所有相册资源
/**
 *  type:
    *  typedef NS_ENUM(NSInteger, PHAssetMediaType) {
            PHAssetMediaTypeUnknown = 0,
            PHAssetMediaTypeImage   = 1,
            PHAssetMediaTypeVideo   = 2,
            PHAssetMediaTypeAudio   = 3,
        };
 */
ioPlugin.queryAlbumAssets(
  {
    type: 1,
  },
  res => {}
);

// 弹出系统自带的相册选择器
ioPlugin.openMediaSelector({
    isEdit: true, // 是否可以编辑 ios14 以下支持
    limit: 10 //  选择最大值 ios14 以上支持
},res => {})

// 弹出系统文件选择器
/**
 *  types 参考链接 https://developer.apple.com/library/archive/documentation/Miscellaneous/Reference/UTIRef/Articles/System-DeclaredUniformTypeIdentifiers.html#//apple_ref/doc/uid/TP40009259-SW1/'
 */
ioPlugin.openDocumentPicker({
    multiple: true, // 是否可以多选
    types: ["public.text"] // 可以选择的文件类型
},res => {})

// 获取视频封面
ioPlugin.getVideoThumbnail({
    localIdentifier: '' ,// 文件标识
    path: "" // 视频地址
},res => {})

// 设置缩略图生成配置 (会影响到缩略图画质、大小)
/**
 *  contentMode:
    *  typedef NS_ENUM(NSInteger, PHImageContentMode) {
            PHImageContentModeAspectFit = 0,
            PHImageContentModeAspectFill = 1,
            PHImageContentModeDefault = PHImageContentModeAspectFit
        };
 */
ioPlugin.setSettings({
    targetSize: [
        100, // width
        100 // height
    ],
    contentMode: 0
})

// 清空缩略图缓存
ioPlugin.clearAssetsDirectory()
// 对选择过的文件进行停止授权
ioPlugin.stopAccessingSecurityScopedResource()
```
