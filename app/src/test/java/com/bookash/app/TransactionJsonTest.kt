package com.bookash.app

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

/**
 * Testes unitários para validar a construção do JSON de transações.
 * Estes testes rodam no GitHub Actions e validam a lógica sem precisar de dispositivo.
 */
class TransactionJsonTest {

    /**
     * Testa se o campo status está sendo incluído corretamente no JSON de atualização.
     * Este teste valida a lógica de construção do body em updateTransaction.
     */
    @Test
    fun testStatusFieldIncludedInUpdateJson() {
        // Simula a construção do JSON como feito em SupabaseService.updateTransaction()
        val transaction = createTestTransaction(
            id = "test-id-123",
            type = "expense",
            amount = 100.0,
            description = "Teste",
            status = "pending"
        )
        
        val body = buildUpdateJson(transaction)
        val json = JSONObject(body)
        
        // Verifica se o campo status está presente
        assertTrue("Campo 'status' deve estar presente no JSON", json.has("status"))
        assertEquals("pending", json.getString("status"))
        
        println("✓ Teste passou: status='pending' incluído corretamente no JSON")
        println("JSON gerado: $body")
    }
    
    @Test
    fun testCompletedStatusIncludedInUpdateJson() {
        val transaction = createTestTransaction(
            id = "test-id-456",
            type = "income",
            amount = 500.0,
            description = "Salário",
            status = "completed"
        )
        
        val body = buildUpdateJson(transaction)
        val json = JSONObject(body)
        
        assertTrue("Campo 'status' deve estar presente", json.has("status"))
        assertEquals("completed", json.getString("status"))
        
        println("✓ Teste passou: status='completed' incluído corretamente")
    }
    
    @Test
    fun testNullStatusExcludedFromJson() {
        val transaction = createTestTransaction(
            id = "test-id-789",
            type = "expense",
            amount = 50.0,
            description = "Teste sem status",
            status = null
        )
        
        val body = buildUpdateJson(transaction)
        val json = JSONObject(body)
        
        // Status null não deve ser incluído
        assertFalse("Campo 'status' não deve estar presente quando null", json.has("status"))
        
        println("✓ Teste passou: status null não é incluído no JSON")
    }
    
    @Test
    fun testAllRequiredFieldsIncluded() {
        val transaction = createTestTransaction(
            id = "test-id-full",
            type = "expense",
            amount = 150.0,
            description = "Transação completa",
            status = "pending",
            categoryId = "cat-123",
            accountId = "acc-456"
        )
        
        val body = buildUpdateJson(transaction)
        val json = JSONObject(body)
        
        // Verifica campos obrigatórios
        assertTrue("type deve estar presente", json.has("type"))
        assertTrue("amount deve estar presente", json.has("amount"))
        assertTrue("description deve estar presente", json.has("description"))
        assertTrue("date deve estar presente", json.has("date"))
        assertTrue("status deve estar presente", json.has("status"))
        
        println("✓ Teste passou: todos os campos obrigatórios presentes")
        println("JSON completo: $body")
    }
    
    @Test
    fun testTransferFieldsIncluded() {
        val transaction = createTestTransaction(
            id = "test-transfer",
            type = "transfer",
            amount = 200.0,
            description = "Transferência",
            status = "completed",
            fromAccountId = "acc-from",
            toAccountId = "acc-to"
        )
        
        val body = buildUpdateJson(transaction)
        val json = JSONObject(body)
        
        assertEquals("transfer", json.getString("type"))
        assertTrue("from_account_id deve estar presente", json.has("from_account_id"))
        assertTrue("to_account_id deve estar presente", json.has("to_account_id"))
        
        println("✓ Teste passou: campos de transferência incluídos")
    }
    
    @Test
    fun testJsonEscapeSpecialCharacters() {
        val transaction = createTestTransaction(
            id = "test-escape",
            type = "expense",
            amount = 100.0,
            description = "Teste com \"aspas\" e\nquebra de linha",
            status = "pending"
        )
        
        val body = buildUpdateJson(transaction)
        
        // Verifica se o JSON é válido (não lança exceção)
        val json = JSONObject(body)
        assertNotNull(json)
        
        println("✓ Teste passou: caracteres especiais escapados corretamente")
        println("JSON: $body")
    }
    
    // ========== TESTES PARA TRANSAÇÕES RECORRENTES ==========
    
