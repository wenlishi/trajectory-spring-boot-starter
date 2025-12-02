package com.track.starter.service;

import com.track.starter.model.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 轨迹压缩服务
 * 使用道格拉斯-普克算法进行轨迹抽稀
 */
@Service
public class CompressionService {

    private static final Logger log = LoggerFactory.getLogger(CompressionService.class);

    /**
     * 压缩轨迹点
     * @param points 原始轨迹点
     * @param threshold 阈值(米)，越小越精准，越大越粗糙
     * @return 压缩后的轨迹点
     */
    public List<Point> compress(List<Point> points, double threshold) {
        return compress(points, threshold, "DOUGLAS_PEUCKER", true, 3);
    }

    /**
     * 压缩轨迹点（完整参数）
     * @param points 原始轨迹点
     * @param threshold 阈值(米)
     * @param algorithm 压缩算法
     * @param keepStartEnd 是否保留起点和终点
     * @param minPoints 最小点数
     * @return 压缩后的轨迹点
     */
    public List<Point> compress(List<Point> points,
                                double threshold,
                                String algorithm,
                                boolean keepStartEnd,
                                int minPoints) {
        if (points == null || points.size() < minPoints) {
            log.debug("点数不足 {}，不进行压缩", minPoints);
            return points;
        }

        log.debug("开始轨迹压缩: 点数={}, 阈值={}m, 算法={}, 保留首尾={}",
                points.size(), threshold, algorithm, keepStartEnd);

        List<Point> compressedPoints;

        switch (algorithm.toUpperCase()) {
            case "DOUGLAS_PEUCKER":
                compressedPoints = douglasPeucker(points, threshold, keepStartEnd);
                break;
            case "VISVALINGAM":
                compressedPoints = visvalingamWhyatt(points, threshold, keepStartEnd);
                break;
            case "REUMANN_WITKAM":
                compressedPoints = reumannWitkam(points, threshold, keepStartEnd);
                break;
            case "PERPENDICULAR_DISTANCE":
                compressedPoints = perpendicularDistance(points, threshold, keepStartEnd);
                break;
            default:
                log.warn("不支持的压缩算法: {}，使用默认算法 DOUGLAS_PEUCKER", algorithm);
                compressedPoints = douglasPeucker(points, threshold, keepStartEnd);
        }

        int compressedCount = points.size() - compressedPoints.size();
        double compressionRate = points.size() > 0 ? 100.0 * compressedCount / points.size() : 0;

        log.debug("轨迹压缩完成: 原始点数={}, 压缩后点数={}, 压缩点数={}, 压缩率={:.2f}%",
                points.size(), compressedPoints.size(), compressedCount, compressionRate);

        return compressedPoints;
    }

    /**
     * 道格拉斯-普克抽稀算法
     * @param points 原始轨迹
     * @param threshold 阈值(米)，越小越精准，越大越粗糙
     * @param keepStartEnd 是否保留起点和终点
     */
    private List<Point> douglasPeucker(List<Point> points, double threshold, boolean keepStartEnd) {
        if (points == null || points.size() < 3) {
            return points;
        }

        // 递归处理
        List<Point> result = douglasPeuckerRecursive(points, 0, points.size() - 1, threshold);

        // 确保保留起点和终点
        if (keepStartEnd) {
            if (!result.get(0).equals(points.get(0))) {
                result.add(0, points.get(0));
            }
            if (!result.get(result.size() - 1).equals(points.get(points.size() - 1))) {
                result.add(points.get(points.size() - 1));
            }
        }

        return result;
    }

    /**
     * 道格拉斯-普克递归实现
     */
    private List<Point> douglasPeuckerRecursive(List<Point> points, int first, int last, double threshold) {
        int count = last - first + 1;
        if (count < 3) {
            List<Point> result = new ArrayList<>();
            result.add(points.get(first));
            if (first != last) {
                result.add(points.get(last));
            }
            return result;
        }

        // 1. 找出距离首尾连线最远的那个点
        double maxDist = 0;
        int index = first;

        for (int i = first + 1; i < last; i++) {
            double dist = calculatePerpendicularDistance(points.get(i), points.get(first), points.get(last));
            if (dist > maxDist) {
                maxDist = dist;
                index = i;
            }
        }

        List<Point> result = new ArrayList<>();

        // 2. 如果最大距离大于阈值，则保留该点，并递归处理两边
        if (maxDist > threshold) {
            List<Point> left = douglasPeuckerRecursive(points, first, index, threshold);
            List<Point> right = douglasPeuckerRecursive(points, index, last, threshold);

            // 合并结果，避免重复添加中间点
            result.addAll(left.subList(0, left.size() - 1));
            result.addAll(right);
        } else {
            // 3. 都在阈值范围内，直接取首尾
            result.add(points.get(first));
            result.add(points.get(last));
        }

        return result;
    }

