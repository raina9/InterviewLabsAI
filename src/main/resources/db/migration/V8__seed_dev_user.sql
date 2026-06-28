INSERT INTO users (id, google_sub, email, name, picture, created_at)
VALUES (
  '00000000-0000-0000-0000-000000000001',
  'dev-google-sub',
  'dev@interviewlab.local',
  'InterviewLab Dev',
  null,
  now()
) ON CONFLICT (id) DO NOTHING;
