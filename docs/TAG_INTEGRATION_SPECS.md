# Tag Data Model - Referência para Implementação

## Data Class (adicionar em Transaction.kt ou criar Tag.kt)

```kotlin
package com.bookash.app

data class Tag(
    val id: String = "",
    val userId: String = "",
    val name: String,
    val color: String = "#357266"
)
```

## Endpoints REST Supabase

### GET - Listar Tags
```
GET /rest/v1/tags?select=*&order=created_at.desc
```

### POST - Criar Tag
```
POST /rest/v1/tags
Body: {"user_id":"<userId>","name":"<name>","color":"#357266"}
Header: Prefer: return=minimal
```

### PATCH - Atualizar Tag
```
PATCH /rest/v1/tags?id=eq.<tagId>
Body: {"name":"<name>","color":"<color>"}
Header: Prefer: return=minimal
```

### DELETE - Deletar Tag
```
DELETE /rest/v1/tags?id=eq.<tagId>
```

## Métodos para SupabaseService.kt

```kotlin
// GET
suspend fun getTags(userId: String): List<Tag>

// CREATE
suspend fun saveTag(tag: Tag): Boolean

// UPDATE
suspend fun updateTag(tag: Tag): Boolean

// DELETE
suspend fun deleteTag(tagId: String): Boolean
```

## Cores Disponíveis (sugerido para UI)

```kotlin
val tagColors = listOf(
    "#357266", // Teal (primary)
    "#2E7D6A", // Green (success)
    "#B85450", // Red (error)
    "#F57C00", // Orange (warning)
    "#4A7C8C", // Blue (info)
    "#65532F", // Bronze
    "#8B7A4A", // Gold
    "#7B68EE", // Purple
    "#20B2AA", // Light teal
    "#DC143C", // Crimson
    "#4682B4", // Steel blue
    "#2E8B57"  // Sea green
)
```

---

**Próximo passo:** TED implementa backend → NICK constrói UI
