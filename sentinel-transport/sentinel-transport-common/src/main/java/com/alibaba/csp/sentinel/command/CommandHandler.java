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
package com.alibaba.csp.sentinel.command;

/**
 * 该接口主要是用来处理接收到的请求的，不同的请求有不同的 handler 类来进行处理，我们可以实现我们自己的 CommandHandler
 * 并注册到 SPI 配置文件中来为 CommandCenter 添加自定义的命令。
 *
 * CommandCenter启动后，就等待dashboard发送消息过来了，当接收到消息后，会把消息通过具体的CommandHandler进行处理，
 * 然后将处理的结果返回给dashboard。
 * 这里需要注意的是，dashboard 给 client 发送消息是通过异步的 httpClient 进行发送的，在 HttpHelper 类中。
 *
 * 提供实际服务的是一些 CommandHandler 的实现类，每个类提供了一种能力，这些类是在 sentinel-transport-common 依赖中提供的，例如请参看
 * FetchActiveRuleCommandHandler的注释
 *
 * Represent a handler that handles a {@link CommandRequest}.
 *
 * @author Eric Zhao
 */
public interface CommandHandler<R> {

    /**
     * Handle the given Courier command request.
     *
     * @param request the request to handle
     * @return the response
     */
    CommandResponse<R> handle(CommandRequest request);
}
