# Todos schema

# --- !Ups

CREATE TABLE Todo (
    id bigint NOT NULL AUTO_INCREMENT PRIMARY KEY,
    title text NOT NULL,
    completed boolean NOT NULL,
    "order" long);

# --- !Downs

DROP TABLE Todo;
