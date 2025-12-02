package com.track.starter.model;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * 轨迹统计结果对象
 */
public class TrajectorySummary {
    /**
     * 原始点数
     */
    private int originalPointCount;

    /**
     * 处理后点数
     */
    private int processedPointCount;

    /**
     * 压缩率（百分比）
     */
    private double compressionRate;

    /**
     * 总距离（米）
     */
    private double totalDistance;

    /**
     * 总时间（毫秒）
     */
    private long totalTime;

    /**
     * 平均速度（km/h）
     */
    private double averageSpeed;

    /**
     * 最大速度（km/h）
     */
    private double maxSpeed;

    /**
     * 最小速度（km/h）
     */
    private double minSpeed;

    /**
     * 起点
     */
    private Point startPoint;

    /**
     * 终点
     */
    private Point endPoint;

    /**
     * 边界框（最小纬度，最小经度，最大纬度，最大经度）
     */
    private double[] boundingBox;

    /**
     * 处理耗时（毫秒）
     */
    private long processingTime;

    /**
     * 过滤掉的点数
     */
    private int filteredPointCount;

    /**
     * 坐标转换次数
     */
    private int coordinateTransformCount;

    // Getter和Setter方法
    public int getOriginalPointCount() {
        return originalPointCount;
    }

    public void setOriginalPointCount(int originalPointCount) {
        this.originalPointCount = originalPointCount;
    }

    public int getProcessedPointCount() {
        return processedPointCount;
    }

    public void setProcessedPointCount(int processedPointCount) {
        this.processedPointCount = processedPointCount;
    }

    public double getCompressionRate() {
        return compressionRate;
    }

    public void setCompressionRate(double compressionRate) {
        this.compressionRate = compressionRate;
    }

    public double getTotalDistance() {
        return totalDistance;
    }

    public void setTotalDistance(double totalDistance) {
        this.totalDistance = totalDistance;
    }

    public long getTotalTime() {
        return totalTime;
    }

    public void setTotalTime(long totalTime) {
        this.totalTime = totalTime;
    }

    public double getAverageSpeed() {
        return averageSpeed;
    }

