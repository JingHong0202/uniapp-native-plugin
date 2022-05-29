package io.surprise.file;

import android.app.Application;
import android.util.Log;

import io.dcloud.feature.uniapp.UniAppHookProxy;

public class AppProxy implements UniAppHookProxy {
    @Override
    public void onCreate(Application application) {
        //当前uni应用进程回调 仅触发一次 多进程不会触发
        //可通过UniSDKEngine注册UniModule或者UniComponent
//        Fresco.initialize(application.getBaseContext());
        Log.d("APP", "onCreate: ");
    }

    @Override
    public void onSubProcessCreate(Application application) {
        //其他子进程初始化回调 可用于初始化需要子进程初始化需要的逻辑
    }
}
