create sequence frp_public.frp_group_id_seq
    start 1
    increment 1;

create table frp_public.frp_group (
    id bigint primary key default nextval('frp_public.frp_group_id_seq'),
    name varchar(255) not null unique,
    description text,

    created_at timestamp not null default now(),
    created_by_user_id bigint not null,
    updated_at timestamp not null default now(),
    updated_by_user_id bigint not null,
    version int not null default 0
);

create table frp_public.frp_user_group (
    user_id bigint not null,
    group_id bigint not null,
    primary key (user_id, group_id),
    constraint fk_user_group_user foreign key (user_id) references frp_public.frp_user(id),
    constraint fk_user_group_group foreign key (group_id) references frp_public.frp_group(id)
);

create sequence frp_public.frp_schema_access_id_seq
    start 1
    increment 1;

create table frp_public.frp_schema_access (
    id bigint primary key default nextval('frp_public.frp_schema_access_id_seq'),
    schema_id bigint not null,
    user_id bigint,
    group_id bigint,
    access_level varchar(50) not null,

    created_at timestamp not null default now(),
    created_by_user_id bigint not null,
    updated_at timestamp not null default now(),
    updated_by_user_id bigint not null,
    version int not null default 0,

    constraint fk_schema_access_schema foreign key (schema_id) references frp_public.frp_schema(id),
    constraint fk_schema_access_user foreign key (user_id) references frp_public.frp_user(id),
    constraint fk_schema_access_group foreign key (group_id) references frp_public.frp_group(id),
    constraint check_user_or_group check ((user_id is not null and group_id is null) or (user_id is null and group_id is not null))
);