    /**
     * 计算点到直线的垂距（以米为单位）
     * 使用海伦公式计算三角形面积，然后计算高
     */
    private double calculatePerpendicularDistance(Point p, Point lineStart, Point lineEnd) {
        // 如果起点和终点相同，返回点到该点的距离
        if (lineStart.equals(lineEnd)) {
            return p.distanceTo(lineStart);
        }

        // 计算三个点之间的距离（以米为单位）
        double a = p.distanceTo(lineStart);      // 点p到起点距离
        double b = p.distanceTo(lineEnd);        // 点p到终点距离
        double c = lineStart.distanceTo(lineEnd); // 起点到终点距离（底边）

        // 使用海伦公式计算三角形面积
        // s = (a + b + c) / 2
        // area = sqrt(s * (s-a) * (s-b) * (s-c))
        double s = (a + b + c) / 2.0;
        double area = Math.sqrt(s * (s - a) * (s - b) * (s - c));

        // 避免除零和数值误差
        if (c < 1e-10) {
            return Math.min(a, b);
        }

        // 垂距 = 2 * 面积 / 底边长度
        return 2.0 * area / c;
    }

    /**
     * Visvalingam-Whyatt 算法
     */
    private List<Point> visvalingamWhyatt(List<Point> points, double threshold, boolean keepStartEnd) {
        if (points == null || points.size() < 3) {
            return points;
        }

        // 创建点的副本
        List<Point> result = new ArrayList<>(points);

        // 计算每个点的有效面积（与相邻两点形成的三角形面积）
        while (result.size() > 2) {
            double minArea = Double.MAX_VALUE;
            int minIndex = -1;

            // 找到最小面积的点（除了首尾）
            for (int i = 1; i < result.size() - 1; i++) {
                double area = calculateTriangleArea(
                    result.get(i - 1),
                    result.get(i),
                    result.get(i + 1)
                );

                if (area < minArea) {
                    minArea = area;
                    minIndex = i;
                }
            }

            // 如果最小面积大于阈值，停止压缩
            if (minArea > threshold) {
                break;
            }

            // 移除最小面积的点
            if (minIndex != -1) {
                result.remove(minIndex);
            }
        }

        return result;
    }

    /**
     * 计算三角形面积
     */
    private double calculateTriangleArea(Point a, Point b, Point c) {
        // 使用海伦公式
        double ab = a.distanceTo(b);
        double bc = b.distanceTo(c);
        double ca = c.distanceTo(a);

        double s = (ab + bc + ca) / 2;
        return Math.sqrt(s * (s - ab) * (s - bc) * (s - ca));
    }

    /**
     * Reumann-Witkam 算法
     */
    private List<Point> reumannWitkam(List<Point> points, double threshold, boolean keepStartEnd) {
        if (points == null || points.size() < 3) {
            return points;
        }

        List<Point> result = new ArrayList<>();
        result.add(points.get(0));

        int i = 0;
        while (i < points.size() - 1) {
            int j = i + 1;
            Point anchor = points.get(i);

            // 找到第一个距离超过阈值的点
            while (j < points.size() && anchor.distanceTo(points.get(j)) <= threshold) {
                j++;
            }

            if (j < points.size()) {
                result.add(points.get(j));
                i = j;
            } else {
                break;
            }
        }

        // 确保包含最后一个点
        if (!result.get(result.size() - 1).equals(points.get(points.size() - 1))) {
            result.add(points.get(points.size() - 1));
        }

        return result;
    }

    /**
     * 获取压缩统计信息
     */
    public CompressionStats getCompressionStats(List<Point> original, List<Point> compressed) {
        CompressionStats stats = new CompressionStats();
        stats.setOriginalCount(original.size());
        stats.setCompressedCount(compressed.size());
        stats.setRemovedCount(original.size() - compressed.size());
        stats.setCompressionRate(original.size() > 0 ?
                100.0 * stats.getRemovedCount() / original.size() : 0);

        // 计算距离保留率
        double originalDistance = calculateTotalDistance(original);
        double compressedDistance = calculateTotalDistance(compressed);
        stats.setDistancePreservationRate(originalDistance > 0 ?
                100.0 * compressedDistance / originalDistance : 100.0);

        return stats;
    }

    /**
     * 计算轨迹总距离
     */
    private double calculateTotalDistance(List<Point> points) {
        if (points == null || points.size() < 2) {
            return 0;
        }

        double totalDistance = 0;
        for (int i = 0; i < points.size() - 1; i++) {
            totalDistance += points.get(i).distanceTo(points.get(i + 1));
        }

        return totalDistance;
    }

    /**
     * 压缩统计信息
     */
    public static class CompressionStats {
        private int originalCount;
        private int compressedCount;
        private int removedCount;
        private double compressionRate;
        private double distancePreservationRate;

