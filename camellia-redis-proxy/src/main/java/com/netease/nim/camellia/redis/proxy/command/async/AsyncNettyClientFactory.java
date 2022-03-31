package com.netease.nim.camellia.redis.proxy.command.async;

import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.redis.exception.CamelliaRedisException;
import com.netease.nim.camellia.redis.proxy.conf.Constants;
import com.netease.nim.camellia.redis.resource.*;

import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * Created by caojiajun on 2019/12/19.
 */
public interface AsyncNettyClientFactory {

    AsyncClient get(String url);

    public static AsyncNettyClientFactory DEFAULT = new Default();

    public static class Default implements AsyncNettyClientFactory {

        private final Object lock = new Object();
        private final ConcurrentHashMap<String, AsyncClient> map = new ConcurrentHashMap<>();
        private int maxAttempts = Constants.Transpond.redisClusterMaxAttempts;

        public Default() {
        }

        public Default(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }

        public AsyncClient get(RedisResource redisResource) {
            AsyncClient client = map.get(redisResource.getUrl());
            if (client == null) {
                client = map.computeIfAbsent(redisResource.getUrl(),
                        k -> new AsyncCamelliaRedisClient(redisResource));
            }
            return client;
        }

        public AsyncClient get(RedisClusterResource redisClusterResource) {
            AsyncClient client = map.get(redisClusterResource.getUrl());
            if (client == null) {
                client = map.computeIfAbsent(redisClusterResource.getUrl(),
                        k -> new AsyncCamelliaRedisClusterClient(redisClusterResource, maxAttempts));
            }
            return client;
        }

        public AsyncClient get(RedisClusterSlavesResource redisClusterSlavesResource) {
            AsyncClient client = map.get(redisClusterSlavesResource.getUrl());
            if (client == null) {
                client = map.computeIfAbsent(redisClusterSlavesResource.getUrl(),
                        k -> new AsyncCamelliaRedisClusterClient(redisClusterSlavesResource, maxAttempts));
            }
            return client;
        }

        public AsyncClient get(RedisSentinelResource redisSentinelResource) {
            AsyncClient client = map.get(redisSentinelResource.getUrl());
            if (client == null) {
                client = map.computeIfAbsent(redisSentinelResource.getUrl(),
                        k -> new AsyncCamelliaRedisSentinelClient(redisSentinelResource));
            }
            return client;
        }

        public AsyncClient get(RedisSentinelSlavesResource redisSentinelSlavesResource) {
            AsyncClient client = map.get(redisSentinelSlavesResource.getUrl());
            if (client == null) {
                client = map.computeIfAbsent(redisSentinelSlavesResource.getUrl(),
                        k -> new AsyncCamelliaRedisSentinelSlavesClient(redisSentinelSlavesResource));
            }
            return client;
        }

        public AsyncClient get(RedisProxiesResource redisProxiesResource) {
            AsyncClient client = map.get(redisProxiesResource.getUrl());
            if (client == null) {
                client = map.computeIfAbsent(redisProxiesResource.getUrl(),
                        k -> new AsyncCameliaRedisProxiesClient(redisProxiesResource));
            }
            return client;
        }

        @Override
        public AsyncClient get(String url) {
            AsyncClient client = map.get(url);
            if (client == null) {
                synchronized (lock) {
                    client = map.get(url);
                    if (client == null) {
                        Resource resource = RedisResourceUtil.parseResourceByUrl(new Resource(url));
                        if (resource instanceof RedisResource) {
                            client = get((RedisResource) resource);
                        } else if (resource instanceof RedisClusterResource) {
                            client = get((RedisClusterResource) resource);
                        } else if (resource instanceof RedisSentinelResource) {
                            client = get((RedisSentinelResource) resource);
                        } else if (resource instanceof RedisSentinelSlavesResource) {
                            client = get((RedisSentinelSlavesResource) resource);
                        } else if (resource instanceof RedisClusterSlavesResource) {
                            client = get((RedisClusterSlavesResource) resource);
                        } else if (resource instanceof RedisProxiesResource) {
                            client = get((RedisProxiesResource) resource);
                        } else {
                            throw new CamelliaRedisException("not support resource");
                        }
                    }
                }
            }
            return client;
        }
    }
}
