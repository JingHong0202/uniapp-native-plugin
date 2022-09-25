package io.surprise.file.libs;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.alibaba.fastjson.JSONObject;

import java.text.DecimalFormat;

import io.dcloud.feature.uniapp.bridge.UniJSCallback;
import io.surprise.file.Manager;
import io.surprise.file.model.File;

public class Common extends Manager {
    public static UniJSCallback Callback;
    public static JSONObject JsonParams;

    public static void requestPermission(Context context, JSONObject json, UniJSCallback callback) {
        Callback = callback;
        JsonParams = json;
        String permission = Manifest.permission.READ_EXTERNAL_STORAGE;
        if (ActivityCompat.shouldShowRequestPermissionRationale((Activity) context,
                permission)) {
            // Log.i(TAG, "禁止后,再次请求会触发此步骤");
            ActivityCompat.requestPermissions((Activity) context,
                    new String[]{permission},
                    110);
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

    public static Boolean checkPermission(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED;
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

}
