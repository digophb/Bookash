package com.bookash.app

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

/**
 * Testes para verificar o fluxo de atualização de transações.
 */
class TransactionUpdateTest {

    /**
     * Simula o jsonEscape de SupabaseService.
     */
    private fun jsonEscape(value: String): String {
        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    /**
     * Simula o jsonBoolean de SupabaseService.
     */
    private fun jsonBoolean(json: JSONObject, key: String, default: Boolean = false): Boolean {
        val value = json.opt(key)
        return when (value) {
            is Boolean -> value
            is String -> value.equals("true", ignoreCase = true)
            else -> default
        }
    }

    /**
     * Simula a construção do body JSON do updateTransaction.
     */
    private fun buildUpdateBody(tx: Transaction): String {
        return buildString {
            append("{")
            append("\"type\":\"${tx.type}\",")
            append("\"amount\":${tx.amount},")
            append("\"description\":\"${jsonEscape(tx.description)}\",")
            append("\"date\":\"${tx.date}\"")
            if (tx.categoryId.isNotEmpty()) {
                append(",\"category_id\":\"${tx.categoryId}\"")
            }
            if (tx.accountId != null) {
                append(",\"account_id\":\"${tx.accountId}\"")
            }
            if (tx.fromAccountId != null) {
                append(",\"from_account_id\":\"${tx.fromAccountId}\"")
            }
            if (tx.toAccountId != null) {
                append(",\"to_account_id\":\"${tx.toAccountId}\"")
            }
            if (tx.status != null) {
                append(",\"status\":\"${tx.status}\"")
            }
            if (!tx.notes.isNullOrEmpty()) {
                append(",\"notes\":\"${jsonEscape(tx.notes!!)}\"")
            }
            if (tx.isRecurring) {
                append(",\"is_recurring\":true")
                if (tx.recurringType != null) {
                    append(",\"recurring_type\":\"${tx.recurringType}\"")
                }
                if (tx.recurringCount != null) {
                    append(",\"recurring_count\":${tx.recurringCount}")
                }
                if (tx.recurringUntil != null) {
                    append(",\"recurring_until\":\"${tx.recurringUntil}\"")
                }
            } else {
                append(",\"is_recurring\":false")
            }
            append("}")
        }
    }

    // ==================== jsonEscape ====================

    @Test
    fun testJsonEscape_aspas() {
        assertEquals("Café \\\"Premium\\\"", jsonEscape("Café \"Premium\""))
    }

    @Test
    fun testJsonEscape_barra_invertida() {
        assertEquals("C:\\\\Users\\\\Test", jsonEscape("C:\\Users\\Test"))
    }

    @Test
    fun testJsonEscape_quebra_linha() {
        assertEquals("Linha1\\nLinha2", jsonEscape("Linha1\nLinha2"))
    }

    @Test
    fun testJsonEscape_tab() {
        assertEquals("Col1\\tCol2", jsonEscape("Col1\tCol2"))
    }

    @Test
    fun testJsonEscape_normal() {
        assertEquals("Descricao normal", jsonEscape("Descricao normal"))
    }

    @Test
    fun testJsonEscape_vazio() {
        assertEquals("", jsonEscape(""))
    }

    // ==================== buildUpdateBody ====================

    @Test
    fun testUpdateBody_despesa_simples() {
        val tx = Transaction(
            id = "abc123",
            description = "Aluguel",
            amount = 1500.0,
            type = "expense",
            date = "2026-03-15",
            categoryId = "cat-001",
            accountId = "acc-001",
            status = "pending"
        )

        val body = buildUpdateBody(tx)
        val json = JSONObject(body)

        assertEquals("expense", json.getString("type"))
        assertEquals(1500.0, json.getDouble("amount"), 0.01)
        assertEquals("Aluguel", json.getString("description"))
        assertEquals("2026-03-15", json.getString("date"))
        assertEquals("cat-001", json.getString("category_id"))
        assertEquals("acc-001", json.getString("account_id"))
        assertEquals("pending", json.getString("status"))
        assertFalse(json.getBoolean("is_recurring"))
    }

    @Test
    fun testUpdateBody_receita() {
        val tx = Transaction(
            id = "xyz789",
            description = "Salário",
            amount = 5000.0,
            type = "income",
            date = "2026-03-01",
            categoryId = "cat-002",
            accountId = "acc-002",
            status = "completed"
        )

        val body = buildUpdateBody(tx)
        val json = JSONObject(body)

        assertEquals("income", json.getString("type"))
        assertEquals(5000.0, json.getDouble("amount"), 0.01)
        assertEquals("completed", json.getString("status"))
    }

    @Test
    fun testUpdateBody_transferencia() {
        val tx = Transaction(
            id = "trf001",
            description = "Transferência",
            amount = 200.0,
            type = "transfer",
            date = "2026-03-10",
            fromAccountId = "acc-001",
            toAccountId = "acc-003",
            status = "completed"
        )

        val body = buildUpdateBody(tx)
        val json = JSONObject(body)

        assertEquals("transfer", json.getString("type"))
        assertEquals("acc-001", json.getString("from_account_id"))
        assertEquals("acc-003", json.getString("to_account_id"))
        assertFalse(json.has("category_id"))
        assertFalse(json.has("account_id"))
    }

    @Test
    fun testUpdateBody_com_notas() {
        val tx = Transaction(
            id = "n001",
            description = "Teste",
            amount = 100.0,
            type = "expense",
            date = "2026-03-14",
            categoryId = "c1",
            accountId = "a1",
            notes = "Nota de teste"
        )

        val body = buildUpdateBody(tx)
        val json = JSONObject(body)

        assertEquals("Nota de teste", json.getString("notes"))
    }

    @Test
    fun testUpdateBody_notas_com_aspas() {
        val tx = Transaction(
            id = "n002",
            description = "Teste",
            amount = 100.0,
            type = "expense",
            date = "2026-03-14",
            categoryId = "c1",
            accountId = "a1",
            notes = "Disse \"ok\" ao cliente"
        )

        val body = buildUpdateBody(tx)
        val json = JSONObject(body)

        assertEquals("Disse \"ok\" ao cliente", json.getString("notes"))
    }

    @Test
    fun testUpdateBody_descricao_com_aspas() {
        val tx = Transaction(
            id = "d001",
            description = "Compra \"especial\"",
            amount = 50.0,
            type = "expense",
            date = "2026-03-14",
            categoryId = "c1",
            accountId = "a1"
        )

        val body = buildUpdateBody(tx)
        val json = JSONObject(body)

        assertEquals("Compra \"especial\"", json.getString("description"))
    }

    @Test
    fun testUpdateBody_recorrente() {
        val tx = Transaction(
            id = "rec001",
            description = "Aluguel mensal",
            amount = 1500.0,
            type = "expense",
            date = "2026-03-15",
            categoryId = "c1",
            accountId = "a1",
            status = "pending",
            isRecurring = true,
            recurringType = "monthly",
            recurringCount = 12,
            recurringUntil = "2027-03-15"
        )

        val body = buildUpdateBody(tx)
        val json = JSONObject(body)

        assertTrue(json.getBoolean("is_recurring"))
        assertEquals("monthly", json.getString("recurring_type"))
        assertEquals(12, json.getInt("recurring_count"))
        assertEquals("2027-03-15", json.getString("recurring_until"))
    }

    @Test
    fun testUpdateBody_sem_notas() {
        val tx = Transaction(
            id = "n003",
            description = "Teste",
            amount = 100.0,
            type = "expense",
            date = "2026-03-14",
            categoryId = "c1",
            accountId = "a1",
            notes = null
        )

        val body = buildUpdateBody(tx)
        val json = JSONObject(body)

        assertFalse(json.has("notes"))
    }

    @Test
    fun testUpdateBody_sem_categoria() {
        val tx = Transaction(
            id = "c001",
            description = "Teste",
            amount = 100.0,
            type = "expense",
            date = "2026-03-14",
            categoryId = "",
            accountId = "a1"
        )

        val body = buildUpdateBody(tx)
        val json = JSONObject(body)

        assertFalse(json.has("category_id"))
    }

    // ==================== jsonBoolean ====================

    @Test
    fun testJsonBoolean_boolean_true() {
        val json = JSONObject("""{"is_recurring": true}""")
        assertTrue(jsonBoolean(json, "is_recurring"))
    }

    @Test
    fun testJsonBoolean_boolean_false() {
        val json = JSONObject("""{"is_recurring": false}""")
        assertFalse(jsonBoolean(json, "is_recurring"))
    }

    @Test
    fun testJsonBoolean_string_true() {
        val json = JSONObject("""{"is_recurring": "true"}""")
        assertTrue(jsonBoolean(json, "is_recurring"))
    }

    @Test
    fun testJsonBoolean_string_false() {
        val json = JSONObject("""{"is_recurring": "false"}""")
        assertFalse(jsonBoolean(json, "is_recurring"))
    }

    @Test
    fun testJsonBoolean_ausente() {
        val json = JSONObject("""{"outro": true}""")
        assertFalse(jsonBoolean(json, "is_recurring"))
    }

    // ==================== Fluxo Completo ====================

    @Test
    fun testFluxo_update_despesa_para_completed() {
        val original = Transaction(
            id = "t001",
            userId = "u001",
            description = "Conta de luz",
            amount = 250.0,
            type = "expense",
            date = "2026-03-14",
            categoryId = "cat-01",
            categoryName = "Moradia",
            accountId = "acc-01",
            status = "pending"
        )

        // Simular atualização (mudar status para completed)
        val updated = original.copy(status = "completed")
        val body = buildUpdateBody(updated)
        val json = JSONObject(body)

        assertEquals("completed", json.getString("status"))
        assertEquals("Conta de luz", json.getString("description"))
        assertEquals(250.0, json.getDouble("amount"), 0.01)
    }

    @Test
    fun testFluxo_update_transferencia_mudar_contas() {
        val original = Transaction(
            id = "t002",
            userId = "u001",
            description = "Transferencia",
            amount = 100.0,
            type = "transfer",
            date = "2026-03-14",
            fromAccountId = "acc-01",
            toAccountId = "acc-02",
            status = "completed"
        )

        // Simular atualização (mudar conta destino)
        val updated = original.copy(toAccountId = "acc-03")
        val body = buildUpdateBody(updated)
        val json = JSONObject(body)

        assertEquals("acc-01", json.getString("from_account_id"))
        assertEquals("acc-03", json.getString("to_account_id"))
    }

    @Test
    fun testBody_eh_json_valido() {
        val tx = Transaction(
            id = "t003",
            description = "Teste com \"aspas\" e \\ barras",
            amount = 99.9,
            type = "expense",
            date = "2026-03-14",
            categoryId = "c1",
            accountId = "a1",
            notes = "Nota com\nquebra e\ttab"
        )

        val body = buildUpdateBody(tx)

        // Deve ser JSON válido (não lançar exceção)
        val json = JSONObject(body)
        assertEquals("Teste com \"aspas\" e \\ barras", json.getString("description"))
        assertEquals("Nota com\nquebra e\ttab", json.getString("notes"))
    }
}
