# Manuel de Test E2E — Frontend
## Sprint : Financial Sync, Search, Audit & Case-Client

> **Prérequis :** Backend démarré, Frontend démarré sur `http://localhost:4200`
> **Compte de test :** `admin` / `admin123`

---

## Légende

| Symbole | Signification |
|---------|--------------|
| ✅ | Résultat attendu (succès) |
| 🔴 | Comportement attendu (erreur/validation) |
| 📋 | Pré-condition |

---

## Scénario 1 — SearchableSelect : Formulaire de création de dossier

**Objectif :** Vérifier que tous les champs du formulaire utilisent le composant de recherche dynamique.

🔗 **URL :** `http://localhost:4200/cases/new`

### 1.1 — Champ Tribunal

| Étape | Action | Résultat attendu |
|-------|--------|-----------------|
| 1 | Aller sur `/cases/new` | ✅ Le formulaire s'affiche |
| 2 | Cliquer dans le champ "Tribunal" | ✅ Une liste de tribunaux apparaît immédiatement |
| 3 | Taper "casa" | ✅ Après ~300ms, seuls les tribunaux contenant "casa" apparaissent (ex: "Tribunal d'appel de Casablanca") |
| 4 | Taper "ZZZZ" | ✅ Message "Aucun résultat trouvé" |
| 5 | Cliquer sur un tribunal dans la liste | ✅ Le nom apparaît dans le champ, la liste se ferme |
| 6 | Cliquer sur le ✕ | ✅ La sélection est effacée |
| 7 | Cliquer ailleurs dans la page | ✅ La liste se ferme sans sélection |

### 1.2 — Champ Type d'affaire

| Étape | Action | Résultat attendu |
|-------|--------|-----------------|
| 1 | Cliquer dans "Type d'affaire" | ✅ Les 4 types apparaissent (Civile, Pénale, Commerciale, Administrative) |
| 2 | Taper "pén" | ✅ Seul "Pénale" filtré |
| 3 | Sélectionner un type | ✅ Type sélectionné |

### 1.3 — Champ Catégorie (dépend du type)

| Étape | Action | Résultat attendu |
|-------|--------|-----------------|
| 1 | Sans type sélectionné, cliquer dans "Catégorie" et taper n'importe quoi | ✅ Aucun résultat (le filtre `caseTypeCode` est vide) |
| 2 | Sélectionner le type "Pénale" d'abord | — |
| 3 | Cliquer dans "Catégorie" et taper quelques lettres | ✅ Seules les catégories de type Pénal apparaissent |
| 4 | Changer le type pour "Civile" | ✅ Le champ Catégorie est vidé automatiquement |
| 5 | Taper dans Catégorie | ✅ Seules les catégories civiles apparaissent |

### 1.4 — Champ Avocats (multi-sélection)

| Étape | Action | Résultat attendu |
|-------|--------|-----------------|
| 1 | Cliquer dans "Avocats" | ✅ Liste d'avocats apparaît |
| 2 | Sélectionner un avocat | ✅ Un chip (badge) avec son nom apparaît sous le champ |
| 3 | Sélectionner un deuxième avocat | ✅ Deux chips affichés |
| 4 | Cliquer sur un chip déjà sélectionné dans la liste | ✅ Le chip est retiré |
| 5 | Cliquer sur ✕ dans un chip | ✅ Cet avocat est désélectionné |

### 1.5 — Champ Client (optionnel)

| Étape | Action | Résultat attendu |
|-------|--------|-----------------|
| 1 | Laisser le champ "Client assigné" vide | ✅ Formulaire valide sans client |
| 2 | Taper un prénom dans le champ client | ✅ Suggestions de clients apparaissent |
| 3 | Sélectionner un client | ✅ Nom affiché dans le champ |

---

## Scénario 2 — Création d'un dossier complet

**Objectif :** Créer un dossier en utilisant tous les nouveaux champs SearchableSelect.

📋 **Pré-condition :** Au moins un avocat et un client existent.

