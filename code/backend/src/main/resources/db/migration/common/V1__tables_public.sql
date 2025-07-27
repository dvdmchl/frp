create sequence frp_public.frp_user_id_seq
    start 1
    increment 1;

create table frp_public.frp_user (
    id serial primary key,
    full_name varchar(255),
    password varchar(255) not null,
    email varchar(255) not null,
    last_login timestamp,
    schema_id bigint,
    token_valid boolean not null default true,

    created_at timestamp not null default now(),
    created_by_user_id bigint not null,
    updated_at timestamp not null default now(),
    updated_by_user_id bigint not null,
    version int not null default 0
);

create sequence frp_public.frp_schema_id_seq
    start 1
    increment 1;

create table frp_public.frp_schema (
    id serial primary key,
    name varchar(255) not null,

    created_at timestamp not null default now(),
    created_by_user_id bigint not null,
    updated_at timestamp not null default now(),
    updated_by_user_id bigint not null,
    version int not null default 0
);