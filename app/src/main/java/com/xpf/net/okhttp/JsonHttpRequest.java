package com.xpf.net.okhttp;

import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * 封装请求
 */
public class JsonHttpRequest implements IHttpRequest {

    private String url;
    private byte[] data;
    private CallbackListener mCallbackListener;

    @Override
    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public void setData(byte[] data) {
        this.data = data;
    }

    @Override
    public void setListener(CallbackListener callbackListener) {
        this.mCallbackListener = callbackListener;
    }

    private HttpURLConnection urlConnection;

    @Override
    public void execute() {
        //访网络的具体操作
        URL url = null;
        try{
            url = new URL(this.url);
            urlConnection = (HttpURLConnection) url.openConnection();//打开http连接
            urlConnection.setConnectTimeout(6000);//连接的超时时间
            urlConnection.setUseCaches(false);//不使用缓存
            urlConnection.setInstanceFollowRedirects(true);//是成员函数，仅作用于当前函数，设置这个连接是否可以被重定向
            urlConnection.setReadTimeout(3000);//响应的超时时间
            urlConnection.setDoInput(true);//设置这个连接是否可以写入数据
            urlConnection.setDoOutput(true);//设置这个连接是否可以输出数据
            urlConnection.setRequestMethod("POST");//设置请求的方式
            urlConnection.setRequestProperty("Content-Type","application/json;charset=UTF-8");//设置消息的类型json
            urlConnection.connect();//连接，从上述至此的配置必须要在connect()之前完成，实际上它只是建立了一个与服务器的tcp连接
            //-----------------------使用字节流发送数据-------------------
            OutputStream out = urlConnection.getOutputStream();
            BufferedOutputStream bos = new BufferedOutputStream(out);//缓冲字节流包装字节流
            bos.write(data);//把这个字节数组的数据写入缓冲区中
            bos.flush();//刷新缓冲区，发送数据
            out.close();
            bos.close();
            //------------------------字符流写入数据----------------------
            if (urlConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {//得到服务端的返回码是连接成功
                InputStream in = urlConnection.getInputStream();
                //这里是不是就把我们的数据返回到我们的框架里了？
                mCallbackListener.onSuccess(in);

            } else {
                throw new RuntimeException("请求失败");
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("请求失败");
        } finally {
            urlConnection.disconnect();//断开连接
        }

    }
}
