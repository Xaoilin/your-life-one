-- Identity links are imported from the same product's existing account provider.
-- The service has no public account-creation or email-based linking endpoint.
create table identity.account_identity (
    id uuid primary key,
    user_id uuid not null,
    provider varchar(64) not null,
    subject varchar(512) not null,
    email varchar(320) not null,
    enabled boolean not null default true,
    unique (provider, subject)
);
create index account_identity_user_idx on identity.account_identity(user_id);

-- Spring Session JDBC's PostgreSQL schema, qualified to this service's own schema.
create table identity.spring_session (
    primary_id char(36) not null,
    session_id char(36) not null,
    creation_time bigint not null,
    last_access_time bigint not null,
    max_inactive_interval int not null,
    expiry_time bigint not null,
    principal_name varchar(100),
    constraint spring_session_pk primary key (primary_id)
);
create unique index spring_session_ix1 on identity.spring_session(session_id);
create index spring_session_ix2 on identity.spring_session(expiry_time);
create index spring_session_ix3 on identity.spring_session(principal_name);
create table identity.spring_session_attributes (
    session_primary_id char(36) not null,
    attribute_name varchar(200) not null,
    attribute_bytes bytea not null,
    constraint spring_session_attributes_pk primary key (session_primary_id, attribute_name),
    constraint spring_session_attributes_fk foreign key (session_primary_id)
        references identity.spring_session(primary_id) on delete cascade
);