    /**
     * Teste CRÍTICO: Simula a edição de uma ocorrência subsequente de transação recorrente.
     * Este é o cenário que está falhando: "Apenas esta ocorrência"
     * 
     * Uma ocorrência subsequente tem:
     * - isRecurring = false (só a primeira tem true)
     * - recurringId = UUID da série (link para a transação original)
     */
    @Test
    fun testRecurringOccurrenceEdit() {
        println("\n========== TESTE: Edição de ocorrência recorrente ==========")
        
        // Simula uma ocorrência subsequente (não a primeira)
        val transaction = TestTransactionRecurring(
            id = "tx-subsequent-123",
            type = "expense",
            amount = 150.0,
            description = "Aluguel - Março",
            date = "2026-03-20",
            status = "pending",
            categoryId = "cat-housing",
            accountId = "acc-main",
            isRecurring = false,  // SUBSEQUENTE: não é a primeira
            recurringType = null, // SUBSEQUENTE: não tem tipo
            recurringCount = null, // SUBSEQUENTE: não tem count
            recurringUntil = null, // SUBSEQUENTE: não tem until
            recurringId = "uuid-series-abc"  // LINK para a série
        )
        
        val body = buildUpdateJsonRecurring(transaction)
        val json = JSONObject(body)
        
        println("JSON gerado para ocorrência subsequente:\n$body")
        
        // Validações críticas
        assertTrue("type deve estar presente", json.has("type"))
        assertTrue("amount deve estar presente", json.has("amount"))
        assertTrue("status deve estar presente", json.has("status"))
        assertEquals("pending", json.getString("status"))
        
        // Campos de recorrência para SUBSEQUENTE
        assertEquals("is_recurring deve ser false para subsequente", false, json.getBoolean("is_recurring"))
        assertTrue("recurring_id deve estar presente", json.has("recurring_id"))
        assertEquals("uuid-series-abc", json.getString("recurring_id"))
        
        // NÃO deve ter recurring_type, recurring_count, recurring_until (só a primeira tem)
        assertFalse("recurring_type NÃO deve estar presente em subsequente", json.has("recurring_type"))
        assertFalse("recurring_count NÃO deve estar presente em subsequente", json.has("recurring_count"))
        assertFalse("recurring_until NÃO deve estar presente em subsequente", json.has("recurring_until"))
        
        println("✓ TESTE PASSOU: Ocorrência subsequente com recurring_id mas is_recurring=false")
        println("==========================================================\n")
    }
    
    /**
     * Teste: Primeira ocorrência de transação recorrente (isRecurring=true)
     */
    @Test
    fun testFirstRecurringOccurrenceEdit() {
        println("\n========== TESTE: Edição da PRIMEIRA ocorrência recorrente ==========")
        
        val transaction = TestTransactionRecurring(
            id = "tx-first-123",
            type = "expense",
            amount = 200.0,
            description = "Aluguel",
            date = "2026-03-01",
            status = "pending",
            categoryId = "cat-housing",
            accountId = "acc-main",
            isRecurring = true,   // PRIMEIRA: tem isRecurring=true
            recurringType = "monthly", // PRIMEIRA: tem tipo
            recurringCount = 12,       // PRIMEIRA: tem count
            recurringUntil = "2027-02-01", // PRIMEIRA: tem until
            recurringId = "uuid-series-abc"  // LINK para a série
        )
        
        val body = buildUpdateJsonRecurring(transaction)
        val json = JSONObject(body)
        
        println("JSON gerado para primeira ocorrência:\n$body")
        
        // Validações para PRIMEIRA ocorrência
        assertEquals("is_recurring deve ser true para primeira", true, json.getBoolean("is_recurring"))
        assertTrue("recurring_type deve estar presente", json.has("recurring_type"))
        assertTrue("recurring_count deve estar presente", json.has("recurring_count"))
        assertTrue("recurring_until deve estar presente", json.has("recurring_until"))
        assertTrue("recurring_id deve estar presente", json.has("recurring_id"))
        assertEquals("monthly", json.getString("recurring_type"))
        assertEquals(12, json.getInt("recurring_count"))
        
        println("✓ TESTE PASSOU: Primeira ocorrência com todos os campos de recorrência")
        println("====================================================================\n")
    }
    
    /**
     * Teste: Mudança de status de pendente para concluído em ocorrência subsequente
     */
    @Test
    fun testStatusChangeOnRecurringOccurrence() {
        println("\n========== TESTE: Mudança de status em ocorrência recorrente ==========")
        
        // Usuário marca como "recebido" (pending → completed)
        val transaction = TestTransactionRecurring(
            id = "tx-subsequent-456",
            type = "income",
            amount = 5000.0,
            description = "Salário - Março",
            date = "2026-03-20",
            status = "completed",  // MUDOU de pending para completed
            categoryId = "cat-salary",
            accountId = "acc-main",
            isRecurring = false,
            recurringType = null,
            recurringCount = null,
            recurringUntil = null,
            recurringId = "uuid-series-salary"
        )
        
        val body = buildUpdateJsonRecurring(transaction)
        val json = JSONObject(body)
        
        println("JSON gerado:\n$body")
        
        assertEquals("completed", json.getString("status"))
        assertTrue("recurring_id deve estar presente", json.has("recurring_id"))
        assertEquals("uuid-series-salary", json.getString("recurring_id"))
        
        println("✓ TESTE PASSOU: Mudança de status em ocorrência recorrente")
        println("====================================================================\n")
    }
    
