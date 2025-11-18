package fun.fan.xc.plugin.proxy.mock;

import fun.fan.xc.plugin.proxy.mock.dto.MockData;
import fun.fan.xc.starter.exception.XcServiceException;
import fun.fan.xc.starter.utils.NetUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.StringUtils;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Proxy Mock数据加载器
 * 支持从文件或远程URL加载YAML格式的mock配置
 *
 * @author fan
 */
@Slf4j
public class ProxyMockDataLoader {

    private static final String HTTPS_PREFIX = "https:";
    private static final String HTTP_PREFIX = "http:";
    private static final String FILE_PREFIX = "file:";
    private static final String URL_PREFIX = "url:";
    private static final int HTTP_TIMEOUT_SECONDS = 10;

    private final ResourceLoader resourceLoader;

    private final Yaml yaml;
    private final ConcurrentHashMap<String, MockData> cache = new ConcurrentHashMap<>();
    private final ReentrantReadWriteLock cacheLock = new ReentrantReadWriteLock();

    public ProxyMockDataLoader(ResourceLoader resourceLoader) {
        Constructor constructor = new Constructor(MockData.class);
        // 创建支持Unicode的YAML解析器
        this.resourceLoader = resourceLoader;
        this.yaml = new Yaml(constructor);
    }

    /**
     * 加载Mock数据
     *
     * @param dataSource 数据源地址，支持file:或url:前缀
     * @return Mock数据配置
     */
    public MockData loadMockData(String dataSource) {
        if (!StringUtils.hasText(dataSource)) {
            throw new XcServiceException("Mock数据源不能为空");
        }

        // 尝试从缓存获取
        cacheLock.readLock().lock();
        try {
            MockData cached = cache.get(dataSource);
            if (cached != null) {
                log.debug("从缓存加载Mock数据: {}", dataSource);
                return cached;
            }
        } finally {
            cacheLock.readLock().unlock();
        }

        // 加载并缓存数据
        cacheLock.writeLock().lock();
        try {
            // 双重检查，防止并发重复加载
            MockData cached = cache.get(dataSource);
            if (cached != null) {
                return cached;
            }

            MockData mockData = loadFromSource(dataSource);
            cache.put(dataSource, mockData);
            log.info("成功加载Mock数据: {}, 配置项数量: {}", dataSource,
                    mockData.getMocks() != null ? mockData.getMocks().size() : 0);
            return mockData;
        } finally {
            cacheLock.writeLock().unlock();
        }
    }

    /**
     * 从数据源加载Mock数据
     */
    private MockData loadFromSource(String dataSource) {
        try {
            if (dataSource.startsWith(URL_PREFIX)) {
                return loadFromUrl(dataSource.substring(URL_PREFIX.length()));
            } else if (dataSource.startsWith(HTTPS_PREFIX) || dataSource.startsWith(HTTP_PREFIX)) {
                return loadFromUrl(dataSource);
            } else if (dataSource.startsWith(FILE_PREFIX)) {
                return loadFromFile(dataSource.substring(FILE_PREFIX.length()));
            } else {
                return loadFromFile(dataSource);
            }
        } catch (Exception e) {
            log.error("加载Mock数据失败: {}", dataSource, e);
            throw new XcServiceException("加载Mock数据失败: " + dataSource, e);
        }
    }

    /**
     * 从文件加载Mock数据
     */
    private MockData loadFromFile(String filePath) throws IOException {
        log.debug("从文件加载Mock数据: {}", filePath);

        Resource resource = null;

        // 尝试多种路径格式
        if (filePath.startsWith("classpath:")) {
            // 类路径资源
            resource = resourceLoader.getResource(filePath);
        } else if (filePath.startsWith("/")) {
            // 绝对路径，尝试作为文件系统路径
            try {
                resource = resourceLoader.getResource("file:" + filePath);
            } catch (Exception e) {
                log.debug("无法作为文件系统路径加载: {}", filePath);
            }
            // 如果失败，尝试作为类路径资源
            if (resource == null || !resource.exists()) {
                resource = resourceLoader.getResource("classpath:" + filePath.substring(1));
            }
        } else {
            // 相对路径，先尝试类路径，再尝试文件系统
            resource = resourceLoader.getResource("classpath:" + filePath);
            if (!resource.exists()) {
                resource = resourceLoader.getResource("file:" + filePath);
            }
        }

        if (!resource.exists()) {
            // 提供更详细的错误信息
            String[] attemptedPaths = {
                    "classpath:" + (filePath.startsWith("/") ? filePath.substring(1) : filePath),
                    "file:" + filePath,
                    filePath
            };
            throw new XcServiceException("Mock文件不存在: " + filePath +
                    "。已尝试的路径: " + String.join(", ", attemptedPaths));
        }

        try (InputStream inputStream = resource.getInputStream();
             InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            MockData mockData = yaml.load(reader);
            if (mockData == null) {
                throw new XcServiceException("Mock文件内容为空: " + filePath);
            }
            log.info("成功从文件加载Mock数据: {}, 配置项数量: {}", filePath,
                    mockData.getMocks() != null ? mockData.getMocks().size() : 0);
            return mockData;
        }
    }

    /**
     * 从URL加载Mock数据
     */
    private MockData loadFromUrl(String url) throws Exception {
        log.debug("从URL加载Mock数据: {}", url);

        try {
            String yamlContent = NetUtils.build(url)
                    .connectTimeout(HTTP_TIMEOUT_SECONDS)
                    .readTimeout(HTTP_TIMEOUT_SECONDS)
                    .addHeader("Accept", "application/x-yaml, application/yaml, text/yaml, text/plain")
                    .doGet(String.class);

            if (yamlContent == null || yamlContent.trim().isEmpty()) {
                throw new XcServiceException("Mock数据为空，URL: " + url);
            }

            MockData mockData = yaml.load(yamlContent);
            if (mockData == null) {
                throw new XcServiceException("解析Mock数据失败，URL: " + url);
            }

            log.debug("成功从URL加载Mock数据: {}, 配置项数量: {}", url,
                    mockData.getMocks() != null ? mockData.getMocks().size() : 0);

            return mockData;

        } catch (Exception e) {
            log.error("从URL加载Mock数据失败: {}", url, e);
            throw new XcServiceException("获取Mock数据失败，URL: " + url + ", 错误: " + e.getMessage(), e);
        }
    }

    /**
     * 清除缓存
     */
    public void clearCache() {
        cacheLock.writeLock().lock();
        try {
            cache.clear();
            log.info("已清除所有Mock数据缓存");
        } finally {
            cacheLock.writeLock().unlock();
        }
    }

    /**
     * 清除指定数据源的缓存
     */
    public void clearCache(String dataSource) {
        cacheLock.writeLock().lock();
        try {
            cache.remove(dataSource);
            log.info("已清除Mock数据缓存: {}", dataSource);
        } finally {
            cacheLock.writeLock().unlock();
        }
    }

    /**
     * 获取缓存大小
     */
    public int getCacheSize() {
        cacheLock.readLock().lock();
        try {
            return cache.size();
        } finally {
            cacheLock.readLock().unlock();
        }
    }
}
