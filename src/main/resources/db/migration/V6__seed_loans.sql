-- V6: seed school grades and visitor roles
INSERT INTO school_grade (level, grade) VALUES
  ('PRIMARY', 1), ('PRIMARY', 2), ('PRIMARY', 3),
  ('PRIMARY', 4), ('PRIMARY', 5), ('PRIMARY', 6),
  ('SECONDARY', 1), ('SECONDARY', 2), ('SECONDARY', 3),
  ('SECONDARY', 4), ('SECONDARY', 5);

INSERT INTO visitor_role (code, name, max_days) VALUES
  ('STUDENT', 'Student', 7),
  ('TEACHER', 'Teacher', 10);
