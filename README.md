# 安康医院体检管理系统

基于 Spring Boot 3、Spring MVC、Spring Data JPA、Thymeleaf、MySQL 和 Maven 的医院体检管理系统。

## 功能

- 管理员登录与工作台统计
- 受检人健康档案管理
- 医生、检查项目和体检套餐管理
- 体检登记与状态跟踪
- 分项目检查结果录入及异常标记
- 总检结论、健康建议和打印版体检报告

## 环境要求

- JDK 17 或更高版本
- Maven 3.9+
- MySQL 8.0+

## 启动

1. 创建数据库：

   ```sql
   CREATE DATABASE physical_examination CHARACTER SET utf8mb4;
   ```

2. 修改 `src/main/resources/application.yml` 中的数据库账号密码，或设置环境变量：

   ```bash
   export DB_USERNAME=root
   export DB_PASSWORD=你的密码
   ```

3. 启动项目：

   ```bash
   mvn spring-boot:run
   ```

4. 访问 http://localhost:8080

默认管理员账号为 `admin`，密码为 `123456`。生产环境请通过 `ADMIN_USERNAME` 和 `ADMIN_PASSWORD` 环境变量修改。

首次启动会自动建表，并初始化示例医生、检查项目和体检套餐。
