package com.yu.yuaicodemother.utils;

import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.json.JSONUtil;

import java.util.Arrays;
import java.util.List;

/**
 * 缓存 key 生成工具类
 *
 * @author yupi
 */
public class CacheKeyUtils {

    /**
     * 根据对象生成缓存key (JSON + MD5)
     *
     * @param obj 要生成key的对象
     * @return MD5哈希后的缓存key
     */
    public static String generateKey(Object obj) {
        if (obj == null) {
            return DigestUtil.md5Hex("null");
        }
        // 先转JSON，再MD5
        String jsonStr = JSONUtil.toJsonStr(obj);
        return DigestUtil.md5Hex(jsonStr);
    }


    /**
     * 根据两个对象生成组合缓存key (JSON + MD5)
     * 适用于 @Cacheable(key = "T(YourUtils).generateKey(#obj1, #obj2)")
     */
    public static String generateKey(Object obj1, Object obj2) {
        // 将两个对象放入列表，保证顺序和结构的固定
        // Arrays.asList 允许元素为 null
        List<Object> keys = Arrays.asList(obj1, obj2);

        // 转为 JSON 数组字符串，例如: ["requestData", "ADMIN"]
        String jsonStr = JSONUtil.toJsonStr(keys);

        // 生成 MD5
        return DigestUtil.md5Hex(jsonStr);
    }

}
