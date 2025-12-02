package com.track.starter.service;

import com.track.starter.model.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 坐标转换服务
 * 解决不同坐标系之间的偏移问题
 */
@Service
public class CoordinateTransformService {

    private static final Logger log = LoggerFactory.getLogger(CoordinateTransformService.class);

    /**
     * 坐标转换
     * @param points 原始点列表
     * @param source 源坐标系
     * @param target 目标坐标系
     * @return 转换后的点列表
     */
    public List<Point> transform(List<Point> points, String source, String target) {
        if (points == null || points.isEmpty()) {
            return points;
        }

        log.debug("开始坐标转换: {} -> {}, 点数: {}", source, target, points.size());

        List<Point> transformedPoints = new ArrayList<>();

        for (Point point : points) {
            Point transformedPoint = transformPoint(point, source, target);
            transformedPoints.add(transformedPoint);
        }

        log.debug("坐标转换完成: {} -> {}, 转换点数: {}", source, target, transformedPoints.size());
        return transformedPoints;
    }

    /**
     * 转换单个点
     */
    private Point transformPoint(Point point, String source, String target) {
        // 如果源坐标系和目标坐标系相同，直接返回
        if (source.equals(target)) {
            return new Point(point.getLat(), point.getLng(), point.getTimestamp(),
                    point.getAltitude(), point.getSpeed(), point.getBearing(), point.getAccuracy());
        }

        double[] transformed = transformCoordinate(point.getLat(), point.getLng(), source, target);

        return new Point(transformed[0], transformed[1], point.getTimestamp(),
                point.getAltitude(), point.getSpeed(), point.getBearing(), point.getAccuracy());
    }

    /**
     * 坐标转换核心算法
     * 这里实现了WGS84、GCJ02、BD09之间的转换
     * 注意：这是简化的实现，实际生产环境可能需要更精确的算法
     */
    private double[] transformCoordinate(double lat, double lng, String source, String target) {
        // 如果源和目标相同，直接返回
        if (source.equals(target)) {
            return new double[]{lat, lng};
        }

        // WGS84 -> GCJ02
        if (source.equals("WGS84") && target.equals("GCJ02")) {
            return wgs84ToGcj02(lat, lng);
        }
        // GCJ02 -> WGS84
        else if (source.equals("GCJ02") && target.equals("WGS84")) {
            return gcj02ToWgs84(lat, lng);
        }
        // WGS84 -> BD09
        else if (source.equals("WGS84") && target.equals("BD09")) {
            double[] gcj02 = wgs84ToGcj02(lat, lng);
            return gcj02ToBd09(gcj02[0], gcj02[1]);
        }
        // BD09 -> WGS84
        else if (source.equals("BD09") && target.equals("WGS84")) {
            double[] gcj02 = bd09ToGcj02(lat, lng);
            return gcj02ToWgs84(gcj02[0], gcj02[1]);
        }
        // GCJ02 -> BD09
        else if (source.equals("GCJ02") && target.equals("BD09")) {
            return gcj02ToBd09(lat, lng);
        }
        // BD09 -> GCJ02
        else if (source.equals("BD09") && target.equals("GCJ02")) {
            return bd09ToGcj02(lat, lng);
        }
        // 其他情况暂不支持
        else {
            log.warn("不支持的坐标转换: {} -> {}, 返回原始坐标", source, target);
            return new double[]{lat, lng};
        }
    }

    /**
     * WGS84转GCJ02（火星坐标系）
     */
    private double[] wgs84ToGcj02(double lat, double lng) {
        // 简化的转换算法，实际生产环境需要使用更精确的算法
        double dLat = transformLat(lng - 105.0, lat - 35.0);
        double dLng = transformLng(lng - 105.0, lat - 35.0);
        double radLat = lat / 180.0 * Math.PI;
        double magic = Math.sin(radLat);
        magic = 1 - 0.00669342162296594323 * magic * magic;
        double sqrtMagic = Math.sqrt(magic);
        dLat = (dLat * 180.0) / ((6378245.0 * (1 - 0.00669342162296594323)) / (magic * sqrtMagic) * Math.PI);
        dLng = (dLng * 180.0) / (6378245.0 / sqrtMagic * Math.cos(radLat) * Math.PI);

        return new double[]{lat + dLat, lng + dLng};
    }

