create table notifications(id bigserial primary key,user_id bigint not null references app_users(id),message varchar(255) not null,path varchar(255),read boolean not null,created_at timestamptz not null);
create index idx_notifications_user on notifications(user_id,created_at desc);
