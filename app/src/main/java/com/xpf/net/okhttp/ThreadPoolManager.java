package com.xpf.net.okhttp;

import android.util.Log;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static com.xpf.net.okhttp.MainActivity.TAG;


/**
 * 线程池管理类
 */
public class ThreadPoolManager {
    private static ThreadPoolManager threadPoolManager = new ThreadPoolManager();

    public static ThreadPoolManager getInstance() {
        return threadPoolManager;
    }



    //任务队列(FIFO 线程安全的)
    private LinkedBlockingQueue<Runnable> mQueue = new LinkedBlockingQueue<>();

    //创建延迟队列
    private DelayQueue<HttpTask> mDelayQueue = new DelayQueue<>();


    //将请求任务添加到队列中
    public void addTask(Runnable runnable) {
        if (runnable != null) {
            try {
                mQueue.put(runnable);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    //将HttpTask添加到失败的队列
    public void addDelayTask(HttpTask task) {
        if (task != null) {
            task.setDelayTime(3000);//设置延迟时间
            mDelayQueue.offer(task);//添加到延迟队列
        }
    }

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

    //创建核心任务(线程)
    public Runnable coreTask = new Runnable() {
        Runnable runnable = null;
        @Override
        public void run() {
            //不停(循环)的从任务队列中取出任务交给线程池处理
            while (true) {
                try {
                    //从任务请求队列取出任务
                    runnable = mQueue.take();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                //交给线程池执行任务
                mThreadPoolExecutor.execute(runnable);
            }
        }

    };

    private ThreadPoolExecutor mThreadPoolExecutor;
    //创建线程池
    private ThreadPoolManager() {
        mThreadPoolExecutor = new ThreadPoolExecutor(3, 10, 15, TimeUnit.SECONDS,
                new ArrayBlockingQueue<Runnable>(4), new RejectedExecutionHandler() {
            @Override
            public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                addTask(r);
            }
        });
        mThreadPoolExecutor.execute(coreTask);//执行任务
        mThreadPoolExecutor.execute(delayTask);//执行延迟任务
    }

}
