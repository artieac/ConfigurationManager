-- V12: drop old secret tables
--
-- Drops the old tables that were replaced by the configuration tables in V11.

DROP TABLE IF EXISTS secret_value_history;
DROP TABLE IF EXISTS secret_values;
DROP TABLE IF EXISTS secrets;
