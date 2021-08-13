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
package com.alibaba.csp.sentinel.slotchain;

import com.alibaba.csp.sentinel.EntryType;
import com.alibaba.csp.sentinel.util.AssertUtil;

/**
 * 资源是 Sentinel 的关键概念。它可以是 Java 应用程序中的任何内容，例如，由应用程序提供的服务，或由应用程序调用的其它服务，甚至可以是一段代码。
 * 只要通过 Sentinel API 定义的代码，就是资源，能够被 Sentinel 保护起来。大部分情况下，可以使用方法签名，URL，甚至服务名称作为资源名来标示资源。
 * 简单来说，资源就是 Sentinel 用来保护系统的一个媒介。
 *
 * 在 Sentinel 中具体表示资源的类是：ResourceWrapper ，他是一个抽象的包装类，包装了资源的 Name 和EntryType。
 * 他有两个实现类，分别是：StringResourceWrapper 和 MethodResourceWrapper
 *      StringResourceWrapper 顾名思义，是通过对一串字符串进行包装，是一个通用的资源包装类
 *      MethodResourceWrapper 是对方法调用的包装。
 *
 * A wrapper of resource name and type.
 *
 * @author qinan.qn
 * @author jialiang.linjl
 * @author Eric Zhao
 */
public abstract class ResourceWrapper {

    protected final String name;

    protected final EntryType entryType;
    protected final int resourceType;

    public ResourceWrapper(String name, EntryType entryType, int resourceType) {
        AssertUtil.notEmpty(name, "resource name cannot be empty");
        AssertUtil.notNull(entryType, "entryType cannot be null");
        this.name = name;
        this.entryType = entryType;
        this.resourceType = resourceType;
    }

    /**
     * Get the resource name.
     *
     * @return the resource name
     */
    public String getName() {
        return name;
    }

    /**
     * Get {@link EntryType} of this wrapper.
     *
     * @return {@link EntryType} of this wrapper.
     */
    public EntryType getEntryType() {
        return entryType;
    }

    /**
     * Get the classification of this resource.
     *
     * @return the classification of this resource
     * @since 1.7.0
     */
    public int getResourceType() {
        return resourceType;
    }

    /**
     * Get the beautified resource name to be showed.
     *
     * @return the beautified resource name
     */
    public abstract String getShowName();

    /**
     * Only {@link #getName()} is considered.
     */
    @Override
    public int hashCode() {
        return getName().hashCode();
    }

    /**
     * Only {@link #getName()} is considered.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ResourceWrapper) {
            ResourceWrapper rw = (ResourceWrapper)obj;
            return rw.getName().equals(getName());
        }
        return false;
    }
}
