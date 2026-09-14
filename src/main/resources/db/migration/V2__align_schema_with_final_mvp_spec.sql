ALTER TABLE events
    ADD COLUMN location VARCHAR(255) NULL AFTER is_all_day;

ALTER TABLE events
    CHANGE COLUMN description memo TEXT NULL AFTER location;

ALTER TABLE events
    ADD COLUMN is_important BOOLEAN NOT NULL DEFAULT FALSE AFTER memo;

ALTER TABLE events
    DROP COLUMN color;

ALTER TABLE todos
    CHANGE COLUMN title content VARCHAR(255) NOT NULL;

CREATE INDEX idx_todos_user_completed_created_at ON todos (user_id, is_completed, created_at);

DROP INDEX idx_todos_user_due_date ON todos;

ALTER TABLE todos
    DROP COLUMN description,
    DROP COLUMN due_date,
    DROP COLUMN display_order;

ALTER TABLE timetables
    CHANGE COLUMN name title VARCHAR(100) NOT NULL;

ALTER TABLE timetables
    ADD COLUMN semester VARCHAR(50) NULL AFTER title;

UPDATE timetables
SET semester = ''
WHERE semester IS NULL;

ALTER TABLE timetables
    MODIFY COLUMN semester VARCHAR(50) NOT NULL;

ALTER TABLE timetables
    CHANGE COLUMN is_default is_active BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX idx_timetables_user_active ON timetables (user_id, is_active);

DROP INDEX idx_timetables_user ON timetables;

ALTER TABLE timetable_entries
    MODIFY COLUMN location VARCHAR(255) NULL;

ALTER TABLE timetable_entries
    ADD COLUMN memo TEXT NULL AFTER location;

ALTER TABLE timetable_entries
    MODIFY COLUMN color CHAR(7) NULL;
