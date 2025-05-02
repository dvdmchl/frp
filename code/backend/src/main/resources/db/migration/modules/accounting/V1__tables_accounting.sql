
-- acc_journal
CREATE SEQUENCE ${schema}.acc_journal_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE TABLE ${schema}.acc_journal (
   id serial primary key,

   created_by_user_id BIGINT NOT NULL,
   created_at TIMESTAMP NOT NULL DEFAULT now(),
   updated_by_user_id BIGINT NOT NULL,
   updated_at TIMESTAMP NOT NULL DEFAULT now(),
   version INT NOT NULL DEFAULT 0
);
