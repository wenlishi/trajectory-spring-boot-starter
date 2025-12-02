package com.track.starter.pipeline;

import com.track.starter.config.TrajectoryProperties;
import com.track.starter.model.Point;
import com.track.starter.model.TrajectorySummary;
import com.track.starter.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 轨迹流水线总管
 * 这是你跟面试官吹牛的核心资本。你把分散的功能组装成了一个自动化的流水线。
 */
@Component
public class TrajectoryPipeline {

    private static final Logger log = LoggerFactory.getLogger(TrajectoryPipeline.class);

    private final TrajectoryProperties properties;
    private final CoordinateTransformService coordinateService;
    private final NoiseFilterService filterService;
    private final CompressionService compressionService;

    public TrajectoryPipeline(TrajectoryProperties properties,
                             CoordinateTransformService coordinateService,
                             NoiseFilterService filterService,
                             CompressionService compressionService) {
        this.properties = properties;
        this.coordinateService = coordinateService;
        this.filterService = filterService;
        this.compressionService = compressionService;
    }

    /**
     * 一键处理：清洗 -> 过滤 -> 压缩
     * @param rawPoints 原始轨迹点
     * @return 处理后的轨迹点
     */
    public List<Point> process(List<Point> rawPoints) {
        return process(rawPoints, null);
    }

    /**
     * 一键处理并返回统计信息
     * @param rawPoints 原始轨迹点
     * @param summary 统计信息对象（可选）
     * @return 处理后的轨迹点
     */
    public List<Point> process(List<Point> rawPoints, TrajectorySummary summary) {
        long startTime = System.currentTimeMillis();

        if (!properties.isEnabled()) {
            log.info("轨迹处理已禁用，返回原始数据");
            return rawPoints;
        }

        if (rawPoints == null || rawPoints.isEmpty()) {
            log.warn("原始轨迹点为空");
            return rawPoints;
        }

        log.info("开始轨迹处理流水线，原始点数: {}", rawPoints.size());
        log.debug("配置信息: {}", properties.getConfigSummary());

        List<Point> result = rawPoints;
        int coordinateTransformCount = 0;
        int filteredPointCount = 0;

        // Stage 1: 坐标转换
        if (properties.getCoordinate().isEnabled()) {
            long stageStart = System.currentTimeMillis();
            result = coordinateService.transform(
                    result,
                    properties.getCoordinate().getSource(),
                    properties.getCoordinate().getTarget()
            );
            coordinateTransformCount = result.size();
            log.debug("坐标转换完成，耗时: {}ms", System.currentTimeMillis() - stageStart);
        }

        // Stage 2: 漂移去噪
        if (properties.getFilter().isEnabled()) {
            long stageStart = System.currentTimeMillis();
            List<Point> beforeFilter = result;
            result = filterService.filter(
                    result,
                    properties.getFilter().getMaxSpeed(),
                    properties.getFilter().getMinSpeed(),
                    properties.getFilter().getMaxAccuracy(),
                    properties.getFilter().getMaxTimeInterval(),
                    properties.getFilter().getMaxDistance()
            );
            filteredPointCount = beforeFilter.size() - result.size();
            log.debug("噪声过滤完成，过滤点数: {}，耗时: {}ms",
                    filteredPointCount, System.currentTimeMillis() - stageStart);
        }

        // Stage 3: 轨迹压缩 (抽稀)
        if (properties.getCompression().isEnabled()) {
            long stageStart = System.currentTimeMillis();
            result = compressionService.compress(
                    result,
                    properties.getCompression().getThreshold(),
                    properties.getCompression().getAlgorithm(),
                    properties.getCompression().isKeepStartEnd(),
                    properties.getCompression().getMinPoints()
            );
            log.debug("轨迹压缩完成，耗时: {}ms", System.currentTimeMillis() - stageStart);
        }

        long totalTime = System.currentTimeMillis() - startTime;

        // 生成统计信息
        if (properties.getStatistics().isEnabled() && summary != null) {
            summary = TrajectorySummary.fromPoints(rawPoints, result, totalTime);
            summary.setCoordinateTransformCount(coordinateTransformCount);
            summary.setFilteredPointCount(filteredPointCount);

            if (properties.getStatistics().isFormattedOutput()) {
                log.info("轨迹处理完成:\n{}", summary.toFormattedString());
            } else {
                log.info("轨迹处理完成: 原始点数={}, 处理后点数={}, 压缩率={:.2f}%, 耗时={}ms",
                        rawPoints.size(), result.size(),
                        100.0 * (rawPoints.size() - result.size()) / rawPoints.size(),
                        totalTime);
            }
        } else {
            log.info("轨迹处理完成: 原始点数={}, 处理后点数={}, 压缩率={:.2f}%, 耗时={}ms",
                    rawPoints.size(), result.size(),
                    100.0 * (rawPoints.size() - result.size()) / rawPoints.size(),
                    totalTime);
        }

        return result;
    }

