INSERT INTO tb_role (name) VALUES ('ROLE_ADMIN');
INSERT INTO tb_role (name) VALUES ('ROLE_USER');

INSERT INTO tb_account (id,created_at,updated_at,deleted,name,email,blog_id,password) VALUES (0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,false,'김환','akon47@naver.com','@kim-hwan','$2a$10$mFOn2DL3wDPtQPWtl4pRfeqRjJad7BFpGoOmFrQcJ1qnqvcAahota');
INSERT INTO tb_account (id,created_at,updated_at,deleted,name,email,blog_id,password) VALUES (1,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,false,'김환-2','akon47-2@naver.com','@kim-hwan-2','$2a$10$mFOn2DL3wDPtQPWtl4pRfeqRjJad7BFpGoOmFrQcJ1qnqvcAahota');

INSERT INTO tb_account_role (id,created_at,updated_at,account_id,role_name) VALUES (0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,0,'ROLE_USER');
INSERT INTO tb_account_role (id,created_at,updated_at,account_id,role_name) VALUES (1,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,0,'ROLE_ADMIN');

INSERT INTO tb_account_role (id,created_at,updated_at,account_id,role_name) VALUES (2,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,1,'ROLE_USER');
INSERT INTO tb_account_role (id,created_at,updated_at,account_id,role_name) VALUES (3,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,1,'ROLE_ADMIN');

