package com.dreaming.hscj.core;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by Isidore
 * on 2020/4/20
 */
public class ThreadPoolProvider {
    private ThreadPoolProvider(){}
    private volatile static ThreadPoolProvider instance;
    private static ThreadPoolProvider getInstance(){
        if(instance == null){
            synchronized (ThreadPoolProvider.class){
                if(instance==null){
                    instance = new ThreadPoolProvider();
                }
            }
        }
        return instance;
    }
    private int MAX_CORE_SIZE = Math.max(1, Runtime.getRuntime().availableProcessors());
    Object lockerOfFixThreadPool = new Object();
    Object lockerOfSingleThreadScheduledExecutor = new Object();
    Object lockerOfScheduledThreadPool = new Object();


    private static volatile ExecutorService instanceOfFixThreadPool;
    public static ExecutorService getFixedThreadPool(){
        if(instanceOfFixThreadPool ==null){
            synchronized (getInstance().lockerOfFixThreadPool){
                if(instanceOfFixThreadPool ==null){
                    instanceOfFixThreadPool =new ThreadPoolExecutor(
                            getInstance().MAX_CORE_SIZE,//可同时使用的线程数
                            Integer.MAX_VALUE, //线程池最大量
                            1L, //线程空闲最大时间 空闲则丢弃任务
                            TimeUnit.HOURS,   //线程空闲的时间单位
                            new LinkedBlockingQueue<Runnable>());//存放线程任务的有序队列
                }
            }
        }
        return instanceOfFixThreadPool;
    }

    private static volatile ScheduledExecutorService instanceOfSingleThreadScheduledExecutor;
    public static ScheduledExecutorService getSingleThreadExecutor(){
        if(instanceOfSingleThreadScheduledExecutor ==null){
            synchronized (getInstance().lockerOfSingleThreadScheduledExecutor){
                if(instanceOfSingleThreadScheduledExecutor ==null){
                    instanceOfSingleThreadScheduledExecutor = Executors.newSingleThreadScheduledExecutor();
                }
            }
        }
        return instanceOfSingleThreadScheduledExecutor;
    }

    private static volatile ScheduledExecutorService instanceOfScheduledThreadPool;
    public static ScheduledExecutorService getScheduledThreadPool(){
        if(instanceOfScheduledThreadPool ==null){
            synchronized (getInstance().lockerOfScheduledThreadPool){
                if(instanceOfScheduledThreadPool ==null){
                    instanceOfScheduledThreadPool = Executors.newScheduledThreadPool(12);
                }
            }
        }
        return instanceOfScheduledThreadPool;
    }
}