# 安康医院体检管理系统

基于 Spring Boot 3、Spring MVC、Spring Data JPA、Thymeleaf、MySQL 和 Maven 的医院体检管理系统。

## 功能

- 管理员登录与工作台统计
- 手机短信验证码注册
- 用户名/手机号密码登录和短信验证码登录
- BCrypt 单向加密保存密码
- 受检人健康档案管理
- 医生、检查项目和体检套餐管理
- 科室基础信息维护，医生与检查项目关联科室
- 科室输入实时查询下拉选择
- 个人体检预约与单位团体体检预约
- 单位信息、联系人和全体参检人关联管理
- 单位参检人 Excel 标准模板下载与一键批量导入
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

## 短信验证码

默认使用开发短信发送器，验证码会输出到服务日志，并在本地页面显示，便于开发测试：

```bash
export SMS_PROVIDER=logging
export SMS_DEV_MODE=true
```

生产环境应实现 `SmsSender` 接口对接阿里云、腾讯云等短信服务，并设置 `SMS_DEV_MODE=false`，避免验证码出现在接口响应中。验证码有效期为 5 分钟，同一手机号 60 秒内不可重复发送，验证成功后立即失效。

## 首页背景图热替换

首页背景图位于 `runtime-assets/home-hero.png`。运行期间直接覆盖这个文件并刷新浏览器即可看到新图片，无需重启服务。建议使用 16:9 横图，分辨率不低于 1600×900。
