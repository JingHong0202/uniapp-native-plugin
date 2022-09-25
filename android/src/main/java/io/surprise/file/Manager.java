package io.surprise.file;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.Nullable;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

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
        if (requestCode == 110) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 授权成功
                Log.d(TAG, "onRequestPermissionsResult: success");
                queryAsync(Common.JsonParams, Common.Callback);
                Common.JsonParams = null;
                Common.Callback = null;
            } else {
                // 授权失败
                Log.d(TAG, "onRequestPermissionsResult: fail");
                Common.showPerMissionToast(mUniSDKInstance.getContext(), "该功能需要授权存储权限才能正常运行");

            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
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
        if (Common.checkPermission(context)) {
            Common.requestPermission(context, JsonParams, callback);
            return;
        }

        String type = null;
        int limit = 0, page = 0;
        if (JsonParams instanceof JSONObject) {
            type = JsonParams.getString("type");
            limit = JsonParams.getIntValue("limit");
            page = JsonParams.getIntValue("page");
            JSONArray mimeFilter = JsonParams.getJSONArray("mime");
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
                results = queryMedia(context, type, page, limit);
                break;
            case "image":
                Boolean isHideThumb = JsonParams.getBoolean("isHideThumb");
                results = queryImage(context, isHideThumb, page, limit);
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
        if (JsonParams instanceof JSONObject) {
            type = JsonParams.getString("type");
            limit = JsonParams.getIntValue("limit");
            page = JsonParams.getIntValue("page");
            JSONArray mimeFilter = JsonParams.getJSONArray("mime");
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
                results = queryMedia(context, type, page, limit);
                break;
            case "image":
                Boolean isHideThumb = JsonParams.getBoolean("isHideThumb");
                results = queryImage(context, isHideThumb, page, limit);
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

    /**
     * @param id *文件ID
     * @description 根据id获取对应的缩略图
     */
    public String queryThumbs(Context context, int id) {
        String result = null;
        Cursor cursor = context.getContentResolver().query(MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.Images.Thumbnails.IMAGE_ID, MediaStore.Images.Thumbnails.DATA}
                , MediaStore.Images.Thumbnails.IMAGE_ID + "=" + id, null, null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                result = "file://" + cursor.getString((int) cursor.getColumnIndex(MediaStore.Images.Thumbnails.DATA));
            }
            cursor.close();
        }
        return result;
    }

    /**
     * @param id       *文件ID
     * @param callback *返回值回调
     * @description 根据id获取对应的缩略图, 该函数专门暴露给uni以便单独调用
     */
    @UniJSMethod()
    public void queryThumbs(int id, UniJSCallback callback) {
        if (callback == null) return;
        Cursor cursor = mUniSDKInstance.getContext().getContentResolver().query(MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.Images.Thumbnails.IMAGE_ID, MediaStore.Images.Thumbnails.DATA}
                , MediaStore.Images.Thumbnails.IMAGE_ID + "=" + id, null, null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                callback.invoke("file://" + cursor.getString((int) cursor.getColumnIndex(MediaStore.Images.Thumbnails.DATA)));
            }
            cursor.close();
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


    private JSONObject queryMedia(Context context, String type, int page, int limit) {
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
                list.add(new Media(fileInfo, cursor.getInt(idColumn), MediaInfo));
            }
            cursor.close();
        }
        return new JSONObject() {{
            put("list", list);
            put("count", getCount(context, tableUri));
        }};
    }

    private JSONObject queryImage(Context context, @Nullable Boolean isHideThumb, int page, int limit) {
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
                if (Boolean.FALSE.equals(isHideThumb) || isHideThumb == null) {
                    String thumb = queryThumbs(context, id);
                    MediaInfo.put("thumb", thumb);
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