INSERT INTO tb_post (id,created_at,updated_at,account_id,title,content,summary,open_type,post_url,deleted,hits) VALUES (0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,0,'제목','내용','요약','PUBLIC','post_url',false,0);
INSERT INTO tb_post (id,created_at,updated_at,account_id,title,content,summary,open_type,post_url,deleted,hits) VALUES (1,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,0,'제목-2','내용-2','요약-2','PUBLIC','post_url_2',false,0);
INSERT INTO tb_post (id,created_at,updated_at,account_id,title,content,summary,open_type,post_url,deleted,hits) VALUES (2,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,0,'제목-3','내용-3','요약-3','PUBLIC','post_url_3',false,0);
INSERT INTO tb_post (id,created_at,updated_at,account_id,title,content,summary,open_type,post_url,deleted,hits) VALUES (3,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,0,'제목-4','내용-4','요약-4','PUBLIC','post_url_4',false,0);
INSERT INTO tb_post (id,created_at,updated_at,account_id,title,content,summary,open_type,post_url,deleted,hits) VALUES (4,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,0,'제목-5','내용-5','요약-5','PUBLIC','post_url_5',false,0);
INSERT INTO tb_post (id,created_at,updated_at,account_id,title,content,summary,open_type,post_url,deleted,hits) VALUES (5,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,0,'제목-6','내용-6','요약-6','PUBLIC','post_url_6',false,0);
INSERT INTO tb_post (id,created_at,updated_at,account_id,title,content,summary,open_type,post_url,deleted,hits) VALUES (6,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,0,'제목-7','내용-7','요약-7','PUBLIC','post_url_7',false,0);
INSERT INTO tb_post (id,created_at,updated_at,account_id,title,content,summary,open_type,post_url,deleted,hits) VALUES (7,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,0,'제목-8','내용-8','요약-8','PUBLIC','post_url_8',false,0);
INSERT INTO tb_post (id,created_at,updated_at,account_id,title,content,summary,open_type,post_url,deleted,hits) VALUES (8,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,0,'제목-9','내용-9','요약-9','PUBLIC','post_url_9',false,0);
INSERT INTO tb_post (id,created_at,updated_at,account_id,title,content,summary,open_type,post_url,deleted,hits) VALUES (9,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,0,'제목-10','내용-10','요약-10','PUBLIC','post_url_10',false,0);
INSERT INTO tb_post (id,created_at,updated_at,account_id,title,content,summary,open_type,post_url,deleted,hits) VALUES (10,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,0,'제목-11','내용-11','요약-11','PUBLIC','post_url_11',false,0);
INSERT INTO tb_post (id,created_at,updated_at,account_id,title,content,summary,open_type,post_url,deleted,hits) VALUES (11,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,0,'제목-12','내용-12','요약-12','PUBLIC','post_url_12',false,0);
INSERT INTO tb_post (id,created_at,updated_at,account_id,title,content,summary,open_type,post_url,deleted,hits) VALUES (12,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,0,'제목-13','내용-13','요약-13','PUBLIC','post_url_13',false,0);
INSERT INTO tb_post (id,created_at,updated_at,account_id,title,content,summary,open_type,post_url,deleted,hits) VALUES (13,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,0,'제목-14','내용-14','요약-14','PUBLIC','post_url_14',false,0);
INSERT INTO tb_post (id,created_at,updated_at,account_id,title,content,summary,open_type,post_url,deleted,hits) VALUES (14,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,0,'제목-15','내용-15','요약-15','PUBLIC','post_url_15',false,0);
INSERT INTO tb_post (id,created_at,updated_at,account_id,title,content,summary,open_type,post_url,deleted,hits) VALUES (15,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,0,'제목-16','내용-16','요약-16','PUBLIC','post_url_16',false,0);
INSERT INTO tb_post (id,created_at,updated_at,account_id,title,content,summary,open_type,post_url,deleted,hits) VALUES (16,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,0,'제목-17','내용-17','요약-17','PUBLIC','post_url_17',false,0);
INSERT INTO tb_post (id,created_at,updated_at,account_id,title,content,summary,open_type,post_url,deleted,hits) VALUES (17,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,0,'제목-18','내용-18','요약-18','PUBLIC','post_url_18',false,0);
INSERT INTO tb_post (id,created_at,updated_at,account_id,title,content,summary,open_type,post_url,deleted,hits) VALUES (18,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,0,'제목-19','내용-19','요약-19','PUBLIC','post_url_19',false,0);
INSERT INTO tb_post (id,created_at,updated_at,account_id,title,content,summary,open_type,post_url,deleted,hits) VALUES (19,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,0,'제목-20','내용-20','요약-20','PUBLIC','post_url_20',false,0);
INSERT INTO tb_post (id,created_at,updated_at,account_id,title,content,summary,open_type,post_url,deleted,hits) VALUES (20,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,0,'제목-21','내용-21','요약-21','PUBLIC','post_url_21',false,0);
INSERT INTO tb_post (id,created_at,updated_at,account_id,title,content,summary,open_type,post_url,deleted,hits) VALUES (21,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,0,'제목-22','내용-22','요약-22','PUBLIC','post_url_22',false,0);
INSERT INTO tb_post (id,created_at,updated_at,account_id,title,content,summary,open_type,post_url,deleted,hits) VALUES (22,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,0,'제목-23','내용-23','요약-23','PUBLIC','post_url_23',false,0);
INSERT INTO tb_post (id,created_at,updated_at,account_id,title,content,summary,open_type,post_url,deleted,hits) VALUES (23,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,0,'제목-24','내용-24','요약-24','PUBLIC','post_url_24',false,0);
INSERT INTO tb_post (id,created_at,updated_at,account_id,title,content,summary,open_type,post_url,deleted,hits) VALUES (24,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,0,'제목-25','내용-25','요약-25','PUBLIC','post_url_25',false,0);
INSERT INTO tb_post (id,created_at,updated_at,account_id,title,content,summary,open_type,post_url,deleted,hits) VALUES (25,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,0,'제목-26','내용-26','요약-26','PUBLIC','post_url_26',false,0);
INSERT INTO tb_post (id,created_at,updated_at,account_id,title,content,summary,open_type,post_url,deleted,hits) VALUES (26,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,0,'제목-27','내용-27','요약-27','PUBLIC','post_url_27',false,0);
INSERT INTO tb_post (id,created_at,updated_at,account_id,title,content,summary,open_type,post_url,deleted,hits) VALUES (27,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,0,'제목-28','내용-28','요약-28','PUBLIC','post_url_28',false,0);
INSERT INTO tb_post (id,created_at,updated_at,account_id,title,content,summary,open_type,post_url,deleted,hits) VALUES (28,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,0,'제목-29','내용-29','요약-29','PUBLIC','post_url_29',false,0);
INSERT INTO tb_post (id,created_at,updated_at,account_id,title,content,summary,open_type,post_url,deleted,hits) VALUES (29,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,0,'제목-30','내용-30','요약-30','PUBLIC','post_url_30',false,0);
INSERT INTO tb_post (id,created_at,updated_at,account_id,title,content,summary,open_type,post_url,deleted,hits) VALUES (30,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,0,'제목-31','내용-31','요약-31','PUBLIC','post_url_31',false,0);
INSERT INTO tb_post (id,created_at,updated_at,account_id,title,content,summary,open_type,post_url,deleted,hits) VALUES (31,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,0,'제목-32','내용-32','요약-32','PUBLIC','post_url_32',false,0);
INSERT INTO tb_post (id,created_at,updated_at,account_id,title,content,summary,open_type,post_url,deleted,hits) VALUES (32,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,0,'제목-33','내용-33','요약-33','PUBLIC','post_url_33',false,0);
INSERT INTO tb_post (id,created_at,updated_at,account_id,title,content,summary,open_type,post_url,deleted,hits) VALUES (33,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,0,'제목-34','내용-34','요약-34','PUBLIC','post_url_34',false,0);
INSERT INTO tb_post (id,created_at,updated_at,account_id,title,content,summary,open_type,post_url,deleted,hits) VALUES (34,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,0,'제목-35','내용-35','요약-35','PUBLIC','post_url_35',false,0);
INSERT INTO tb_post (id,created_at,updated_at,account_id,title,content,summary,open_type,post_url,deleted,hits) VALUES (35,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,0,'제목-36','내용-36','요약-36','PUBLIC','post_url_36',false,0);
INSERT INTO tb_post (id,created_at,updated_at,account_id,title,content,summary,open_type,post_url,deleted,hits) VALUES (36,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,0,'제목-37','내용-37','요약-37','PUBLIC','post_url_37',false,0);
INSERT INTO tb_post (id,created_at,updated_at,account_id,title,content,summary,open_type,post_url,deleted,hits) VALUES (37,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,0,'제목-38','내용-38','요약-38','PUBLIC','post_url_38',false,0);
INSERT INTO tb_post (id,created_at,updated_at,account_id,title,content,summary,open_type,post_url,deleted,hits) VALUES (38,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,0,'제목-39','내용-39','요약-39','PUBLIC','post_url_39',false,0);
INSERT INTO tb_post (id,created_at,updated_at,account_id,title,content,summary,open_type,post_url,deleted,hits) VALUES (39,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,0,'제목-40','내용-40','요약-40','PUBLIC','post_url_40',false,0);
INSERT INTO tb_post (id,created_at,updated_at,account_id,title,content,summary,open_type,post_url,deleted,hits) VALUES (40,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,0,'제목-41','내용-41','요약-41','PUBLIC','post_url_41',false,0);
INSERT INTO tb_post (id,created_at,updated_at,account_id,title,content,summary,open_type,post_url,deleted,hits) VALUES (41,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,0,'제목-42','내용-42','요약-42','PUBLIC','post_url_42',false,0);
INSERT INTO tb_post (id,created_at,updated_at,account_id,title,content,summary,open_type,post_url,deleted,hits) VALUES (42,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,0,'제목-43','내용-43','요약-43','PUBLIC','post_url_43',false,0);
INSERT INTO tb_post (id,created_at,updated_at,account_id,title,content,summary,open_type,post_url,deleted,hits) VALUES (43,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,0,'제목-44','내용-44','요약-44','PUBLIC','post_url_44',false,0);
INSERT INTO tb_post (id,created_at,updated_at,account_id,title,content,summary,open_type,post_url,deleted,hits) VALUES (44,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,0,'제목-45','내용-45','요약-45','PUBLIC','post_url_45',false,0);
INSERT INTO tb_post (id,created_at,updated_at,account_id,title,content,summary,open_type,post_url,deleted,hits) VALUES (45,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,0,'제목-46','내용-46','요약-46','PUBLIC','post_url_46',false,0);
INSERT INTO tb_post (id,created_at,updated_at,account_id,title,content,summary,open_type,post_url,deleted,hits) VALUES (46,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,0,'제목-47','내용-47','요약-47','PUBLIC','post_url_47',false,0);
INSERT INTO tb_post (id,created_at,updated_at,account_id,title,content,summary,open_type,post_url,deleted,hits) VALUES (47,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,0,'제목-48','내용-48','요약-48','PUBLIC','post_url_48',false,0);



INSERT INTO tb_series (id,created_at,updated_at,account_id,title,series_url) VALUES (0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,0,'시리즈제목','series_url');

INSERT INTO tb_post_series (post_id,created_at,updated_at,series_id) VALUES (0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,0);
INSERT INTO tb_post_series (post_id,created_at,updated_at,series_id) VALUES (1,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,0);