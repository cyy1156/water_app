# 喝水健康陪伴小程序 - 后端项目

## 项目结构

```
src/main/java/com/waterapp/
├── controller/          # 控制器层
│   ├── UserController.java
│   ├── CheckinController.java
│   ├── PointsController.java
│   ├── MallController.java
│   └── ChatController.java
├── service/            # 业务逻辑层
│   ├── UserService.java
│   ├── CheckinService.java
│   ├── PointsService.java
│   ├── MallService.java
│   └── ChatService.java
├── mapper/             # 数据访问层
│   ├── UserMapper.java
│   ├── CheckinRecordMapper.java
│   ├── PointsRecordMapper.java
│   ├── GoodsMapper.java
│   ├── ExchangeRecordMapper.java
│   └── ChatRecordMapper.java
├── entity/             # 实体类
│   ├── User.java
│   ├── CheckinRecord.java
│   ├── PointsRecord.java
│   ├── Goods.java
│   ├── ExchangeRecord.java
│   └── ChatRecord.java
├── dto/                # 数据传输对象
│   ├── LoginRequest.java
│   ├── CheckinRequest.java
│   └── ExchangeRequest.java
├── vo/                 # 视图对象
│   ├── UserVO.java
│   ├── TodayCheckinVO.java
│   └── GoodsVO.java
├── common/             # 公共类
│   ├── Result.java
│   ├── exception/
│   │   ├── BusinessException.java
│   │   └── GlobalExceptionHandler.java
│   └── constant/
│       └── ResponseCode.java
├── config/             # 配置类
│   ├── MyBatisPlusConfig.java
│   ├── RedisConfig.java
│   └── WebMvcConfig.java
├── util/               # 工具类
│   ├── JwtUtil.java
│   └── EncryptionUtil.java
└── interceptor/        # 拦截器
    └── AuthInterceptor.java
```

## 技术栈

- Spring Boot 2.7+
- MyBatis Plus 3.5+
- MySQL 8.0+
- Redis 6.0+
- JWT (jjwt)

## 快速开始

1. 创建数据库 `water_app`
2. 执行 SQL 脚本创建表结构
3. 配置 `application.yml` 中的数据库和Redis连接
4. 运行 `WaterAppApplication.java`

