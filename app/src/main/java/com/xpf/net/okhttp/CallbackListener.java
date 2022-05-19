package com.xpf.net.okhttp;

import java.io.InputStream;

public interface CallbackListener {

    void onSuccess(InputStream is);

    void onFailure();
}
