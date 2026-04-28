# Financial Management — Manual Testing Scenarios

**Feature:** Financial Ledger & Invoice Management
**Date:** 2026-03-01

---

## Setup

- Start backend: `mvn spring-boot:run`
- Start frontend: `pnpm dev`
- Log in as `admin / admin123`
- Make sure at least one case exists

---

## Scenario 1 — View the Ledger Page

1. Log in as `admin`.
2. In the sidebar, click **Financial** → you should land on `/financial/ledger`.
3. The page shows three summary cards: **Revenus**, **Dépenses**, **Solde**.
4. Below the cards, a table appears with columns: Dossier, Direction, Type, Montant, Date, Référence.
5. Log out, log in as `test_viewer`. The **Financial** link should not appear in the sidebar.

---

## Scenario 2 — Create a Revenue Transaction

1. On the ledger page, click **+ Nouvelle transaction**.
2. A modal opens. Set:
   - Direction: **Revenu**
   - Notice the "Type d'opération" field disappears.
   - Montant: `2000`
   - Select a payment mode and a date (optional).
3. Click **Enregistrer**.
4. The modal closes and the new transaction appears in the table with a green **Revenu** badge.
5. The **Revenus** summary card updates to include the 2000 MAD.

---

## Scenario 3 — Create an Expense Transaction

1. Click **+ Nouvelle transaction** again.
2. Set Direction to **Dépense** → the "Type d'opération" field appears.
3. Pick **Frais d'ouverture**, enter amount `500`.
4. Save → red **Dépense** badge appears in the table.
5. The **Dépenses** and **Solde** cards update accordingly.

---

## Scenario 4 — Validate Required Fields on the Transaction Form

1. Open the create modal.
2. Leave the amount empty → click **Enregistrer** → error message appears.
3. Enter an amount but leave the case selector empty (if visible) → same result.
4. Click **Annuler** → modal closes with no changes.

---

## Scenario 5 — Filter Transactions

1. On the ledger page, open the **Direction** dropdown and select **Revenus**.
2. Only green-badged rows remain in the table.
3. Switch to **Dépenses** → only red-badged rows.
4. Switch back to **Toutes directions** → all rows return.
5. Set a **Date début** and **Date fin** → table filters to that range.
6. Combine a direction filter with a date range → both applied together.

---

## Scenario 6 — Delete a Transaction

1. In the table, click **Supprimer** on any row.
2. A confirmation dialog appears — click **Cancel** → nothing changes.
3. Click **Supprimer** again → confirm → the row disappears and the summary cards update.
4. Log in as `test_viewer` — the Supprimer column should not be visible (no FINANCIAL_UPDATE permission).

---

## Scenario 7 — Export to Excel

1. On the ledger page, click **Exporter Excel**.
2. The button changes to **Export...** and disables during the request.
3. A file named `transactions.xlsx` downloads.
4. Open it — verify it has a header row and one data row per transaction.
5. Apply a direction filter, then export again → the file contains only the filtered rows.

---

## Scenario 8 — Financial Tab on a Case

1. Open any case detail page.
2. A **Finances** tab appears next to History.
3. Click it → summary cards show totals for this case only.
4. Click **+ Nouvelle transaction** inside the tab → the modal opens with the case pre-selected (no case selector visible).
5. Create a revenue transaction of `1000 MAD`.
6. The transaction appears in the tab table and the summary updates.
7. Delete it → it disappears and the summary resets.
8. Log in as `test_viewer` → the Finances tab is not visible on the case detail page.

---


## Scenario 9 — Browse the Invoice List

1. In the sidebar, click **Financial** → then navigate to `/financial/invoices`. 
2. The page shows a table with: N° Facture, Dossier, Date, Statut, Total.
3. Status badges are color-coded: gray = Brouillon, blue = Envoyée, green = Payée, red = Annulée.
4. Log in as `test_viewer` → the invoice list should be inaccessible (no INVOICE_READ). 

---

## Scenario 10 — Create an Invoice

1. Click **+ Nouvelle facture** → you land on `/financial/invoices/new`. 
2. Select a case from the dropdown.
3. Set an issue date.
4. The form already has one line item — fill in a description (`Consultation`), leave type as **Autre**, qty `2`, unit price `750`.
5. Notice the subtotal updates to `1500 MAD`.
6. Click **+ Ajouter** → a second line appears. Fill it: `Frais de dossier`, qty `1`, price `200`.
7. Subtotal is now `1700 MAD`. Set TVA to `100` → Total shows `1800 MAD`.
8. Click **Créer la facture** → you are redirected to the invoice detail page.
9. Invoice number is generated: `FAC-2026-0001` (or next in sequence).
10. Status is **Brouillon**.

---

## Scenario 11 — Validate the Invoice Form

1. Go to `/financial/invoices/new`.
2. Click **Créer la facture** without filling anything → error: "Dossier, date et au moins un article sont obligatoires."
3. Select a case but skip the date → same error.
4. Click **Annuler** → navigate back to the invoice list without creating anything.

---

## Scenario 12 — Invoice Status Transitions (Happy Path)

1. Open the invoice created in Scenario 10 (status = Brouillon).
2. Two buttons are visible: **→ Envoyée** and **→ Annulée**.
3. Click **→ Envoyée** → confirm → status badge changes to **Envoyée** (blue).
4. Now two new buttons: **→ Payée** and **→ Annulée**.
5. Click **→ Payée** → confirm → status = **Payée** (green). No more transition buttons.

---

## Scenario 13 — Invoice Cancellation

1. Create a new invoice (status = Brouillon).
2. Click **→ Annulée** → confirm → status = **Annulée** (red). No transition buttons.
3. Create another invoice, advance it to Envoyée, then cancel it → same result.

---

## Scenario 14 — Invoice Detail Display

1. Open any invoice detail page.
2. Verify the meta section shows: Dossier, Date d'émission, Date d'échéance (or —), creation date.
3. The items table shows each line with description, type, qty, unit price, and line total.
4. The footer shows: Sous-total, TVA, and **Total** in blue.
5. The breadcrumb "Factures / FAC-..." clicking "Factures" navigates back to the list.

---

## Scenario 15 — Delete an Invoice

1. On the invoice list, click **Supprimer** on any invoice → confirm → it disappears.
2. Manually navigate to its URL (`/financial/invoices/{id}`) → the page shows an error state.
3. Log in as a user without INVOICE_MANAGE → the Supprimer button is not visible.

---

## Scenario 16 — Permissions Spot-Check

1. Log in as `admin` → all buttons visible: create transaction, delete transaction, export, create invoice, transition status, delete invoice.
2. Log in as `test_viewer` (no financial permissions):
   - No **Financial** link in the sidebar.
   - Navigating to `/financial/ledger` shows an error or empty state.
3. If a Moderator account is available (FINANCIAL_READ + INVOICE_READ only):
   - Ledger page loads but no **+ Nouvelle transaction**, no **Supprimer** column.
   - Invoice list loads but no **+ Nouvelle facture**, no status buttons, no **Supprimer**.
