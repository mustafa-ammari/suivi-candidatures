CREATE TABLE IF NOT EXISTS profil (
                                      id INTEGER PRIMARY KEY AUTOINCREMENT,
                                      nom TEXT NOT NULL,
                                      cv_path TEXT NOT NULL,
                                      lm_path TEXT NOT NULL,
                                      domaine TEXT,
                                      niveau TEXT,
                                      competences TEXT,
                                      mots_cles TEXT,
                                      created_at DATETIME DEFAULT CURRENT_TIMESTAMP
)