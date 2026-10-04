alter table organizations add column type varchar(30) not null default 'STUDENT_COUNCIL' check(type in ('STUDENT_COUNCIL','CLUB'));
create table membership_requests (
 id bigserial primary key, organization_id bigint not null references organizations(id), user_id bigint not null references app_users(id),
 message varchar(2000), status varchar(30) not null check(status in ('PENDING','ACCEPTED','REJECTED','CANCELLED')),
 created_at timestamptz not null, resolved_at timestamptz
);
create index idx_membership_requests_org on membership_requests(organization_id,created_at);
create unique index uq_membership_pending on membership_requests(organization_id,user_id) where status='PENDING';
