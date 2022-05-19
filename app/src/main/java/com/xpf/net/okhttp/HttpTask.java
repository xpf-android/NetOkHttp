package com.xpf.net.okhttp;


import com.alibaba.fastjson.JSON;

import java.io.UnsupportedEncodingException;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;


public class HttpTask<T> implements Runnable, Delayed {

    private IHttpRequest httpRequest;

    public HttpTask(String url,T requestData,IHttpRequest httpRequest,CallbackListener listener) {
        this.httpRequest = httpRequest;
        httpRequest.setUrl(url);
        httpRequest.setListener(listener);
        //解析服务端返回数据 json-->string
        String content = JSON.toJSONString(requestData);
        try {
            httpRequest.setData(content.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

    }


    @Override
    public void run() {
        try {
            httpRequest.execute();
        } catch (Exception e) {
            //当请求任务失败时，把请求任务添加到延迟队列
            ThreadPoolManager.getInstance().addDelayTask(this);
        }
    }

    private long delayTime;
    private int retryCount;


    public long getDelayTime() {
        return delayTime;
    }

    //设置延迟时间
    public void setDelayTime(long delayTime) {
        this.delayTime = System.currentTimeMillis()+ delayTime;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    @Override
    public long getDelay(TimeUnit unit) {
        return unit.convert(this.delayTime-System.currentTimeMillis(),TimeUnit.MILLISECONDS);
    }

    @Override
    public int compareTo(Delayed o) {
        return 0;
    }
}
