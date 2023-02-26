INSERT INTO tb_role (name) VALUES ('ROLE_ADMIN');
INSERT INTO tb_role (name) VALUES ('ROLE_USER');

INSERT INTO tb_account (id,created_at,updated_at,deleted,name,email,blog_id,password) VALUES (0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,false,'김환','akon47@naver.com','@kim-hwan','$2a$10$mFOn2DL3wDPtQPWtl4pRfeqRjJad7BFpGoOmFrQcJ1qnqvcAahota');

INSERT INTO tb_account_role (id,created_at,updated_at,account_id,role_name) VALUES (0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,0,'ROLE_USER');
INSERT INTO tb_account_role (id,created_at,updated_at,account_id,role_name) VALUES (1,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,0,'ROLE_ADMIN');

INSERT INTO tb_post (id,created_at,updated_at,account_id,title,content,summary,open_type,post_url,blog_id,deleted) VALUES (0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,0,'제목','내용','요약','PUBLIC','post_url','@kim-hwan',false);