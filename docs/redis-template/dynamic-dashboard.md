
### 动态配置（整合camellia-dashboard）
* CamelliaRedisTemplate支持动态修改ResourceTable而不需要重新初始化新的CamelliaRedisTemplate实例，原理是CamelliaRedisTemplate将ResourceTable的配置托管给camellia-dashboard，CamelliaRedisTemplate会定时检查ResourceTable是否有变更     
* camellia-dashboard支持管理多组ResourceTable配置，CamelliaRedisTemplate使用bid/bgroup来指定需要使用哪组配置，如下：  
```java
public class TestCamelliaDashboard {

    public static void test() {
        String dashboardUrl = "http://127.0.0.1:8080";//dashboard地址
        long bid = 1;
        String bgroup = "default";
        boolean monitorEnable = true;//是否上报监控数据到dashboard
        long checkIntervalMillis = 5000;//检查resourceTable的间隔

        CamelliaRedisEnv redisEnv = CamelliaRedisEnv.defaultRedisEnv();

        CamelliaRedisTemplate template = new CamelliaRedisTemplate(redisEnv, dashboardUrl, bid, bgroup, monitorEnable, checkIntervalMillis);
        String k1 = template.get("k1");
        System.out.println(k1);

        //如果要同时管理多组bid/bgroup，你可以使用CamelliaRedisTemplateManager
        CamelliaRedisTemplateManager manager = new CamelliaRedisTemplateManager(dashboardUrl);
        CamelliaRedisTemplate redisTemplate1 = manager.getRedisTemplate(1L, "default");
        String k2 = redisTemplate1.get("k2");
        System.out.println(k2);
        CamelliaRedisTemplate redisTemplate2 = manager.getRedisTemplate(2L, "default");
        String k3 = redisTemplate2.get("k3");
        System.out.println(k3);
    }

    public static void main(String[] args) {
        test();
    }
}
```