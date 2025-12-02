package com.track.starter.service;

import com.track.starter.model.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 噪声过滤服务
 * 解决GPS漂移问题
 */
@Service
public class NoiseFilterService {

    private static final Logger log = LoggerFactory.getLogger(NoiseFilterService.class);

    /**
     * 过滤噪声点
     * @param points 原始点列表
     * @param maxSpeed 最大速度阈值 (km/h)
     * @return 过滤后的点列表
     */
    public List<Point> filter(List<Point> points, double maxSpeed) {
        return filter(points, maxSpeed, 0.5, 100.0, 300000, 10000);
    }

    /**
     * 过滤噪声点（完整参数）
     * @param points 原始点列表
     * @param maxSpeed 最大速度阈值 (km/h)
     * @param minSpeed 最小速度阈值 (km/h)
     * @param maxAccuracy 最大精度阈值 (米)
     * @param maxTimeInterval 最大时间间隔 (毫秒)
     * @param maxDistance 最大距离阈值 (米)
     * @return 过滤后的点列表
     */
    public List<Point> filter(List<Point> points,
                              double maxSpeed,
                              double minSpeed,
                              double maxAccuracy,
                              long maxTimeInterval,
                              double maxDistance) {
        if (points == null || points.size() < 2) {
            return points;
        }

        log.debug("开始噪声过滤: 点数={}, 最大速度={}km/h, 最小速度={}km/h, 最大精度={}m",
                points.size(), maxSpeed, minSpeed, maxAccuracy);

        List<Point> filteredPoints = new ArrayList<>();
        filteredPoints.add(points.get(0)); // 总是保留第一个点

        for (int i = 1; i < points.size(); i++) {
            Point current = points.get(i);
            Point previous = filteredPoints.get(filteredPoints.size() - 1);

            // 检查各种过滤条件
            if (isValidPoint(current, previous,
                    maxSpeed, minSpeed, maxAccuracy, maxTimeInterval, maxDistance)) {
                filteredPoints.add(current);
            } else {
                log.debug("过滤掉点 {}: lat={}, lng={}, timestamp={}",
                        i, current.getLat(), current.getLng(), current.getTimestamp());
            }
        }

        int filteredCount = points.size() - filteredPoints.size();
        log.debug("噪声过滤完成: 原始点数={}, 过滤后点数={}, 过滤点数={}",
                points.size(), filteredPoints.size(), filteredCount);

        return filteredPoints;
    }

    /**
     * 检查点是否有效
     */
    private boolean isValidPoint(Point current,
                                 Point previous,
                                 double maxSpeed,
                                 double minSpeed,
                                 double maxAccuracy,
                                 long maxTimeInterval,
                                 double maxDistance) {
        // 1. 检查精度
        if (current.getAccuracy() != null && current.getAccuracy() > maxAccuracy) {
            return false;
        }

        // 2. 检查时间间隔
        long timeDiff = current.timeDiffTo(previous);
        if (timeDiff > maxTimeInterval) {
            return false;
        }

        // 3. 检查距离
        double distance = current.distanceTo(previous);
        if (distance > maxDistance) {
            return false;
        }

        // 4. 检查速度（如果时间差大于0）
        if (timeDiff > 0) {
            double speed = current.averageSpeedTo(previous);

            // 速度太快（可能是漂移）
            if (speed > maxSpeed) {
                return false;
            }

            // 速度太慢（可能是静止点）
            if (speed < minSpeed && distance < 10) { // 10米内低速认为是静止
                return false;
            }
        }

        // 5. 检查海拔变化（如果可用）
        if (current.getAltitude() != null && previous.getAltitude() != null) {
            double altitudeDiff = Math.abs(current.getAltitude() - previous.getAltitude());
            if (altitudeDiff > 1000 && distance < 100) { // 短距离内海拔变化过大
                return false;
            }
        }

        return true;
    }

