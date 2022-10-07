package io.surprise.file;


import static android.content.pm.PackageManager.PERMISSION_GRANTED;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import io.dcloud.common.util.BaseInfo;
import io.dcloud.feature.uniapp.annotation.UniJSMethod;
import io.dcloud.feature.uniapp.bridge.UniJSCallback;
import io.dcloud.feature.uniapp.common.UniModule;
import io.surprise.file.libs.Common;
import io.surprise.file.model.File;
import io.surprise.file.model.Media;

public class Manager extends UniModule {
    private final String TAG = "surprise-io";

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {

        if (grantResults.length > 0 ) {
            boolean status = true;
            for (int grant : grantResults) {
                if (PERMISSION_GRANTED != grant) {
                    status = false;
                    continue;
                }
            }
            if (status) {
                // 授权成功
                if (requestCode == 110) {
                    queryAsync(Common.JsonParams, Common.Callback);
                } else if (requestCode == 111) {
                    openFileManager(Common.JsonParams, Common.Callback);
                }
                Common.JsonParams = null;
                Common.Callback = null;
            } else {
                // 授权失败
                Common.showPerMissionToast(mUniSDKInstance.getContext(), "该功能需要授权相应的权限才能正常运行");
                Common.requestPermission(mUniSDKInstance.getContext(), requestCode);
            }
        }

    }

//    @Override
//    public void onActivityResult(int requestCode, int resultCode, Intent data) {
//        if (requestCode == 111 && data.hasExtra("list")) {
//            JSONObject JSONDATA = new JSONObject() {{
//                put("list", data.getStringExtra("list"));
//            }};
//            mUniSDKInstance.fireGlobalEventCallback("onResult", JSONDATA);
//        } else {
//            super.onActivityResult(requestCode, resultCode, data);
//        }
//    }




    @UniJSMethod()
    public void openFileManager(JSONObject JsonParams, UniJSCallback callback) {
        if (mUniSDKInstance != null && mUniSDKInstance.getContext() instanceof Activity && callback != null) {

            if (!Common.checkPermission(mUniSDKInstance.getContext())) {
                ActivityCompat.requestPermissions((Activity) mUniSDKInstance.getContext(), Common.getNeedsPermission(),111);
                Common.JsonParams = JsonParams;
                Common.Callback = callback;
//            Common.requestPermission(context, JsonParams, callback);
                return;
            }

            Intent intent = new Intent(mUniSDKInstance.getContext(), FileManagerActivity.class);
            if (JsonParams.containsKey("options") && JsonParams.getString("options") != null && !JsonParams.getString("options").equals("[]")) {
                intent.putExtra("options", JsonParams.getString("options"));
                FileManagerActivity.callback = callback;
                ((Activity) mUniSDKInstance.getContext()).startActivityForResult(intent, 111);
            } else {
                callback = null;
                JsonParams = null;
            }

        }
    }


