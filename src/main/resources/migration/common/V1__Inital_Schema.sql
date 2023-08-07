CREATE TABLE identify_remand_decision
(
    id                     UUID                           NOT NULL constraint adjustment_pk PRIMARY KEY,
    accepted               boolean                        NOT NULL,
    days                   integer                        NOT NULL,
    person                 varchar(10)                    NOT NULL,
    reject_comment         varchar(255)                   NULL,
    decision_at            timestamp with time zone       NOT NULL,
    decision_by_username   varchar(255)                   NOT NULL
);

CREATE INDEX identify_remand_decision_person_reference ON identify_remand_decision(person);