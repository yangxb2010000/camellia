package com.netease.nim.camellia.redis.proxy.monitor;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.util.ExecutorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 *
 * Created by caojiajun on 2020/12/1
 */
public class SlowCommandMonitor {

    private static final Logger logger = LoggerFactory.getLogger(SlowCommandMonitor.class);

    private static final int defaultMaxCount = 100;
    private static final LinkedBlockingQueue<SlowCommandStats> queue;
    static {
        queue = new LinkedBlockingQueue<>(ProxyDynamicConf.getInt("slow.command.monitor.json.max.count", defaultMaxCount) * 10);
    }

    public static void init(int seconds) {
        ExecutorUtils.scheduleAtFixedRate(SlowCommandMonitor::calc, seconds, seconds, TimeUnit.SECONDS);
    }

    public static void slowCommand(Command command, double spendMillis, long thresholdMillis) {
        try {
            if (queue.size() >= ProxyDynamicConf.getInt("slow.command.monitor.json.max.count", defaultMaxCount)) {
                return;
            }
            Long bid = command.getCommandContext().getBid();
            String bgroup = command.getCommandContext().getBgroup();
            SlowCommandStats slowCommandStats = new SlowCommandStats();
            slowCommandStats.bid = bid == null ? "default" : String.valueOf(bid);
            slowCommandStats.bgroup = bgroup == null ? "default" : bgroup;
            slowCommandStats.command = command.getName();
            slowCommandStats.keys = command.getKeysStr();
            slowCommandStats.spendMillis = spendMillis;
            slowCommandStats.thresholdMillis = thresholdMillis;
            queue.offer(slowCommandStats);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    private static JSONObject monitorJson = new JSONObject();

    private static void calc() {
        try {
            JSONObject json = new JSONObject();
            JSONArray slowCommandJsonArray = new JSONArray();
            while (!queue.isEmpty()) {
                SlowCommandStats slowCommandStats = queue.poll();
                JSONObject slowCommandJson = new JSONObject();
                slowCommandJson.put("bid", slowCommandStats.bid);
                slowCommandJson.put("bgroup", slowCommandStats.bgroup);
                slowCommandJson.put("command", slowCommandStats.command);
                slowCommandJson.put("keys", slowCommandStats.keys);
                slowCommandJson.put("spendMillis", slowCommandStats.spendMillis);
                slowCommandJson.put("thresholdMillis", slowCommandStats.thresholdMillis);
                slowCommandJsonArray.add(slowCommandJson);
            }
            json.put("slowCommandStats", slowCommandJsonArray);
            monitorJson = json;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    public static JSONObject getSlowCommandStatsJson() {
        return monitorJson;
    }

    private static class SlowCommandStats {
        String bid;
        String bgroup;
        String command;
        String keys;
        double spendMillis;
        long thresholdMillis;
    }
}
