ALTER TABLE candidature
    ADD COLUMN profil_id INTEGER
        REFERENCES profil(id)