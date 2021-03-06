package com.leo.appmaster.mgr.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

/**
 * 签名保护Receiver
 * Created by Jasper on 2016/1/29.
 */
public class IpcSignatureReceiver extends BroadcastReceiver {
    private IpcHandleLayer mHandlerLayer;

    public IpcSignatureReceiver() {
        mHandlerLayer = new IpcHandleLayer();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) {
            return;
        }
        String action = intent.getAction();
        if (TextUtils.isEmpty(action)) {
            return;
        }
        IpcRequest ipcRequest = new IpcRequest();
        ipcRequest.id = intent.getIntExtra(IpcConst.KEY_REQUEST_ID, 0);
        ipcRequest.command = intent.getStringExtra(IpcConst.KEY_REQUEST_COMMAND);
        ipcRequest.pkgName = intent.getStringExtra(IpcConst.KEY_REQUEST_PKG);
        ipcRequest.data = intent;

        mHandlerLayer.handleRequest(ipcRequest);
    }
}
