package fun.fan.xc.plugin.upload;

import fun.fan.xc.starter.exception.XcRunException;
import fun.fan.xc.starter.utils.Dict;
import io.minio.*;
import io.minio.messages.Bucket;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author fan
 */
@Slf4j
@RequiredArgsConstructor
@ConditionalOnClass(MinioClient.class)
@ConditionalOnProperty(prefix = "xc.upload.minio", value = "enable", havingValue = "true")
public class MinioUtils implements InitializingBean {
    private static MinioClient client;
    private static String endpoint;
    private static String accessKey;
    private static String secretKey;

    /**
     * 获取上传文件前缀路径
     */
    public static String getBasisUrl(String bucketName) {
        return endpoint + Dict.SEPARATOR + bucketName + Dict.SEPARATOR;
    }

    /**
     * 获取永久访问地址
     *
     * @param response 上传结果
     */
    public static String getUrl(ObjectWriteResponse response) {
        if (Objects.isNull(response)) {
            return null;
        }
        return getBasisUrl(response.bucket()) + response.object();
    }

    /**
     * 判断Bucket是否存在，true：存在，false：不存在
     */
    public static boolean bucketExists(String bucketName) {
        try {
            return client.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
        } catch (Exception e) {
            throw new XcRunException("minio异常", e);
        }
    }

    /**
     * 创建Bucket
     */
    private static void createBucket(String bucketName) {
        if (!bucketExists(bucketName)) {
            try {
                client.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            } catch (Exception e) {
                throw new XcRunException("minio异常", e);
            }
        }
    }

    /**
     * 根据bucketName删除Bucket
     */
    public static void removeBucket(String bucketName) {
        try {
            client.removeBucket(RemoveBucketArgs.builder().bucket(bucketName).build());
        } catch (Exception e) {
            throw new XcRunException("minio异常", e);
        }
    }

    /**
     * 获得所有Bucket列表
     */
    public static List<Bucket> listBuckets() {
        try {
            return client.listBuckets();
        } catch (Exception e) {
            throw new XcRunException("minio异常", e);
        }
    }

    /**
     * 判断文件是否存在
     *
     * @param bucketName 存储桶
     * @param objectName 文件名
     */
    public static boolean isObjectExist(String bucketName, String objectName) {
        boolean exist = true;
        try {
            client.statObject(StatObjectArgs.builder().bucket(bucketName).object(objectName).build());
        } catch (Exception e) {
            log.error("[Minio工具类]>>>> 判断文件是否存在, 异常：", e);
            exist = false;
        }
        return exist;
    }

    /**
     * 判断文件夹是否存在
     *
     * @param bucketName 存储桶
     * @param objectName 文件夹名称
     */
    public static boolean isFolderExist(String bucketName, String objectName) {
        boolean exist = false;
        try {
            Iterable<Result<Item>> results = client.listObjects(
                    ListObjectsArgs.builder().bucket(bucketName).prefix(objectName).recursive(false).build());
            for (Result<Item> result : results) {
                Item item = result.get();
                if (item.isDir() && objectName.equals(item.objectName())) {
                    exist = true;
                }
            }
        } catch (Exception e) {
            log.error("[Minio工具类]>>>> 判断文件夹是否存在，异常：", e);
            exist = false;
        }
        return exist;
    }

    /**
     * 根据文件前置查询文件
     *
     * @param bucketName 存储桶
     * @param prefix     前缀
     * @param recursive  是否使用递归查询
     * @return MinioItem 列表
     */
    public static List<Item> getAllObjectsByPrefix(String bucketName, String prefix, boolean recursive) {
        List<Item> list = new ArrayList<>();
        try {
            Iterable<Result<Item>> objectsIterator = client.listObjects(
                    ListObjectsArgs.builder().bucket(bucketName).prefix(prefix).recursive(recursive).build());
            if (objectsIterator != null) {
                for (Result<Item> o : objectsIterator) {
                    Item item = o.get();
                    list.add(item);
                }
            }
        } catch (Exception e) {
            throw new XcRunException("minio异常", e);
        }
        return list;
    }

