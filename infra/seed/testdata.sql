--
-- PostgreSQL database dump
--

\restrict c21WpcfjzzrK8x4xjfUvfSE0sP3eBFhDTQpJCU6nsNbjYwnZV9BlltPi35X6rcR

-- Dumped from database version 17.9 (Debian 17.9-1.pgdg13+1)
-- Dumped by pg_dump version 17.9 (Debian 17.9-1.pgdg13+1)

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Data for Name: labs; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO public.labs (id, created_at, description, difficulty, docker_image, max_score, status, title, updated_at, creator_id) VALUES ('02a0f4e0-913a-4ecc-892c-8b89c1c0c5e2', '2026-05-03 09:54:53.793865', '<p>Wir machen bischen User Auth wuhu mega cool</p>', 'MEDIUM', 'ghcr.io/pm4-istp/campus-helpdesk:latest', 4, 'PUBLIC', 'User Auth', '2026-05-03 09:54:53.793865', '9064345a-61d9-4b34-a477-85aaf925b596') ON CONFLICT DO NOTHING;
INSERT INTO public.labs (id, created_at, description, difficulty, docker_image, max_score, status, title, updated_at, creator_id) VALUES ('1c2bbc16-9b0f-4f1c-8683-ffec7dd002c5', '2026-05-03 11:31:31.060343', '<p>Unlimted</p>', 'MEDIUM', 'ghcr.io/pm4-istp/campus-helpdesk:latest', 1, 'PRIVATE', 'Unlimted', '2026-05-03 11:32:31.497833', '9064345a-61d9-4b34-a477-85aaf925b596') ON CONFLICT DO NOTHING;
INSERT INTO public.labs (id, created_at, description, difficulty, docker_image, max_score, status, title, updated_at, creator_id) VALUES ('c38a14d8-a3f6-4cc5-a5ec-0efda41a4fba', '2026-05-03 11:32:14.240017', '<p>not unlimetet</p>', 'MEDIUM', 'ghcr.io/pm4-istp/campus-helpdesk:latest', 1, 'PRIVATE', 'not unlimetet', '2026-05-03 11:32:43.828908', '9064345a-61d9-4b34-a477-85aaf925b596') ON CONFLICT DO NOTHING;
INSERT INTO public.labs (id, created_at, description, difficulty, docker_image, max_score, status, title, updated_at, creator_id) VALUES ('a7c223bd-6765-41ad-83a3-028ea7fcc026', '2026-05-03 15:56:01.924934', '<p>Campus Helpdesk is an internal support web application for a school or company. The app includes a login page, support dashboard, ticket search, ticket details, knowledge base, and an internal audit trail.</p><p>Your goal is to investigate the application like a helpdesk analyst and find four server-side flags.</p><p>Notes:</p><ul><li><p>You only need a browser and DevTools.</p></li><li><p>The ticket search is a good starting point.</p></li><li><p>Pay attention to error messages, diagnostic output, tickets, and knowledge base articles.</p></li><li><p>Some features behave like normal internal legacy systems.</p></li><li><p>The app does not include a flag submission form. Submit found flags here in the platform.</p></li></ul><p>Recommended start:</p><p>Log in with a normal test account to explore the app.</p><p>Test account:</p><ul><li><p>Username: student.demo</p></li><li><p>Password: welcome1</p></li></ul><p></p>', 'MEDIUM', 'ghcr.io/pm4-istp/campus-helpdesk:latest', 5, 'PUBLIC', 'Campus Helpdesk', '2026-05-03 15:56:01.924934', 'cb9628d2-2019-4aba-87b6-27b4ac47df62') ON CONFLICT DO NOTHING;
INSERT INTO public.labs (id, created_at, description, difficulty, docker_image, max_score, status, title, updated_at, creator_id) VALUES ('d92b165b-9c80-4d81-a25a-01e581476d26', '2026-05-03 16:00:39.312752', '<p>In this challenge, you analyze an internal admin portal with login, dashboard, reports, and URL preview functionality.</p><p>The application looks like a normal operations tool, but it intentionally contains several web security vulnerabilities. Use reconnaissance, request manipulation, broken access control, and SSRF to discover the hidden flags.</p><p>The lab application itself does not handle flag submission or scoring. Submit discovered flags through the ISTP platform.</p><p></p>', 'MEDIUM', 'ghcr.io/pm4-istp/internal-admin-portal:latest', 5, 'PUBLIC', 'Internal Admin Portal', '2026-05-03 16:00:39.312752', 'cb9628d2-2019-4aba-87b6-27b4ac47df62') ON CONFLICT DO NOTHING;
INSERT INTO public.labs (id, created_at, description, difficulty, docker_image, max_score, status, title, updated_at, creator_id) VALUES ('d833ef4b-8bc6-474c-af02-6768a1212e8f', '2026-05-07 08:27:16.888824', '<p>Add a description...</p>', 'MEDIUM', 'ghcr.io/school-org/lab:1.0.0', 1, 'DRAFT', 'TestLab', '2026-05-07 08:27:16.888824', '910cd5f6-945d-46df-923b-2e6e1d6051e5') ON CONFLICT DO NOTHING;


