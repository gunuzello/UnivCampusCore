create table organization_notices (
 id bigserial primary key, organization_id bigint not null references organizations(id),
 title varchar(200) not null, content text not null, visibility varchar(30) not null check(visibility in ('PUBLIC','MEMBERS')),
 created_at timestamptz not null, updated_at timestamptz not null
);
create index idx_org_notices on organization_notices(organization_id,updated_at);