    /**
     * 获取文件流
     *
     * @param bucketName 存储桶
     * @param objectName 文件名
     * @return 二进制流
     */
    public static InputStream getObject(String bucketName, String objectName) {
        try {
            return client.getObject(GetObjectArgs.builder().bucket(bucketName).object(objectName).build());
        } catch (Exception e) {
            throw new XcRunException("minio异常", e);
        }
    }

    /**
     * 使用MultipartFile进行文件上传
     *
     * @param bucketName 存储桶
     * @param file       文件名
     */
    public static ObjectWriteResponse uploadFile(String bucketName, MultipartFile file) {
        try {
            String type = file.getContentType();
            String filename = file.getOriginalFilename();
            InputStream inputStream = file.getInputStream();
            return client.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(filename)
                            .contentType(type)
                            .stream(inputStream, inputStream.available(), -1)
                            .build());
        } catch (Exception e) {
            throw new XcRunException("minio异常", e);
        }
    }

    /**
     * 通过流上传文件
     *
     * @param bucketName  存储桶
     * @param objectName  文件对象
     * @param inputStream 文件流
     */
    public static ObjectWriteResponse uploadFile(String bucketName, String objectName, InputStream inputStream) {
        try {
            return client.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(inputStream, inputStream.available(), -1)
                            .build());
        } catch (Exception e) {
            throw new XcRunException("minio异常", e);
        }
    }

    /**
     * 通过流上传文件
     *
     * @param bucketName  存储桶
     * @param objectName  文件对象
     * @param contentType 文件类型
     * @param inputStream 文件流
     */
    public static ObjectWriteResponse uploadFile(String bucketName, String objectName, String contentType, InputStream inputStream) {
        try {
            return client.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(inputStream, inputStream.available(), -1)
                            .build());
        } catch (Exception e) {
            throw new XcRunException("minio异常", e);
        }
    }

    /**
     * 创建文件夹或目录
     *
     * @param bucketName 存储桶
     * @param objectName 目录路径
     */
    public static ObjectWriteResponse createDir(String bucketName, String objectName) {
        try {
            return client.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(new ByteArrayInputStream(new byte[]{}), 0, -1)
                            .build());
        } catch (Exception e) {
            throw new XcRunException("minio异常", e);
        }
    }

    /**
     * 获取文件信息, 如果抛出异常则说明文件不存在
     *
     * @param bucketName 存储桶
     * @param objectName 文件名称
     */
    public static StatObjectResponse getFileStatusInfo(String bucketName, String objectName) {
        try {
            return client.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 拷贝文件
     *
     * @param bucketName    存储桶
     * @param objectName    文件名
     * @param srcBucketName 目标存储桶
     * @param srcObjectName 目标文件名
     */
    public static ObjectWriteResponse copyFile(String bucketName, String objectName,
                                               String srcBucketName, String srcObjectName) {
        try {
            return client.copyObject(
                    CopyObjectArgs.builder()
                            .source(CopySource.builder().bucket(bucketName).object(objectName).build())
                            .bucket(srcBucketName)
                            .object(srcObjectName)
                            .build());
        } catch (Exception e) {
            throw new XcRunException("minio异常", e);
        }
    }

    /**
     * 删除文件
     *
     * @param bucketName 存储桶
     * @param objectName 文件名称
     */
    public static void removeFile(String bucketName, String objectName) {
        try {
            client.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build());
        } catch (Exception e) {
            throw new XcRunException("minio异常", e);
        }
    }

    /**
     * 批量删除文件
     *
     * @param bucketName 存储桶
     * @param keys       需要删除的文件列表
     */
    public static void removeFiles(String bucketName, List<String> keys) {
        keys.forEach(s -> removeFile(bucketName, s));
    }

    private final UploadConfig config;

    private void createClient() {
        if (null == client) {
            log.info("开始创建 MinioClient...");
            client = MinioClient
                    .builder()
                    .endpoint(endpoint)
                    .credentials(accessKey, secretKey)
                    .build();
            log.info("创建完毕 MinioClient...");
        }
    }

    @Override
    public void afterPropertiesSet() {
        log.info("===> init minio client");
        endpoint = config.getMinio().getEndpoint();
        accessKey = config.getMinio().getAccessKey();
        secretKey = config.getMinio().getSecretKey();
        createClient();
    }
}
