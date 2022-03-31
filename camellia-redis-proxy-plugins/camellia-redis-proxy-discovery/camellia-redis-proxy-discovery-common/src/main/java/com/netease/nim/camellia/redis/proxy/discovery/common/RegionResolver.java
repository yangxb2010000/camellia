package com.netease.nim.camellia.redis.proxy.discovery.common;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 * Created by caojiajun on 2020/12/16
 */
public interface RegionResolver {

    static final String DEFAULT_REGION = "default";

    String resolve(String host);

    public static class DummyRegionResolver implements RegionResolver {

        @Override
        public String resolve(String host) {
            return DEFAULT_REGION;
        }
    }

    public static class IpSegmentRegionResolver implements RegionResolver {

        private final List<IpSegmentRegion> list = new ArrayList<>();
        private final String defaultRegion;

        /**
         *
         * @param configStr 10.189.0.0/20:region1,10.189.208.0/21:region2
         * @param defaultRegion default region if not match
         */
        public IpSegmentRegionResolver(String configStr, String defaultRegion) {
            if (configStr != null && configStr.length() > 0) {
                String[] split = configStr.split(",");
                if (split.length > 0) {
                    for (String str : split) {
                        String[] split1 = str.split(":");
                        String ipWithMask = split1[0];
                        String region = split1[1];
                        String[] split3 = ipWithMask.split("/");
                        String ip = split3[0];
                        String mask = split3[1];
                        IPMatcher matcher = new IPMatcher(ip, mask);
                        list.add(new IpSegmentRegion(matcher, region));
                    }
                }
            }
            if (defaultRegion != null && defaultRegion.length() > 0) {
                this.defaultRegion = defaultRegion;
            } else {
                this.defaultRegion = DEFAULT_REGION;
            }
        }

        /**
         * @param config key=ip-mask, value=region
         *         config.put("10.189.0.0/20", "REGION1");
         *         config.put("10.189.208.0/21", "REGION2");
         * @param defaultRegion default region if not match
         */
        public IpSegmentRegionResolver(Map<String, String> config, String defaultRegion) {
            if (config != null) {
                for (Map.Entry<String, String> entry : config.entrySet()) {
                    String ipWithMask = entry.getKey();
                    String region = entry.getValue();
                    String[] split = ipWithMask.split("/");
                    String ip = split[0];
                    String mask = split[1];
                    IPMatcher matcher = new IPMatcher(ip, mask);
                    list.add(new IpSegmentRegion(matcher, region));
                }
            }
            if (defaultRegion != null && defaultRegion.trim().length() > 0) {
                this.defaultRegion = defaultRegion;
            } else {
                this.defaultRegion = DEFAULT_REGION;
            }
        }

        private static class IpSegmentRegion {
            private final IPMatcher ipMatcher;
            private final String region;

            public IpSegmentRegion(IPMatcher ipMatcher, String region) {
                this.ipMatcher = ipMatcher;
                this.region = region;
            }
        }

        @Override
        public String resolve(String host) {
            try {
                for (IpSegmentRegion item : list) {
                    if (item.ipMatcher.match(host)) {
                        return item.region;
                    }
                }
                return defaultRegion;
            } catch (Exception e) {
                return defaultRegion;
            }
        }
    }
}
