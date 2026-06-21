USE club_db;

SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE comments;
TRUNCATE TABLE topics;
TRUNCATE TABLE announcements;
TRUNCATE TABLE activity_registrations;
TRUNCATE TABLE activities;
TRUNCATE TABLE clubs;
TRUNCATE TABLE users;
SET FOREIGN_KEY_CHECKS = 1;

-- 插入用户 (密码均为 123456 的明文，实际应用中由于明文回退机制可直接登录)
INSERT INTO users (id, username, password, real_name, student_id, role, avatar) VALUES 
(1, 'admin', '123456', '系统管理员', 'ADMIN001', 'ADMIN', 'https://images.unsplash.com/photo-1599566150163-29194dcaad36?w=150&h=150&fit=crop'),
(2, 'union_admin', '123456', '社联主席王老师', 'UNION001', 'UNION_ADMIN', 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150&h=150&fit=crop'),
(3, 'leader_basketball', '123456', '李四(篮球社)', '10001', 'CLUB_LEADER', 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150&h=150&fit=crop'),
(4, 'leader_photo', '123456', '张伟(摄影协会)', '10002', 'CLUB_LEADER', 'https://images.unsplash.com/photo-1527980965255-d3b416303d12?w=150&h=150&fit=crop'),
(5, 'leader_dance', '123456', '王芳(街舞社)', '10003', 'CLUB_LEADER', 'https://images.unsplash.com/photo-1438761681033-6461ffad8d80?w=150&h=150&fit=crop'),
(6, 'leader_debate', '123456', '陈明(辩论社)', '10004', 'CLUB_LEADER', 'https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=150&h=150&fit=crop'),

-- 普通成员
(7, 'student1', '123456', '赵雷', '20001', 'MEMBER', 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150&h=150&fit=crop'),
(8, 'student2', '123456', '钱枫', '20002', 'MEMBER', 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150&h=150&fit=crop'),
(9, 'student3', '123456', '孙俪', '20003', 'MEMBER', 'https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=150&h=150&fit=crop'),
(10, 'student4', '123456', '周杰', '20004', 'MEMBER', 'https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?w=150&h=150&fit=crop'),
(11, 'student5', '123456', '吴亦', '20005', 'MEMBER', 'https://images.unsplash.com/photo-1554151228-14d9def656e4?w=150&h=150&fit=crop'),
(12, 'club_leader1', '123456', '测试社团负责人', '20260001', 'CLUB_LEADER', 'https://api.dicebear.com/7.x/avataaars/svg?seed=cl1');

-- 插入社团
INSERT INTO clubs (id, name, description, leader_id, status) VALUES 
(1, '篮球社', '热爱篮球，挥洒青春。每周定期组织院系对抗赛和基础训练。', 3, 'NORMAL'),
(2, '摄影协会', '用镜头定格校园的美好瞬间，提供单反借用与修图培训。', 4, 'NORMAL'),
(3, '街舞社', '释放活力，展示自我。涵盖Popping, Hip-Hop, Breaking等多个舞种。', 5, 'NORMAL'),
(4, '辩论社', '唇枪舌剑，思维碰撞。培养逻辑思维与表达能力，每年举办新生杯辩论赛。', 6, 'NORMAL');

-- 同步负责人的club_id
UPDATE users SET club_id = 1 WHERE id = 3;
UPDATE users SET club_id = 2 WHERE id = 4;
UPDATE users SET club_id = 3 WHERE id = 5;
UPDATE users SET club_id = 4 WHERE id = 6;
UPDATE users SET club_id = 1 WHERE id = 12;

-- 添加部分普通成员入社记录（模拟，在真实系统中可能还需要一个 user_club 关系表，但目前用户表自带单个 club_id 标识所属）
UPDATE users SET club_id = 1 WHERE id IN (7, 8);
UPDATE users SET club_id = 2 WHERE id IN (9, 10);
UPDATE users SET club_id = 4 WHERE id IN (11);

-- 插入活动
INSERT INTO activities (id, title, description, club_id, start_time, end_time, location, max_count, budget, process, status, reject_reason) VALUES 
(1, '2026春季校园篮球迎新赛', '针对大一新生的篮球友谊赛，欢迎踊跃报名！', 1, DATE_ADD(NOW(), INTERVAL 3 DAY), DATE_ADD(NOW(), INTERVAL 4 DAY), '东区室外篮球场', 50, 1500.00, '首日签到、分组对抗、次日决赛、颁奖总结', 'APPROVED', NULL),
(2, '室内投篮训练营', '提升投篮技巧的特训，专业教练指导。', 1, DATE_ADD(NOW(), INTERVAL 10 DAY), DATE_ADD(NOW(), INTERVAL 11 DAY), '西区体育馆篮球馆', 30, 500.00, '基础教学、动作纠正、自由投篮', 'PENDING_UNION', NULL),
(3, '春日校园风光摄影外拍', '漫步校园，捕捉春暖花开的景色，活动结束后将有作品评比。', 2, DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY), '综合楼前广场集合', 40, 200.00, '集合讲解、分组外拍、作品回收、线上评比', 'FINISHED', NULL),
(4, '暗房冲洗技术分享会', '胶片摄影爱好者的福音，分享黑白胶片冲印流程。', 2, DATE_ADD(NOW(), INTERVAL 2 DAY), DATE_ADD(NOW(), INTERVAL 2 DAY), '实验楼A栋302', 20, 800.00, '理论讲解、冲洗演示、学员实践、总结分享', 'REJECTED', '预算过高，请重新调整器材耗材费用的申报'),
(5, '街舞迎新狂欢夜', '大型校园街舞晚会，各舞种轮番上演，燃爆全场！', 3, DATE_ADD(NOW(), INTERVAL 15 DAY), DATE_ADD(NOW(), INTERVAL 15 DAY), '大学生活动中心报告厅', 300, 3500.00, '开场舞、各队展示、观众互动、自由齐舞', 'APPROVED', NULL),
(6, '“新生杯”辩论赛初赛', '逻辑的较量，第一轮淘汰赛正式打响。', 4, DATE_ADD(NOW(), INTERVAL 7 DAY), DATE_ADD(NOW(), INTERVAL 7 DAY), '第三教学楼阶梯教室', 100, 300.00, '赛前抽签、正反方陈词、自由辩论、评委点评', 'APPROVED', NULL);

-- 活动报名与反馈
INSERT INTO activity_registrations (id, activity_id, user_id, status, rating, feedback, reply) VALUES 
(1, 1, 7, 'REGISTERED', NULL, NULL, NULL),
(2, 1, 8, 'REGISTERED', NULL, NULL, NULL),
(3, 1, 9, 'REGISTERED', NULL, NULL, NULL),
(4, 3, 9, 'SIGNED_IN', 5, '活动非常棒！借到了很好的长焦镜头，学长教得很细心。', '谢谢支持！下次可以参加我们的夜景外拍。'),
(5, 3, 10, 'SIGNED_IN', 4, '路线规划还可以再优化一下，走得有点累。', '收到建议，下次我们会安排得更合理！'),
(6, 3, 11, 'SIGNED_IN', 5, '拍到了很多好看的照片，开心！', NULL),
(7, 5, 7, 'REGISTERED', NULL, NULL, NULL),
(8, 5, 11, 'REGISTERED', NULL, NULL, NULL),
(9, 6, 10, 'REGISTERED', NULL, NULL, NULL);

-- 公告表
INSERT INTO announcements (id, title, content, publisher_id, club_id) VALUES 
(1, '2026年度百团大战（社团招新）启动通知', '同学们好！本年度社团招新活动将于下周三在博学广场隆重举办，届时全校60余个学生社团将带来丰富多彩的展示活动，欢迎大家前往了解并加入！', 2, NULL),
(2, '关于下发《学生社团活动场地审批管理办法》的通知', '各社团负责人注意，即日起所有使用室内场馆的活动，必须提前7个工作日提交场地租赁申请表，并在系统中完成活动报备！', 2, NULL),
(3, '篮球社新队服统一采购通知', '请各位社员于本周日前在群内填写尺码统计表，我们将统一向赞助商提交采购订单。', 3, 1),
(4, '摄影协会近期参赛信息同步', '全国大学生摄影大赛的通知已经下发，有意向参赛的同学可以联系社长索取报名表和作品要求。', 4, 2);

-- 话题讨论表
INSERT INTO topics (id, title, content, author_id, club_id, audit_status, status) VALUES 
(1, '【求助】想学单反，预算5000以内求推荐', 'RT，完全的新手，平时主要拍人像和风景，是买微单好还是单反好？求摄影社的大佬解答！', 7, 2, 'APPROVED', 'NORMAL'),
(2, '街舞社今晚的齐舞太炸了！！！', '刚在操场看到街舞社的排练，大家动作好整齐，氛围绝了！想问下大大二的还能报名加入吗，完全没有基础可以进去学吗？', 9, 3, 'APPROVED', 'TOP'),
(3, '篮球新生杯赛程预测', '下周就要开始了，大家觉得哪个院能拿冠军？听说软工院来了一个一米九的特长生。', 8, 1, 'APPROVED', 'NORMAL');

-- 评论表
INSERT INTO comments (id, topic_id, author_id, content, reply_id) VALUES 
(1, 1, 4, '5000预算的话，推荐你考虑二手微单，比如索尼A6400或者富士X-T20，套机完全够日常练习了。单反现在有点太重了，新手带出去的意愿会低很多。', NULL),
(2, 1, 7, '哇，谢谢社长！我这就去闲鱼看看A6400的价格！', 1),
(3, 2, 5, '哈哈哈谢谢夸奖，我们可以随时加入的！社团内部有零基础启蒙班，会有大二大三的骨干从最基础的bounce教起，欢迎下周来活动中心体验！', NULL),
(4, 2, 9, '太好了，下周一定来！', 3),
(5, 3, 3, '今年确实藏龙卧虎，不过我们院也不是吃素的，大家场上见真章吧！', NULL);