    /**
     * @param JsonParams JSON格式  查询配置参数
     *                   {
     *                   type: default (全部类型) | image | audio | video, 文件类型
     *                   isHideThumb: Boolean, 禁止返回缩略图地址 （支持类型: image）
     *                   }
     * @param callback   *返回值会通过此回调返回
     * @description 本地文件查询函数 （异步）
     * @example pluginModule.queryAsync({
     *type : image
     *},
     *res = >
     *})
     */
    @UniJSMethod()
    public void queryAsync(@Nullable JSONObject JsonParams, UniJSCallback callback) {
        if (callback == null && callback instanceof UniJSCallback) return;
        Context context = mUniSDKInstance.getContext();
        if (!Common.checkPermission(context)) {
            ActivityCompat.requestPermissions((Activity) context, Common.getNeedsPermission(),110);
            Common.JsonParams = JsonParams;
            Common.Callback = callback;
//            Common.requestPermission(context, JsonParams, callback);
            return;
        }

        String type = null;
        int limit = 0, page = 0;
        JSONObject thumbOptions = new JSONObject();
        if (JsonParams instanceof JSONObject) {
            type = JsonParams.getString("type");
            limit = JsonParams.getIntValue("limit");
            page = JsonParams.getIntValue("page");
            JSONArray mimeFilter = JsonParams.getJSONArray("mime");
            thumbOptions = JsonParams.getJSONObject("thumb") == null ? new JSONObject() : JsonParams.getJSONObject("thumb");
            if (mimeFilter != null && mimeFilter.size() > 0) {
                StringBuilder mimePlaceholder = new StringBuilder();
                String[] mimeVal = new String[mimeFilter.size()];
                for (int i = 0; i < mimeFilter.size(); i++) {
                    mimePlaceholder.append(i == mimeFilter.size() - 1 ? "?" : "?,");
                    mimeVal[i] = (String) mimeFilter.get(i);
                }
                File.mimeSelection = " AND " + MediaStore.Files.FileColumns.MIME_TYPE + " IN(" + mimePlaceholder + ") ";
                File.mimeSelectionArgs = mimeVal;
            } else {
                File.mimeSelection = " AND " + MediaStore.Files.FileColumns.MIME_TYPE + " NOT NULL ";
                File.mimeSelectionArgs = null;
            }
            File.titleSelection = " AND " + (JsonParams.containsKey("name") ?
                    MediaStore.Files.FileColumns.TITLE + " LIKE '%" + JsonParams.getString("name") + "%' " : MediaStore.Files.FileColumns.TITLE + " NOT NULL");
        }
        type = type instanceof String ? type : "";
        page = page == 0 ? 1 : page;
        limit = limit == 0 ? 10 : limit;

        JSONObject results;

        switch (type.toLowerCase()) {
            case "audio":
            case "video":
                results = queryMedia(context, type, page, limit,thumbOptions);
                break;
            case "image":
                results = queryImage(context, thumbOptions, page, limit);
                break;
            default:
                results = queryFile(context, page, limit);
        }
        callback.invoke(results);
    }

//    public int getCount(@Nullable Uri uri) {
//        Cursor cursor = mUniSDKInstance.getContext().getContentResolver().query(uri, new String[]{MediaStore.Files.FileColumns._ID}, File.baseSelection + File.mimeSelection + File.titleSelection, File.mimeSelectionArgs, null);
//        int Count = cursor.getCount();
//        cursor.close();
//        return Count;
//    }

    public JSONObject queryAsync(Context context, JSONObject JsonParams) {
//        if (Common.checkPermission(context)) {
//            Common.requestPermission(context);
//            return;
//        }

        String type = null;
        int limit = 0, page = 0;
        JSONObject thumbOptions = new JSONObject();
        if (JsonParams instanceof JSONObject) {
            type = JsonParams.getString("type");
            limit = JsonParams.getIntValue("limit");
            page = JsonParams.getIntValue("page");
            JSONArray mimeFilter = JsonParams.getJSONArray("mime");
            thumbOptions = JsonParams.getJSONObject("thumb") == null ? new JSONObject() : JsonParams.getJSONObject("thumb");
            if (mimeFilter != null && mimeFilter.size() > 0) {
                StringBuilder mimePlaceholder = new StringBuilder();
                String[] mimeVal = new String[mimeFilter.size()];
                for (int i = 0; i < mimeFilter.size(); i++) {
                    mimePlaceholder.append(i == mimeFilter.size() - 1 ? "?" : "?,");
                    mimeVal[i] = (String) mimeFilter.get(i);
                }
                File.mimeSelection = " AND " + MediaStore.Files.FileColumns.MIME_TYPE + " IN(" + mimePlaceholder + ") ";
                File.mimeSelectionArgs = mimeVal;
            } else {
                File.mimeSelection = " AND " + MediaStore.Files.FileColumns.MIME_TYPE + " NOT NULL ";
                File.mimeSelectionArgs = null;
            }

            File.titleSelection = " AND " + (JsonParams.containsKey("name") ?
                    MediaStore.Files.FileColumns.TITLE + " LIKE '%" + JsonParams.getString("name") + "%' " : MediaStore.Files.FileColumns.TITLE + " NOT NULL");
        }
        type = type instanceof String ? type : "";
        page = page == 0 ? 1 : page;
        limit = limit == 0 ? 10 : limit;



        JSONObject results;

        switch (type.toLowerCase()) {
            case "audio":
            case "video":
                results = queryMedia(context, type, page, limit,thumbOptions);
                break;
            case "image":
                results = queryImage(context,thumbOptions, page, limit);
                break;
            default:
                results = queryFile(context, page, limit);
        }
        return results;
    }

