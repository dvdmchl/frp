alter table frp_public.frp_user add column active boolean not null default true;
alter table frp_public.frp_user add column admin boolean not null default false;
