-- Hibernate ne met pas a jour les CHECK constraints d'un enum deja cree.
-- Conserver les roles historiques permet au DataSeeder de migrer les lignes
-- existantes vers les nouveaux roles sans bloquer le demarrage.
ALTER TABLE utilisateur_roles
    DROP CONSTRAINT IF EXISTS utilisateur_roles_role_check;

ALTER TABLE utilisateur_roles
    ADD CONSTRAINT utilisateur_roles_role_check
    CHECK (role IN (
        'CLIENT',
        'ADMIN_PLATFORM',
        'OPERATOR_ADMIN',
        'OPERATOR_AGENT',
        'ADMIN',
        'OPERATEUR'
    ));

ALTER TABLE utilisateur
    DROP CONSTRAINT IF EXISTS utilisateur_statut_check;

ALTER TABLE utilisateur
    ADD CONSTRAINT utilisateur_statut_check
    CHECK (statut IN ('EN_ATTENTE', 'ACTIF', 'REJETE', 'SUSPENDU'));
