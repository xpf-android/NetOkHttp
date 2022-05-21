# OkHttp原理解析


![](https://github.com/xpf-android/NetOkHttp/raw/master/images/okhttp原理1.png)

![](https://github.com/xpf-android/NetOkHttp/raw/master/images/okhttp原理2.png)



### 封装请求

```java
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
                mCallbackListener.onSuccess(in);
                //这里是不是就把我们的数据返回到我们的框架里了？
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
```



### 封装响应

```java
public class ResponseClass {
    private int error_code;
    private String reason;

    public int getError_code() {
        return error_code;
    }

    public void setError_code(int error_code) {
        this.error_code = error_code;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    @Override
    public String toString() {
        return "ResponseClass{" +
                "error_code=" + error_code +
                ", reason='" + reason + '\'' +
                '}';
    }
}
```



### 封装请求任务

```java
public class HttpTask<T> implements Runnable, Delayed {

    private IHttpRequest httpRequest;

    public HttpTask(String url,T requestData,IHttpRequest httpRequest,CallbackListener listener) {
        this.httpRequest = httpRequest;
        httpRequest.setUrl(url);
        httpRequest.setListener(listener);
        //将服务端返回的数据解析为指定类型json-->string
        String content = JSON.toJSONString(requestData);
        try {
            httpRequest.setData(content.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

    }

    //当请求任务失败时，把请求任务添加到延迟队列
    @Override
    public void run() {
        try {
            httpRequest.execute();
        } catch (Exception e) {
            ThreadPoolManager.getInstance().addDelayTask(this);
        }
    }

    private long delayTime;
    private int retryCount;


    public long getDelayTime() {
        return delayTime;
    }

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
```



### 封装“使用工具”类



```java
public class NetHttp {
    public static <T,M> void sendJsonRequest(String url, T requestData,Class<M> response,IJsonDataTrans listener) {
        IHttpRequest httPRequest = new JsonHttpRequest();
        CallbackListener callbackListener = new JsonCallbackListener<>(response,listener);
        HttpTask httpTask = new HttpTask<>(url,requestData,httPRequest,callbackListener);
        ThreadPoolManager.getInstance().addTask(httpTask);
    }
}
```



### 测试

```java
public class MainActivity extends AppCompatActivity {
    public static final String TAG = "NetHttp";

    //private String url = "http://xxx";//测试重试机制
    private String url = "http://v.juhe.cn/historyWeather/citys?province_id=2&key=bb52107206585ab0745e59a8c73875b";
    //{"resultcode":"101","reason":"错误的请求KEY","result":null,"error_code":10001}
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
```

使用正确的请求url效果如下：

![](https://github.com/xpf-android/NetOkHttp/raw/master/images/测试请求是否成功.png)


### 重试机制
![](https://github.com/xpf-android/NetOkHttp/raw/master/images/重试机制.png)
```java
//创建专门处理延迟队列的任务(线程)
    public Runnable delayTask = new Runnable() {
        @Override
        public void run() {
            HttpTask task = null;
            //不停(循环)的从延迟任务队列中取出任务交给线程池处理
            while (true) {
                try {
                    //从延迟队列中取出任务
                    task = mDelayQueue.take();
                    if (task.getRetryCount() < 3) {//重试机制的的条件
                        mThreadPoolExecutor.execute(task);
                        //重试机制的核心
                        //设置任务请求的次数
                        task.setRetryCount(task.getRetryCount() + 1);
                        Log.e(TAG, "重试机制: "+task.getRetryCount() + " 次数");
                    } else {
                        Log.e(TAG, "重试机制: "+"失败次数太多，不再处理");
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    };
```


使用错误url效果如下：

![](https://github.com/xpf-android/NetOkHttp/raw/master/images/重试机制验证结果.png)

重试机制生效，注意看重试的次数，以及请求间隔的时间，就是我们设置的延迟时间3秒。

### 小结

![](https://github.com/xpf-android/NetOkHttp/raw/master/images/小结.png)

线程池执行核心任务，该任务就是从请求任务队列中循环的取出请求任务交给线程池执行，同时线程也会执行延迟任务，该延迟任务就是循环从延迟任务队列中取出延迟任务执行。封装的任务中包含了请求和响应过程，请求失败会将任务添加到延迟队列中。