--
-- Data for Name: challenges; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO public.challenges (id, created_at, description, flag, hint, order_index, points, title, type, updated_at, challenge_id) VALUES ('b8ec2862-d4c8-4014-b975-ca767936c83d', '2026-05-03 09:54:53.802851', '<p>Was gibt 1+1?</p>', NULL, NULL, 0, 3, 'Mathe grundlage', 'MULTIPLE_CHOICE', '2026-05-03 09:54:53.802851', '02a0f4e0-913a-4ecc-892c-8b89c1c0c5e2') ON CONFLICT DO NOTHING;
INSERT INTO public.challenges (id, created_at, description, flag, hint, order_index, points, title, type, updated_at, challenge_id) VALUES ('7e5aa7d4-f386-4102-bc8b-d32e103c2046', '2026-05-03 09:54:53.808155', '<p>Hast du den Kurs verstanden wenn nicht gehe nachhause:)</p>', NULL, 'Bruda du brauchst keien verfickten hint yarak', 1, 1, 'Hast du den Kurs verstanden?', 'FLAG', '2026-05-03 09:54:53.808155', '02a0f4e0-913a-4ecc-892c-8b89c1c0c5e2') ON CONFLICT DO NOTHING;
INSERT INTO public.challenges (id, created_at, description, flag, hint, order_index, points, title, type, updated_at, challenge_id) VALUES ('d34013ea-ba2a-4bbd-b1ce-1bbf71318529', '2026-05-03 11:31:31.067388', '<p>Unlimted</p>', NULL, NULL, 0, 1, 'Unlimted', 'MULTIPLE_CHOICE', '2026-05-03 11:31:31.067388', '1c2bbc16-9b0f-4f1c-8683-ffec7dd002c5') ON CONFLICT DO NOTHING;
INSERT INTO public.challenges (id, created_at, description, flag, hint, order_index, points, title, type, updated_at, challenge_id) VALUES ('cdefcbf0-bc45-4578-b4ea-cb9d44280ba1', '2026-05-03 11:32:14.240697', '<p>not unlimetet</p>', NULL, NULL, 0, 1, 'not unlimetet', 'MULTIPLE_CHOICE', '2026-05-03 11:32:14.240697', 'c38a14d8-a3f6-4cc5-a5ec-0efda41a4fba') ON CONFLICT DO NOTHING;
INSERT INTO public.challenges (id, created_at, description, flag, hint, order_index, points, title, type, updated_at, challenge_id) VALUES ('81cdf69d-aea5-43bd-8393-b12730dbadde', '2026-05-03 15:56:01.925816', '<p>Investigate the ticket search. Some inputs are not handled safely.</p><p>Goal:</p><p>Find a way to trigger a server-side diagnostic or debug response.</p>', 'ISTP{SQL_ERROR_REVEALED}', 'A single special character may be enough.
Look closely at the database error message.
The Diagnostic Trace box also reveals which columns the search uses.
The flag is not shown as a normal ticket row. It appears in an internal diagnostic marker.', 0, 1, 'Recon / SQL Error Hint', 'FLAG', '2026-05-03 15:56:01.925816', 'a7c223bd-6765-41ad-83a3-028ea7fcc026') ON CONFLICT DO NOTHING;
INSERT INTO public.challenges (id, created_at, description, flag, hint, order_index, points, title, type, updated_at, challenge_id) VALUES ('27654d1e-66b9-4a00-b8d8-995932c4cff2', '2026-05-03 15:56:01.926148', '<p>The login still uses an old legacy authentication flow.</p><p>Goal:</p><p>Log in successfully without knowing a valid password.</p>', 'ISTP{LOGIN_BYPASS_SUCCESS}', 'Check the Knowledge Base and tickets related to legacy auth migration.
Classic SQL injection payloads may work here.
The username field is a good place to test.
After a successful bypass, the flag appears on the dashboard.', 1, 1, 'Login Bypass', 'FLAG', '2026-05-03 15:56:01.926148', 'a7c223bd-6765-41ad-83a3-028ea7fcc026') ON CONFLICT DO NOTHING;
INSERT INTO public.challenges (id, created_at, description, flag, hint, order_index, points, title, type, updated_at, challenge_id) VALUES ('653742e6-9603-4734-a7e2-3dadfd420876', '2026-05-03 15:56:01.926408', '<p>The ticket search renders four visible columns: ID, Title, Requester, and Status.</p><p>Goal:</p><p>Use UNION-based SQL injection to extract data from an internal table.</p>', 'ISTP{UNION_HELPDESK_LEAK}', 'The error from Subtask 1 helps with the column count.
UNION data is rendered visibly in the ticket list.
There is a table named secrets.
This table contains key and value columns.
Look for a flag and a short audit token.', 2, 1, 'UNION-Based Ticket Extraction', 'FLAG', '2026-05-03 15:56:01.926408', 'a7c223bd-6765-41ad-83a3-028ea7fcc026') ON CONFLICT DO NOTHING;
INSERT INTO public.challenges (id, created_at, description, flag, hint, order_index, points, title, type, updated_at, challenge_id) VALUES ('2d8995b8-1529-4c6a-8396-5778ed298114', '2026-05-03 15:56:01.926638', '<p>The internal audit trail requires a review token.</p><p>Goal:</p><p>Use the audit token found in Subtask 3 to open the audit trail.</p>', 'ISTP{DATABASE_OWNED}', 'The token can be extracted from secrets through ticket search.
Open Audit Trail from the sidebar or directly through the URL.
The token can be passed as a token query parameter.', 3, 1, 'Internal Audit Trail', 'FLAG', '2026-05-03 15:56:01.926638', 'a7c223bd-6765-41ad-83a3-028ea7fcc026') ON CONFLICT DO NOTHING;
INSERT INTO public.challenges (id, created_at, description, flag, hint, order_index, points, title, type, updated_at, challenge_id) VALUES ('e0bc32fa-5cd7-43a8-a6db-409bb9e357bb', '2026-05-03 15:56:01.927577', '<p>Rate this challenge</p>', NULL, NULL, 4, 1, 'How did you like this challenge?', 'MULTIPLE_CHOICE', '2026-05-03 15:56:01.927577', 'a7c223bd-6765-41ad-83a3-028ea7fcc026') ON CONFLICT DO NOTHING;
INSERT INTO public.challenges (id, created_at, description, flag, hint, order_index, points, title, type, updated_at, challenge_id) VALUES ('32bb24a2-ab2b-488e-9ba8-0a5364171e57', '2026-05-03 16:00:39.313309', '<p>Before attacking the portal features, perform basic web reconnaissance.</p><p>Start by checking common discovery files that websites expose for crawlers. One of them can reveal paths that are not linked in the UI. Follow the discovered legacy path and inspect its response carefully for another internal route.</p><p>Your goal is to find a hidden debug or health endpoint and recover its flag.</p><p></p>', 'ISTP{DEBUG_ROUTE_FOUND}', '/robots.txt', 0, 1, 'Recon / Hidden Debug Route', 'FLAG', '2026-05-03 16:00:39.313309', 'd92b165b-9c80-4d81-a25a-01e581476d26') ON CONFLICT DO NOTHING;
INSERT INTO public.challenges (id, created_at, description, flag, hint, order_index, points, title, type, updated_at, challenge_id) VALUES ('80e78bbe-d791-4c56-994e-b01c391a8285', '2026-05-03 16:00:39.313606', '<p>After logging in, open the dashboard and look at the session context refresh widget.</p><p>Click the refresh button once while your browser DevTools Network tab is open. Inspect the request body sent to the role/session endpoint. The UI only sends a normal user context, but the server may trust values controlled by the client.</p><p>Replay or copy that request, change the role value to a higher-privileged one, and inspect the JSON response for the flag.</p><p></p>', 'ISTP{WEAK_ROLE_CHECK}', NULL, 1, 1, 'Auth Bypass / Role Escalation', 'FLAG', '2026-05-03 16:00:39.313606', 'd92b165b-9c80-4d81-a25a-01e581476d26') ON CONFLICT DO NOTHING;
INSERT INTO public.challenges (id, created_at, description, flag, hint, order_index, points, title, type, updated_at, challenge_id) VALUES ('f3c677f9-f489-46f9-bfd9-87a109a88186', '2026-05-03 16:00:39.31389', '<p>Open the Reports page and load the report that is visible to your user.</p><p>Inspect the API request used to fetch the report. Notice how the report ID appears in the URL. Try nearby or sequential report IDs and check whether the server enforces ownership correctly.</p><p>Find a report that should belong to another user or department and recover its flag.</p><p></p>', 'ISTP{IDOR_REPORT_LEAK}', NULL, 2, 1, 'IDOR / Broken Access Control**', 'FLAG', '2026-05-03 16:00:39.31389', 'd92b165b-9c80-4d81-a25a-01e581476d26') ON CONFLICT DO NOTHING;
INSERT INTO public.challenges (id, created_at, description, flag, hint, order_index, points, title, type, updated_at, challenge_id) VALUES ('89df47d8-7ae1-4325-a8c4-9693813f0712', '2026-05-03 16:00:39.31411', '<p>Open the URL Preview page and test how the preview behaves with normal external URLs.</p><p>The preview is generated server-side, meaning the backend fetches the URL for you. Try using it to request localhost from the server''s perspective. The challenge container runs an internal-only service on port 8080.</p><p>Reach the internal service through the preview feature and recover the final flag.</p><p></p>', 'ISTP{SSRF_INTERNAL_SERVICE}', NULL, 3, 1, 'SSRF / Internal Local Service**', 'FLAG', '2026-05-03 16:00:39.31411', 'd92b165b-9c80-4d81-a25a-01e581476d26') ON CONFLICT DO NOTHING;
INSERT INTO public.challenges (id, created_at, description, flag, hint, order_index, points, title, type, updated_at, challenge_id) VALUES ('2aaf9497-4949-4788-989d-129b207dae3e', '2026-05-03 16:00:39.314324', '<p>Rate this challenge</p>', NULL, NULL, 4, 1, 'How did you like this challenge?', 'MULTIPLE_CHOICE', '2026-05-03 16:00:39.314324', 'd92b165b-9c80-4d81-a25a-01e581476d26') ON CONFLICT DO NOTHING;
INSERT INTO public.challenges (id, created_at, description, flag, hint, order_index, points, title, type, updated_at, challenge_id) VALUES ('547c5007-6f3e-4820-9ad6-ad6b72a63a1e', '2026-05-07 08:27:16.895657', '<p>test</p>', NULL, NULL, 0, 1, 'test', 'FLAG', '2026-05-07 08:27:16.895657', 'd833ef4b-8bc6-474c-af02-6768a1212e8f') ON CONFLICT DO NOTHING;


--
-- Data for Name: challenge_options; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO public.challenge_options (id, is_correct, order_index, text, sub_task_id) VALUES ('d509cc92-081f-43b9-a237-fed096419d46', true, 0, '2', 'b8ec2862-d4c8-4014-b975-ca767936c83d') ON CONFLICT DO NOTHING;
INSERT INTO public.challenge_options (id, is_correct, order_index, text, sub_task_id) VALUES ('556cb8f0-4fde-41e2-b520-34f860fec8e1', false, 1, '3', 'b8ec2862-d4c8-4014-b975-ca767936c83d') ON CONFLICT DO NOTHING;
INSERT INTO public.challenge_options (id, is_correct, order_index, text, sub_task_id) VALUES ('5bf8b4ad-c5a5-447f-9c26-a498b0d542e6', false, 2, '4', 'b8ec2862-d4c8-4014-b975-ca767936c83d') ON CONFLICT DO NOTHING;
INSERT INTO public.challenge_options (id, is_correct, order_index, text, sub_task_id) VALUES ('8bd14ce6-717e-4c0a-87da-ca8681f169cb', true, 0, 'd', 'd34013ea-ba2a-4bbd-b1ce-1bbf71318529') ON CONFLICT DO NOTHING;
INSERT INTO public.challenge_options (id, is_correct, order_index, text, sub_task_id) VALUES ('cc5d2a55-70a7-4072-a422-3d9fd138c656', false, 1, 'd', 'd34013ea-ba2a-4bbd-b1ce-1bbf71318529') ON CONFLICT DO NOTHING;
INSERT INTO public.challenge_options (id, is_correct, order_index, text, sub_task_id) VALUES ('04053b51-3f1f-4794-8002-6099a48eaf8a', true, 0, 'asd', 'cdefcbf0-bc45-4578-b4ea-cb9d44280ba1') ON CONFLICT DO NOTHING;
INSERT INTO public.challenge_options (id, is_correct, order_index, text, sub_task_id) VALUES ('e3e95380-87bb-4e5b-a4f2-d998fb632f4b', false, 1, 'ads', 'cdefcbf0-bc45-4578-b4ea-cb9d44280ba1') ON CONFLICT DO NOTHING;
INSERT INTO public.challenge_options (id, is_correct, order_index, text, sub_task_id) VALUES ('eba2c9c8-4f13-4743-9575-e456ad8d534c', false, 2, 'adsd', 'cdefcbf0-bc45-4578-b4ea-cb9d44280ba1') ON CONFLICT DO NOTHING;
INSERT INTO public.challenge_options (id, is_correct, order_index, text, sub_task_id) VALUES ('6637c973-6cfe-43de-ab8f-5a2e58c90ef2', true, 0, 'good', '2aaf9497-4949-4788-989d-129b207dae3e') ON CONFLICT DO NOTHING;
INSERT INTO public.challenge_options (id, is_correct, order_index, text, sub_task_id) VALUES ('1759daac-f2de-43a9-aeee-5c9bb2af4a12', false, 1, 'meh', '2aaf9497-4949-4788-989d-129b207dae3e') ON CONFLICT DO NOTHING;
INSERT INTO public.challenge_options (id, is_correct, order_index, text, sub_task_id) VALUES ('02d1d32b-a0eb-4761-a5fe-5041ce4d2e2d', false, 2, 'bad', '2aaf9497-4949-4788-989d-129b207dae3e') ON CONFLICT DO NOTHING;
INSERT INTO public.challenge_options (id, is_correct, order_index, text, sub_task_id) VALUES ('d52d2547-3d2a-4640-8fde-8acad051b550', true, 0, 'good', 'e0bc32fa-5cd7-43a8-a6db-409bb9e357bb') ON CONFLICT DO NOTHING;
INSERT INTO public.challenge_options (id, is_correct, order_index, text, sub_task_id) VALUES ('c5bf6187-3592-47e7-852e-827169fce758', false, 1, 'meh', 'e0bc32fa-5cd7-43a8-a6db-409bb9e357bb') ON CONFLICT DO NOTHING;
INSERT INTO public.challenge_options (id, is_correct, order_index, text, sub_task_id) VALUES ('a2269810-4bcc-4b2c-b147-b154d8669997', false, 2, 'bad', 'e0bc32fa-5cd7-43a8-a6db-409bb9e357bb') ON CONFLICT DO NOTHING;


--
-- Data for Name: courses; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO public.courses (id, badge_icon, badge_primary_color, badge_template, badge_text_color, created_at, description, image_url, invite_code, is_private, is_published, short_description, title, topic, updated_at, mc_attempts_mode, badge_enabled) VALUES ('81195733-da60-4665-ad05-302bbe9e4724', '🎓', '#4f46e5', 2, '#ffffff', '2026-05-03 09:55:28.331572', '<p>Wir hacken alles LMAO weil der kurs so mega gut ist</p>', NULL, NULL, false, true, 'Wir hacken alles LMAO', 'IT Security Kurs von Hausen', NULL, '2026-05-03 11:33:53.45889', 'ONCE', true) ON CONFLICT DO NOTHING;
INSERT INTO public.courses (id, badge_icon, badge_primary_color, badge_template, badge_text_color, created_at, description, image_url, invite_code, is_private, is_published, short_description, title, topic, updated_at, mc_attempts_mode, badge_enabled) VALUES ('11c9da46-a2cc-4362-900b-f375825163c2', '🚀', '#ffffff', 1, '#000000', '2026-05-03 15:49:07.041405', '<p>This course contains hands-on web security challenges that simulate common vulnerabilities found in modern web applications.</p><p>The labs cover topics such as reconnaissance, authentication and authorization flaws, broken access control, IDOR, SSRF, insecure API endpoints, and other practical attack surfaces.</p><p>Each challenge runs as its own vulnerable lab web application. Subtasks and flag submission are handled by the ISTP platform.</p><p></p>', 'https://images.unsplash.com/photo-1667264501379-c1537934c7ab?q=80&w=1074&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D', NULL, false, true, 'Practice real-world web security vulnerabilities in isolated lab environments.', 'Web Security Labs', 'Web-Security', '2026-05-03 15:49:39.248695', 'ONCE', true) ON CONFLICT DO NOTHING;
INSERT INTO public.courses (id, badge_icon, badge_primary_color, badge_template, badge_text_color, created_at, description, image_url, invite_code, is_private, is_published, short_description, title, topic, updated_at, mc_attempts_mode, badge_enabled) VALUES ('b3e7b146-ba2f-4aef-b77c-4076ad1fd8dd', NULL, NULL, NULL, NULL, '2026-05-03 18:12:47.964964', '<p>Add a description...</p>', NULL, NULL, false, true, 'Auto Enrollment Test Short Description', 'Auto Enrollment Test', NULL, '2026-05-03 18:12:47.964964', 'UNLIMITED', true) ON CONFLICT DO NOTHING;
INSERT INTO public.courses (id, badge_icon, badge_primary_color, badge_template, badge_text_color, created_at, description, image_url, invite_code, is_private, is_published, short_description, title, topic, updated_at, mc_attempts_mode, badge_enabled) VALUES ('4d0fd360-5d1b-462b-88ee-39281085698e', NULL, NULL, NULL, NULL, '2026-05-03 19:15:16.052595', '<p>Add a description...</p>', NULL, '2K582G', true, false, 'dave test 1', 'dave test 1', NULL, '2026-05-03 19:15:16.052595', 'UNLIMITED', true) ON CONFLICT DO NOTHING;
INSERT INTO public.courses (id, badge_icon, badge_primary_color, badge_template, badge_text_color, created_at, description, image_url, invite_code, is_private, is_published, short_description, title, topic, updated_at, mc_attempts_mode, badge_enabled) VALUES ('2032ea00-5c15-45ca-86df-1184772c8260', NULL, NULL, NULL, NULL, '2026-05-06 10:34:29.600098', '<p>Add a description...</p>', NULL, NULL, false, false, 'Test', 'TZest', 'Web-Security', '2026-05-06 10:34:29.600098', 'UNLIMITED', true) ON CONFLICT DO NOTHING;


--
-- Data for Name: course_labs; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO public.course_labs (id, created_at, due_at, order_index, updated_at, challenge_id, course_id) VALUES ('2ea77ee2-f777-4bc5-82c5-edc99bb8eb36', '2026-05-03 11:33:53.562033', '2026-05-09 11:56:00', 0, '2026-05-03 11:33:53.562033', '02a0f4e0-913a-4ecc-892c-8b89c1c0c5e2', '81195733-da60-4665-ad05-302bbe9e4724') ON CONFLICT DO NOTHING;
INSERT INTO public.course_labs (id, created_at, due_at, order_index, updated_at, challenge_id, course_id) VALUES ('70024c28-03d4-4637-b1b2-46ac77bccca2', '2026-05-03 11:33:53.562556', NULL, 1, '2026-05-03 11:33:53.562556', '1c2bbc16-9b0f-4f1c-8683-ffec7dd002c5', '81195733-da60-4665-ad05-302bbe9e4724') ON CONFLICT DO NOTHING;
INSERT INTO public.course_labs (id, created_at, due_at, order_index, updated_at, challenge_id, course_id) VALUES ('cde1e0d2-650c-4172-8708-46573a2baa55', '2026-05-03 11:33:53.562963', NULL, 2, '2026-05-03 11:33:53.562963', 'c38a14d8-a3f6-4cc5-a5ec-0efda41a4fba', '81195733-da60-4665-ad05-302bbe9e4724') ON CONFLICT DO NOTHING;
INSERT INTO public.course_labs (id, created_at, due_at, order_index, updated_at, challenge_id, course_id) VALUES ('67b47751-49c3-48db-97a3-d17f8e2a7dc3', '2026-05-06 07:30:51.03065', '2026-06-01 17:56:00', 0, '2026-05-06 07:30:51.03065', 'a7c223bd-6765-41ad-83a3-028ea7fcc026', '11c9da46-a2cc-4362-900b-f375825163c2') ON CONFLICT DO NOTHING;
INSERT INTO public.course_labs (id, created_at, due_at, order_index, updated_at, challenge_id, course_id) VALUES ('9436a9dd-21a7-4e45-9484-fccbd58f2e7f', '2026-05-06 07:30:51.035431', '2026-06-01 18:00:00', 1, '2026-05-06 07:30:51.035431', 'd92b165b-9c80-4d81-a25a-01e581476d26', '11c9da46-a2cc-4362-900b-f375825163c2') ON CONFLICT DO NOTHING;


--
-- Data for Name: course_topics; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO public.course_topics (topic, is_active, created_at, updated_at) VALUES ('Web-Security', true, '2026-05-03 15:48:17.134403', '2026-05-03 15:48:17.134403') ON CONFLICT DO NOTHING;


--
-- PostgreSQL database dump complete
--

\unrestrict c21WpcfjzzrK8x4xjfUvfSE0sP3eBFhDTQpJCU6nsNbjYwnZV9BlltPi35X6rcR