    /**
     * 基于速度的过滤（主要过滤漂移点）
     */
    public List<Point> filterBySpeed(List<Point> points, double maxSpeed) {
        if (points == null || points.size() < 2) {
            return points;
        }

        List<Point> filteredPoints = new ArrayList<>();
        filteredPoints.add(points.get(0));

        for (int i = 1; i < points.size(); i++) {
            Point current = points.get(i);
            Point previous = filteredPoints.get(filteredPoints.size() - 1);

            long timeDiff = current.timeDiffTo(previous);
            if (timeDiff == 0) {
                filteredPoints.add(current);
                continue;
            }

            double speed = current.averageSpeedTo(previous);
            if (speed <= maxSpeed) {
                filteredPoints.add(current);
            } else {
                log.debug("速度过滤: 点 {} 速度 {} km/h 超过阈值 {} km/h",
                        i, speed, maxSpeed);
            }
        }

        return filteredPoints;
    }

    /**
     * 基于精度的过滤
     */
    public List<Point> filterByAccuracy(List<Point> points, double maxAccuracy) {
        if (points == null || points.isEmpty()) {
            return points;
        }

        List<Point> filteredPoints = new ArrayList<>();

        for (Point point : points) {
            if (point.getAccuracy() == null || point.getAccuracy() <= maxAccuracy) {
                filteredPoints.add(point);
            } else {
                log.debug("精度过滤: 点精度 {} m 超过阈值 {} m",
                        point.getAccuracy(), maxAccuracy);
            }
        }

        return filteredPoints;
    }

    /**
     * 基于距离的过滤（过滤异常跳跃点）
     */
    public List<Point> filterByDistance(List<Point> points, double maxDistance) {
        if (points == null || points.size() < 2) {
            return points;
        }

        List<Point> filteredPoints = new ArrayList<>();
        filteredPoints.add(points.get(0));

        for (int i = 1; i < points.size(); i++) {
            Point current = points.get(i);
            Point previous = filteredPoints.get(filteredPoints.size() - 1);

            double distance = current.distanceTo(previous);
            if (distance <= maxDistance) {
                filteredPoints.add(current);
            } else {
                log.debug("距离过滤: 点 {} 距离 {} m 超过阈值 {} m",
                        i, distance, maxDistance);
            }
        }

        return filteredPoints;
    }

    /**
     * 基于时间的过滤（过滤异常时间间隔）
     */
    public List<Point> filterByTimeInterval(List<Point> points, long maxTimeInterval) {
        if (points == null || points.size() < 2) {
            return points;
        }

        List<Point> filteredPoints = new ArrayList<>();
        filteredPoints.add(points.get(0));

        for (int i = 1; i < points.size(); i++) {
            Point current = points.get(i);
            Point previous = filteredPoints.get(filteredPoints.size() - 1);

            long timeDiff = current.timeDiffTo(previous);
            if (timeDiff <= maxTimeInterval) {
                filteredPoints.add(current);
            } else {
                log.debug("时间过滤: 点 {} 时间间隔 {} ms 超过阈值 {} ms",
                        i, timeDiff, maxTimeInterval);
            }
        }

        return filteredPoints;
    }

    /**
     * 获取过滤统计信息
     */
    public FilterStats getFilterStats(List<Point> original, List<Point> filtered) {
        FilterStats stats = new FilterStats();
        stats.setOriginalCount(original.size());
        stats.setFilteredCount(filtered.size());
        stats.setRemovedCount(original.size() - filtered.size());
        stats.setRemovalRate(original.size() > 0 ?
                100.0 * stats.getRemovedCount() / original.size() : 0);

        return stats;
    }

    /**
     * 过滤统计信息
     */
    public static class FilterStats {
        private int originalCount;
        private int filteredCount;
        private int removedCount;
        private double removalRate;

        // Getter和Setter方法
        public int getOriginalCount() {
            return originalCount;
        }

        public void setOriginalCount(int originalCount) {
            this.originalCount = originalCount;
        }

        public int getFilteredCount() {
            return filteredCount;
        }

        public void setFilteredCount(int filteredCount) {
            this.filteredCount = filteredCount;
        }

        public int getRemovedCount() {
            return removedCount;
        }

        public void setRemovedCount(int removedCount) {
            this.removedCount = removedCount;
        }

        public double getRemovalRate() {
            return removalRate;
        }

        public void setRemovalRate(double removalRate) {
            this.removalRate = removalRate;
        }

        public String toString() {
            return String.format(
                    "过滤统计: 原始点数=%d, 过滤后点数=%d, 移除点数=%d, 移除率=%.2f%%",
                    originalCount, filteredCount, removedCount, removalRate);
        }
    }
}