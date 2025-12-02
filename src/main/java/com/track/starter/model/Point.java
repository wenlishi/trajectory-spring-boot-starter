package com.track.starter.model;

import java.util.Objects;

/**
 * 轨迹点对象
 * 这是整个流水线流通的血液
 */
public class Point {
    /**
     * 纬度
     */
    private double lat;

    /**
     * 经度
     */
    private double lng;

    /**
     * 时间戳（毫秒）
     */
    private long timestamp;

    /**
     * 海拔高度（可选）
     */
    private Double altitude;

    /**
     * 速度（km/h，可选）
     */
    private Double speed;

    /**
     * 方向角（0-360度，可选）
     */
    private Double bearing;

    /**
     * 精度（米，可选）
     */
    private Double accuracy;

    /**
     * 无参构造函数
     */
    public Point() {
    }

    /**
     * 全参构造函数
     */
    public Point(double lat, double lng, long timestamp, Double altitude, Double speed, Double bearing, Double accuracy) {
        this.lat = lat;
        this.lng = lng;
        this.timestamp = timestamp;
        this.altitude = altitude;
        this.speed = speed;
        this.bearing = bearing;
        this.accuracy = accuracy;
    }

    /**
     * 创建简单的点对象
     * @param lat 纬度
     * @param lng 经度
     * @param timestamp 时间戳
     */
    public Point(double lat, double lng, long timestamp) {
        this.lat = lat;
        this.lng = lng;
        this.timestamp = timestamp;
    }

    // Getter和Setter方法
    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLng() {
        return lng;
    }

    public void setLng(double lng) {
        this.lng = lng;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public Double getAltitude() {
        return altitude;
    }

    public void setAltitude(Double altitude) {
        this.altitude = altitude;
    }

    public Double getSpeed() {
        return speed;
    }

    public void setSpeed(Double speed) {
        this.speed = speed;
    }

    public Double getBearing() {
        return bearing;
    }

    public void setBearing(Double bearing) {
        this.bearing = bearing;
    }

    public Double getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(Double accuracy) {
        this.accuracy = accuracy;
    }

    /**
     * 计算两点之间的距离（使用Haversine公式）
     * @param other 另一个点
     * @return 距离（米）
     */
    public double distanceTo(Point other) {
        final int R = 6371000; // 地球半径（米）
        double lat1 = Math.toRadians(this.lat);
        double lat2 = Math.toRadians(other.lat);
        double deltaLat = Math.toRadians(other.lat - this.lat);
        double deltaLng = Math.toRadians(other.lng - this.lng);

        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
                   Math.cos(lat1) * Math.cos(lat2) *
                   Math.sin(deltaLng / 2) * Math.sin(deltaLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }

    /**
     * 计算两点之间的时间差
     * @param other 另一个点
     * @return 时间差（毫秒）
     */
    public long timeDiffTo(Point other) {
        return Math.abs(other.timestamp - this.timestamp);
    }

    /**
     * 计算两点之间的平均速度
     * @param other 另一个点
     * @return 平均速度（km/h）
     */
    public double averageSpeedTo(Point other) {
        double distance = distanceTo(other);
        long timeDiff = timeDiffTo(other);

        if (timeDiff == 0) {
            return 0;
        }

        // 距离（米）/ 时间（秒） * 3.6 = km/h
        return (distance / (timeDiff / 1000.0)) * 3.6;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Point point = (Point) o;
        return Double.compare(point.lat, lat) == 0 &&
               Double.compare(point.lng, lng) == 0 &&
               timestamp == point.timestamp &&
               Objects.equals(altitude, point.altitude) &&
               Objects.equals(speed, point.speed) &&
               Objects.equals(bearing, point.bearing) &&
               Objects.equals(accuracy, point.accuracy);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lat, lng, timestamp, altitude, speed, bearing, accuracy);
    }

    @Override
    public String toString() {
        return "Point{" +
               "lat=" + lat +
               ", lng=" + lng +
               ", timestamp=" + timestamp +
               ", altitude=" + altitude +
               ", speed=" + speed +
               ", bearing=" + bearing +
               ", accuracy=" + accuracy +
               '}';
    }
}