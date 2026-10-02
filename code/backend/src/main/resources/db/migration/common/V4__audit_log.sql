create sequence frp_public.frp_audit_log_id_seq
    start 1
    increment 1;

create table frp_public.frp_audit_log (
    id bigint primary key default nextval('frp_public.frp_audit_log_id_seq'),
    created_at timestamptz not null default now(),
    user_id bigint,
    user_email varchar(255),
    action varchar(50) not null,
    resource varchar(255),
    details text
);

create index idx_audit_log_created_at on frp_public.frp_audit_log (created_at desc);
create index idx_audit_log_user_id on frp_public.frp_audit_log (user_id);
create index idx_audit_log_action on frp_public.frp_audit_log (action);