    /**
     * Teste: Transferência que é ocorrência de série recorrente
     */
    @Test
    fun testRecurringTransferOccurrence() {
        println("\n========== TESTE: Transferência recorrente subsequente ==========")
        
        val transaction = TestTransactionRecurring(
            id = "tx-transfer-789",
            type = "transfer",
            amount = 500.0,
            description = "Poupanço mensal",
            date = "2026-03-20",
            status = "pending",
            fromAccountId = "acc-checking",
            toAccountId = "acc-savings",
            categoryId = "",
            accountId = null,
            isRecurring = false,
            recurringType = null,
            recurringCount = null,
            recurringUntil = null,
            recurringId = "uuid-series-transfer"
        )
        
        val body = buildUpdateJsonRecurring(transaction)
        val json = JSONObject(body)
        
        println("JSON gerado:\n$body")
        
        assertEquals("transfer", json.getString("type"))
        assertTrue("from_account_id deve estar presente", json.has("from_account_id"))
        assertTrue("to_account_id deve estar presente", json.has("to_account_id"))
        assertTrue("recurring_id deve estar presente", json.has("recurring_id"))
        assertEquals("pending", json.getString("status"))
        
        println("✓ TESTE PASSOU: Transferência recorrente subsequente")
        println("====================================================================\n")
    }
    
    // ========== Métodos auxiliares ==========
    
    private fun createTestTransaction(
        id: String,
        type: String,
        amount: Double,
        description: String,
        status: String?,
        categoryId: String? = null,
        accountId: String? = null,
        fromAccountId: String? = null,
        toAccountId: String? = null
    ): TestTransaction {
        return TestTransaction(
            id = id,
            type = type,
            amount = amount,
            description = description,
            date = "2026-03-20",
            status = status,
            categoryId = categoryId,
            accountId = accountId,
            fromAccountId = fromAccountId,
            toAccountId = toAccountId
        )
    }
    
    /**
     * Replica a lógica de construção do JSON do SupabaseService.updateTransaction()
     */
    private fun buildUpdateJson(tx: TestTransaction): String {
        return buildString {
            append("{")
            append("\"type\":\"${tx.type}\",")
            append("\"amount\":${tx.amount},")
            append("\"description\":\"${jsonEscape(tx.description)}\",")
            append("\"date\":\"${tx.date}\"")
            
            if (tx.categoryId != null) {
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
    
    /**
     * Classe auxiliar para representar uma transação de teste
     */
    data class TestTransaction(
        val id: String,
        val type: String,
        val amount: Double,
        val description: String,
        val date: String,
        val status: String?,
        val categoryId: String? = null,
        val accountId: String? = null,
        val fromAccountId: String? = null,
        val toAccountId: String? = null
    )
    
    /**
     * Classe auxiliar para transações recorrentes de teste
     */
    data class TestTransactionRecurring(
        val id: String,
        val type: String,
        val amount: Double,
        val description: String,
        val date: String,
        val status: String?,
        val categoryId: String?,
        val accountId: String?,
        val fromAccountId: String? = null,
        val toAccountId: String? = null,
        val isRecurring: Boolean = false,
        val recurringType: String? = null,
        val recurringCount: Int? = null,
        val recurringUntil: String? = null,
        val recurringId: String? = null
    )
    
    /**
     * Constrói o JSON de atualização para transações recorrentes.
     * Replica EXATAMENTE a lógica do SupabaseService.updateTransaction()
     */
    private fun buildUpdateJsonRecurring(tx: TestTransactionRecurring): String {
        return buildString {
            append("{")
            append("\"type\":\"${tx.type}\",")
            append("\"amount\":${tx.amount},")
            append("\"description\":\"${jsonEscape(tx.description)}\",")
            append("\"date\":\"${tx.date}\"")
            
            if (!tx.categoryId.isNullOrEmpty()) {
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
            
            // Campos de recorrência - LÓGICA EXATA DO SUPABASESERVICE
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
            
            // Recurring ID (preservar o link para a série)
            if (tx.recurringId != null) {
                append(",\"recurring_id\":\"${tx.recurringId}\"")
            }
            
            append("}")
        }
    }
}
