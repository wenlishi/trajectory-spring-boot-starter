package com.track.starter.config;

import com.track.starter.pipeline.TrajectoryPipeline;
import com.track.starter.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.type.AnnotationMetadata;

import java.lang.annotation.*;

/**
 * 轨迹处理自动配置类
 */
@Configuration
@ConditionalOnClass(TrajectoryPipeline.class)
@EnableConfigurationProperties(TrajectoryProperties.class)
@ConditionalOnProperty(prefix = "trajectory", name = "enabled", havingValue = "true", matchIfMissing = true)
public class TrajectoryAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(TrajectoryAutoConfiguration.class);

    /**
     * 坐标转换服务
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "trajectory.coordinate", name = "enabled", havingValue = "true", matchIfMissing = false)
    public CoordinateTransformService coordinateTransformService() {
        log.info("创建坐标转换服务");
        return new CoordinateTransformService();
    }

    /**
     * 噪声过滤服务
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "trajectory.filter", name = "enabled", havingValue = "true", matchIfMissing = false)
    public NoiseFilterService noiseFilterService() {
        log.info("创建噪声过滤服务");
        return new NoiseFilterService();
    }

    /**
     * 轨迹压缩服务
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "trajectory.compression", name = "enabled", havingValue = "true", matchIfMissing = true)
    public CompressionService compressionService() {
        log.info("创建轨迹压缩服务");
        return new CompressionService();
    }

    /**
     * 轨迹流水线
     */
    @Bean
    @ConditionalOnMissingBean
    public TrajectoryPipeline trajectoryPipeline(
            TrajectoryProperties properties,
            CoordinateTransformService coordinateTransformService,
            NoiseFilterService noiseFilterService,
            CompressionService compressionService) {

        log.info("创建轨迹处理流水线");
        log.debug("轨迹处理配置: {}", properties.getConfigSummary());

        // 验证配置
        try {
            properties.validate();
        } catch (IllegalArgumentException e) {
            log.error("轨迹处理配置验证失败: {}", e.getMessage());
            throw e;
        }

        return new TrajectoryPipeline(
                properties,
                coordinateTransformService,
                noiseFilterService,
                compressionService
        );
    }

    /**
     * 备用坐标转换服务（当坐标转换未启用时）
     */
    @Bean
    @ConditionalOnMissingBean(name = "coordinateTransformService")
    public CoordinateTransformService fallbackCoordinateTransformService() {
        log.debug("创建备用坐标转换服务（禁用状态）");
        return new CoordinateTransformService();
    }

    /**
     * 备用噪声过滤服务（当噪声过滤未启用时）
     */
    @Bean
    @ConditionalOnMissingBean(name = "noiseFilterService")
    public NoiseFilterService fallbackNoiseFilterService() {
        log.debug("创建备用噪声过滤服务（禁用状态）");
        return new NoiseFilterService();
    }

    /**
     * 配置后处理器，用于记录配置信息
     */
    @Bean
    public TrajectoryConfigPostProcessor trajectoryConfigPostProcessor(TrajectoryProperties properties) {
        return new TrajectoryConfigPostProcessor(properties);
    }

    /**
     * 配置后处理器
     */
    public static class TrajectoryConfigPostProcessor {

        private static final Logger log = LoggerFactory.getLogger(TrajectoryConfigPostProcessor.class);
        private final TrajectoryProperties properties;

        public TrajectoryConfigPostProcessor(TrajectoryProperties properties) {
            this.properties = properties;
        }

        /**
         * 初始化方法，在Bean创建后调用
         */
        @javax.annotation.PostConstruct
        public void init() {
            log.info("轨迹处理Starter已启用");
            log.info("配置摘要:\n{}", properties.getConfigSummary());

            // 记录支持的坐标系
            if (properties.getCoordinate().isEnabled()) {
                log.info("支持的坐标系: WGS84, GCJ02, BD09");
            }

            // 记录支持的压缩算法
            if (properties.getCompression().isEnabled()) {
                log.info("支持的压缩算法: DOUGLAS_PEUCKER, VISVALINGAM, REUMANN_WITKAM");
            }
        }
    }

    /**
     * 启用注解
     * 用户可以使用 @EnableTrajectoryProcessing 来显式启用轨迹处理功能
     */
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @Import(TrajectoryAutoConfiguration.class)
    public @interface EnableTrajectoryProcessing {
    }

    /**
     * 配置导入选择器
     * 用于支持 @EnableTrajectoryProcessing 注解
     */
    public static class TrajectoryConfigurationSelector implements ImportSelector {
        @Override
        public String[] selectImports(AnnotationMetadata importingClassMetadata) {
            return new String[] { TrajectoryAutoConfiguration.class.getName() };
        }
    }
}