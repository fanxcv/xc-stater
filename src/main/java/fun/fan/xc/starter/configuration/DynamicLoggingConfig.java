package fun.fan.xc.starter.configuration;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;
import cn.hutool.core.util.StrUtil;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * 动态日志配置类
 * 根据 application.yml 中的配置动态创建 logger 和 appender
 */
@Slf4j
@Configuration
@ConfigurationProperties(prefix = "logging")
public class DynamicLoggingConfig {
    /**
     * 日志包配置列表
     */
    @Getter
    @Setter
    private List<PackageConfig> packages;

    /**
     * 日志模式
     */
    @Value("${logging.pattern.console:%d{MM-dd HH:mm:ss.SSS} %-5p [%-16.16t{16}] %-40.40logger{40} : %m%n")
    private String pattern;

    /**
     * 日志保存目录
     */
    @Value("${logging.file.path:}")
    private String path;

    /**
     * 在Spring初始化时动态配置日志
     */
    @PostConstruct
    public void configureLoggers() {
        log.info("开始配置动态日志，路径: {}", path);
        if (packages == null || packages.isEmpty()) {
            log.warn("未配置包日志，跳过动态日志配置");
            return;
        }

        log.info("发现 {} 个包需要配置日志", packages.size());
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        PatternLayoutEncoder encoder = createEncoder(context);

        Logger root = context.getLogger("ROOT");
        // 获取root的文件输出适配器
        Appender<ILoggingEvent> rootFileAppender = root.getAppender("FILE");
        Appender<ILoggingEvent> rootFileErrorAppender = root.getAppender("FILE-ERROR");

        for (PackageConfig pkg : packages) {
            log.info("配置包日志: {} -> {}; 级别: {}; 控制台: {}", pkg.getName(), pkg.getFile(), pkg.getLevel(), pkg.isConsole());

            // 获取或创建logger
            Logger logger = context.getLogger(pkg.getName());
            // 设置日志级别
            logger.setLevel(Level.valueOf(pkg.getLevel()));

            // 创建文件appender
            if (StrUtil.isNotBlank(path)) {
                logger.addAppender(rootFileErrorAppender);
                if (StrUtil.isNotBlank(pkg.getFile())) {
                    // 创建自己独立的适配器
                    FileAppender<ILoggingEvent> fileAppender = createFileAppender(context, encoder, pkg);
                    logger.addAppender(fileAppender);
                } else {
                    logger.addAppender(rootFileAppender);
                }
            }

            // 如果需要控制台输出，创建并添加控制台appender
            // 设置是否继承父logger的appender
            logger.setAdditive(pkg.isConsole());

            log.info("包 {} 日志配置完成", pkg.getName());
        }

        log.info("动态日志配置全部完成");
    }

    /**
     * 创建编码器
     *
     * @param context LoggerContext
     * @return PatternLayoutEncoder
     */
    private PatternLayoutEncoder createEncoder(LoggerContext context) {
        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(context);
        encoder.setPattern(pattern);
        encoder.setCharset(StandardCharsets.UTF_8);
        encoder.start();
        return encoder;
    }

    /**
     * 创建文件appender
     *
     * @param context LoggerContext
     * @param encoder 编码器
     * @param pkg     包配置
     * @return FileAppender
     */
    private FileAppender<ILoggingEvent> createFileAppender(LoggerContext context, PatternLayoutEncoder encoder, PackageConfig pkg) {
        RollingFileAppender<ILoggingEvent> fileAppender = new RollingFileAppender<>();
        fileAppender.setContext(context);
        fileAppender.setName(pkg.getName() + "FileAppender");

        // 使用Path.resolve正确处理路径拼接，避免路径分隔符问题
        Path logPath = Paths.get(path).resolve(pkg.getFile());
        fileAppender.setFile(logPath.toString());

        // 设置编码器
        fileAppender.setEncoder(encoder);

        // 创建并设置滚动策略
        // 设置滚动文件名模式：原文件名.yyyyMMdd
        String fileNamePattern = logPath + ".%d{yyyyMMdd}";
        TimeBasedRollingPolicy<ILoggingEvent> rollingPolicy = createRollingPolicy(context, fileNamePattern, fileAppender);
        fileAppender.setRollingPolicy(rollingPolicy);
        fileAppender.start();
        return fileAppender;
    }

    /**
     * 创建滚动策略
     *
     * @param context         LoggerContext
     * @param fileNamePattern 文件名模式
     * @param parent          父appender
     * @return TimeBasedRollingPolicy
     */
    private TimeBasedRollingPolicy<ILoggingEvent> createRollingPolicy(LoggerContext context, String fileNamePattern, RollingFileAppender<ILoggingEvent> parent) {
        TimeBasedRollingPolicy<ILoggingEvent> rollingPolicy = new TimeBasedRollingPolicy<>();
        rollingPolicy.setContext(context);
        rollingPolicy.setFileNamePattern(fileNamePattern);
        rollingPolicy.setMaxHistory(30); // 最多保留30天
        rollingPolicy.setParent(parent);
        rollingPolicy.start();
        return rollingPolicy;
    }

    /**
     * 创建控制台appender
     *
     * @param context LoggerContext
     * @param encoder 编码器
     * @param pkg     包配置
     * @return ConsoleAppender
     */
    private ConsoleAppender<ILoggingEvent> createConsoleAppender(LoggerContext context, PatternLayoutEncoder encoder, PackageConfig pkg) {
        ConsoleAppender<ILoggingEvent> consoleAppender = new ConsoleAppender<>();
        consoleAppender.setContext(context);
        consoleAppender.setName(pkg.getName() + "ConsoleAppender");
        consoleAppender.setEncoder(encoder);
        consoleAppender.start();
        return consoleAppender;
    }

    /**
     * 包配置内部类
     */
    @Data
    public static class PackageConfig {
        /**
         * 包名
         */
        private String name;

        /**
         * 日志文件名
         */
        private String file;

        /**
         * 日志级别
         */
        private String level = "INFO";

        /**
         * 是否输出到控制台
         */
        private boolean console = false;
    }
}