🔗 **URL :** `http://localhost:4200/cases/new`

| Étape | Action | Résultat attendu |
|-------|--------|-----------------|
| 1 | Remplir "Type d'affaire" → sélectionner "Civile" | ✅ Type sélectionné |
| 2 | Remplir "Catégorie" → taper et sélectionner une catégorie civile | ✅ Catégorie sélectionnée |
| 3 | Remplir "Tribunal" → taper "rabat" et sélectionner un tribunal | ✅ Tribunal sélectionné |
| 4 | Remplir "Avocats" → sélectionner au moins un avocat | ✅ Chip avocat visible |
| 5 | Remplir "Description" avec du texte | — |
| 6 | Dans "Client", sélectionner un client | ✅ Nom du client visible |
| 7 | Cliquer "Create Case" | ✅ Redirection vers le détail du dossier |
| 8 | Dans le détail, section "Client assigné" | ✅ Nom du client affiché |
| 9 | Section "Lawyers" | ✅ Avocat(s) sélectionné(s) affichés en badges |

---

## Scénario 3 — Assignation de client depuis la page de détail

**Objectif :** Assigner, modifier et retirer un client directement depuis la fiche dossier.

### 3.1 — Assigner un client à un dossier sans client

📋 **Pré-condition :** Un dossier sans client assigné.

🔗 **URL :** `http://localhost:4200/cases/{id}`

| Étape | Action | Résultat attendu |
|-------|--------|-----------------|
| 1 | Ouvrir un dossier sans client | ✅ Section "Client assigné" affiche "Aucun client" + lien "Assigner" |
| 2 | Cliquer "Assigner" | ✅ Un champ de recherche inline apparaît avec boutons "OK" et "Annuler" |
| 3 | Taper un nom dans le champ et sélectionner un client | ✅ Client sélectionné visible dans le champ |
| 4 | Cliquer "OK" | ✅ Nom du client s'affiche, boutons "Modifier" et "Retirer" apparaissent |
| 5 | Rafraîchir la page (F5) | ✅ Le client est toujours assigné (persistance) |

### 3.2 — Modifier le client assigné

| Étape | Action | Résultat attendu |
|-------|--------|-----------------|
| 1 | Sur un dossier avec client, cliquer "Modifier" | ✅ Champ de recherche inline réapparaît |
| 2 | Taper et sélectionner un autre client | — |
| 3 | Cliquer "OK" | ✅ Nouveau nom de client affiché immédiatement |
| 4 | Rafraîchir la page | ✅ Nouveau client persisté |

### 3.3 — Retirer le client

| Étape | Action | Résultat attendu |
|-------|--------|-----------------|
| 1 | Sur un dossier avec client, cliquer "Retirer" | ✅ Boîte de confirmation apparaît |
| 2 | Cliquer "Annuler" | ✅ Rien ne change |
| 3 | Cliquer "Retirer" à nouveau puis confirmer | ✅ Section affiche "Aucun client" + bouton "Assigner" |
| 4 | Rafraîchir la page | ✅ Désassignation persistée |

### 3.4 — Modification du client depuis le formulaire d'édition

🔗 **URL :** `http://localhost:4200/cases/{id}/edit`

