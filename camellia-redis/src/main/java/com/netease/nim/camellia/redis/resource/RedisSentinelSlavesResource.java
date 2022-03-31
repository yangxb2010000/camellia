package com.netease.nim.camellia.redis.resource;

import com.netease.nim.camellia.core.model.Resource;

import java.util.List;

/**
 * 格式如下：
 * 1、没有密码
 * redis-sentinel-slaves://@host:port,host:port,host:port/masterName?withMaster=false
 * 2、有密码
 * redis-sentinel-slaves://password@host:port,host:port,host:port/masterName?withMaster=false
 * 3、有密码且有账号
 * redis-sentinel-slaves://username:password@host:port,host:port,host:port/masterName?withMaster=false
 *
 * only for read
 *
 * Created by caojiajun on 2021/4/7
 */
public class RedisSentinelSlavesResource extends Resource {

    private final String master;
    private final List<RedisSentinelResource.Node> nodes;
    private final String password;
    private final boolean withMaster;
    private final String userName;

    public RedisSentinelSlavesResource(String master, List<RedisSentinelResource.Node> nodes, String userName, String password, boolean withMaster) {
        this.master = master;
        this.nodes = nodes;
        this.password = password;
        this.withMaster = withMaster;
        this.userName = userName;
        StringBuilder url = new StringBuilder();
        url.append(RedisType.RedisSentinelSlaves.getPrefix());
        if (userName != null && password != null) {
            url.append(userName).append(":").append(password);
        } else if (userName == null && password != null) {
            url.append(password);
        }
        url.append("@");
        for (RedisSentinelResource.Node node : nodes) {
            url.append(node.getHost()).append(":").append(node.getPort());
            url.append(",");
        }
        url.deleteCharAt(url.length() - 1);
        url.append("/");
        url.append(master);
        url.append("?withMaster=").append(withMaster);
        this.setUrl(url.toString());
    }

    public RedisSentinelSlavesResource(String master, List<RedisSentinelResource.Node> nodes, String password, boolean withMaster) {
        this(master, nodes, null, password, withMaster);
    }

    public String getMaster() {
        return master;
    }

    public List<RedisSentinelResource.Node> getNodes() {
        return nodes;
    }

    public String getPassword() {
        return password;
    }

    public boolean isWithMaster() {
        return withMaster;
    }

    public String getUserName() {
        return userName;
    }
}
