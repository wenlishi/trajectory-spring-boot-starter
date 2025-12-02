# 轨迹处理自定义Starter

一个用于轨迹数据处理的Spring Boot自定义Starter，提供坐标转换、噪声过滤、轨迹压缩等功能。

## 功能特性

### 1. 坐标转换
- 支持WGS84、GCJ02、BD09坐标系之间的转换
- 解决不同坐标系之间的偏移问题
- 可配置是否保留原始坐标

### 2. 噪声过滤
- 基于速度的漂移点过滤
- 基于精度的低质量点过滤
- 基于距离的异常跳跃点过滤
- 基于时间的异常间隔点过滤

### 3. 轨迹压缩
- 道格拉斯-普克算法（Douglas-Peucker）
- Visvalingam-Whyatt算法
- Reumann-Witkam算法
- 垂距法（Perpendicular Distance）
- 可配置压缩阈值和算法

### 4. 统计信息
- 压缩率统计
- 距离和时间统计
- 速度统计
- 边界框计算

## 项目结构

```
trajectory-processing-starter/
├── src/main/java/com/track/starter/
│   ├── model/
│   │   ├── Point.java                    # 轨迹点对象
│   │   └── TrajectorySummary.java        # 统计结果对象
│   ├── config/
│   │   ├── TrajectoryProperties.java     # 配置属性类
│   │   └── TrajectoryAutoConfiguration.java # 自动配置类
│   ├── service/
│   │   ├── CoordinateTransformService.java # 坐标转换服务
│   │   ├── NoiseFilterService.java       # 噪声过滤服务
│   │   └── CompressionService.java       # 轨迹压缩服务
│   └── pipeline/
│       └── TrajectoryPipeline.java       # 轨迹流水线总管
├── src/main/resources/
│   └── META-INF/
│       ├── spring.factories              # Spring Boot 2.6及以下
│       └── spring/
│           └── org.springframework.boot.autoconfigure.AutoConfiguration.imports # Spring Boot 2.7+
├── src/test/java/com/track/starter/      # 测试代码
├── pom.xml                              # Maven配置
├── README.md                            # 本文档
└── USAGE_EXAMPLE.md                     # 使用示例
```

## 快速开始

### 1. 引入依赖

```xml
<dependency>
    <groupId>com.track</groupId>
    <artifactId>trajectory-processing-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 2. 基础配置

```yaml
trajectory:
  enabled: true
  compression:
    enabled: true
    threshold: 5.0
```

### 3. 在代码中使用

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

## 详细配置

### 完整配置示例

```yaml
trajectory:
  enabled: true

  coordinate:
    enabled: true
    source: WGS84
    target: GCJ02
    keep-original: false

  filter:
    enabled: true
    max-speed: 180.0
    min-speed: 0.5
    max-accuracy: 100.0
    max-time-interval: 300000
    max-distance: 10000

  compression:
    enabled: true
    threshold: 5.0
    algorithm: DOUGLAS_PEUCKER
    keep-start-end: true
    min-points: 3

  statistics:
    enabled: true
    detailed: false
    formatted-output: true
```

### 配置说明

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `trajectory.enabled` | `true` | 是否启用轨迹处理 |
| `trajectory.coordinate.enabled` | `false` | 是否启用坐标转换 |
| `trajectory.coordinate.source` | `WGS84` | 源坐标系 |
| `trajectory.coordinate.target` | `GCJ02` | 目标坐标系 |
| `trajectory.filter.enabled` | `false` | 是否启用噪声过滤 |
| `trajectory.filter.max-speed` | `180.0` | 最大速度阈值 (km/h) |
| `trajectory.compression.enabled` | `true` | 是否启用轨迹压缩 |
| `trajectory.compression.threshold` | `5.0` | 压缩阈值 (米) |
| `trajectory.compression.algorithm` | `DOUGLAS_PEUCKER` | 压缩算法（DOUGLAS_PEUCKER, VISVALINGAM, REUMANN_WITKAM, PERPENDICULAR_DISTANCE） |

## 核心类说明

### Point - 轨迹点对象
```java
Point point = new Point(39.9042, 116.4074, System.currentTimeMillis());
double distance = point1.distanceTo(point2); // 计算距离
double speed = point1.averageSpeedTo(point2); // 计算速度
```

### TrajectoryPipeline - 流水线总管
```java
// 一键处理
List<Point> result = pipeline.process(rawPoints);

// 处理并获取统计信息
TrajectoryPipeline.ProcessingResult result = pipeline.processWithResult(rawPoints);
TrajectorySummary summary = result.getSummary();

// 单独使用某个功能
List<Point> transformed = pipeline.transformOnly(rawPoints);
List<Point> filtered = pipeline.filterOnly(rawPoints);
List<Point> compressed = pipeline.compressOnly(rawPoints);
```

### 底层服务
- `CoordinateTransformService`: 坐标转换服务
- `NoiseFilterService`: 噪声过滤服务
- `CompressionService`: 轨迹压缩服务

## 算法实现

### 道格拉斯-普克算法
- 时间复杂度: O(n log n)
- 空间复杂度: O(n)
- 特点: 保留轨迹形状，适合弯曲轨迹

### Visvalingam-Whyatt算法
- 时间复杂度: O(n log n)
- 空间复杂度: O(n)
- 特点: 基于三角形面积，保留重要点

### Reumann-Witkam算法
- 时间复杂度: O(n)
- 空间复杂度: O(1)
- 特点: 简单快速，适合实时处理

### 垂距法（Perpendicular Distance）
- 时间复杂度: O(n)
- 空间复杂度: O(1)
- 特点: 基于点到相邻两点连线的垂直距离，适合平滑轨迹
- 变种算法:
  - 基本垂距法: 检查当前点与前后相邻点连线的距离
  - 滑动窗口垂距法: 检查当前点与窗口内任意两点连线的距离
  - 自适应垂距法: 根据局部曲率动态调整阈值

## 性能优化建议

1. **批量处理**: 尽量批量处理轨迹点
2. **合理配置**: 根据数据特点调整阈值
3. **选择性启用**: 不需要的功能可以禁用
4. **异步处理**: 大数据量考虑异步处理

## 测试

运行测试：
```bash
cd trajectory-processing-starter
mvn test
```

测试覆盖率：
- 单元测试: 覆盖核心功能
- 集成测试: 验证Spring Boot集成
- 性能测试: 验证处理效率

## 构建和部署

### 本地安装
```bash
mvn clean install
```

### 发布到Maven仓库
```bash
mvn clean deploy -P release
```

## 版本历史

### v1.0.0 (当前版本)
- 初始版本发布
- 支持坐标转换、噪声过滤、轨迹压缩
- 提供完整的配置系统
- 包含详细的使用文档

## 贡献指南

1. Fork 项目
2. 创建功能分支
3. 提交更改
4. 推送到分支
5. 创建Pull Request

## 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情。

## 联系方式

如有问题或建议，请通过以下方式联系：
- 提交Issue
- 发送邮件

---

**注意**: 本Starter已针对Spring Boot 2.7.18优化，确保与你的项目兼容。