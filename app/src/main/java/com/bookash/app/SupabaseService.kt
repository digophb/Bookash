package com.bookash.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

object SupabaseService {
    
    private const val BASE_URL = "https://gqbxasjoxxslpaxjqfeg.supabase.co"
    private const val API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImdxYnhhc2pveHhzbHBheGpxZmVnIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzIwMzA4MTcsImV4cCI6MjA4NzYwNjgxN30.8arAkeAFEsUSTdyJpmafsp8T2yYgWEaZm9fCGnckaWs"
    
    // ============== CATEGORIES ==============
    
    suspend fun getCategories(type: String? = null): List<Category> = withContext(Dispatchers.IO) {
        try {
            val endpoint = if (type != null) {
                "$BASE_URL/rest/v1/categories?type=eq.$type&select=*"
            } else {
                "$BASE_URL/rest/v1/categories?select=*"
            }
            
            val conn = URL(endpoint).openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("apikey", API_KEY)
            conn.setRequestProperty("Authorization", "Bearer $API_KEY")
            
            if (conn.responseCode == 200) {
                val response = conn.inputStream.bufferedReader().readText()
                parseCategories(JSONArray(response))
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    suspend fun saveCategory(category: Category): Boolean = withContext(Dispatchers.IO) {
        try {
            val conn = URL("$BASE_URL/rest/v1/categories").openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("apikey", API_KEY)
            conn.setRequestProperty("Authorization", "Bearer $API_KEY")
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Prefer", "return=minimal")
            conn.doOutput = true
            
            val body = """{"name":"${category.name}","type":"${category.type}","color":"${category.color}","icon":"${category.icon}"}"""
            conn.outputStream.write(body.toByteArray())
            
            conn.responseCode in 200..299
        } catch (e: Exception) {
            false
        }
    }
    
    suspend fun deleteCategory(categoryId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val conn = URL("$BASE_URL/rest/v1/categories?id=eq.$categoryId").openConnection() as HttpURLConnection
            conn.requestMethod = "DELETE"
            conn.setRequestProperty("apikey", API_KEY)
            conn.setRequestProperty("Authorization", "Bearer $API_KEY")
            conn.setRequestProperty("Prefer", "return=minimal")
            
            conn.responseCode in 200..299
        } catch (e: Exception) {
            false
        }
    }
    
    suspend fun updateCategory(category: Category): Boolean = withContext(Dispatchers.IO) {
        try {
            val conn = URL("$BASE_URL/rest/v1/categories?id=eq.${category.id}").openConnection() as HttpURLConnection
            conn.requestMethod = "PATCH"
            conn.setRequestProperty("apikey", API_KEY)
            conn.setRequestProperty("Authorization", "Bearer $API_KEY")
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Prefer", "return=minimal")
            conn.doOutput = true
            
            val body = """{"name":"${category.name}","type":"${category.type}","color":"${category.color}","icon":"${category.icon}"}"""
            conn.outputStream.write(body.toByteArray())
            
            conn.responseCode in 200..299
        } catch (e: Exception) {
            false
        }
    }
    
    private fun parseCategories(jsonArray: JSONArray): List<Category> {
        val list = mutableListOf<Category>()
        for (i in 0 until jsonArray.length()) {
            val json = jsonArray.getJSONObject(i)
            list.add(Category(
                id = json.optString("id"),
                name = json.optString("name"),
                type = json.optString("type"),
                color = json.optString("color", "#357266"),
                icon = json.optString("icon", "category")
            ))
        }
        return list
    }
    
    // ============== ACCOUNTS ==============
    
    suspend fun getAccounts(archived: Boolean = false): List<Account> = withContext(Dispatchers.IO) {
        try {
            val endpoint = "$BASE_URL/rest/v1/accounts?is_archived=eq.$archived&select=*&order=created_at.desc"
            
            val conn = URL(endpoint).openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("apikey", API_KEY)
            conn.setRequestProperty("Authorization", "Bearer $API_KEY")
            
            if (conn.responseCode == 200) {
                val response = conn.inputStream.bufferedReader().readText()
                parseAccounts(JSONArray(response))
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    suspend fun saveAccount(account: Account): Boolean = withContext(Dispatchers.IO) {
        try {
            val conn = URL("$BASE_URL/rest/v1/accounts").openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("apikey", API_KEY)
            conn.setRequestProperty("Authorization", "Bearer $API_KEY")
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Prefer", "return=minimal")
            conn.doOutput = true
            
            val body = """{"name":"${account.name}","balance":${account.balance},"type":"${account.type}","icon":"${account.icon}"}"""
            conn.outputStream.write(body.toByteArray())
            
            conn.responseCode in 200..299
        } catch (e: Exception) {
            false
        }
    }
    
    suspend fun updateAccount(account: Account): Boolean = withContext(Dispatchers.IO) {
        try {
            val conn = URL("$BASE_URL/rest/v1/accounts?id=eq.${account.id}").openConnection() as HttpURLConnection
            conn.requestMethod = "PATCH"
            conn.setRequestProperty("apikey", API_KEY)
            conn.setRequestProperty("Authorization", "Bearer $API_KEY")
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Prefer", "return=minimal")
            conn.doOutput = true
            
            val body = """{"name":"${account.name}","balance":${account.balance},"type":"${account.type}","icon":"${account.icon}"}"""
            conn.outputStream.write(body.toByteArray())
            
            conn.responseCode in 200..299
        } catch (e: Exception) {
            false
        }
    }
    
    suspend fun archiveAccount(accountId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val conn = URL("$BASE_URL/rest/v1/accounts?id=eq.$accountId").openConnection() as HttpURLConnection
            conn.requestMethod = "PATCH"
            conn.setRequestProperty("apikey", API_KEY)
            conn.setRequestProperty("Authorization", "Bearer $API_KEY")
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Prefer", "return=minimal")
            conn.doOutput = true
            
            val body = """{"is_archived":true}"""
            conn.outputStream.write(body.toByteArray())
            
            conn.responseCode in 200..299
        } catch (e: Exception) {
            false
        }
    }
    
    suspend fun reactivateAccount(accountId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val conn = URL("$BASE_URL/rest/v1/accounts?id=eq.$accountId").openConnection() as HttpURLConnection
            conn.requestMethod = "PATCH"
            conn.setRequestProperty("apikey", API_KEY)
            conn.setRequestProperty("Authorization", "Bearer $API_KEY")
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Prefer", "return=minimal")
            conn.doOutput = true
            
            val body = """{"is_archived":false}"""
            conn.outputStream.write(body.toByteArray())
            
            conn.responseCode in 200..299
        } catch (e: Exception) {
            false
        }
    }
    
    suspend fun deleteAccount(accountId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val conn = URL("$BASE_URL/rest/v1/accounts?id=eq.$accountId").openConnection() as HttpURLConnection
            conn.requestMethod = "DELETE"
            conn.setRequestProperty("apikey", API_KEY)
            conn.setRequestProperty("Authorization", "Bearer $API_KEY")
            conn.setRequestProperty("Prefer", "return=minimal")
            
            conn.responseCode in 200..299
        } catch (e: Exception) {
            false
        }
    }
    
    private fun parseAccounts(jsonArray: JSONArray): List<Account> {
        val list = mutableListOf<Account>()
        for (i in 0 until jsonArray.length()) {
            val json = jsonArray.getJSONObject(i)
            list.add(Account(
                id = json.optString("id"),
                name = json.optString("name"),
                balance = json.optDouble("balance", 0.0),
                type = json.optString("type", "corrente"),
                icon = json.optString("icon", "wallet"),
                isArchived = json.optBoolean("is_archived", false)
            ))
        }
        return list
    }
    
    // ============== TRANSACTIONS ==============
    
    suspend fun getTransactions(userId: String, limit: Int = 50): List<Transaction> = withContext(Dispatchers.IO) {
        try {
            val endpoint = "$BASE_URL/rest/v1/transactions?user_id=eq.$userId&order=date.desc&limit=$limit&select=*"
            
            val conn = URL(endpoint).openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("apikey", API_KEY)
            conn.setRequestProperty("Authorization", "Bearer $API_KEY")
            
            if (conn.responseCode == 200) {
                val response = conn.inputStream.bufferedReader().readText()
                parseTransactions(JSONArray(response))
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    suspend fun saveTransaction(transaction: Transaction, token: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val conn = URL("$BASE_URL/rest/v1/transactions").openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("apikey", API_KEY)
            conn.setRequestProperty("Authorization", "Bearer $token")
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Prefer", "return=minimal")
            conn.doOutput = true
            
            val body = buildString {
                append("{")
                append("\"user_id\":\"${transaction.userId}\",")
                append("\"type\":\"${transaction.type}\",")
                append("\"amount\":${transaction.amount},")
                append("\"description\":\"${transaction.description}\",")
                append("\"category\":\"${transaction.category}\",")
                append("\"date\":\"${transaction.date}\",")
                append("\"status\":\"${transaction.status}\"")
                if (transaction.accountId.isNotEmpty()) {
                    append(",\"account_id\":\"${transaction.accountId}\"")
                }
                if (transaction.isRecurring) {
                    append(",\"is_recurring\":true")
                    append(",\"recurrence_period\":\"${transaction.recurrencePeriod}\"")
                    append(",\"recurrence_count\":${transaction.recurrenceCount}")
                }
                append("}")
            }
            conn.outputStream.write(body.toByteArray())
            
            conn.responseCode in 200..299
        } catch (e: Exception) {
            false
        }
    }
    
    private fun parseTransactions(jsonArray: JSONArray): List<Transaction> {
        val list = mutableListOf<Transaction>()
        for (i in 0 until jsonArray.length()) {
            val json = jsonArray.getJSONObject(i)
            val type = json.optString("type")
            list.add(Transaction(
                id = json.optString("id"),
                userId = json.optString("user_id"),
                description = json.optString("description"),
                category = json.optString("category"),
                amount = json.optDouble("amount", 0.0),
                type = type,
                date = json.optString("date"),
                status = json.optString("status", "paid"),
                accountId = json.optString("account_id", ""),
                iconRes = if (type == "income") R.drawable.ic_arrow_up else R.drawable.ic_arrow_down
            ))
        }
        return list
    }
}
