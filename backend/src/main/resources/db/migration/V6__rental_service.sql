create table rental_items (
 id bigserial primary key, organization_id bigint not null references organizations(id),
 name varchar(200) not null, description text, total_quantity integer not null check(total_quantity >= 0),
 loan_days integer not null check(loan_days between 1 and 60), enabled boolean not null default true,
 created_at timestamptz not null
);
create index idx_rental_items_org on rental_items(organization_id);
create table rental_loans (
 id bigserial primary key, item_id bigint not null references rental_items(id), user_id bigint not null references app_users(id),
 quantity integer not null check(quantity > 0), status varchar(30) not null check(status in ('REQUESTED','BORROWED','RETURNED','CANCELLED','REJECTED')),
 name varchar(80) not null, email varchar(255) not null, department varchar(120), student_number varchar(40),
 requested_at timestamptz not null, borrowed_at timestamptz, due_at timestamptz, returned_at timestamptz
);
create index idx_rental_loans_item on rental_loans(item_id,status);
create index idx_rental_loans_user on rental_loans(user_id,requested_at);
create unique index uq_rental_active_user on rental_loans(item_id,user_id) where status in ('REQUESTED','BORROWED');
