package io.dynamic.threadpool.starter.core;

import io.dynamic.threadpool.starter.common.Constants;
import io.dynamic.threadpool.starter.listener.Listener;
import io.dynamic.threadpool.starter.wrap.ManagerListenerWrap;
import io.dynamict.hreadpool.common.toolkit.Md5Util;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CopyOnWriteArrayList;

/**
 * CacheData.
 *
 * @author chen.ma
 * @date 2021/6/22 20:46
 */
@Slf4j
public class CacheData {

    private volatile String md5;

    private volatile String content;

    public final String tpId;

    private int taskId;

    private volatile long localConfigLastModified;

    private final CopyOnWriteArrayList<ManagerListenerWrap> listeners;

    public CacheData(String tpId) {
        this.tpId = tpId;
        // TODO：nacos 走的本地文件获取, 这里思考下如何优雅获取
        this.content = null;
        this.md5 = getMd5String(content);
        this.listeners = new CopyOnWriteArrayList();

    }

    public void addListener(Listener listener) {
        if (null == listener) {
            throw new IllegalArgumentException("listener is null");
        }

        ManagerListenerWrap managerListenerWrap = new ManagerListenerWrap(md5, listener);

        if (listeners.addIfAbsent(managerListenerWrap)) {
            log.info("[add-listener] ok, tpId :: {}, cnt :: {}", tpId, listeners.size());
        }
    }

    public void checkListenerMd5() {
        for (ManagerListenerWrap wrap : listeners) {
            if (!md5.equals(wrap.getLastCallMd5())) {
                safeNotifyListener(content, md5, wrap);
            }
        }
    }

    private void safeNotifyListener(String content, String md5, ManagerListenerWrap wrap) {
        Listener listener = wrap.getListener();

        Runnable runnable = () -> {
            wrap.setLastCallMd5(md5);

            listener.receiveConfigInfo(content);
        };

        listener.getExecutor().execute(runnable);
    }

    public void setContent(String content) {
        this.content = content;
        this.md5 = getMd5String(this.content);
    }

    public static String getMd5String(String config) {
        return (null == config) ? Constants.NULL : Md5Util.md5Hex(config, Constants.ENCODE);
    }

    public String getMd5() {
        return this.md5;
    }
}