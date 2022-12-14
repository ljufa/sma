/**
  SMA schema
 */
create schema sma;

create table sma.user
(
    id           integer GENERATED BY DEFAULT AS IDENTITY not null primary key,
    auth_id      text                                     not null unique,
    enabled      boolean                                  not null default true,
    created_date timestamp                                not null default current_timestamp
);

create table sma.account
(
    id                    integer GENERATED BY DEFAULT AS IDENTITY not null primary key,
    user_id               integer                                  not null,
    external_account_id   text                                     not null,
    external_account_type text                                     not null,
    created_date          timestamp                                not null default current_timestamp,
    enabled               boolean                                  not null default true,
    foreign key (user_id) references sma.user (id)
);

/**
  Twitter schema
 */
create schema twitter;

create table twitter.global_rule
(
    rule_id    text not null,
    rule_query text not null,
    rule_tag   text not null
);