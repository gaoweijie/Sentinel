/*
 * Copyright 1999-2018 Alibaba Group Holding Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.csp.sentinel.transport.init;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.DiscardOldestPolicy;
import java.util.concurrent.TimeUnit;

import com.alibaba.csp.sentinel.concurrent.NamedThreadFactory;
import com.alibaba.csp.sentinel.config.SentinelConfig;
import com.alibaba.csp.sentinel.heartbeat.HeartbeatSenderProvider;
import com.alibaba.csp.sentinel.init.InitFunc;
import com.alibaba.csp.sentinel.init.InitOrder;
import com.alibaba.csp.sentinel.log.RecordLog;
import com.alibaba.csp.sentinel.transport.HeartbeatSender;
import com.alibaba.csp.sentinel.transport.config.TransportConfig;

/**
 * HeartBeat 的初始化与心跳发送
 *
 * Global init function for heartbeat sender.
 *
 * @author Eric Zhao
 */
@InitOrder(-1)
public class HeartbeatSenderInitFunc implements InitFunc {

    private ScheduledExecutorService pool = null;

    private void initSchedulerIfNeeded() {
        if (pool == null) {
            pool = new ScheduledThreadPoolExecutor(2,
                new NamedThreadFactory("sentinel-heartbeat-send-task", true),
                new DiscardOldestPolicy());
        }
    }

    @Override
    public void init() {
        HeartbeatSender sender = HeartbeatSenderProvider.getHeartbeatSender();
        if (sender == null) {
            RecordLog.warn("[HeartbeatSenderInitFunc] WARN: No HeartbeatSender loaded");
            return;
        }

        initSchedulerIfNeeded();
        long interval = retrieveInterval(sender);
        setIntervalIfNotExists(interval);
        scheduleHeartbeatTask(sender, interval);
    }

    private boolean isValidHeartbeatInterval(Long interval) {
        return interval != null && interval > 0;
    }

    private void setIntervalIfNotExists(long interval) {
        SentinelConfig.setConfig(TransportConfig.HEARTBEAT_INTERVAL_MS, String.valueOf(interval));
    }

    long retrieveInterval(/*@NonNull*/ HeartbeatSender sender) {
        Long intervalInConfig = TransportConfig.getHeartbeatIntervalMs();
        if (isValidHeartbeatInterval(intervalInConfig)) {
            RecordLog.info("[HeartbeatSenderInitFunc] Using heartbeat interval "
                + "in Sentinel config property: " + intervalInConfig);
            return intervalInConfig;
        } else {
            long senderInterval = sender.intervalMs();
            RecordLog.info("[HeartbeatSenderInit] Heartbeat interval not configured in "
                + "config property or invalid, using sender default: " + senderInterval);
            return senderInterval;
        }
    }

    private void scheduleHeartbeatTask(/*@NonNull*/ final HeartbeatSender sender, /*@Valid*/ long interval) {
        /**
         * sentinel-core 连接上 dashboard 之后，并不是就结束了，事实上 sentinel-core 是通过一个 ScheduledExecutorService 的定时任务，
         * 每隔 10 秒钟向 dashboard 发送一次心跳信息。发送心跳的目的主要是告诉 dashboard 我这台 sentinel 的实例还活着，你可以继续向我请求数据。
         *
         * 这也就是为什么 dashboard 中每个 app 对应的机器列表要用 Set 来保存的原因，如果用 List 来保存的话就可能存在同一台机器保存了多次的情况。
         * 心跳可以维持双方之间的连接是正常的，但是也有可能因为各种原因，某一方或者双方都离线了，那他们之间的连接就丢失了。
         */
        pool.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    sender.sendHeartbeat();
                } catch (Throwable e) {
                    RecordLog.warn("[HeartbeatSender] Send heartbeat error", e);
                }
            }
        }, 5000, interval, TimeUnit.MILLISECONDS);
        RecordLog.info("[HeartbeatSenderInit] HeartbeatSender started: "
            + sender.getClass().getCanonicalName());
    }
}
