# Todos schema

# --- !Ups

CREATE TABLE Todo (
    id SERIAL,
    title text NOT NULL,
    completed boolean NOT NULL,
    "order" int);

# --- !Downs

DROP TABLE Todo;
