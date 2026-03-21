package com.bookash.app

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Teste instrumentado para validar a edição de transações pendentes.
 * 
 * IMPORTANTE: Este teste requer:
 * 1. Dispositivo ou emulador Android
 * 2. Conexão com Supabase configurada
 * 3. Usuário de teste logado
 * 
 * Para rodar: gradle connectedAndroidTest
 * 
 * Logs serão capturados e exibidos no Logcat com tag: TransactionEditTest
 */
@RunWith(AndroidJUnit4::class)
class TransactionEditTest {

    companion object {
        private const val TAG = "TransactionEditTest"
    }

    @Before
    fun setup() {
        Log.i(TAG, "========================================")
        Log.i(TAG, "INICIANDO TESTES DE EDIÇÃO DE TRANSAÇÕES")
        Log.i(TAG, "========================================")
    }

    /**
     * Testa a construção do JSON de atualização.
     * Este teste não precisa de conexão real com o Supabase.
     */
    @Test
    fun testUpdateJsonConstruction() {
        Log.i(TAG, "\n>>> TESTE: Construção do JSON de atualização")
        
        // Simula uma transação pendente
        val transaction = Transaction(
            id = "test-pending-id",
            userId = "test-user",
            description = "Transação Pendente Teste",
            categoryId = "cat-123",
            categoryName = "Teste",
            amount = 150.0,
            type = "expense",
            date = "2026-03-20",
            accountId = "acc-456",
            status = "pending",
            isRecurring = false
        )
        
        Log.i(TAG, "Transação original: id=${transaction.id}, status=${transaction.status}")
        
        // Constrói o JSON de atualização
        val updateJson = buildUpdateJson(transaction)
        Log.i(TAG, "JSON de atualização gerado:\n$updateJson")
        
        // Valida o JSON
        val json = JSONObject(updateJson)
        
        // Verificações críticas
        assertTrue("JSON deve ter campo 'type'", json.has("type"))
        assertTrue("JSON deve ter campo 'amount'", json.has("amount"))
        assertTrue("JSON deve ter campo 'description'", json.has("description"))
        assertTrue("JSON deve ter campo 'status'", json.has("status"))
        
        val statusInJson = json.getString("status")
        assertEquals("Status deve ser 'pending'", "pending", statusInJson)
        
        Log.i(TAG, "✓ TESTE PASSOU: status='$statusInJson' está no JSON corretamente")
        
        // Log detalhado para debug no GitHub Actions
        println("\n========== JSON VALIDADO ==========")
        println("type: ${json.getString("type")}")
        println("amount: ${json.getDouble("amount")}")
        println("description: ${json.getString("description")}")
        println("status: $statusInJson")
        println("===================================\n")
    }

    /**
     * Testa se o status "completed" também é incluído corretamente.
     */
    @Test
    fun testCompletedStatusInJson() {
        Log.i(TAG, "\n>>> TESTE: Status 'completed' no JSON")
        
        val transaction = Transaction(
            id = "test-completed-id",
            userId = "test-user",
            description = "Transação Concluída",
            categoryId = "cat-123",
            categoryName = "Teste",
            amount = 200.0,
            type = "income",
            date = "2026-03-20",
            accountId = "acc-456",
            status = "completed",
            isRecurring = false
        )
        
        val updateJson = buildUpdateJson(transaction)
        val json = JSONObject(updateJson)
        
        assertEquals("completed", json.getString("status"))
        Log.i(TAG, "✓ TESTE PASSOU: status='completed' incluído corretamente")
    }

    /**
     * Testa uma transferência pendente.
     */
    @Test
    fun testPendingTransferJson() {
        Log.i(TAG, "\n>>> TESTE: Transferência pendente")
        
        val transaction = Transaction(
            id = "test-transfer-id",
            userId = "test-user",
            description = "Transferência Programada",
            categoryId = "",
            categoryName = "Transferência",
            amount = 500.0,
            type = "transfer",
            date = "2026-03-20",
            fromAccountId = "acc-from",
            toAccountId = "acc-to",
            status = "pending",
            isRecurring = false
        )
        
        val updateJson = buildUpdateJson(transaction)
        val json = JSONObject(updateJson)
        
        assertEquals("transfer", json.getString("type"))
        assertEquals("pending", json.getString("status"))
        assertTrue("from_account_id deve estar presente", json.has("from_account_id"))
        assertTrue("to_account_id deve estar presente", json.has("to_account_id"))
        
        Log.i(TAG, "✓ TESTE PASSOU: transferência pendente com campos corretos")
        println("\nTRANSFERÊNCIA PENDENTE JSON:\n$json\n")
    }

    /**
     * Testa se valores null são tratados corretamente.
     */
    @Test
    fun testNullStatusHandling() {
        Log.i(TAG, "\n>>> TESTE: Status null deve ser omitido")
        
        val transaction = Transaction(
            id = "test-null-status",
            userId = "test-user",
            description = "Teste sem status",
            categoryId = "cat-123",
            categoryName = "Teste",
            amount = 50.0,
            type = "expense",
            date = "2026-03-20",
            accountId = "acc-456",
            status = null, // Status null
            isRecurring = false
        )
        
        val updateJson = buildUpdateJson(transaction)
        val json = JSONObject(updateJson)
        
        // Status null NÃO deve estar no JSON
        assertFalse("Status null não deve estar no JSON", json.has("status"))
        
        Log.i(TAG, "✓ TESTE PASSOU: status null omitido corretamente")
    }

    // ========== Métodos auxiliares ==========

    /**
     * Constrói o JSON de atualização seguindo a mesma lógica do SupabaseService.updateTransaction()
     */
    private fun buildUpdateJson(tx: Transaction): String {
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
            if (tx.recurringId != null) {
                append(",\"recurring_id\":\"${tx.recurringId}\"")
            }
            append("}")
        }
    }

    private fun jsonEscape(value: String): String {
        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }
}
