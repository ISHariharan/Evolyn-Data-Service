-- V1: the tables Evolyn already had before Flyway took over.
--
-- These were created by Hibernate (ddl-auto=update). On a database that already
-- has them (your laptop, Neon), Flyway does NOT run this file: with
-- spring.flyway.baseline-on-migrate=true it only records "V1 is done" and
-- leaves the tables and their data alone.
-- On a brand-new empty database this file creates them from scratch.
--
-- Constraint names are the ones Hibernate generated, so a fresh database
-- matches an existing one exactly.

CREATE TABLE evolyn_user_details (
    uuid       uuid NOT NULL,
    email      varchar(255),
    first_name varchar(255),
    last_name  varchar(255),
    password   varchar(255),
    status     varchar(255),
    CONSTRAINT evolyn_user_details_pkey PRIMARY KEY (uuid),
    CONSTRAINT uk83yyh52ssmd5prl3hyvbkt7fb UNIQUE (email)
);

CREATE TABLE workspace (
    workspace_id          uuid NOT NULL,
    user_id               uuid,
    workspace_description varchar(255),
    workspace_icon        varchar(255),
    workspace_name        varchar(255),
    CONSTRAINT workspace_pkey PRIMARY KEY (workspace_id),
    CONSTRAINT ukfc8jqg19vkrmfrlpys7s034vs UNIQUE (workspace_name)
);

CREATE TABLE user_bank_details (
    user_id                                varchar(255) NOT NULL,
    user_account_balance                   varchar(255),
    user_account_ifsc_code                 varchar(255),
    user_account_name                      varchar(255),
    user_account_number                    varchar(255),
    user_account_type                      varchar(255),
    user_bank_name                         varchar(255),
    user_cash_details                      varchar(255),
    user_account_linked_credit_card_limit  varchar(255),
    user_account_linked_credit_card_number varchar(255),
    user_account_linked_debit_card_number  varchar(255),
    user_notes                             varchar(255),
    CONSTRAINT user_bank_details_pkey PRIMARY KEY (user_id),
    CONSTRAINT ukelnqfdi3duow7f4e8jo693gn6 UNIQUE (user_account_linked_debit_card_number),
    CONSTRAINT ukesudxagcv8ma7pjs4sjro7qo4 UNIQUE (user_account_linked_credit_card_number),
    CONSTRAINT ukgeld2fxhef57ganld02x3xaso UNIQUE (user_account_name),
    CONSTRAINT uklnrrkpi80x343pfserik4fw17 UNIQUE (user_account_number)
);

CREATE TABLE accounts (
    account_id           uuid NOT NULL,
    account_type         varchar(20) NOT NULL,
    created_at           timestamp(6) with time zone NOT NULL,
    last4                varchar(4),
    name                 varchar(60) NOT NULL,
    user_id              uuid NOT NULL,
    credit_limit         numeric(12,2),
    opening_balance      numeric(12,2),
    opening_balance_date date,
    statement_day        integer,
    CONSTRAINT accounts_pkey PRIMARY KEY (account_id)
);
CREATE INDEX idx_accounts_user ON accounts (user_id);

CREATE TABLE categories (
    category_id uuid NOT NULL,
    color       varchar(7) NOT NULL,
    created_at  timestamp(6) with time zone NOT NULL,
    description varchar(120),
    icon        varchar(60) NOT NULL,
    name        varchar(50) NOT NULL,
    sort_order  integer NOT NULL,
    type        varchar(10) NOT NULL,
    user_id     uuid NOT NULL,
    CONSTRAINT categories_pkey PRIMARY KEY (category_id),
    CONSTRAINT uk_categories_user_type_name UNIQUE (user_id, type, name)
);
CREATE INDEX idx_categories_user ON categories (user_id);

CREATE TABLE transactions (
    transaction_id   uuid NOT NULL,
    amount           numeric(12,2) NOT NULL,
    category         varchar(50) NOT NULL,
    created_at       timestamp(6) with time zone NOT NULL,
    merchant         varchar(100) NOT NULL,
    notes            varchar(500),
    payment_method   varchar(20) NOT NULL,
    source           varchar(20) NOT NULL,
    transaction_date date NOT NULL,
    type             varchar(10) NOT NULL,
    updated_at       timestamp(6) with time zone NOT NULL,
    user_id          uuid NOT NULL,
    account_id       uuid NOT NULL,
    category_id      uuid,
    to_account_id    uuid,
    CONSTRAINT transactions_pkey PRIMARY KEY (transaction_id),
    CONSTRAINT fk20w7wsg13u9srbq3bd7chfxdh FOREIGN KEY (account_id) REFERENCES accounts (account_id),
    CONSTRAINT fkra0an432c5wjo76mojluk0v28 FOREIGN KEY (to_account_id) REFERENCES accounts (account_id),
    CONSTRAINT fksqqi7sneo04kast0o138h19mv FOREIGN KEY (category_id) REFERENCES categories (category_id)
);
CREATE INDEX idx_transactions_account ON transactions (account_id);
CREATE INDEX idx_transactions_category ON transactions (category_id);
CREATE INDEX idx_transactions_to_account ON transactions (to_account_id);
CREATE INDEX idx_transactions_user_date ON transactions (user_id, transaction_date);
