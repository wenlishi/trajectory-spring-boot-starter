package com.track.starter;

import com.track.starter.model.Point;
import com.track.starter.service.CompressionService;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * 垂距法算法测试
 */
public class PerpendicularDistanceTest {

    private CompressionService compressionService;

    @Before
    public void setUp() {
        compressionService = new CompressionService();
    }

    @Test
    public void testBasicPerpendicularDistance() {
        // 创建一条直线轨迹（所有点都在一条直线上）
        List<Point> straightLine = createStraightLine(10);

        // 使用垂距法压缩，阈值设为1米
        List<Point> compressed = compressionService.compress(
            straightLine,
            1.0,
            "PERPENDICULAR_DISTANCE",
            true,
            3
        );

        // 直线上的点应该被大量压缩（只保留首尾）
        assertTrue(compressed.size() < straightLine.size());
        assertEquals(2, compressed.size()); // 只保留首尾
        assertEquals(straightLine.get(0), compressed.get(0));
        assertEquals(straightLine.get(straightLine.size() - 1), compressed.get(1));
    }

    @Test
    public void testPerpendicularDistanceWithCurve() {
        // 创建一条弯曲轨迹
        List<Point> curvedLine = createCurvedLine(20);

        // 使用较小的阈值，应该保留更多点
        List<Point> compressedSmallThreshold = compressionService.compress(
            curvedLine,
            1.0,
            "PERPENDICULAR_DISTANCE",
            true,
            3
        );

        // 使用较大的阈值，应该保留更少点
        List<Point> compressedLargeThreshold = compressionService.compress(
            curvedLine,
            10.0,
            "PERPENDICULAR_DISTANCE",
            true,
            3
        );

        // 阈值越小，保留的点越多
        assertTrue(compressedSmallThreshold.size() > compressedLargeThreshold.size());

        // 确保首尾点被保留
        assertEquals(curvedLine.get(0), compressedSmallThreshold.get(0));
        assertEquals(curvedLine.get(curvedLine.size() - 1),
                    compressedSmallThreshold.get(compressedSmallThreshold.size() - 1));
    }

    @Test
    public void testPerpendicularDistanceWithWindow() {
        List<Point> points = createMixedTrajectory(15);

        // 使用基本垂距法
        List<Point> basicResult = compressionService.compress(
            points,
            5.0,
            "PERPENDICULAR_DISTANCE",
            true,
            3
        );

        // 使用滑动窗口垂距法（窗口大小=3）
        List<Point> windowResult = compressionService.perpendicularDistanceWithWindow(
            points,
            5.0,
            true,
            3
        );

        // 滑动窗口版本应该更严格（保留更少点）
        assertTrue(windowResult.size() <= basicResult.size());

        // 确保首尾点被保留
        assertEquals(points.get(0), windowResult.get(0));
        assertEquals(points.get(points.size() - 1),
                    windowResult.get(windowResult.size() - 1));
    }

    @Test
    public void testAdaptivePerpendicularDistance() {
        List<Point> points = createTrajectoryWithVaryingCurvature(20);

        // 使用自适应垂距法
        List<Point> adaptiveResult = compressionService.adaptivePerpendicularDistance(
            points,
            5.0,
            true
        );

        // 使用固定阈值垂距法
        List<Point> fixedResult = compressionService.compress(
            points,
            5.0,
            "PERPENDICULAR_DISTANCE",
            true,
            3
        );

        // 自适应算法应该在不同曲率区域有不同的压缩效果
        // 这里我们主要验证算法能正常运行
        assertNotNull(adaptiveResult);
        assertTrue(adaptiveResult.size() >= 2);
        assertEquals(points.get(0), adaptiveResult.get(0));
        assertEquals(points.get(points.size() - 1),
                    adaptiveResult.get(adaptiveResult.size() - 1));
    }

    @Test
    public void testAlgorithmComparison() {
        List<Point> points = createComplexTrajectory(30);

        // 测试不同算法
        String[] algorithms = {"DOUGLAS_PEUCKER", "VISVALINGAM", "REUMANN_WITKAM", "PERPENDICULAR_DISTANCE"};

        for (String algorithm : algorithms) {
            List<Point> result = compressionService.compress(
                points,
                5.0,
                algorithm,
                true,
                3
            );

            assertNotNull("算法 " + algorithm + " 返回null", result);
            assertTrue("算法 " + algorithm + " 结果为空", result.size() > 0);
            assertEquals("算法 " + algorithm + " 未保留起点",
                        points.get(0), result.get(0));
            assertEquals("算法 " + algorithm + " 未保留终点",
                        points.get(points.size() - 1),
                        result.get(result.size() - 1));

            System.out.println(algorithm + ": 原始点数=" + points.size() +
                             ", 压缩后点数=" + result.size() +
                             ", 压缩率=" + (100.0 * (points.size() - result.size()) / points.size()) + "%");
        }
    }

