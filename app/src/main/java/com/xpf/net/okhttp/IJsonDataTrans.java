package com.xpf.net.okhttp;

public interface IJsonDataTrans<T> {
    void onSuccess(T t);

    void onFailure();
}
