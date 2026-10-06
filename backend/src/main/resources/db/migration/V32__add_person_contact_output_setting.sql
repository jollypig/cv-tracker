ALTER TABLE person_contact ADD COLUMN show_contact boolean NOT NULL DEFAULT true;

UPDATE person_contact contact
SET show_contact = person.show_contacts
FROM person
WHERE contact.person_id = person.id;

ALTER TABLE person DROP COLUMN show_contacts;