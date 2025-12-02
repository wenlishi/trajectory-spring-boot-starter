package com.track.starter;

import com.track.starter.config.TrajectoryProperties;
import com.track.starter.model.Point;
import com.track.starter.model.TrajectorySummary;
import com.track.starter.pipeline.TrajectoryPipeline;
import com.track.starter.service.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TrajectoryPipelineTest {

    @Mock
    private TrajectoryProperties properties;

    @Mock
    private TrajectoryProperties.CoordinateConfig coordinateConfig;

    @Mock
    private TrajectoryProperties.FilterConfig filterConfig;

    @Mock
    private TrajectoryProperties.CompressionConfig compressionConfig;

    @Mock
    private CoordinateTransformService coordinateService;

    @Mock
    private NoiseFilterService filterService;

    @Mock
    private CompressionService compressionService;

    private TrajectoryPipeline pipeline;

    @Before
    public void setUp() {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.getCoordinate()).thenReturn(coordinateConfig);
        when(properties.getFilter()).thenReturn(filterConfig);
        when(properties.getCompression()).thenReturn(compressionConfig);

        when(coordinateConfig.isEnabled()).thenReturn(false);
        when(filterConfig.isEnabled()).thenReturn(false);
        when(compressionConfig.isEnabled()).thenReturn(true);
        when(compressionConfig.getThreshold()).thenReturn(5.0);
        when(compressionConfig.getAlgorithm()).thenReturn("DOUGLAS_PEUCKER");
        when(compressionConfig.isKeepStartEnd()).thenReturn(true);
        when(compressionConfig.getMinPoints()).thenReturn(3);

        pipeline = new TrajectoryPipeline(properties, coordinateService, filterService, compressionService);
    }

    @Test
    public void testPipelineDisabled() {
        when(properties.isEnabled()).thenReturn(false);

        List<Point> points = createSamplePoints(10);
        List<Point> result = pipeline.process(points);

        assertEquals(10, result.size());
        assertEquals(points, result);
    }

    @Test
    public void testPipelineWithCompressionOnly() {
        List<Point> points = createSamplePoints(10);
        List<Point> compressedPoints = createSamplePoints(5);

        when(compressionService.compress(anyList(), anyDouble(), any(), any(), any()))
                .thenReturn(compressedPoints);

        List<Point> result = pipeline.process(points);

        assertEquals(5, result.size());
        assertEquals(compressedPoints, result);
    }

    @Test
    public void testPipelineWithAllStages() {
        // 启用所有阶段
        when(coordinateConfig.isEnabled()).thenReturn(true);
        when(coordinateConfig.getSource()).thenReturn("WGS84");
        when(coordinateConfig.getTarget()).thenReturn("GCJ02");
        when(filterConfig.isEnabled()).thenReturn(true);
        when(filterConfig.getMaxSpeed()).thenReturn(180.0);
        when(filterConfig.getMinSpeed()).thenReturn(0.5);
        when(filterConfig.getMaxAccuracy()).thenReturn(100.0);
        when(filterConfig.getMaxTimeInterval()).thenReturn(300000L);
        when(filterConfig.getMaxDistance()).thenReturn(10000.0);

        List<Point> points = createSamplePoints(20);
        List<Point> transformedPoints = createSamplePoints(20);
        List<Point> filteredPoints = createSamplePoints(15);
        List<Point> compressedPoints = createSamplePoints(8);

        when(coordinateService.transform(anyList(), any(), any()))
                .thenReturn(transformedPoints);
        when(filterService.filter(anyList(), anyDouble(), anyDouble(), anyDouble(), any(), anyDouble()))
                .thenReturn(filteredPoints);
        when(compressionService.compress(anyList(), anyDouble(), any(), any(), any()))
                .thenReturn(compressedPoints);

        List<Point> result = pipeline.process(points);

        assertEquals(8, result.size());
        assertEquals(compressedPoints, result);
    }

    @Test
    public void testProcessWithSummary() {
        List<Point> points = createSamplePoints(10);
        List<Point> compressedPoints = createSamplePoints(5);

        when(compressionService.compress(anyList(), anyDouble(), any(), any(), any()))
                .thenReturn(compressedPoints);

        TrajectoryPipeline.ProcessingResult result = pipeline.processWithResult(points);

        assertNotNull(result);
        assertEquals(5, result.getProcessedPoints().size());
        assertNotNull(result.getSummary());
        assertEquals(10, result.getSummary().getOriginalPointCount());
        assertEquals(5, result.getSummary().getProcessedPointCount());
        assertEquals(50.0, result.getCompressionRate(), 0.01);
    }

    @Test
    public void testTransformOnly() {
        when(coordinateConfig.isEnabled()).thenReturn(true);
        when(coordinateConfig.getSource()).thenReturn("WGS84");
        when(coordinateConfig.getTarget()).thenReturn("GCJ02");

        List<Point> points = createSamplePoints(10);
        List<Point> transformedPoints = createSamplePoints(10);

        when(coordinateService.transform(anyList(), any(), any()))
                .thenReturn(transformedPoints);

        List<Point> result = pipeline.transformOnly(points);

        assertEquals(10, result.size());
        assertEquals(transformedPoints, result);
    }

    @Test
    public void testFilterOnly() {
        when(filterConfig.isEnabled()).thenReturn(true);

        List<Point> points = createSamplePoints(10);
        List<Point> filteredPoints = createSamplePoints(8);

        when(filterService.filter(anyList(), anyDouble(), anyDouble(), anyDouble(), any(), anyDouble()))
                .thenReturn(filteredPoints);

        List<Point> result = pipeline.filterOnly(points);

        assertEquals(8, result.size());
        assertEquals(filteredPoints, result);
    }

    @Test
    public void testCompressOnly() {
        List<Point> points = createSamplePoints(10);
        List<Point> compressedPoints = createSamplePoints(5);

        when(compressionService.compress(anyList(), anyDouble(), any(), any(), any()))
                .thenReturn(compressedPoints);

        List<Point> result = pipeline.compressOnly(points);

        assertEquals(5, result.size());
        assertEquals(compressedPoints, result);
    }

    @Test
    public void testGetPipelineInfo() {
        String info = pipeline.getPipelineInfo();

        assertNotNull(info);
        assertTrue(info.contains("轨迹处理流水线配置"));
        assertTrue(info.contains("坐标转换"));
        assertTrue(info.contains("噪声过滤"));
        assertTrue(info.contains("轨迹压缩"));
    }

    @Test
    public void testEmptyPoints() {
        List<Point> points = new ArrayList<>();
        List<Point> result = pipeline.process(points);

        assertTrue(result.isEmpty());
    }

    @Test
    public void testNullPoints() {
        List<Point> result = pipeline.process(null);

        assertNull(result);
    }

    @Test
    public void testPointDistanceCalculation() {
        Point p1 = new Point(39.9042, 116.4074, 1000);
        Point p2 = new Point(39.9092, 116.3974, 2000);

        double distance = p1.distanceTo(p2);
        long timeDiff = p1.timeDiffTo(p2);
        double speed = p1.averageSpeedTo(p2);

        assertTrue(distance > 0);
        assertEquals(1000, timeDiff);
        assertTrue(speed > 0);
    }

    @Test
    public void testTrajectorySummary() {
        List<Point> original = createSamplePoints(10);
        List<Point> processed = createSamplePoints(5);

        TrajectorySummary summary = TrajectorySummary.fromPoints(original, processed, 100);

        assertNotNull(summary);
        assertEquals(10, summary.getOriginalPointCount());
        assertEquals(5, summary.getProcessedPointCount());
        assertEquals(50.0, summary.getCompressionRate(), 0.01);
        assertEquals(100, summary.getProcessingTime());
        assertEquals(5, summary.getFilteredPointCount());
    }

    private List<Point> createSamplePoints(int count) {
        List<Point> points = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            double lat = 39.9 + i * 0.001;
            double lng = 116.4 + i * 0.001;
            long timestamp = 1000L * i;
            points.add(new Point(lat, lng, timestamp));
        }
        return points;
    }
}