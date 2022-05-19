package com.xpf.net.okhttp;

public class NetHttp {
    public static <T,M> void sendJsonRequest(String url, T requestData,Class<M> response,IJsonDataTrans listener) {
        //封装请求
        IHttpRequest httpRequest = new JsonHttpRequest();
        CallbackListener callbackListener = new JsonCallbackListener<>(response,listener);
        //封装请求任务
        HttpTask httpTask = new HttpTask<>(url,requestData,httpRequest,callbackListener);
        //将请求任务添加到任务队列
        ThreadPoolManager.getInstance().addTask(httpTask);
    }
}