        // Getter和Setter方法
        public int getOriginalCount() {
            return originalCount;
        }

        public void setOriginalCount(int originalCount) {
            this.originalCount = originalCount;
        }

        public int getCompressedCount() {
            return compressedCount;
        }

        public void setCompressedCount(int compressedCount) {
            this.compressedCount = compressedCount;
        }

        public int getRemovedCount() {
            return removedCount;
        }

        public void setRemovedCount(int removedCount) {
            this.removedCount = removedCount;
        }

        public double getCompressionRate() {
            return compressionRate;
        }

        public void setCompressionRate(double compressionRate) {
            this.compressionRate = compressionRate;
        }

        public double getDistancePreservationRate() {
            return distancePreservationRate;
        }

        public void setDistancePreservationRate(double distancePreservationRate) {
            this.distancePreservationRate = distancePreservationRate;
        }

        public String toString() {
            return String.format(
                    "压缩统计: 原始点数=%d, 压缩后点数=%d, 移除点数=%d, 压缩率=%.2f%%, 距离保留率=%.2f%%",
                    originalCount, compressedCount, removedCount,
                    compressionRate, distancePreservationRate);
        }
    }

    /**
     * 垂距法压缩算法
     * 基于点到相邻两点连线的垂直距离进行压缩
     * @param points 原始轨迹点
     * @param threshold 阈值(米)
     * @param keepStartEnd 是否保留起点和终点
     * @return 压缩后的轨迹点
     */
    private List<Point> perpendicularDistance(List<Point> points, double threshold, boolean keepStartEnd) {
        if (points == null || points.size() < 3) {
            return points;
        }

        List<Point> result = new ArrayList<>();

        // 总是保留第一个点
        result.add(points.get(0));

        // 遍历所有中间点
        for (int i = 1; i < points.size() - 1; i++) {
            Point prev = result.get(result.size() - 1);
            Point current = points.get(i);
            Point next = points.get(i + 1);

            // 计算当前点到前后两点连线的垂直距离
            double distance = calculatePerpendicularDistance(current, prev, next);

            // 如果距离大于阈值，保留该点
            if (distance > threshold) {
                result.add(current);
            } else {
                log.debug("垂距法过滤点 {}: 距离={}m, 阈值={}m", i, distance, threshold);
            }
        }

        // 总是保留最后一个点
        if (keepStartEnd) {
            Point lastPoint = points.get(points.size() - 1);
            if (!result.get(result.size() - 1).equals(lastPoint)) {
                result.add(lastPoint);
            }
        } else {
            // 如果不强制保留终点，检查最后一个点是否需要保留
            if (result.size() >= 2) {
                Point prev = result.get(result.size() - 2);
                Point current = result.get(result.size() - 1);
                Point last = points.get(points.size() - 1);

                double distance = calculatePerpendicularDistance(last, prev, current);
                if (distance > threshold) {
                    result.add(last);
                }
            }
        }

        return result;
    }

    /**
     * 改进的垂距法（滑动窗口版本）
     * 使用滑动窗口检查多个相邻点
     * @param points 原始轨迹点
     * @param threshold 阈值(米)
     * @param keepStartEnd 是否保留起点和终点
     * @param windowSize 滑动窗口大小
     * @return 压缩后的轨迹点
     */
    public List<Point> perpendicularDistanceWithWindow(List<Point> points,
                                                      double threshold,
                                                      boolean keepStartEnd,
                                                      int windowSize) {
        if (points == null || points.size() < 3) {
            return points;
        }

        if (windowSize < 2) {
            windowSize = 2;
        }

        List<Point> result = new ArrayList<>();
        result.add(points.get(0));

        for (int i = 1; i < points.size() - 1; i++) {
            Point current = points.get(i);
            boolean keepPoint = false;

            // 检查当前点与前后窗口内点的关系
            int start = Math.max(0, i - windowSize);
            int end = Math.min(points.size() - 1, i + windowSize);

            for (int j = start; j < end - 1 && !keepPoint; j++) {
                for (int k = j + 1; k <= end && !keepPoint; k++) {
                    if (j == i || k == i) continue;

                    double distance = calculatePerpendicularDistance(current, points.get(j), points.get(k));
                    if (distance > threshold) {
                        keepPoint = true;
                    }
                }
            }

            if (keepPoint) {
                result.add(current);
            }
        }

        if (keepStartEnd) {
            Point lastPoint = points.get(points.size() - 1);
            if (!result.get(result.size() - 1).equals(lastPoint)) {
                result.add(lastPoint);
            }
        }

        return result;
    }

