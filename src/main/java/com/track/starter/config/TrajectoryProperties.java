package com.track.starter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 轨迹处理配置属性
 * 让你的组件变得可配置，这是 Starter 的灵魂
 */
@ConfigurationProperties(prefix = "trajectory")
public class TrajectoryProperties {

    /**
     * 是否启用轨迹处理
     */
    private boolean enabled = true;

    /**
     * 坐标转换配置
     */
    private CoordinateConfig coordinate = new CoordinateConfig();

    /**
     * 去噪配置
     */
    private FilterConfig filter = new FilterConfig();

    /**
     * 压缩配置
     */
    private CompressionConfig compression = new CompressionConfig();

    /**
     * 统计配置
     */
    private StatisticsConfig statistics = new StatisticsConfig();

    // Getter和Setter方法
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public CoordinateConfig getCoordinate() {
        return coordinate;
    }

    public void setCoordinate(CoordinateConfig coordinate) {
        this.coordinate = coordinate;
    }

    public FilterConfig getFilter() {
        return filter;
    }

    public void setFilter(FilterConfig filter) {
        this.filter = filter;
    }

    public CompressionConfig getCompression() {
        return compression;
    }

    public void setCompression(CompressionConfig compression) {
        this.compression = compression;
    }

    public StatisticsConfig getStatistics() {
        return statistics;
    }

    public void setStatistics(StatisticsConfig statistics) {
        this.statistics = statistics;
    }

    /**
     * 坐标转换配置
     */
    public static class CoordinateConfig {
        /**
         * 是否启用坐标转换
         */
        private boolean enabled = false;

        /**
         * 源坐标系
         * 可选值: WGS84, GCJ02, BD09
         */
        private String source = "WGS84";

        /**
         * 目标坐标系
         * 可选值: WGS84, GCJ02, BD09
         */
        private String target = "GCJ02";

        /**
         * 是否保留原始坐标
         */
        private boolean keepOriginal = false;

        // Getter和Setter方法
        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getSource() {
            return source;
        }

        public void setSource(String source) {
            this.source = source;
        }

        public String getTarget() {
            return target;
        }

        public void setTarget(String target) {
            this.target = target;
        }

        public boolean isKeepOriginal() {
            return keepOriginal;
        }

        public void setKeepOriginal(boolean keepOriginal) {
            this.keepOriginal = keepOriginal;
        }
    }

    /**
     * 去噪配置
     */
    public static class FilterConfig {
        /**
         * 是否启用去噪
         */
        private boolean enabled = false;

        /**
         * 最大速度阈值 (km/h)
         */
        private double maxSpeed = 180.0;

        /**
         * 最小速度阈值 (km/h)
         */
        private double minSpeed = 0.5;

        /**
         * 最大精度阈值 (米)
         */
        private double maxAccuracy = 100.0;

        /**
         * 时间间隔阈值 (毫秒)
         */
        private long maxTimeInterval = 300000; // 5分钟

        /**
         * 距离阈值 (米)
         */
        private double maxDistance = 10000; // 10公里

        // Getter和Setter方法
        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public double getMaxSpeed() {
            return maxSpeed;
        }

        public void setMaxSpeed(double maxSpeed) {
            this.maxSpeed = maxSpeed;
        }

        public double getMinSpeed() {
            return minSpeed;
        }

        public void setMinSpeed(double minSpeed) {
            this.minSpeed = minSpeed;
        }

        public double getMaxAccuracy() {
            return maxAccuracy;
        }

        public void setMaxAccuracy(double maxAccuracy) {
            this.maxAccuracy = maxAccuracy;
        }

        public long getMaxTimeInterval() {
            return maxTimeInterval;
        }

        public void setMaxTimeInterval(long maxTimeInterval) {
            this.maxTimeInterval = maxTimeInterval;
        }

        public double getMaxDistance() {
            return maxDistance;
        }

        public void setMaxDistance(double maxDistance) {
            this.maxDistance = maxDistance;
        }
    }

    /**
     * 压缩配置
     */
    public static class CompressionConfig {
        /**
         * 是否启用压缩
         */
        private boolean enabled = true;

        /**
         * 距离阈值(米)，越小越精准，越大越粗糙
         */
        private double threshold = 5.0;

