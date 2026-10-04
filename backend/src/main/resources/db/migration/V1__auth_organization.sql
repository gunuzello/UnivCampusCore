create table app_users (
 id bigserial primary key,email varchar(254) not null unique,password_hash varchar(255) not null,
 name varchar(80) not null,department varchar(120),student_number varchar(40)
);
create table organizations(id bigserial primary key,name varchar(120) not null,department varchar(120),description text);
create table memberships(id bigserial primary key,organization_id bigint not null references organizations(id),user_id bigint not null references app_users(id),role varchar(20) not null check(role in ('MEMBER','STAFF','LEADER')),unique(organization_id,user_id));
create index idx_memberships_user on memberships(user_id);
