INSERT INTO "users" ("id", "anonymized_at", "created_at", "deleted_at", "email", "first_name", "last_name", "name", "picture", "title", "updated_at", "username", "total_seconds_online") VALUES
('f730669a-055b-4362-8a01-605d9881c5b0',	NULL,	'2026-05-11 17:03:00.348842',	NULL,	'e2e-admin@istp.local',	'E2E',	'Admin',	'E2E Admin',	NULL,	'Test Administrator',	'2026-05-11 17:05:00.830215',	'e2e-admin',	101142),
('de73d811-d875-4bae-9fb4-b5dd1f1dba41',	NULL,	'2026-05-14 14:34:10.950312',	NULL,	'e2e-instructor-without-courses-or-labs@istp.local',	'E2E',	'Instructor No Courses Or Labs',	'E2E Instructor No Courses Or Labs',	NULL,	'Test Instructor',	'2026-05-14 14:34:38.335242',	'e2e-instructor-without-courses-or-labs',	10008),
('42bf88b0-2626-497d-a696-a864d8f1f27f',	NULL,	'2026-05-14 11:57:32.046572',	NULL,	'e2e-student@istp.local',	'E2E',	'Student',	'E2E Student',	NULL,	'Test Student',	'2026-05-14 11:57:32.046572',	'e2e-student',	70228),
('e4f2814e-0bd9-4fe9-acd3-8d08cfb11179',	NULL,	'2026-05-11 17:04:08.432332',	NULL,	'e2e-instructor@istp.local',	'E2E',	'Instructor',	'E2E Instructor',	NULL,	'Test Instructor',	'2026-05-11 17:05:07.381682',	'e2e-instructor',	123964);

INSERT INTO "user_roles" ("user_id", "role") VALUES
('f730669a-055b-4362-8a01-605d9881c5b0',	'ROLE_ADMINISTRATOR'),
('de73d811-d875-4bae-9fb4-b5dd1f1dba41',	'ROLE_INSTRUCTOR'),
('42bf88b0-2626-497d-a696-a864d8f1f27f',	'ROLE_STUDENT'),
('e4f2814e-0bd9-4fe9-acd3-8d08cfb11179',	'ROLE_INSTRUCTOR');