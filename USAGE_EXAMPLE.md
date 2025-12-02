# 轨迹处理Starter使用指南

## 1. 引入依赖

在你的Spring Boot项目中，添加以下依赖：

```xml
<dependency>
    <groupId>com.track</groupId>
    <artifactId>trajectory-processing-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

## 2. 配置示例

在 `application.yml` 或 `application.properties` 中配置：

### application.yml 示例

```yaml
trajectory:
  enabled: true  # 启用轨迹处理

  coordinate:
    enabled: true  # 启用坐标转换
    source: WGS84  # 源坐标系
    target: GCJ02  # 目标坐标系（火星坐标系）
    keep-original: false  # 是否保留原始坐标

  filter:
    enabled: true  # 启用噪声过滤
    max-speed: 180.0  # 最大速度阈值 (km/h)
    min-speed: 0.5    # 最小速度阈值 (km/h)
    max-accuracy: 100.0  # 最大精度阈值 (米)
    max-time-interval: 300000  # 最大时间间隔 (5分钟)
    max-distance: 10000  # 最大距离阈值 (10公里)

  compression:
    enabled: true  # 启用轨迹压缩
    threshold: 5.0  # 距离阈值(米)，越小越精准，越大越粗糙
    algorithm: DOUGLAS_PEUCKER  # 压缩算法
    keep-start-end: true  # 是否保留起点和终点
    min-points: 3  # 最小点数

  statistics:
    enabled: true  # 生成统计信息
    detailed: false  # 是否计算详细统计
    formatted-output: true  # 是否输出格式化统计信息
```

### application.properties 示例

```properties
trajectory.enabled=true

trajectory.coordinate.enabled=true
trajectory.coordinate.source=WGS84
trajectory.coordinate.target=GCJ02
trajectory.coordinate.keep-original=false

trajectory.filter.enabled=true
trajectory.filter.max-speed=180.0
trajectory.filter.min-speed=0.5
trajectory.filter.max-accuracy=100.0
trajectory.filter.max-time-interval=300000
trajectory.filter.max-distance=10000

trajectory.compression.enabled=true
trajectory.compression.threshold=5.0
trajectory.compression.algorithm=DOUGLAS_PEUCKER
trajectory.compression.keep-start-end=true
trajectory.compression.min-points=3

trajectory.statistics.enabled=true
trajectory.statistics.detailed=false
trajectory.statistics.formatted-output=true
```

## 3. 在代码中使用

### 3.1 基本使用

```java
@Service
public class TrackService {

    @Autowired
    private TrajectoryPipeline pipeline;

    public void processTrack(List<Point> rawPoints) {
        // 一键处理：坐标转换 -> 噪声过滤 -> 轨迹压缩
        List<Point> cleanPoints = pipeline.process(rawPoints);

        // 保存到数据库
        trackRepository.save(cleanPoints);
    }
}
```

### 3.2 获取统计信息

```java
@Service
public class TrackService {

    @Autowired
    private TrajectoryPipeline pipeline;

    public ProcessingResult processTrackWithStats(List<Point> rawPoints) {
        // 处理并获取统计信息
        TrajectoryPipeline.ProcessingResult result = pipeline.processWithResult(rawPoints);

        // 获取处理后的点
        List<Point> cleanPoints = result.getProcessedPoints();

        // 获取统计信息
        TrajectorySummary summary = result.getSummary();
        System.out.println("压缩率: " + summary.getCompressionRate() + "%");
        System.out.println("处理耗时: " + summary.getProcessingTime() + "ms");
        System.out.println("总距离: " + summary.getTotalDistance() + "米");

        // 保存到数据库
        trackRepository.save(cleanPoints);

        return result;
    }
}
```

### 3.3 单独使用某个功能

```java
@Service
public class TrackService {

    @Autowired
    private TrajectoryPipeline pipeline;

