CREATE TABLE identify_remand_calculation
(
    id                             UUID            NOT NULL CONSTRAINT identify_remand_calculation_pk PRIMARY KEY,
    identify_remand_decision_id    UUID            NOT NULL CONSTRAINT identify_remand_decision_fk REFERENCES identify_remand_decision (id),
    input_data                     JSONB           NOT NULL,
    output_data                    JSONB           NOT NULL,
    options                        JSONB           NOT NULL
);

CREATE TABLE calculated_remand
(
    id                                UUID                           NOT NULL CONSTRAINT calculated_remand_pk PRIMARY KEY,
    identify_remand_calculation_id    UUID                           NOT NULL CONSTRAINT identify_remand_calculation_fk REFERENCES identify_remand_calculation (id),
    start_date                        timestamp with time zone       NOT NULL,
    end_date                          timestamp with time zone       NOT NULL,
    statuses                          varchar(255)                   NOT NULL
);

ALTER TABLE identify_remand_decision DROP COLUMN options;