    public int getCount(Context context, @Nullable Uri uri) {
        Cursor cursor = context.getContentResolver().query(uri, new String[]{MediaStore.Files.FileColumns._ID}, File.baseSelection + File.mimeSelection + File.titleSelection, File.mimeSelectionArgs, null);
        int Count = cursor.getCount();
        cursor.close();
        return Count;
    }


    // 清空缩略图缓存
    @UniJSMethod(uiThread = false)
    public void clearThumbs() {
        String savePath = BaseInfo.sBaseFsAppsPath + BaseInfo.sCurrentAppOriginalAppid + "/" + BaseInfo.REAL_PRIVATE_DOC_DIR + "thumbs/";
        java.io.File dir = new java.io.File(savePath);
        if (dir.exists() && dir.isDirectory()) {
            java.io.File[] files = dir.listFiles();
            for (java.io.File file : files) {
                //如果是文件直接删除
                if (file.isFile()){
                    file.delete();
                }
                //如果是文件夹 则当成file对象调用本方法进如该文件夹执行
//                if (file.isDirectory()){
//                    delectFile(file);
//                }
            }
        }
    }





    /**
     * @param id *文件ID
     * @param thumbOptions *缩略图配置
     * @description 根据id获取对应的缩略图
     */
    public String queryImgThumbs(Context context, int id, JSONObject thumbOptions) {
        String result = null;
        Cursor cursor = context.getContentResolver().query(MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.Images.Thumbnails.IMAGE_ID, MediaStore.Images.Thumbnails.DATA}
                , MediaStore.Images.Thumbnails.IMAGE_ID + "=" + id, null, null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                result = Common.createImgThumb(cursor.getString((int) cursor.getColumnIndex(MediaStore.Images.Thumbnails.DATA)), id, thumbOptions);
            }
            cursor.close();
        }
        return result;
    }

    /**
     * @param id       *文件ID
     * @param callback *返回值回调
     * @param thumb *缩略图配置
     * @description 根据id获取对应的缩略图, 该函数专门暴露给uni以便单独调用
     */
    @UniJSMethod()
    public void queryImgThumbs(int id, UniJSCallback callback, JSONObject thumb) {
        if (callback == null) return;
        if (thumb == null) thumb = new JSONObject();
        if (!Common.createDirectors(BaseInfo.sBaseFsAppsPath + BaseInfo.sCurrentAppOriginalAppid, BaseInfo.REAL_PRIVATE_DOC_DIR + "/thumbs")) {
            callback.invoke(new JSONObject() {{
                put("msg", "缩略图存储目录创建失败，请检查是否有对应权限");
            }});
            return;
        }

        Cursor cursor = mUniSDKInstance.getContext().getContentResolver().query(MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.Images.Thumbnails.IMAGE_ID, MediaStore.Images.Thumbnails.DATA}
                , MediaStore.Images.Thumbnails.IMAGE_ID + "=" + id, null, null);
        String result = "";

        if (cursor != null) {
            while (cursor.moveToNext()) {
                result = Common.createImgThumb(cursor.getString((int) cursor.getColumnIndex(MediaStore.Images.Thumbnails.DATA)), id, thumb);
            }
            cursor.close();
            callback.invoke(result);
        }
    }

    /**
     * @param id       *文件ID
     * @param callback *返回值回调
     * @param thumb *缩略图配置
     * @description 根据id获取对应的缩略图, 该函数专门暴露给uni以便单独调用
     */
    @UniJSMethod()
    public void queryVideoThumbs(int id, UniJSCallback callback, JSONObject thumb) {
        if (callback == null) return;
        if (thumb == null) thumb = new JSONObject();
        if (!Common.createDirectors(BaseInfo.sBaseFsAppsPath + BaseInfo.sCurrentAppOriginalAppid, BaseInfo.REAL_PRIVATE_DOC_DIR + "/thumbs")) {
            callback.invoke(new JSONObject() {{
                put("msg", "缩略图存储目录创建失败，请检查是否有对应权限");
            }});
            return;
        }

        Cursor cursor = mUniSDKInstance.getContext().getContentResolver().query(MediaStore.Video.Thumbnails.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.Video.Thumbnails.VIDEO_ID, MediaStore.Video.Thumbnails.DATA}
                , MediaStore.Video.Thumbnails.VIDEO_ID + "=" + id, null, null);
        String result = "";

        if (cursor != null) {
            while (cursor.moveToNext()) {
                result = Common.createVideoThumb(cursor.getString((int) cursor.getColumnIndex(MediaStore.Video.Thumbnails.DATA)), id, thumb);
            }
            cursor.close();
            callback.invoke(result);
        }
    }

    private JSONObject queryFile(Context context, int page, int limit) {
        Uri tableUri = MediaStore.Files.getContentUri("external");
        String selection = File.baseSelection + File.mimeSelection + File.titleSelection;
        JSONArray list = new JSONArray();
        Cursor cursor = Common.getCursor(context, tableUri, File.baseProjection, selection, page, limit);
        if (cursor != null) {
            int dataColumn = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA);
            int typeColumn = cursor.getColumnIndex(MediaStore.Files.FileColumns.MIME_TYPE);
            int sizeColumn = cursor.getColumnIndex(MediaStore.Files.FileColumns.SIZE);
            int nameColumn = cursor.getColumnIndex(MediaStore.Files.FileColumns.TITLE);
            int idColumn = cursor.getColumnIndex(MediaStore.Files.FileColumns._ID);
            while (cursor.moveToNext()) {
                list.add(new File(cursor.getString(nameColumn), cursor.getString(typeColumn), cursor.getLong(sizeColumn), "file://" + cursor.getString(dataColumn), cursor.getInt(idColumn)));
            }
            cursor.close();
        }
        return new JSONObject() {{
            put("list", list);
            put("count", getCount(context, tableUri));
        }};
    }


    private JSONObject queryMedia(Context context, String type, int page, int limit, JSONObject thumbOptions) {
        if (!Common.createDirectors(BaseInfo.sBaseFsAppsPath + BaseInfo.sCurrentAppOriginalAppid, BaseInfo.REAL_PRIVATE_DOC_DIR + "/thumbs")) {
            return new JSONObject() {{
                put("msg", "缩略图存储目录创建失败，请检查是否有对应权限");
            }};
        }

        Uri tableUri = type.equals("audio") ? MediaStore.Audio.Media.EXTERNAL_CONTENT_URI : MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        List<String> projectionList = new ArrayList<>(Arrays.asList(File.baseProjection));
        projectionList.addAll(Media.videoAndAudioProjection);
        String selection = File.baseSelection + File.mimeSelection + File.titleSelection;
        JSONArray list = new JSONArray();
        Cursor cursor = Common.getCursor(context, tableUri, projectionList.toArray(new String[]{}), selection, page, limit);
        if (cursor != null) {
            int dataColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DATA);
            int typeColumn = cursor.getColumnIndex(MediaStore.Audio.Media.MIME_TYPE);
            int sizeColumn = cursor.getColumnIndex(MediaStore.Audio.Media.SIZE);
            int nameColumn = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
            int artistColumn = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
            int albumColumn = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM);
            int durationColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION);
            int idColumn = cursor.getColumnIndex(MediaStore.Images.Media._ID);
            while (cursor.moveToNext()) {
                File fileInfo = new File(cursor.getString(nameColumn), cursor.getString(typeColumn), cursor.getLong(sizeColumn), "file://" + cursor.getString(dataColumn),cursor.getInt(idColumn));
                HashMap<String, String> MediaInfo = new HashMap<String, String>() {{
                    put("artist", cursor.getString(artistColumn));
                    put("album", cursor.getString(albumColumn));
                    put("duration", cursor.getString(durationColumn));
                }};
                if (tableUri == MediaStore.Video.Media.EXTERNAL_CONTENT_URI && !Boolean.FALSE.equals(thumbOptions.getBoolean("enable"))) {
                    String thumb = Common.createVideoThumb(cursor.getString(dataColumn), cursor.getInt(idColumn), thumbOptions);
                    if (!thumb.equals(""))  MediaInfo.put("thumb", thumb);
                }
                list.add(new Media(fileInfo, cursor.getInt(idColumn), MediaInfo));
            }
            cursor.close();
        }
        return new JSONObject() {{
            put("list", list);
            put("count", getCount(context, tableUri));
        }};
    }

    private JSONObject queryImage(Context context, JSONObject thumbOptions, int page, int limit) {
        if (!Common.createDirectors(BaseInfo.sBaseFsAppsPath + BaseInfo.sCurrentAppOriginalAppid, BaseInfo.REAL_PRIVATE_DOC_DIR + "/thumbs")) {
            return new JSONObject() {{
                put("msg", "缩略图存储目录创建失败，请检查是否有对应权限");
            }};
        }

        Uri tableUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        List<String> projectionList = new ArrayList<>(Arrays.asList(File.baseProjection));
        projectionList.addAll(Media.imageProjection);
        String selection = File.baseSelection + File.mimeSelection + File.titleSelection;
        JSONArray list = new JSONArray();
        Cursor cursor = Common.getCursor(context, tableUri, projectionList.toArray(new String[]{}), selection, page, limit);
        if (cursor != null) {
            int dataColumn = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
            int typeColumn = cursor.getColumnIndex(MediaStore.Images.Media.MIME_TYPE);
            int sizeColumn = cursor.getColumnIndex(MediaStore.Images.Media.SIZE);
            int nameColumn = cursor.getColumnIndex(MediaStore.Images.Media.TITLE);
            int widthColumn = cursor.getColumnIndex(MediaStore.Images.Media.WIDTH);
            int heightColumn = cursor.getColumnIndex(MediaStore.Images.Media.HEIGHT);
            int idColumn = cursor.getColumnIndex(MediaStore.Images.Media._ID);
            while (cursor.moveToNext()) {
                int id = cursor.getInt(idColumn);
                File fileInfo = new File(cursor.getString(nameColumn), cursor.getString(typeColumn), cursor.getLong(sizeColumn), "file://" + cursor.getString(dataColumn),cursor.getInt(idColumn));
                HashMap<String, String> MediaInfo = new HashMap<String, String>() {{
                    put("height", cursor.getString(heightColumn));
                    put("width", cursor.getString(widthColumn));
                }};
                if (!Boolean.FALSE.equals(thumbOptions.getBoolean("enable"))) {
                    String thumb = queryImgThumbs(context, id, thumbOptions);
                    if (!thumb.equals("")) MediaInfo.put("thumb", thumb);
                }
                list.add(new Media(fileInfo, id, MediaInfo));
            }
            cursor.close();
        }

        return new JSONObject() {{
            put("list", list);
            put("count", getCount(context, tableUri));
        }};
    }

}