    @Test
    public void testSupportedAlgorithms() {
        List<String> algorithms = compressionService.getSupportedAlgorithms();

        assertTrue(algorithms.contains("DOUGLAS_PEUCKER"));
        assertTrue(algorithms.contains("VISVALINGAM"));
        assertTrue(algorithms.contains("REUMANN_WITKAM"));
        assertTrue(algorithms.contains("PERPENDICULAR_DISTANCE"));
        assertEquals(4, algorithms.size());
    }

    @Test
    public void testAlgorithmDescriptions() {
        // 测试算法描述
        assertNotNull(compressionService.getAlgorithmDescription("DOUGLAS_PEUCKER"));
        assertNotNull(compressionService.getAlgorithmDescription("VISVALINGAM"));
        assertNotNull(compressionService.getAlgorithmDescription("REUMANN_WITKAM"));
        assertNotNull(compressionService.getAlgorithmDescription("PERPENDICULAR_DISTANCE"));

        // 测试算法复杂度
        assertNotNull(compressionService.getAlgorithmComplexity("DOUGLAS_PEUCKER"));
        assertNotNull(compressionService.getAlgorithmComplexity("VISVALINGAM"));
        assertNotNull(compressionService.getAlgorithmComplexity("REUMANN_WITKAM"));
        assertNotNull(compressionService.getAlgorithmComplexity("PERPENDICULAR_DISTANCE"));
    }

    @Test
    public void testInvalidAlgorithm() {
        List<Point> points = createSamplePoints(10);

        // 使用不存在的算法，应该回退到默认算法
        List<Point> result = compressionService.compress(
            points,
            5.0,
            "INVALID_ALGORITHM",
            true,
            3
        );

        // 应该仍然有结果（使用默认算法）
        assertNotNull(result);
        assertTrue(result.size() > 0);
    }

    @Test
    public void testEdgeCases() {
        // 测试点数不足的情况
        List<Point> fewPoints = createSamplePoints(2);
        List<Point> result = compressionService.compress(
            fewPoints,
            5.0,
            "PERPENDICULAR_DISTANCE",
            true,
            3
        );
        assertEquals(2, result.size());

        // 测试空列表
        List<Point> emptyList = new ArrayList<>();
        result = compressionService.compress(
            emptyList,
            5.0,
            "PERPENDICULAR_DISTANCE",
            true,
            3
        );
        assertTrue(result.isEmpty());

        // 测试null
        result = compressionService.compress(
            null,
            5.0,
            "PERPENDICULAR_DISTANCE",
            true,
            3
        );
        assertNull(result);
    }

    // ========== 测试数据生成方法 ==========

    private List<Point> createStraightLine(int count) {
        List<Point> points = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            // 创建一条直线：y = x
            points.add(new Point(i * 0.001, i * 0.001, i * 1000L));
        }
        return points;
    }

    private List<Point> createCurvedLine(int count) {
        List<Point> points = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            // 创建一条正弦曲线
            double x = i * 0.001;
            double y = Math.sin(i * 0.1) * 0.001;
            points.add(new Point(x, y, i * 1000L));
        }
        return points;
    }

    private List<Point> createMixedTrajectory(int count) {
        List<Point> points = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            // 混合直线和曲线段
            double x = i * 0.001;
            double y;
            if (i % 4 < 2) {
                y = i * 0.001; // 直线段
            } else {
                y = Math.sin(i * 0.2) * 0.001; // 曲线段
            }
            points.add(new Point(x, y, i * 1000L));
        }
        return points;
    }

    private List<Point> createTrajectoryWithVaryingCurvature(int count) {
        List<Point> points = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            // 曲率逐渐变化
            double curvature = i / (double) count;
            double x = i * 0.001;
            double y = Math.sin(i * 0.1 * curvature) * 0.001;
            points.add(new Point(x, y, i * 1000L));
        }
        return points;
    }

    private List<Point> createComplexTrajectory(int count) {
        List<Point> points = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            // 创建复杂轨迹：直线 + 曲线 + 折线
            double x = i * 0.001;
            double y;
            if (i % 6 == 0) {
                y = i * 0.001; // 直线
            } else if (i % 6 == 1 || i % 6 == 2) {
                y = Math.sin(i * 0.15) * 0.001; // 曲线
            } else {
                y = (i % 3) * 0.001; // 折线
            }
            points.add(new Point(x, y, i * 1000L));
        }
        return points;
    }

    private List<Point> createSamplePoints(int count) {
        List<Point> points = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            points.add(new Point(39.9 + i * 0.001, 116.4 + i * 0.001, i * 1000L));
        }
        return points;
    }
}