    public void customProcess(List<Point> rawPoints) {
        // 只进行坐标转换
        List<Point> transformed = pipeline.transformOnly(rawPoints);

        // 只进行噪声过滤
        List<Point> filtered = pipeline.filterOnly(transformed);

        // 只进行轨迹压缩
        List<Point> compressed = pipeline.compressOnly(filtered);

        // 或者使用底层服务直接调用
        @Autowired
        private CompressionService compressionService;

        List<Point> customCompressed = compressionService.compress(
            filtered,
            10.0,  // 自定义阈值
            "VISVALINGAM",  // 自定义算法
            true,  // 保留首尾
            5  // 最小点数
        );
    }
}
```

## 4. 点对象创建

```java
// 创建基本点
Point point1 = new Point(39.9042, 116.4074, System.currentTimeMillis());

// 创建带额外信息的点
Point point2 = new Point(
    39.9042,  // 纬度
    116.4074, // 经度
    System.currentTimeMillis(), // 时间戳
    50.0,     // 海拔高度（可选）
    30.5,     // 速度 km/h（可选）
    180.0,    // 方向角 0-360度（可选）
    10.0      // 精度 米（可选）
);

// 计算两点距离
double distance = point1.distanceTo(point2); // 单位：米

// 计算时间差
long timeDiff = point1.timeDiffTo(point2); // 单位：毫秒

// 计算平均速度
double speed = point1.averageSpeedTo(point2); // 单位：km/h
```

## 5. 支持的坐标系

- **WGS84**: 国际标准坐标系（GPS使用）
- **GCJ02**: 中国国家测绘局制定的坐标系（火星坐标系）
- **BD09**: 百度坐标系

## 6. 支持的压缩算法

- **DOUGLAS_PEUCKER**: 道格拉斯-普克算法（默认）- 递归分割，保留形状特征
- **VISVALINGAM**: Visvalingam-Whyatt算法 - 基于三角形面积，保留重要点
- **REUMANN_WITKAM**: Reumann-Witkam算法 - 简单快速，适合实时处理
- **PERPENDICULAR_DISTANCE**: 垂距法 - 基于点到相邻两点连线的垂直距离，适合平滑轨迹

### 垂距法变种（可通过CompressionService直接调用）：
- **基本垂距法**: 检查当前点与前后相邻点连线的距离
- **滑动窗口垂距法**: 检查当前点与窗口内任意两点连线的距离
- **自适应垂距法**: 根据局部曲率动态调整阈值

## 7. 配置验证

Starter会自动验证配置的有效性，如果配置错误会抛出异常：

```java
@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    public CommandLineRunner validateConfig(TrajectoryPipeline pipeline) {
        return args -> {
            try {
                pipeline.validateConfig();
                System.out.println("轨迹处理配置验证通过");
            } catch (IllegalArgumentException e) {
                System.err.println("配置错误: " + e.getMessage());
            }
        };
    }
}
```

## 8. 日志输出

Starter会输出详细的处理日志：

```
开始轨迹处理流水线，原始点数: 100
坐标转换完成，耗时: 50ms
噪声过滤完成，过滤点数: 20，耗时: 30ms
轨迹压缩完成，耗时: 80ms
轨迹处理完成: 原始点数=100, 处理后点数=65, 压缩率=35.00%, 耗时=160ms
```

## 9. 高级功能

### 9.1 自定义配置

```java
@Configuration
public class TrajectoryConfig {

    @Bean
    public TrajectoryProperties trajectoryProperties() {
        TrajectoryProperties properties = new TrajectoryProperties();
        properties.setEnabled(true);

        TrajectoryProperties.CompressionConfig compression = new TrajectoryProperties.CompressionConfig();
        compression.setEnabled(true);
        compression.setThreshold(10.0);
        compression.setAlgorithm("VISVALINGAM");
        properties.setCompression(compression);

        return properties;
    }
}
```

### 9.3 使用垂距法变种算法

```java
@Service
public class AdvancedTrackService {

