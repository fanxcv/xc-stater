package fun.fan.xc.plugin.redis;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.util.Assert;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * @author fan
 */
@Slf4j
@RequiredArgsConstructor
@ConditionalOnBean(RedisTemplate.class)
public class SpringRedisImpl implements Redis {
    private final RedisTemplate<String, Object> template;

    private static final Long RELEASE_SUCCESS = 1L;

    @Override
    public boolean set(String key, Object value) {
        template.opsForValue().set(key, value);
        return true;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) template.opsForValue().get(key);
    }

    @Override
    public <T> T getOrLoad(String key, Supplier<T> load) {
        T value = get(key);
        return Optional.ofNullable(value).orElseGet(() -> {
            T v = load.get();
            Assert.notNull(v, "result of load is not be null");
            set(key, v);
            return v;
        });
    }

    @Override
    public <T> T getOrLoadEx(String key, int time, TimeUnit timeUnit, Supplier<T> load) {
        T value = get(key);
        return Optional.ofNullable(value).orElseGet(() -> {
            T v = load.get();
            Assert.notNull(v, "result of load is not be null");
            setEx(key, v, time, timeUnit);
            return v;
        });
    }

    @Override
    public boolean exists(String key) {
        final Boolean hasKey = template.hasKey(key);
        return hasKey != null && hasKey;
    }

    @Override
    public boolean expire(String key, int seconds) {
        final Boolean expire = template.expire(key, seconds, TimeUnit.SECONDS);
        return expire != null && expire;
    }

    @Override
    public boolean expire(String key, int time, TimeUnit timeUnit) {
        final Boolean expire = template.expire(key, time, timeUnit);
        return expire != null && expire;
    }

    @Override
    public long ttl(String key) {
        final Long expire = template.getExpire(key, TimeUnit.SECONDS);
        return expire == null ? 0 : expire;
    }

    @Override
    public boolean setNx(String key, Object value) {
        final Boolean res = template.opsForValue().setIfAbsent(key, value);
        return res != null && res;
    }

    @Override
    public boolean setEx(String key, Object value, int seconds) {
        template.opsForValue().set(key, value, seconds, TimeUnit.SECONDS);
        return true;
    }

    @Override
    public boolean setEx(String key, Object value, int time, TimeUnit timeUnit) {
        template.opsForValue().set(key, value, time, timeUnit);
        return true;
    }

    @Override
    public boolean setExNx(String key, Object value, long millisecond) {
        final Boolean res = template.opsForValue().setIfAbsent(key, value, millisecond, TimeUnit.MILLISECONDS);
        return res != null && res;
    }

    @Override
    public boolean setExNx(String key, Object value, long time, TimeUnit timeUnit) {
        final Boolean res = template.opsForValue().setIfAbsent(key, value, time, timeUnit);
        return res != null && res;
    }

    @Override
    public long decr(String key) {
        final Long decrement = template.opsForValue().decrement(key);
        return decrement == null ? 0 : decrement;
    }

    @Override
    public long incr(String key) {
        final Long increment = template.opsForValue().increment(key);
        return increment == null ? 0 : increment;
    }

    @Override
    public boolean hSet(String key, String field, Object value) {
        template.opsForHash().put(key, field, value);
        return true;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T hGet(String key, String field) {
        return (T) template.opsForHash().get(key, field);
    }

    @Override
    public boolean hSetNx(String key, String field, Object value) {
        return template.opsForHash().putIfAbsent(key, field, value);
    }

    @Override
    public boolean hmSet(String key, Map<String, Object> hash) {
        template.opsForHash().putAll(key, hash);
        return true;
    }

    @Override
    public boolean hExists(String key, String field) {
        return template.opsForHash().hasKey(key, field);
    }

    @Override
    public long hDel(String key, String... field) {
        Object[] array = Arrays.stream(field).toArray();
        return template.opsForHash().delete(key, array);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Map<String, T> hScan(String key, String pattern) {
        final Map<String, T> scanResult = Maps.newHashMap();
        try (Cursor<Map.Entry<Object, Object>> cursor = template.opsForHash().scan(key, ScanOptions.scanOptions()
                .count(Integer.MAX_VALUE)
                .match(pattern)
                .build())) {
            while (cursor.hasNext()) {
                final Map.Entry<Object, Object> entry = cursor.next();
                scanResult.put(String.valueOf(entry.getKey()), (T) entry.getValue());
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return scanResult;
    }

    @Override
    public <T> Map<String, T> hGetAll(String key) {
        return this.hScan(key, "*");
    }

    @Override
    public long rPush(String key, Object... string) {
        final Long len = template.opsForList().rightPushAll(key, string);
        return len == null ? 0 : len;
    }

    @Override
    public long lPush(String key, Object... string) {
        final Long len = template.opsForList().leftPushAll(key, string);
        return len == null ? 0 : len;
    }

    @Override
    public long lLen(String key) {
        final Long size = template.opsForList().size(key);
        return size == null ? 0 : size;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> List<T> lRange(String key, long start, long stop) {
        return (List<T>) template.opsForList().range(key, start, stop);
    }

    @Override
    public boolean lTrim(String key, long start, long stop) {
        template.opsForList().trim(key, start, stop);
        return true;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T lPop(String key) {
        return (T) template.opsForList().leftPop(key);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T rPop(String key) {
        return (T) template.opsForList().rightPop(key);
    }

    @Override
    public long del(String... key) {
        final Long len = template.delete(Arrays.asList(key));
        return len == null ? 0 : len;
    }

    @Override
    public long delByPattern(String pattern) {
        final Set<String> keys = scan(pattern);
        final Long len = template.delete(keys);
        return len == null ? 0 : len;
    }

    @Override
    public Set<String> scan(String pattern) {
        return template.execute((RedisCallback<Set<String>>) connection -> {
            Set<String> keysTmp = Sets.newHashSet();
            try (Cursor<byte[]> cursor = connection.keyCommands()
                    .scan(ScanOptions.scanOptions()
                            .count(Integer.MAX_VALUE)
                            .match(pattern)
                            .build())) {
                while (cursor.hasNext()) {
                    keysTmp.add(new String(cursor.next()));
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
            return keysTmp;
        });
    }

    @Override
    public <T> T exec(String script, Class<T> clazz, List<String> keys, Object... args) {
        return template.execute(RedisScript.of(script, clazz), keys, args);
    }

    @Override
    public boolean tryGetDistributedLock(String lockKey, String requestId, long millisecond) {
        final Boolean result = template.opsForValue().setIfAbsent(lockKey, requestId, millisecond, TimeUnit.MILLISECONDS);
        return result != null && result;
    }

    @Override
    public boolean releaseDistributedLock(String lockKey, String requestId) {
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
        Long result = template.execute(RedisScript.of(script, Long.class), Collections.singletonList(lockKey), requestId);
        return RELEASE_SUCCESS.equals(result);
    }
}
