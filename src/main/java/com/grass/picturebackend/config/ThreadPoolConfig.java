package com.grass.picturebackend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author Mr.Liuxq
 * @description: 线程配置
 * @date 2025年05月14日 14:09
 */

@Configuration
public class ThreadPoolConfig {

    /**
     * 创建一个自定义的线程池执行器（ThreadPoolExecutor）Bean。
     * 该线程池执行器用于管理线程的生命周期，包括线程的创建、执行任务、以及线程的回收。
     *
     * @return ThreadPoolExecutor 返回一个配置好的线程池执行器实例。
     * <p>
     * 参数说明：
     * - corePoolSize: 核心线程数，线程池中始终保持的线程数量，即使它们处于空闲状态。
     * - maximumPoolSize: 最大线程数，线程池中允许的最大线程数量。
     * - keepAliveTime: 线程空闲时间，当线程池中的线程数量超过核心线程数时，多余的空闲线程在终止前等待新任务的最长时间。
     * - unit: 时间单位，用于指定keepAliveTime的时间单位。
     * - workQueue: 任务队列，用于保存等待执行的任务的阻塞队列。
     */
    @Bean
    public ThreadPoolExecutor threadPoolExecutor() {
        // 配置线程池的核心参数
        int corePoolSize = 10;
        int maximumPoolSize = 20;
        long keepAliveTime = 60;
        TimeUnit unit = TimeUnit.SECONDS;
        LinkedBlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<>(100);

        // 创建并返回一个配置好的线程池执行器实例
        return new ThreadPoolExecutor(
                corePoolSize,
                maximumPoolSize,
                keepAliveTime,
                unit,
                workQueue
        );
    }


}