    /**
     * 自适应垂距法
     * 根据局部曲率动态调整阈值
     * @param points 原始轨迹点
     * @param baseThreshold 基础阈值(米)
     * @param keepStartEnd 是否保留起点和终点
     * @return 压缩后的轨迹点
     */
    public List<Point> adaptivePerpendicularDistance(List<Point> points,
                                                    double baseThreshold,
                                                    boolean keepStartEnd) {
        if (points == null || points.size() < 3) {
            return points;
        }

        List<Point> result = new ArrayList<>();
        result.add(points.get(0));

        // 计算局部曲率
        List<Double> curvatures = calculateLocalCurvature(points);

        for (int i = 1; i < points.size() - 1; i++) {
            Point prev = result.get(result.size() - 1);
            Point current = points.get(i);
            Point next = points.get(i + 1);

            // 根据曲率调整阈值（曲率越大，阈值越小，保留更多点）
            double adaptiveThreshold = baseThreshold;
            if (i < curvatures.size()) {
                double curvature = curvatures.get(i);
                // 曲率在0-1之间，曲率越大，阈值越小
                adaptiveThreshold = baseThreshold * (1.0 - curvature * 0.5);
                adaptiveThreshold = Math.max(adaptiveThreshold, baseThreshold * 0.1);
            }

            double distance = calculatePerpendicularDistance(current, prev, next);
            if (distance > adaptiveThreshold) {
                result.add(current);
            }
        }

        if (keepStartEnd) {
            Point lastPoint = points.get(points.size() - 1);
            if (!result.get(result.size() - 1).equals(lastPoint)) {
                result.add(lastPoint);
            }
        }

        return result;
    }

    /**
     * 计算局部曲率
     * 曲率表示轨迹的弯曲程度，值在0-1之间
     */
    private List<Double> calculateLocalCurvature(List<Point> points) {
        List<Double> curvatures = new ArrayList<>();

        for (int i = 1; i < points.size() - 1; i++) {
            Point prev = points.get(i - 1);
            Point current = points.get(i);
            Point next = points.get(i + 1);

            // 计算三个点形成的角度
            double angle = calculateAngle(prev, current, next);

            // 将角度转换为曲率（0-180度映射到0-1）
            double curvature = 1.0 - Math.abs(angle - 90.0) / 90.0;
            curvatures.add(curvature);
        }

        return curvatures;
    }

    /**
     * 计算三个点形成的角度
     */
    private double calculateAngle(Point a, Point b, Point c) {
        // 计算向量BA和BC
        double baX = a.getLng() - b.getLng();
        double baY = a.getLat() - b.getLat();
        double bcX = c.getLng() - b.getLng();
        double bcY = c.getLat() - b.getLat();

        // 计算点积
        double dotProduct = baX * bcX + baY * bcY;

        // 计算模长
        double baLength = Math.sqrt(baX * baX + baY * baY);
        double bcLength = Math.sqrt(bcX * bcX + bcY * bcY);

        // 计算夹角（弧度）
        double cosAngle = dotProduct / (baLength * bcLength);
        cosAngle = Math.max(-1.0, Math.min(1.0, cosAngle)); // 防止数值误差

        // 转换为角度
        return Math.toDegrees(Math.acos(cosAngle));
    }

    /**
     * 获取支持的压缩算法列表（更新版本）
     */
    public List<String> getSupportedAlgorithms() {
        return Arrays.asList("DOUGLAS_PEUCKER", "VISVALINGAM", "REUMANN_WITKAM", "PERPENDICULAR_DISTANCE");
    }

    /**
     * 获取算法描述
     */
    public String getAlgorithmDescription(String algorithm) {
        switch (algorithm.toUpperCase()) {
            case "DOUGLAS_PEUCKER":
                return "道格拉斯-普克算法：递归分割，保留形状特征，适合弯曲轨迹";
            case "VISVALINGAM":
                return "Visvalingam-Whyatt算法：基于三角形面积，保留重要点";
            case "REUMANN_WITKAM":
                return "Reumann-Witkam算法：简单快速，适合实时处理";
            case "PERPENDICULAR_DISTANCE":
                return "垂距法：基于点到相邻两点连线的垂直距离，适合平滑轨迹";
            default:
                return "未知算法";
        }
    }

    /**
     * 获取算法复杂度
     */
    public String getAlgorithmComplexity(String algorithm) {
        switch (algorithm.toUpperCase()) {
            case "DOUGLAS_PEUCKER":
                return "时间复杂度: O(n log n), 空间复杂度: O(n)";
            case "VISVALINGAM":
                return "时间复杂度: O(n log n), 空间复杂度: O(n)";
            case "REUMANN_WITKAM":
                return "时间复杂度: O(n), 空间复杂度: O(1)";
            case "PERPENDICULAR_DISTANCE":
                return "时间复杂度: O(n), 空间复杂度: O(1)";
            default:
                return "未知";
        }
    }
}