        /**
         * 压缩算法
         * 可选值: DOUGLAS_PEUCKER, VISVALINGAM, REUMANN_WITKAM, PERPENDICULAR_DISTANCE
         */
        private String algorithm = "DOUGLAS_PEUCKER";

        /**
         * 是否保留起点和终点
         */
        private boolean keepStartEnd = true;

        /**
         * 最小点数（如果原始点数小于此值，不进行压缩）
         */
        private int minPoints = 3;

        // Getter和Setter方法
        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public double getThreshold() {
            return threshold;
        }

        public void setThreshold(double threshold) {
            this.threshold = threshold;
        }

        public String getAlgorithm() {
            return algorithm;
        }

        public void setAlgorithm(String algorithm) {
            this.algorithm = algorithm;
        }

        public boolean isKeepStartEnd() {
            return keepStartEnd;
        }

        public void setKeepStartEnd(boolean keepStartEnd) {
            this.keepStartEnd = keepStartEnd;
        }

        public int getMinPoints() {
            return minPoints;
        }

        public void setMinPoints(int minPoints) {
            this.minPoints = minPoints;
        }
    }

    /**
     * 统计配置
     */
    public static class StatisticsConfig {
        /**
         * 是否生成统计信息
         */
        private boolean enabled = true;

        /**
         * 是否计算详细统计
         */
        private boolean detailed = false;

        /**
         * 是否输出格式化统计信息
         */
        private boolean formattedOutput = true;

        // Getter和Setter方法
        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isDetailed() {
            return detailed;
        }

        public void setDetailed(boolean detailed) {
            this.detailed = detailed;
        }

        public boolean isFormattedOutput() {
            return formattedOutput;
        }

        public void setFormattedOutput(boolean formattedOutput) {
            this.formattedOutput = formattedOutput;
        }
    }

    /**
     * 验证配置有效性
     */
    public void validate() {
        if (coordinate.enabled) {
            validateCoordinateSystem(coordinate.source);
            validateCoordinateSystem(coordinate.target);
        }

        if (filter.enabled) {
            if (filter.maxSpeed <= 0) {
                throw new IllegalArgumentException("最大速度阈值必须大于0");
            }
            if (filter.maxAccuracy <= 0) {
                throw new IllegalArgumentException("最大精度阈值必须大于0");
            }
        }

        if (compression.enabled) {
            if (compression.threshold <= 0) {
                throw new IllegalArgumentException("压缩阈值必须大于0");
            }
            if (compression.minPoints < 2) {
                throw new IllegalArgumentException("最小点数必须大于等于2");
            }
            validateCompressionAlgorithm(compression.algorithm);
        }
    }

    /**
     * 验证坐标系
     */
    private void validateCoordinateSystem(String coordinateSystem) {
        if (!coordinateSystem.equals("WGS84") &&
            !coordinateSystem.equals("GCJ02") &&
            !coordinateSystem.equals("BD09")) {
            throw new IllegalArgumentException("不支持的坐标系: " + coordinateSystem + "，支持: WGS84, GCJ02, BD09");
        }
    }

    /**
     * 验证压缩算法
     */
    private void validateCompressionAlgorithm(String algorithm) {
        if (!algorithm.equals("DOUGLAS_PEUCKER") &&
            !algorithm.equals("VISVALINGAM") &&
            !algorithm.equals("REUMANN_WITKAM") &&
            !algorithm.equals("PERPENDICULAR_DISTANCE")) {
            throw new IllegalArgumentException("不支持的压缩算法: " + algorithm +
                "，支持: DOUGLAS_PEUCKER, VISVALINGAM, REUMANN_WITKAM, PERPENDICULAR_DISTANCE");
        }
    }

    /**
     * 获取配置摘要
     */
    public String getConfigSummary() {
        return String.format(
            "轨迹处理配置:\n" +
            "启用: %b\n" +
            "坐标转换: %b (源: %s -> 目标: %s)\n" +
            "去噪: %b (最大速度: %.1f km/h)\n" +
            "压缩: %b (阈值: %.1f米, 算法: %s)\n" +
            "统计: %b",
            enabled,
            coordinate.enabled, coordinate.source, coordinate.target,
            filter.enabled, filter.maxSpeed,
            compression.enabled, compression.threshold, compression.algorithm,
            statistics.enabled
        );
    }
}