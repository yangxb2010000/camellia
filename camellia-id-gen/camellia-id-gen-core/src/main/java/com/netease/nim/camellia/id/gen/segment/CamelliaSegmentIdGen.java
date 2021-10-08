package com.netease.nim.camellia.id.gen.segment;


import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import com.netease.nim.camellia.id.gen.common.CamelliaIdGenException;
import com.netease.nim.camellia.id.gen.common.IDLoader;
import com.netease.nim.camellia.id.gen.common.IDRange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by caojiajun on 2021/9/24
 */
public class CamelliaSegmentIdGen extends AbstractCamelliaSegmentIdGen {

    private static final Logger logger = LoggerFactory.getLogger(CamelliaSegmentIdGen.class);

    private final IDLoader idLoader;
    private final int regionBits;
    private final long regionId;

    public CamelliaSegmentIdGen(CamelliaSegmentIdGenConfig config) {
        this.idLoader = config.getIdLoader();
        if (idLoader == null) {
            throw new CamelliaIdGenException("idLoader not found");
        }
        this.regionBits = config.getRegionBits();
        this.regionId = config.getRegionId();

        this.step = config.getStep();
        this.cacheMaxCapacity = step * 10;
        this.maxRetry = config.getMaxRetry();
        this.retryIntervalMillis = config.getRetryIntervalMillis();

        this.cacheMap = new ConcurrentLinkedHashMap.Builder<String, LinkedBlockingQueue<Long>>()
                .initialCapacity(config.getTagCount()).maximumWeightedCapacity(config.getTagCount()).build();
        this.lockMap = new ConcurrentLinkedHashMap.Builder<String, AtomicBoolean>()
                .initialCapacity(config.getTagCount() * 2).maximumWeightedCapacity(config.getTagCount() * 2).build();
        this.asyncLoadThreadPool = config.getAsyncLoadThreadPool();

        if (this.regionBits < 0) {
            throw new CamelliaIdGenException("regionBits should >= 0");
        }
        long maxRegionId = (1L << config.getRegionBits()) - 1;
        if (this.regionId > maxRegionId) {
            throw new CamelliaIdGenException("regionId too long");
        }

        logger.info("CamelliaSegmentIdGen init success, regionId = {}, regionBits = {}, step = {}, maxRetry = {}, retryIntervalMillis = {}",
                regionId, regionBits, step, maxRetry, retryIntervalMillis);
    }

    @Override
    protected void loadCache(LinkedBlockingQueue<Long> cache, String tag, int loadCount) {
        try {
            IDRange load = idLoader.load(tag, loadCount);
            for (long i=load.getStart(); i<=load.getEnd(); i++) {
                long id = (i << regionBits) | regionId;
                cache.offer(id);
            }
            if (logger.isDebugEnabled()) {
                logger.debug("load ids from idLoader success, tag = {}, start = {}, end = {}", tag, load.getStart(), load.getEnd());
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new CamelliaIdGenException("load ids from idLoader error", e);
        }
    }
}
