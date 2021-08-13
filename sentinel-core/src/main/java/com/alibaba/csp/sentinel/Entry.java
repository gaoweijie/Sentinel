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

import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.util.TimeUtil;
import com.alibaba.csp.sentinel.util.function.BiConsumer;
import com.alibaba.csp.sentinel.context.ContextUtil;
import com.alibaba.csp.sentinel.node.Node;
import com.alibaba.csp.sentinel.slotchain.ResourceWrapper;
import com.alibaba.csp.sentinel.context.Context;

/**
 * Entry 是 Sentinel 中用来表示是否通过限流的一个凭证，就像一个token一样。
 * 每次执行 SphU.entry() 或 SphO.entry() 都会返回一个 Entry 给调用者，意思就是告诉调用者：
 * 如果正确返回了 Entry 给你，那表示你可以正常访问被 Sentinel 保护的后方服务了，
 * 否则 Sentinel 会抛出一个BlockException(如果是 SphO.entry() 会返回false)，这就表示调用者想要访问的服务被保护了，也就是说调用者本身被限流了。
 *
 * 当在一个上下文中多次调用了 SphU.entry() 方法时，就会创建一个调用树，这个树的节点之间是通过parent和child关系维持的。
 * 需要注意的是：parent和child是在 CtSph 类的一个私有内部类 CtEntry 中定义的，CtEntry 是 Entry 的一个子类。 由于context中总是保存着调用链树中的当前入口，所以当当前entry执行exit退出时，需要将parent设置为当前入口。
 *
 * Each {@link SphU}#entry() will return an {@link Entry}. This class holds information of current invocation:<br/>
 *
 * <ul>
 * <li>createTime, the create time of this entry, using for rt statistics.</li>
 * <li>current {@link Node}, that is statistics of the resource in current context.</li>
 * <li>origin {@link Node}, that is statistics for the specific origin. Usually the
 * origin could be the Service Consumer's app name, see
 * {@link ContextUtil#enter(String name, String origin)} </li>
 * <li>{@link ResourceWrapper}, that is resource name.</li>
 * <br/>
 * </ul>
 *
 * <p>
 * A invocation tree will be created if we invoke SphU#entry() multi times in the same {@link Context},
 * so parent or child entry may be held by this to form the tree. Since {@link Context} always holds
 * the current entry in the invocation tree, every {@link Entry#exit()} call should modify
 * {@link Context#setCurEntry(Entry)} as parent entry of this.
 * </p>
 *
 * @author qinan.qn
 * @author jialiang.linjl
 * @author leyou(lihao)
 * @author Eric Zhao
 * @see SphU
 * @see Context
 * @see ContextUtil
 */
public abstract class Entry implements AutoCloseable {

    private static final Object[] OBJECTS0 = new Object[0];

    // 当前Entry的创建时间，主要用来后期计算rt
    private final long createTimestamp;
    // 当前Entry的完成时间，主要用来后期计算rt
    private long completeTimestamp;

    // 当前Entry所关联的node，该node主要是记录了当前context下该资源的统计信息
    private Node curNode;
    /**
     * 当前Entry的调用来源，通常是调用方的应用名称，在 ClusterBuilderSlot.entry() 方法中设置的
     * {@link Node} of the specific origin, Usually the origin is the Service Consumer.
     */
    private Node originNode;

    private Throwable error;
    private BlockException blockError;

    // 当前Entry所关联的资源
    protected final ResourceWrapper resourceWrapper;

    public Entry(ResourceWrapper resourceWrapper) {
        this.resourceWrapper = resourceWrapper;
        this.createTimestamp = TimeUtil.currentTimeMillis();
    }

    public ResourceWrapper getResourceWrapper() {
        return resourceWrapper;
    }

    /**
     * Complete the current resource entry and restore the entry stack in context.
     *
     * @throws ErrorEntryFreeException if entry in current context does not match current entry
     */
    public void exit() throws ErrorEntryFreeException {
        exit(1, OBJECTS0);
    }

    public void exit(int count) throws ErrorEntryFreeException {
        exit(count, OBJECTS0);
    }

    /**
     * Equivalent to {@link #exit()}. Support try-with-resources since JDK 1.7.
     *
     * @since 1.5.0
     */
    @Override
    public void close() {
        exit();
    }

    /**
     * Exit this entry. This method should invoke if and only if once at the end of the resource protection.
     *
     * @param count tokens to release.
     * @param args extra parameters
     * @throws ErrorEntryFreeException, if {@link Context#getCurEntry()} is not this entry.
     */
    public abstract void exit(int count, Object... args) throws ErrorEntryFreeException;

    /**
     * Exit this entry.
     *
     * @param count tokens to release.
     * @param args extra parameters
     * @return next available entry after exit, that is the parent entry.
     * @throws ErrorEntryFreeException, if {@link Context#getCurEntry()} is not this entry.
     */
    protected abstract Entry trueExit(int count, Object... args) throws ErrorEntryFreeException;

    /**
     * Get related {@link Node} of the parent {@link Entry}.
     *
     * @return
     */
    public abstract Node getLastNode();

    public long getCreateTimestamp() {
        return createTimestamp;
    }

    public long getCompleteTimestamp() {
        return completeTimestamp;
    }

    public Entry setCompleteTimestamp(long completeTimestamp) {
        this.completeTimestamp = completeTimestamp;
        return this;
    }

    public Node getCurNode() {
        return curNode;
    }

    public void setCurNode(Node node) {
        this.curNode = node;
    }

    public BlockException getBlockError() {
        return blockError;
    }

    public Entry setBlockError(BlockException blockError) {
        this.blockError = blockError;
        return this;
    }

    public Throwable getError() {
        return error;
    }

    public void setError(Throwable error) {
        this.error = error;
    }

    /**
     * Get origin {@link Node} of the this {@link Entry}.
     *
     * @return origin {@link Node} of the this {@link Entry}, may be null if no origin specified by
     * {@link ContextUtil#enter(String name, String origin)}.
     */
    public Node getOriginNode() {
        return originNode;
    }

    public void setOriginNode(Node originNode) {
        this.originNode = originNode;
    }

    /**
     * Like {@code CompletableFuture} since JDK 8, it guarantees specified handler
     * is invoked when this entry terminated (exited), no matter it's blocked or permitted.
     * Use it when you did some STATEFUL operations on entries.
     * 
     * @param handler handler function on the invocation terminates
     * @since 1.8.0
     */
    public abstract void whenTerminate(BiConsumer<Context, Entry> handler);
    
}