    public void setAverageSpeed(double averageSpeed) {
        this.averageSpeed = averageSpeed;
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

    public Point getStartPoint() {
        return startPoint;
    }

    public void setStartPoint(Point startPoint) {
        this.startPoint = startPoint;
    }

    public Point getEndPoint() {
        return endPoint;
    }

    public void setEndPoint(Point endPoint) {
        this.endPoint = endPoint;
    }

    public double[] getBoundingBox() {
        return boundingBox;
    }

    public void setBoundingBox(double[] boundingBox) {
        this.boundingBox = boundingBox;
    }

    public long getProcessingTime() {
        return processingTime;
    }

    public void setProcessingTime(long processingTime) {
        this.processingTime = processingTime;
    }

    public int getFilteredPointCount() {
        return filteredPointCount;
    }

    public void setFilteredPointCount(int filteredPointCount) {
        this.filteredPointCount = filteredPointCount;
    }

    public int getCoordinateTransformCount() {
        return coordinateTransformCount;
    }

    public void setCoordinateTransformCount(int coordinateTransformCount) {
        this.coordinateTransformCount = coordinateTransformCount;
    }

    /**
     * 从原始点和处理后点创建统计信息
     * @param originalPoints 原始点列表
     * @param processedPoints 处理后点列表
     * @param processingTime 处理耗时（毫秒）
     * @return 轨迹统计信息
     */
    public static TrajectorySummary fromPoints(List<Point> originalPoints,
                                               List<Point> processedPoints,
                                               long processingTime) {
        if (originalPoints == null || originalPoints.isEmpty()) {
            return null;
        }

        TrajectorySummary summary = new TrajectorySummary();
        summary.setOriginalPointCount(originalPoints.size());
        summary.setProcessedPointCount(processedPoints != null ? processedPoints.size() : 0);
        summary.setProcessingTime(processingTime);
        summary.setFilteredPointCount(originalPoints.size() - summary.getProcessedPointCount());

        // 计算压缩率
        if (originalPoints.size() > 0) {
            summary.setCompressionRate(100.0 * (originalPoints.size() - summary.getProcessedPointCount()) / originalPoints.size());
        }

        // 设置起点和终点
        summary.setStartPoint(originalPoints.get(0));
        summary.setEndPoint(originalPoints.get(originalPoints.size() - 1));

        // 计算总距离、总时间、速度统计
        if (processedPoints != null && processedPoints.size() > 1) {
            calculateStatistics(processedPoints, summary);
        }

        // 计算边界框
        calculateBoundingBox(originalPoints, summary);

        return summary;
    }

    /**
     * 计算轨迹统计信息
     */
    private static void calculateStatistics(List<Point> points, TrajectorySummary summary) {
        double totalDistance = 0;
        long totalTime = 0;
        double maxSpeed = 0;
        double minSpeed = Double.MAX_VALUE;
        double totalSpeed = 0;
        int speedCount = 0;

        for (int i = 0; i < points.size() - 1; i++) {
            Point current = points.get(i);
            Point next = points.get(i + 1);

            // 计算距离
            double distance = current.distanceTo(next);
            totalDistance += distance;

            // 计算时间差
            long timeDiff = current.timeDiffTo(next);
            totalTime += timeDiff;

            // 计算速度
            if (timeDiff > 0) {
                double speed = current.averageSpeedTo(next);
                maxSpeed = Math.max(maxSpeed, speed);
                minSpeed = Math.min(minSpeed, speed);
                totalSpeed += speed;
                speedCount++;
            }
        }

        summary.setTotalDistance(totalDistance);
        summary.setTotalTime(totalTime);
        summary.setMaxSpeed(maxSpeed);
        summary.setMinSpeed(minSpeed == Double.MAX_VALUE ? 0 : minSpeed);
        summary.setAverageSpeed(speedCount > 0 ? totalSpeed / speedCount : 0);
    }

    /**
     * 计算边界框
     */
    private static void calculateBoundingBox(List<Point> points, TrajectorySummary summary) {
        if (points.isEmpty()) {
            summary.setBoundingBox(new double[]{0, 0, 0, 0});
            return;
        }

        double minLat = Double.MAX_VALUE;
        double maxLat = -Double.MAX_VALUE;
        double minLng = Double.MAX_VALUE;
        double maxLng = -Double.MAX_VALUE;

        for (Point point : points) {
            minLat = Math.min(minLat, point.getLat());
            maxLat = Math.max(maxLat, point.getLat());
            minLng = Math.min(minLng, point.getLng());
            maxLng = Math.max(maxLng, point.getLng());
        }

        summary.setBoundingBox(new double[]{minLat, minLng, maxLat, maxLng});
    }

    /**
     * 获取格式化字符串表示
     */
    public String toFormattedString() {
        return String.format(
            "轨迹统计信息:\n" +
            "原始点数: %d\n" +
            "处理后点数: %d\n" +
            "压缩率: %.2f%%\n" +
            "过滤点数: %d\n" +
            "总距离: %.2f米\n" +
            "总时间: %.1f分钟\n" +
            "平均速度: %.2f km/h\n" +
            "最大速度: %.2f km/h\n" +
            "最小速度: %.2f km/h\n" +
            "处理耗时: %d毫秒",
            originalPointCount,
            processedPointCount,
            compressionRate,
            filteredPointCount,
            totalDistance,
            totalTime / 60000.0,
            averageSpeed,
            maxSpeed,
            minSpeed,
            processingTime
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TrajectorySummary that = (TrajectorySummary) o;
        return originalPointCount == that.originalPointCount &&
               processedPointCount == that.processedPointCount &&
               Double.compare(that.compressionRate, compressionRate) == 0 &&
               Double.compare(that.totalDistance, totalDistance) == 0 &&
               totalTime == that.totalTime &&
               Double.compare(that.averageSpeed, averageSpeed) == 0 &&
               Double.compare(that.maxSpeed, maxSpeed) == 0 &&
               Double.compare(that.minSpeed, minSpeed) == 0 &&
               processingTime == that.processingTime &&
               filteredPointCount == that.filteredPointCount &&
               coordinateTransformCount == that.coordinateTransformCount &&
               Objects.equals(startPoint, that.startPoint) &&
               Objects.equals(endPoint, that.endPoint) &&
               Objects.deepEquals(boundingBox, that.boundingBox);
    }

    @Override
    public int hashCode() {
        return Objects.hash(originalPointCount, processedPointCount, compressionRate, totalDistance, totalTime,
                           averageSpeed, maxSpeed, minSpeed, startPoint, endPoint, Arrays.hashCode(boundingBox),
                           processingTime, filteredPointCount, coordinateTransformCount);
    }

    @Override
    public String toString() {
        return "TrajectorySummary{" +
               "originalPointCount=" + originalPointCount +
               ", processedPointCount=" + processedPointCount +
               ", compressionRate=" + compressionRate +
               ", totalDistance=" + totalDistance +
               ", totalTime=" + totalTime +
               ", averageSpeed=" + averageSpeed +
               ", maxSpeed=" + maxSpeed +
               ", minSpeed=" + minSpeed +
               ", startPoint=" + startPoint +
               ", endPoint=" + endPoint +
               ", boundingBox=" + Arrays.toString(boundingBox) +
               ", processingTime=" + processingTime +
               ", filteredPointCount=" + filteredPointCount +
               ", coordinateTransformCount=" + coordinateTransformCount +
               '}';
    }
}