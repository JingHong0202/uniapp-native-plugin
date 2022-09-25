# surprise-io

> 本插件用于APP端本地文件选择，可批量返回文件路径或弹出文件选择器UI进行自定义筛选

# Support Platform 

- Android
- iOS

# Usage

Android：

```javascript
const ioPlugin = uni.requireNativePlugin('surprise-io');

// 直接返回文件路径
ioPlugin.queryAsync(
    {
        type: "video" || "audio" || "image" || "为空或其他值则默认全部类型",
        limit: 10, // 返回数量
        page: 1, // 页数
        mime: ["image/png","image/jpeg"],// 返回指定 MIMETYPE 类型的文件,传数组可多项
        isHideThumb: false // 是否禁止返回缩略图 目前只支持 type 值为 image类型时才会生效,其他类型无效
    }
    , (res) => {
    // 返回值在此回调进行接受
	console.log(
        res.list, // 文件列表
        res.count // 当前查询参数所有文件数量
    )
})

// 弹出文件选择器UI
ioPlugin.openFileManager({
    // options 数组类型，用于创建Tab页面，可创建多个Tab
    options: [{
        // query 传参格式和 queryAsync 一样
        query: { 
            limit: 200
        }, 
        title: "全部文件" // Tab 标题
    },
    {
        query: {
            limit: 200,
            type: "image",
            mime: ["image/png"]
        }, 
        title: "图片"
    }]
}, res => {
    // 返回选择的文件
    console.log(res.length)
})
```

