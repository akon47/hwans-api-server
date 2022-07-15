drop table if exists user CASCADE;
CREATE table tb_user
(
    id varchar(255) not null,
    name varchar(255),
    password varchar(255),
    primary key (id)
)