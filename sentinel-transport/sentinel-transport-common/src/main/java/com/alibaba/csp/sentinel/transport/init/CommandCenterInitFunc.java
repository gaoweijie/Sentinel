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

import com.alibaba.csp.sentinel.command.CommandCenterProvider;
import com.alibaba.csp.sentinel.init.InitFunc;
import com.alibaba.csp.sentinel.init.InitOrder;
import com.alibaba.csp.sentinel.log.RecordLog;
import com.alibaba.csp.sentinel.transport.CommandCenter;

/**
 * client在启动时，会通过 CommandCenterInitFunc 选择一个，并且只选择一个 CommandCenter 进行启动。
 *
 * @author Eric Zhao
 */
@InitOrder(-1)
public class CommandCenterInitFunc implements InitFunc {

    @Override
    public void init() throws Exception {
        /**
         * 启动之前会通过spi的方式扫描获取到所有的 CommandHandler 的实现类，然后将所有的 CommandHandler 注册到一个 HashMap 中去，待后期使用。
         *
         * PS：考虑一下，为什么 CommandHandler 不需要做持久化，而是直接保存在内存中。
         */
        CommandCenter commandCenter = CommandCenterProvider.getCommandCenter();

        if (commandCenter == null) {
            RecordLog.warn("[CommandCenterInitFunc] Cannot resolve CommandCenter");
            return;
        }

        /**
         * 注册完 CommandHandler 之后，紧接着就启动 CommandCenter 了，目前 CommandCenter 有三个实现类：
         *
         * SimpleHttpCommandCenter 通过ServerSocket启动一个服务端，接受socket连接
         * NettyHttpCommandCenter 通过Netty启动一个服务端，接受channel连接
         * SpringMvcHttpCommandCenter TODO：
         */
        commandCenter.beforeStart();
        commandCenter.start();
        RecordLog.info("[CommandCenterInit] Starting command center: "
                + commandCenter.getClass().getCanonicalName());
        /**
         * CommandCenter启动后，就等待dashboard发送消息过来了，当接收到消息后，会把消息通过具体的CommandHandler进行处理，
         * 然后将处理的结果返回给dashboard。
         */
    }
}
