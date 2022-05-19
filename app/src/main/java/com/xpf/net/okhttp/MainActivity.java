package com.xpf.net.okhttp;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    public static final String TAG = "NetOkHttp";

    //private String url = "http://xxx";//测试重试机制
    private String url = "http://apis.juhe.cn/ip/ipNewV3?ip=192.168.28.124&key=18682ed3411dd8e8768cdd38d901afe1";
    //ResponseClass{error_code=0, reason='查询成功'}
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sendRequest();
    }

    private void sendRequest() {
        NetHttp.sendJsonRequest(url, null, ResponseClass.class, new IJsonDataTrans<ResponseClass>() {


            @Override
            public void onSuccess(ResponseClass m) {
                Log.e(TAG, "onSuccess: " + m.toString());
            }

            @Override
            public void onFailure() {

            }
        });
    }
}