    @Autowired
    private CompressionService compressionService;

    public void processWithAdvancedAlgorithms(List<Point> points) {
        // 1. 使用滑动窗口垂距法（窗口大小=3）
        List<Point> windowCompressed = compressionService.perpendicularDistanceWithWindow(
            points,
            5.0,    // 阈值
            true,   // 保留首尾
            3       // 窗口大小
        );

        // 2. 使用自适应垂距法（根据曲率动态调整阈值）
        List<Point> adaptiveCompressed = compressionService.adaptivePerpendicularDistance(
            points,
            5.0,    // 基础阈值
            true    // 保留首尾
        );

        // 3. 获取算法信息
        System.out.println("支持的算法: " + compressionService.getSupportedAlgorithms());
        System.out.println("道格拉斯-普克算法描述: " + compressionService.getAlgorithmDescription("DOUGLAS_PEUCKER"));
        System.out.println("垂距法复杂度: " + compressionService.getAlgorithmComplexity("PERPENDICULAR_DISTANCE"));
    }
}
```

### 9.4 算法选择建议

| 场景 | 推荐算法 | 理由 |
|------|----------|------|
| 弯曲轨迹 | DOUGLAS_PEUCKER | 保留形状特征最好 |
| 平滑轨迹 | PERPENDICULAR_DISTANCE | 计算简单，效果良好 |
| 实时处理 | REUMANN_WITKAM | 时间复杂度O(n)，最快 |
| 需要保留重要点 | VISVALINGAM | 基于三角形面积，保留关键点 |
| 轨迹曲率变化大 | 自适应垂距法 | 动态调整阈值，适应不同曲率 |

### 9.5 性能对比

```java
public void benchmarkAlgorithms(List<Point> points) {
    long startTime, endTime;

    // 测试道格拉斯-普克算法
    startTime = System.currentTimeMillis();
    List<Point> dpResult = compressionService.compress(points, 5.0, "DOUGLAS_PEUCKER", true, 3);
    endTime = System.currentTimeMillis();
    System.out.println("道格拉斯-普克算法耗时: " + (endTime - startTime) + "ms, 压缩率: " +
                      (100.0 * (points.size() - dpResult.size()) / points.size()) + "%");

    // 测试垂距法
    startTime = System.currentTimeMillis();
    List<Point> pdResult = compressionService.compress(points, 5.0, "PERPENDICULAR_DISTANCE", true, 3);
    endTime = System.currentTimeMillis();
    System.out.println("垂距法耗时: " + (endTime - startTime) + "ms, 压缩率: " +
                      (100.0 * (points.size() - pdResult.size()) / points.size()) + "%");
}
```

### 9.2 禁用特定功能

```yaml
trajectory:
  enabled: true
  coordinate:
    enabled: false  # 禁用坐标转换
  filter:
    enabled: false  # 禁用噪声过滤
  compression:
    enabled: true   # 只启用压缩
```

## 10. 性能建议

1. **批量处理**: 尽量批量处理轨迹点，减少IO开销
2. **合理配置阈值**: 根据实际需求调整压缩阈值
3. **选择性启用**: 不需要的功能可以禁用
4. **监控统计**: 关注压缩率和处理耗时，优化配置

## 11. 故障排除

### 11.1 坐标转换失败
- 检查源坐标系和目标坐标系是否支持
- 确认坐标值是否在合理范围内

### 11.2 压缩率过低
- 调整压缩阈值
- 尝试不同的压缩算法
- 检查原始数据质量

### 11.3 处理速度慢
- 减少不必要的处理步骤
- 调整过滤阈值
- 考虑异步处理

## 12. 版本兼容性

- Spring Boot: 2.7.x
- Java: 1.8+
- 需要 Lombok 插件支持

---

通过这个Starter，你可以轻松实现轨迹数据的清洗、过滤和压缩，大大减少存储空间，提高数据处理效率。