    /**
     * GCJ02转WGS84
     */
    private double[] gcj02ToWgs84(double lat, double lng) {
        double[] gcj02 = wgs84ToGcj02(lat, lng);
        double dLat = gcj02[0] - lat;
        double dLng = gcj02[1] - lng;

        return new double[]{lat - dLat, lng - dLng};
    }

    /**
     * GCJ02转BD09（百度坐标系）
     */
    private double[] gcj02ToBd09(double lat, double lng) {
        double x = lng;
        double y = lat;
        double z = Math.sqrt(x * x + y * y) + 0.00002 * Math.sin(y * Math.PI);
        double theta = Math.atan2(y, x) + 0.000003 * Math.cos(x * Math.PI);
        double bdLng = z * Math.cos(theta) + 0.0065;
        double bdLat = z * Math.sin(theta) + 0.006;

        return new double[]{bdLat, bdLng};
    }

    /**
     * BD09转GCJ02
     */
    private double[] bd09ToGcj02(double lat, double lng) {
        double x = lng - 0.0065;
        double y = lat - 0.006;
        double z = Math.sqrt(x * x + y * y) - 0.00002 * Math.sin(y * Math.PI);
        double theta = Math.atan2(y, x) - 0.000003 * Math.cos(x * Math.PI);
        double gcjLng = z * Math.cos(theta);
        double gcjLat = z * Math.sin(theta);

        return new double[]{gcjLat, gcjLng};
    }

    /**
     * 纬度转换
     */
    private double transformLat(double x, double y) {
        double ret = -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y + 0.1 * x * y + 0.2 * Math.sqrt(Math.abs(x));
        ret += (20.0 * Math.sin(6.0 * x * Math.PI) + 20.0 * Math.sin(2.0 * x * Math.PI)) * 2.0 / 3.0;
        ret += (20.0 * Math.sin(y * Math.PI) + 40.0 * Math.sin(y / 3.0 * Math.PI)) * 2.0 / 3.0;
        ret += (160.0 * Math.sin(y / 12.0 * Math.PI) + 320 * Math.sin(y * Math.PI / 30.0)) * 2.0 / 3.0;
        return ret;
    }

    /**
     * 经度转换
     */
    private double transformLng(double x, double y) {
        double ret = 300.0 + x + 2.0 * y + 0.1 * x * x + 0.1 * x * y + 0.1 * Math.sqrt(Math.abs(x));
        ret += (20.0 * Math.sin(6.0 * x * Math.PI) + 20.0 * Math.sin(2.0 * x * Math.PI)) * 2.0 / 3.0;
        ret += (20.0 * Math.sin(x * Math.PI) + 40.0 * Math.sin(x / 3.0 * Math.PI)) * 2.0 / 3.0;
        ret += (150.0 * Math.sin(x / 12.0 * Math.PI) + 300.0 * Math.sin(x / 30.0 * Math.PI)) * 2.0 / 3.0;
        return ret;
    }

    /**
     * 批量转换并保留原始坐标
     */
    public List<Point> transformWithOriginal(List<Point> points, String source, String target) {
        if (points == null || points.isEmpty()) {
            return points;
        }

        List<Point> result = new ArrayList<>();
        for (Point point : points) {
            // 创建新点，包含原始坐标和转换后坐标
            Point transformed = transformPoint(point, source, target);
            result.add(transformed);
        }

        return result;
    }

    /**
     * 验证坐标系是否支持
     */
    public boolean isCoordinateSystemSupported(String coordinateSystem) {
        return coordinateSystem.equals("WGS84") ||
               coordinateSystem.equals("GCJ02") ||
               coordinateSystem.equals("BD09");
    }

    /**
     * 获取支持的坐标系列表
     */
    public List<String> getSupportedCoordinateSystems() {
        return Arrays.asList("WGS84", "GCJ02", "BD09");
    }
}