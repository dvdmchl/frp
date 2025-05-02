create database if not exists frp-database character set utf8 collate utf8_general_ci;
\connect frp_database;
create schema if not exists frp_public;
\connect frp_database frp_public;

create sequence if not exists frp_user_id_seq;
create table if not exists frp_user (
    id serial primary key,
    username varchar(255) not null,
    email varchar(255) not null,
    last_login timestamp not null default current_timestamp,
    created_at timestamp not null default current_timestamp,
    created_by varchar(255) not null,
    updated_at timestamp not null default current_timestamp,
    updated_by varchar(255) not null
);