-- Migration: Add type/points/hint to sub_tasks + create sub_task_options
-- Run once against your local postgres DB:
--   psql -h localhost -U postgres -d postgres -f migrate_subtask_mc.sql

-- 1) New columns on sub_tasks
ALTER TABLE sub_tasks ADD COLUMN IF NOT EXISTS type    VARCHAR(50)   NOT NULL DEFAULT 'FLAG';
ALTER TABLE sub_tasks ADD COLUMN IF NOT EXISTS points  INT           NOT NULL DEFAULT 1;
ALTER TABLE sub_tasks ADD COLUMN IF NOT EXISTS hint    VARCHAR(1000);

-- 2) New table for multiple-choice options
CREATE TABLE IF NOT EXISTS sub_task_options (
    id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    sub_task_id  UUID         NOT NULL REFERENCES sub_tasks(id) ON DELETE CASCADE,
    text         VARCHAR(500) NOT NULL,
    is_correct   BOOLEAN      NOT NULL DEFAULT FALSE,
    order_index  INT          NOT NULL
);
