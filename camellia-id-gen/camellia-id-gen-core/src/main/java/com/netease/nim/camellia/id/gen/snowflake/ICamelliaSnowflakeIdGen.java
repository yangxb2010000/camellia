package com.netease.nim.camellia.id.gen.snowflake;

/**
 * 雪花算法
 * Created by caojiajun on 2021/9/24
 */
public interface ICamelliaSnowflakeIdGen {

    /**
     * 生成一个id
     * @return id
     */
    long genId();

    /**
     * 解析id中的时间戳
     * @param id id
     * @return 时间戳
     */
    long decodeTs(long id);

    /**
     * 解析id中的regionId
     * @param id id
     * @return regionId
     */
    long decodeRegionId(long id);

    /**
     * 解析id中的workerId
     * @param id id
     * @return workerId
     */
    long decodeWorkerId(long id);

    /**
     * 解析id中的sequence
     * @param id id
     * @return sequence
     */
    long decodeSequence(long id);
}
