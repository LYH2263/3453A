USE club_db;

-- 操作日志表
CREATE TABLE IF NOT EXISTS operation_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL COMMENT '操作人',
    operation VARCHAR(100) NOT NULL COMMENT '操作描述',
    method VARCHAR(255) COMMENT '请求方法',
    params TEXT COMMENT '请求参数',
    time BIGINT COMMENT '执行时长(毫秒)',
    ip VARCHAR(64) COMMENT 'IP地址',
    status TINYINT(1) DEFAULT 1 COMMENT '状态(0:失败 1:成功)',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) COMMENT='操作日志表';

-- 登录日志表
CREATE TABLE IF NOT EXISTS login_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL COMMENT '登录账号',
    ip VARCHAR(64) COMMENT '登录IP',
    location VARCHAR(255) COMMENT '登录地点',
    browser VARCHAR(100) COMMENT '浏览器',
    os VARCHAR(100) COMMENT '操作系统',
    status TINYINT(1) DEFAULT 1 COMMENT '状态(0:失败 1:成功)',
    msg VARCHAR(255) COMMENT '提示消息',
    login_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间'
) COMMENT='登录日志表';

-- 异常日志表
CREATE TABLE IF NOT EXISTS exception_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) COMMENT '操作人',
    operation VARCHAR(100) COMMENT '操作描述',
    method VARCHAR(255) COMMENT '方法名',
    params TEXT COMMENT '参数',
    exception_name VARCHAR(255) COMMENT '异常名称',
    exception_message TEXT COMMENT '异常信息',
    stack_trace TEXT COMMENT '堆栈详情',
    ip VARCHAR(64) COMMENT 'IP地址',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '发生时间'
) COMMENT='异常日志表';

-- 审核流程配置表
CREATE TABLE IF NOT EXISTS audit_configs (
    id INT AUTO_INCREMENT PRIMARY KEY,
    type VARCHAR(50) NOT NULL UNIQUE COMMENT '业务类型(ACTIVITY, CLUB_REG, etc.)',
    nodes TEXT NOT NULL COMMENT '审核节点JSON (顺序)',
    is_active TINYINT(1) DEFAULT 1 COMMENT '是否激活',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) COMMENT='审核流程配置表';
