# 高校学生社团管理系统 (University Club Manager)

本项目是一个为高校打造的社团管理全流程解决方案，包含社团入驻、活动审批、成员互动等核心功能。

## 🛠 技术栈
- **Frontend**: Vue 3 + Element Plus + Pinia + Vite
- **Backend**: SpringBoot 3 + MyBatis-Plus + Spring Security (JWT)
- **Database**: MySQL 8.0
- **Deployment**: Docker + Docker Compose

## How to Run
1. 确保已安装 **Docker Desktop**。
2. 在项目根目录执行：
   ```bash
   docker compose up --build -d
   ```
3. 等待容器启动完成。

## Services
- **前端页面**: [http://localhost:3453](http://localhost:3453)
- **后端 API 文档**: [http://localhost:8453/swagger-ui/index.html](http://localhost:8453/swagger-ui/index.html)
- **MySQL 数据库**: `localhost:3306` (用户名/密码: root/root)

## 🧪 测试账号
- **超级管理员**: `admin` / `123456`
- **社团负责人**: `club_leader1` / `123456`
- **普通学生**: `student1` / `123456`

## ✨ 核心特性
- **现代 UI**: 基于 Glassmorphism (玻璃拟态) 设计风格，审美超前。
- **全生命周期活动管理**: 从申请到审核，再到报名、签到、反馈的闭环。
- **动态权限控制**: 多级权限隔离，确保数据安全。
- **高性能**: 容器化一键部署，阿里源加速构建。

## Verification
1. 访问前端地址 `http://localhost:3453`，使用账号 `admin` / `123456` 登录。
2. 在“活动中心”标签页可以查看并管理全校所有活动，在“数据看板”可查看全校统计。
3. 退出登录并使用 `club_leader1` / `123456` 登录，可进入“社团管理”及“管理看板”查看社团专属后台。
4. 使用 `student1` / `123456` 登录，可以测试通过前台交互社区（帖子/评论）和活动报名。
5. 访问 API 文档测试后端接口连通性。
