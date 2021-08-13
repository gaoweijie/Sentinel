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
package com.alibaba.csp.sentinel.command.handler;

import com.alibaba.csp.sentinel.command.CommandHandler;
import com.alibaba.csp.sentinel.command.CommandRequest;
import com.alibaba.csp.sentinel.command.CommandResponse;
import com.alibaba.csp.sentinel.command.annotation.CommandMapping;
import com.alibaba.csp.sentinel.slots.block.authority.AuthorityRuleManager;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import com.alibaba.csp.sentinel.slots.system.SystemRuleManager;
import com.alibaba.fastjson.JSON;

/**
 * 该接口对外提供的服务为：http://ip:8719/getRules?type=xxx
 * 这是查询规则的接口：会返回现有生效的规则
 * 类似的查询规则接口为{@link ModifyRulesCommandHandler}
 *
 * @author jialiang.linjl
 */
@CommandMapping(name = "getRules", desc = "get all active rules by type, request param: type={ruleType}")
public class FetchActiveRuleCommandHandler implements CommandHandler<String> {

    @Override
    public CommandResponse<String> handle(CommandRequest request) {
        String type = request.getParam("type");
        // 目前只支持 流控规则、降级规则、授权认证规则、系统保护规则
        if ("flow".equalsIgnoreCase(type)) {
            // flow 以JSON格式返回现有的限流规则；
            return CommandResponse.ofSuccess(JSON.toJSONString(FlowRuleManager.getRules()));
        } else if ("degrade".equalsIgnoreCase(type)) {
            // degrade 则返回现有生效的降级规则列表；
            return CommandResponse.ofSuccess(JSON.toJSONString(DegradeRuleManager.getRules()));
        } else if ("authority".equalsIgnoreCase(type)) {
            return CommandResponse.ofSuccess(JSON.toJSONString(AuthorityRuleManager.getRules()));
        } else if ("system".equalsIgnoreCase(type)) {
            // system 则返回系统保护规则
            return CommandResponse.ofSuccess(JSON.toJSONString(SystemRuleManager.getRules()));
        } else {
            return CommandResponse.ofFailure(new IllegalArgumentException("invalid type"));
        }
    }

}
