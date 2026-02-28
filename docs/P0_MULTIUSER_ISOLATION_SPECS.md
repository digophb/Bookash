# P0 - Isolamento Multiusuário — Especificações Técnicas

## Problema

**CRÍTICO:** Dados de todos os usuários estão compartilhados.
- Categorias, contas e transações são GLOBAIS
- Qualquer usuário vê dados de todos os outros

## Solução em 3 Etapas

### 1️⃣ TED — Backend (PRIMEIRO)

#### Migration SQL
Arquivo: `supabase/migrations/003_add_user_id_isolation.sql`

#### SupabaseService.kt — Alterações Necessárias

```kotlin
// ============================================
// CATEGORIES - Adicionar userId
// ============================================

// GET - Filtrar por userId
suspend fun getCategories(userId: String, type: String? = null): List<Category> {
    val endpoint = if (type != null) {
        "$BASE_URL/rest/v1/categories?user_id=eq.$userId&type=eq.$type&select=*"
    } else {
        "$BASE_URL/rest/v1/categories?user_id=eq.$userId&select=*"
    }
    // ... resto do código
}

// OU buscar categorias do usuário + categorias globais (user_id = null)
suspend fun getCategories(userId: String, type: String? = null): List<Category> {
    val endpoint = if (type != null) {
        "$BASE_URL/rest/v1/categories?or=(user_id.eq.$userId,user_id.is.null)&type=eq.$type&select=*"
    } else {
        "$BASE_URL/rest/v1/categories?or=(user_id.eq.$userId,user_id.is.null)&select=*"
    }
    // ... resto do código
}

// SAVE - Incluir userId
suspend fun saveCategory(category: Category, userId: String): Boolean {
    val body = """{
        "name":"${category.name}",
        "type":"${category.type}",
        "color":"${category.color}",
        "icon":"${category.icon}",
        "user_id":"$userId"
    }"""
    // ...
}

// UPDATE - Incluir userId
suspend fun updateCategory(category: Category, userId: String): Boolean {
    // Verificar se a categoria pertence ao usuário antes de atualizar
    // ...
}

// EXISTS - Verificar duplicatas apenas do usuário
suspend fun categoryExists(name: String, type: String, userId: String, excludeId: String? = null): Boolean {
    val endpoint = "$BASE_URL/rest/v1/categories?name=ilike.$name&type=eq.$type&user_id=eq.$userId&select=id"
    // ...
}

// ============================================
// ACCOUNTS - Adicionar userId
// ============================================

// GET - Filtrar por userId
suspend fun getAccounts(userId: String, archived: Boolean = false): List<Account> {
    val endpoint = "$BASE_URL/rest/v1/accounts?user_id=eq.$userId&is_archived=eq.$archived&select=*&order=created_at.desc"
    // ...
}

// SAVE - Incluir userId
suspend fun saveAccount(account: Account, userId: String): Boolean {
    val body = """{
        "name":"${account.name}",
        "balance":${account.balance},
        "type":"${account.type}",
        "icon":"${account.icon}",
        "user_id":"$userId"
    }"""
    // ...
}

// UPDATE, ARCHIVE, REACTIVATE, DELETE - Todos precisam verificar user_id
```

---

### 2️⃣ ACE — Integração (DEPOIS DO TED)

#### UserSession.kt (NOVO)

```kotlin
package com.bookash.app

import android.content.Context

object UserSession {
    private const val PREFS_NAME = "bookash_prefs"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_TOKEN = "access_token"

    private var cachedUserId: String? = null

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        cachedUserId = prefs.getString(KEY_USER_ID, null)
    }

    fun getUserId(): String {
        return cachedUserId ?: throw IllegalStateException("User not logged in")
    }

    fun isLoggedIn(): Boolean = cachedUserId != null

    fun setUserId(context: Context, userId: String) {
        cachedUserId = userId
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_USER_ID, userId)
            .apply()
    }

    fun clear(context: Context) {
        cachedUserId = null
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }
}
```

#### Activities — Atualizar Chamadas

**CategoriesActivity.kt:**
```kotlin
private fun loadCategories() {
    lifecycleScope.launch {
        val userId = UserSession.getUserId()
        val loadedCategories = SupabaseService.getCategories(userId)
        // ...
    }
}
```

**AccountsActivity.kt:**
```kotlin
private fun loadAccounts() {
    lifecycleScope.launch {
        val userId = UserSession.getUserId()
        val loadedAccounts = SupabaseService.getAccounts(userId, archived = false)
        // ...
    }
}
```

**MainActivity.kt:**
```kotlin
private fun updateTotals() {
    lifecycleScope.launch {
        val userId = UserSession.getUserId()
        val activeAccounts = SupabaseService.getAccounts(userId, archived = false)
        // ...
    }
}
```

**AddCategoryActivity.kt:**
```kotlin
private fun saveCategory() {
    lifecycleScope.launch {
        val userId = UserSession.getUserId()
        // ...
        val success = if (editingCategoryId != null) {
            SupabaseService.updateCategory(category, userId)
        } else {
            SupabaseService.saveCategory(category, userId)
        }
        // ...
    }
}
```

**AddAccountActivity.kt:**
```kotlin
private fun saveAccount() {
    lifecycleScope.launch {
        val userId = UserSession.getUserId()
        // ...
        val success = if (editingAccountId != null) {
            SupabaseService.updateAccount(account, userId)
        } else {
            SupabaseService.saveAccount(account, userId)
        }
        // ...
    }
}
```

---

### 3️⃣ NICK — Frontend (POR ÚLTIMO)

Nenhuma alteração visual necessária.
Apenas garantir que Activities passam userId correto.

---

## Checklist de Execução

### TED (Backend)
- [ ] Migration executada no Supabase
- [ ] `getCategories(userId)` implementado
- [ ] `saveCategory(category, userId)` implementado
- [ ] `updateCategory(category, userId)` implementado
- [ ] `categoryExists(name, type, userId)` implementado
- [ ] `getAccounts(userId, archived)` implementado
- [ ] `saveAccount(account, userId)` implementado
- [ ] `updateAccount(account, userId)` implementado
- [ ] `archiveAccount(accountId, userId)` implementado
- [ ] `deleteAccount(accountId, userId)` implementado

### ACE (Integração)
- [ ] `UserSession.kt` criado
- [ ] `LoginActivity` usa UserSession
- [ ] `CategoriesActivity` passa userId
- [ ] `AccountsActivity` passa userId
- [ ] `MainActivity` passa userId
- [ ] `AddCategoryActivity` passa userId
- [ ] `AddAccountActivity` passa userId

### Testes
- [ ] Login com usuário A
- [ ] Criar categoria/conta
- [ ] Login com usuário B
- [ ] Verificar que NÃO vê dados do usuário A
- [ ] Criar categoria/conta para B
- [ ] Verificar isolamento completo

---

## Prioridade

**P0 — CRÍTICO**

Este é um problema de segurança. Dados de usuários não podem ser compartilhados.

---

*Documento criado: 2026-02-28*
*Responsáveis: TED → ACE → NICK*