| Étape | Action | Résultat attendu |
|-------|--------|-----------------|
| 1 | Ouvrir un dossier avec client en mode édition | ✅ Le champ "Client assigné" affiche la valeur actuelle (peut afficher l'ID si non résolu) |
| 2 | Effacer et chercher un autre client | ✅ Suggestions apparaissent |
| 3 | Sélectionner un nouveau client et sauvegarder | ✅ Page détail affiche le nouveau client |

---

## Scénario 4 — Historique avec diff avant/après

**Objectif :** Vérifier que l'onglet "History" affiche un tableau de différences lisible.

📋 **Pré-condition :** Un dossier existant.

### 4.1 — Modifier un dossier et consulter le diff

| Étape | Action | Résultat attendu |
|-------|--------|-----------------|
| 1 | Ouvrir un dossier et cliquer "Edit Case" | — |
| 2 | Changer la priorité (ex: NORMAL → URGENT) | — |
| 3 | Changer quelques mots dans la description | — |
| 4 | Sauvegarder | ✅ Redirection vers la page de détail |
| 5 | Cliquer sur l'onglet "History" | ✅ Nouvelle entrée "CASE_UPDATED" apparaît en premier |
| 6 | Inspecter l'entrée | ✅ Un tableau à 3 colonnes : "Champ", "Avant" (rouge barré), "Après" (vert) |
| 7 | Ligne "Priorité" | ✅ Avant : "NORMAL" barré en rouge — Après : "URGENT" en vert |
| 8 | Ligne "Description" | ✅ Ancienne description barrée — Nouvelle description en vert |
| 9 | Champs non modifiés | ✅ Absents du tableau (filtre correct) |

### 4.2 — Anciennes entrées d'audit (avant ce sprint)

| Étape | Action | Résultat attendu |
|-------|--------|-----------------|
| 1 | Inspecter une entrée d'audit créée avant ce sprint | ✅ Pas de tableau diff, affichage "repli" avec tags gris (champs modifiés) |
| 2 | Aucune erreur JavaScript dans la console | ✅ Page stable |

### 4.3 — Libellés français

| Étape | Action | Résultat attendu |
|-------|--------|-----------------|
| 1 | Vérifier les noms de champs dans le tableau diff | ✅ "Priorité" (pas "priority"), "Statut" (pas "status"), "Tribunal" (pas "tribunal") |

---

## Scénario 5 — Modal de paiement de facture

**Objectif :** Vérifier que marquer une facture comme "Payée" ouvre un modal et crée une transaction.

📋 **Pré-condition :** Une facture en statut "Envoyée" (SENT) existe.

🔗 **URL :** `http://localhost:4200/financial/invoices/{id}`

### 5.1 — Ouvrir le modal

| Étape | Action | Résultat attendu |
|-------|--------|-----------------|
| 1 | Ouvrir une facture en statut "Envoyée" | ✅ Bouton vert "Marquer comme Payée" visible |
| 2 | Cliquer "Marquer comme Payée" | ✅ Modal s'ouvre avec : titre, numéro de facture, montant total |
| 3 | Vérifier le contenu du modal | ✅ Champs : Mode de paiement (select), Date de paiement (date picker), Référence (texte optionnel) |
| 4 | Cliquer "Annuler" | ✅ Modal se ferme, statut inchangé |

### 5.2 — Validation du modal

| Étape | Action | Résultat attendu |
|-------|--------|-----------------|
| 1 | Cliquer "Confirmer le paiement" sans remplir les champs | 🔴 Message d'erreur "Mode de paiement et date sont obligatoires" |
| 2 | Sélectionner un mode de paiement mais pas de date | 🔴 Même erreur |
| 3 | Remplir la date mais pas le mode | 🔴 Même erreur |

### 5.3 — Paiement réussi

| Étape | Action | Résultat attendu |
|-------|--------|-----------------|
| 1 | Sélectionner "Virement" comme mode de paiement | — |
| 2 | Laisser la date d'aujourd'hui (pré-remplie) | — |
| 3 | Saisir une référence optionnelle (ex: "VIR-2026-001") | — |
| 4 | Cliquer "Confirmer le paiement" | ✅ Modal se ferme, statut de la facture passe à "Payée" (badge vert) |
| 5 | Aller sur `/financial/ledger` ou onglet "Finances" du dossier lié | ✅ Une nouvelle transaction REVENUE apparaît avec le montant de la facture |

### 5.4 — Transitions disponibles après paiement

| Étape | Action | Résultat attendu |
|-------|--------|-----------------|
| 1 | Sur une facture "Payée", vérifier les boutons d'action | ✅ Aucun bouton de transition visible (statut terminal) |

### 5.5 — Annuler une facture (DRAFT ou SENT)

| Étape | Action | Résultat attendu |
|-------|--------|-----------------|
| 1 | Sur une facture "Brouillon" ou "Envoyée", cliquer "→ Annulée" | ✅ Boîte de confirmation navigateur apparaît |
| 2 | Confirmer | ✅ Statut passe à "Annulée", aucune transaction créée |

---

## Scénario 6 — Tests de régression

**Objectif :** Vérifier qu'aucune fonctionnalité existante n'a été cassée.

### 6.1 — Création de dossier (sans client)

| Étape | Action | Résultat attendu |
|-------|--------|-----------------|
| 1 | Créer un dossier sans remplir le champ "Client assigné" | ✅ Dossier créé sans erreur |
| 2 | Page de détail | ✅ Section "Client assigné" affiche "Aucun client" |

### 6.2 — Validation des champs obligatoires

| Étape | Action | Résultat attendu |
|-------|--------|-----------------|
| 1 | Soumettre le formulaire de création sans type d'affaire | 🔴 Message "Le type d'affaire est obligatoire" |
| 2 | Soumettre sans tribunal | 🔴 Message "Le tribunal est obligatoire" |
| 3 | Soumettre sans avocat | 🔴 Message d'erreur "At least one lawyer must be selected" |

### 6.3 — Changement de statut de dossier

| Étape | Action | Résultat attendu |
|-------|--------|-----------------|
| 1 | Sur un dossier, cliquer "Change Status" | ✅ Modal de changement de statut s'ouvre |
| 2 | Sélectionner un statut valide et confirmer | ✅ Statut mis à jour dans l'en-tête du dossier |
| 3 | Aller dans l'onglet "History" | ✅ Nouvelle entrée visible |

### 6.4 — Navigation et liste des dossiers

| Étape | Action | Résultat attendu |
|-------|--------|-----------------|
| 1 | Aller sur `/cases` | ✅ Liste des dossiers s'affiche sans erreur |
| 2 | Rechercher un dossier par numéro | ✅ Résultats filtrés correctement |
| 3 | Cliquer sur un dossier | ✅ Page de détail s'ouvre |

### 6.5 — Ledger financier

| Étape | Action | Résultat attendu |
|-------|--------|-----------------|
| 1 | Aller sur `/financial/ledger` | ✅ Liste des transactions affichée (dont les 11 transactions seed) |
| 2 | Aller sur `/financial/invoices` | ✅ Liste des factures affichée |
| 3 | Aller sur une facture en DRAFT | ✅ Boutons "→ Envoyée" et "→ Annulée" visibles |

---

## Checklist finale

Cocher chaque point après validation :

- [ ] SearchableSelect Tribunal — recherche, sélection, effacement, clic extérieur
- [ ] SearchableSelect Type d'affaire — filtre les 4 types
- [ ] SearchableSelect Catégorie — filtre par type sélectionné, se vide au changement de type
- [ ] SearchableSelect Avocats — multi-sélection avec chips, désélection
- [ ] SearchableSelect Client — optionnel, sélection/effacement
- [ ] Création de dossier avec client → client visible dans le détail
- [ ] Assignation de client depuis le détail → "Assigner" → OK
- [ ] Modification du client depuis le détail → "Modifier" → OK
- [ ] Suppression du client depuis le détail → "Retirer" → confirmé
- [ ] Persistance de toutes les modifications après F5
- [ ] Historique : tableau diff avec libellés français après modification
- [ ] Historique : seuls les champs modifiés apparaissent dans le diff
- [ ] Historique : anciennes entrées sans snapshot affichent les tags gris (pas d'erreur)
- [ ] Modal paiement : s'ouvre depuis facture SENT
- [ ] Modal paiement : validation mode + date obligatoires
- [ ] Modal paiement : paiement confirmé → statut PAID + transaction créée dans le ledger
- [ ] Facture annulée → pas de transaction créée
- [ ] Régression : création dossier sans client fonctionne
- [ ] Régression : liste dossiers, détail dossier, changement statut
- [ ] Régression : ledger et liste factures s'affichent