    /**
     * 处理并返回统计信息
     * @param rawPoints 原始轨迹点
     * @return 处理结果（包含处理后的点和统计信息）
     */
    public ProcessingResult processWithResult(List<Point> rawPoints) {
        TrajectorySummary summary = new TrajectorySummary();
        List<Point> processedPoints = process(rawPoints, summary);

        return new ProcessingResult(processedPoints, summary);
    }

    /**
     * 仅进行坐标转换
     */
    public List<Point> transformOnly(List<Point> rawPoints) {
        if (!properties.getCoordinate().isEnabled()) {
            log.warn("坐标转换未启用");
            return rawPoints;
        }

        return coordinateService.transform(
                rawPoints,
                properties.getCoordinate().getSource(),
                properties.getCoordinate().getTarget()
        );
    }

    /**
     * 仅进行噪声过滤
     */
    public List<Point> filterOnly(List<Point> rawPoints) {
        if (!properties.getFilter().isEnabled()) {
            log.warn("噪声过滤未启用");
            return rawPoints;
        }

        return filterService.filter(
                rawPoints,
                properties.getFilter().getMaxSpeed(),
                properties.getFilter().getMinSpeed(),
                properties.getFilter().getMaxAccuracy(),
                properties.getFilter().getMaxTimeInterval(),
                properties.getFilter().getMaxDistance()
        );
    }

    /**
     * 仅进行轨迹压缩
     */
    public List<Point> compressOnly(List<Point> rawPoints) {
        if (!properties.getCompression().isEnabled()) {
            log.warn("轨迹压缩未启用");
            return rawPoints;
        }

        return compressionService.compress(
                rawPoints,
                properties.getCompression().getThreshold(),
                properties.getCompression().getAlgorithm(),
                properties.getCompression().isKeepStartEnd(),
                properties.getCompression().getMinPoints()
        );
    }

    /**
     * 获取流水线配置信息
     */
    public String getPipelineInfo() {
        StringBuilder info = new StringBuilder();
        info.append("轨迹处理流水线配置:\n");
        info.append("====================\n");
        info.append(properties.getConfigSummary()).append("\n");
        info.append("支持的功能:\n");
        info.append("1. 坐标转换: ").append(coordinateService.getSupportedCoordinateSystems()).append("\n");
        info.append("2. 噪声过滤: 速度/精度/距离/时间过滤\n");
        info.append("3. 轨迹压缩: ").append(compressionService.getSupportedAlgorithms()).append("\n");
        info.append("====================");

        return info.toString();
    }

    /**
     * 验证配置
     */
    public void validateConfig() {
        try {
            properties.validate();
            log.info("轨迹处理配置验证通过");
        } catch (IllegalArgumentException e) {
            log.error("轨迹处理配置验证失败: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 处理结果封装
     */
    public static class ProcessingResult {
        private List<Point> processedPoints;
        private TrajectorySummary summary;

        public ProcessingResult(List<Point> processedPoints, TrajectorySummary summary) {
            this.processedPoints = processedPoints;
            this.summary = summary;
        }

        public List<Point> getProcessedPoints() {
            return processedPoints;
        }

        public void setProcessedPoints(List<Point> processedPoints) {
            this.processedPoints = processedPoints;
        }

        public TrajectorySummary getSummary() {
            return summary;
        }

        public void setSummary(TrajectorySummary summary) {
            this.summary = summary;
        }

        /**
         * 获取压缩率
         */
        public double getCompressionRate() {
            return summary != null ? summary.getCompressionRate() : 0;
        }

        /**
         * 获取处理耗时
         */
        public long getProcessingTime() {
            return summary != null ? summary.getProcessingTime() : 0;
        }

        /**
         * 获取格式化结果
         */
        public String toFormattedString() {
            if (summary != null) {
                return summary.toFormattedString();
            }
            return String.format("处理完成: 点数=%d", processedPoints.size());
        }
    }
}