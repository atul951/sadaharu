-- classroom = 1
UPDATE specializations SET room_type_id = 1 WHERE name IN ('Mathematics', 'English', 'Social_Studies', 'Foreign_Language');

-- science_lab = 2
UPDATE specializations SET room_type_id = 2 WHERE name IN ('Science');

-- art_studio = 3
UPDATE specializations SET room_type_id = 3 WHERE name IN ('Arts');

-- gym = 4
UPDATE specializations SET room_type_id = 4 WHERE name IN ('Physical_Education');

-- computer_lab = 5
UPDATE specializations SET room_type_id = 5 WHERE name IN ('Computer_Science');

-- music_room = 6
UPDATE specializations SET room_type_id = 6 WHERE name IN ('Music');
