package io.surprise.file.libs;


import static io.dcloud.common.util.PdrUtil.parseFloat;
import static io.dcloud.common.util.PdrUtil.parseInt;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import com.alibaba.fastjson.JSONObject;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;

import io.dcloud.common.util.BaseInfo;
import io.dcloud.feature.uniapp.bridge.UniJSCallback;
import io.surprise.file.Manager;
import io.surprise.file.model.File;

public class Common extends Manager {
    public static UniJSCallback Callback;
    public static JSONObject JsonParams;

    public static String[] getNeedsPermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            return new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, "android.permission.READ_MEDIA_IMAGES", "android.permission.READ_MEDIA_AUDIO", "android.permission.READ_MEDIA_VIDEO"};
        } else {
            return new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        }
    }

    public static void requestPermission(Context context, int code) {
//        Callback = callback;
//        JsonParams = json;
        String[] permissions = getNeedsPermission();

        if (checkShouldShowRequestPermissionRationale(context,permissions)) {
            // Log.i(TAG, "禁止后,再次请求会触发此步骤");
            ActivityCompat.requestPermissions((Activity) context, permissions,code);
        } else {
            //  Log.i(TAG, "禁止并不再询问后，再次请求会触发此步骤");
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    .setData(Uri.fromParts("package", context.getPackageName(), null));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            showPerMissionToast(context);
        }
    }


    public static void showPerMissionToast(Context context) {
        showPerMissionToast(context, "该功能需要授权存储权限才能正常运行,请到应用设置—权限—打开存储权限");
    }

    public static void showPerMissionToast(Context context, String msg) {
        Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
    }

    public  static boolean checkShouldShowRequestPermissionRationale(Context context, String[] permissions) {
        boolean result = false;
        for (String permission : permissions) {
            if (ActivityCompat.shouldShowRequestPermissionRationale((Activity) context, permission)) {
                result = true;
                break;
            }
        }
        return result;
    }

    public static boolean checkPermission(Context context) {
        boolean result = true;
        String[] permissions = getNeedsPermission();

        for (String permission : permissions) {
            if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                result = false;
                break;
            }
        }

        return  result;
    }

    public static Cursor getCursor(Context context, Uri uri, @Nullable String[] projection, @Nullable String selection, int page, int limit) {
        int start = (page - 1) * limit;
        if (Build.VERSION.SDK_INT >= 30) {
            Bundle select = new Bundle();
            select.putInt(ContentResolver.QUERY_ARG_LIMIT, limit);
            select.putInt(ContentResolver.QUERY_ARG_OFFSET, start);
            select.putStringArray(ContentResolver.QUERY_ARG_SORT_COLUMNS, new String[]{MediaStore.Files.FileColumns.DATE_MODIFIED});
            select.putInt(ContentResolver.QUERY_ARG_SORT_DIRECTION, ContentResolver.QUERY_SORT_DIRECTION_DESCENDING);
            select.putString(ContentResolver.QUERY_ARG_SQL_SELECTION, selection);
            select.putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, File.mimeSelectionArgs);
            return context.getContentResolver().query(uri, projection, select, null);
        } else {
//            String sortOrder = MediaStore.Files.FileColumns.DATE_MODIFIED + " DESC limit " + limit + " offset " + start;
            return context.getContentResolver().query(uri.buildUpon().encodedQuery("limit=" + start + "," + limit).build(), projection, selection, File.mimeSelectionArgs, MediaStore.Files.FileColumns.DATE_MODIFIED + " DESC ");
        }
    }


    /**
     * 转换文件大
     * @param fileSize 文件大小 字节
     */
    public static String formatFileSize(long fileSize) {
        DecimalFormat df = new DecimalFormat("#.00");
        String fileSizeString;
        String wrongSize = "0B";
        if (fileSize == 0) {
            return wrongSize;
        }
        if (fileSize < 1024) {
            fileSizeString = df.format((double) fileSize) + "B";
        } else if (fileSize < 1048576) {
            fileSizeString = df.format((double) fileSize / 1024) + "KB";
        } else if (fileSize < 1073741824) {
            fileSizeString = df.format((double) fileSize / 1048576) + "MB";
        } else {
            fileSizeString = df.format((double) fileSize / 1073741824) + "GB";
        }
        return fileSizeString;

    }

    // 创建多级目录
    public static boolean createDirectors(String base, String target) {
        java.io.File file = new java.io.File(base + "/" + target + "/");
        if (file.exists() && file.isDirectory()) {
            return true;
        } else {
            return file.mkdirs();
        }
//        String[] fileDirs= target.split("\\/");
//        String path =
//        for (int i = 0; i < fileDirs.length; i++) {
//            path += "/"+fileDirs[i];
//            if (file.exists()) {
//                continue;
//            }else {
//                file.mkdir();
//            }
//        }
    }
    // 创建视频缩略图
    public static String createVideoThumb(String filePath, int id, JSONObject thumbOptions) {
        String savePath = BaseInfo.sBaseFsAppsPath + BaseInfo.sCurrentAppOriginalAppid + "/" + BaseInfo.REAL_PRIVATE_DOC_DIR + "thumbs/";
        java.io.File file = new java.io.File(savePath + id + "-thumb.jpeg");
        if (file.exists()) {
            return "file://" + savePath + id + "-thumb.jpeg";
        }
        // 生成缩略图并保存
        String result = "";
        Bitmap thumbnail = ThumbnailUtils.createVideoThumbnail(filePath, MediaStore.Images.Thumbnails.MINI_KIND);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Matrix matrix = new Matrix();
        matrix.setScale(parseFloat(thumbOptions.getString("scaleW"), 0.5f), parseFloat(thumbOptions.getString("scaleH"), 0.5f));
        //把bitmap100%高质量压缩 到 output对象里
        Bitmap.createBitmap(thumbnail, 0,0, thumbnail.getWidth(), thumbnail.getHeight(), matrix,true).compress(Bitmap.CompressFormat.JPEG, parseInt(thumbOptions.getString("quality"), 50), outputStream);
        BufferedOutputStream outStream = null;
        try {
            outStream = new BufferedOutputStream(new FileOutputStream(file));
            outStream.write(outputStream.toByteArray());
            outStream.flush();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (null != outStream) {
                try {
                    outStream.close();
                    result = "file://" + savePath + id + "-thumb.jpeg";
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return result;
    }


    // 创建图片缩略图
    public static String createImgThumb(String filePath, int id, JSONObject thumbOptions) {
//        thumbOptions = new JSONObject();
        String savePath = BaseInfo.sBaseFsAppsPath + BaseInfo.sCurrentAppOriginalAppid + "/" + BaseInfo.REAL_PRIVATE_DOC_DIR + "thumbs/";
        java.io.File file = new java.io.File(savePath + id + "-thumb.jpeg");
        if (file.exists()) {
            return "file://" + savePath + id + "-thumb.jpeg";
        }
        // 生成缩略图并保存
        String result = "";
        Bitmap thumbnail = BitmapFactory.decodeFile(filePath);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Matrix matrix = new Matrix();
        matrix.setScale(parseFloat(thumbOptions.getString("scaleW"), 0.5f), parseFloat(thumbOptions.getString("scaleH"), 0.5f));
        //把bitmap100%高质量压缩 到 output对象里
        Bitmap.createBitmap(thumbnail, 0,0, thumbnail.getWidth(), thumbnail.getHeight(), matrix,true).compress(Bitmap.CompressFormat.JPEG, parseInt(thumbOptions.getString("quality"), 50), outputStream);
        BufferedOutputStream outStream = null;
        try {
            outStream = new BufferedOutputStream(new FileOutputStream(file));
            outStream.write(outputStream.toByteArray());
            outStream.flush();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (null != outStream) {
                try {
                    outStream.close();
                    result = "file://" + savePath + id + "-thumb.jpeg";
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return result;
    }

}
