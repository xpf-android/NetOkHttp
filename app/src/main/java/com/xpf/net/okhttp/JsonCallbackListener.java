package com.xpf.net.okhttp;

import android.os.Handler;
import android.os.Looper;

import com.alibaba.fastjson.JSON;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class JsonCallbackListener<T> implements CallbackListener {

    //服务端返回数据类型对象
    private Class<T> responseClass;
    private IJsonDataTrans iJsonDataTrans;
    private Handler handler = new Handler(Looper.getMainLooper());

    public JsonCallbackListener(Class<T> responseClass,IJsonDataTrans iJsonDataTrans) {
        this.responseClass = responseClass;
        this.iJsonDataTrans = iJsonDataTrans;
    }

    //解析服务端返回的数据，解析为具体的数据类型
    @Override
    public void onSuccess(InputStream inputStream) {
        //将服务端返回的数据(流)转换为指定的responseClass
        String response = getContent(inputStream);
        final T clazz = JSON.parseObject(response, responseClass);
        handler.post(new Runnable() {
            @Override
            public void run() {
                iJsonDataTrans.onSuccess(clazz);
            }
        });
    }

    @Override
    public void onFailure() {

    }

    private String getContent(InputStream inputStream) {
        String content = null;
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder sb = new StringBuilder();
            String line = null;
            try {
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return content;
    }
}
