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
package com.alibaba.csp.sentinel;

import com.alibaba.csp.sentinel.init.InitExecutor;

/**
 * 触发客户端连接控制台的入口类：
 *
 * 客户端与控制台的连接初始化是在 Env 的类中触发的，客户端配置好了与控制台的连接参数之后，并不会主动连接上控制台，
 * 需要触发一次客户端的规则才会开始进行初始化，并向控制台发送心跳和客户端规则等信息。
 *
 * Sentinel Env. This class will trigger all initialization for Sentinel.
 *
 * <p>
 * NOTE: to prevent deadlocks, other classes' static code block or static field should
 * NEVER refer to this class.
 * </p>
 *
 * @author jialiang.linjl
 */
public class Env {

    public static final Sph sph = new CtSph();

    static {
        // If init fails, the process will exit.
        InitExecutor.doInit();
    }

}
