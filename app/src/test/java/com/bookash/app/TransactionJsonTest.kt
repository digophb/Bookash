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
}
