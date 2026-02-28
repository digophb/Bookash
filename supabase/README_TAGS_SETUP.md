# Instruções para Criar a Tabela Tags no Supabase

## Método 1: SQL Editor (Recomendado)

1. Acesse o [Supabase Dashboard](https://supabase.com/dashboard)
2. Selecione o projeto `gqbxasjoxxslpaxjqfeg`
3. Vá em **SQL Editor** no menu lateral
4. Clique em **New Query**
5. Cole o conteúdo do arquivo `migrations/002_create_tags_table.sql`
6. Clique em **Run**

## Método 2: Via API (requer service_role key)

Se você tiver a chave `service_role`, pode executar via curl:

```bash
curl -X POST "https://gqbxasjoxxslpaxjqfeg.supabase.co/rest/v1/rpc/exec_sql" \
  -H "apikey: <SUA_SERVICE_ROLE_KEY>" \
  -H "Authorization: Bearer <SUA_SERVICE_ROLE_KEY>" \
  -H "Content-Type: application/json" \
  -d '{"query": "<SQL_DO_ARQUIVO>"}'
```

## Verificar Criação

Após executar, verifique se a tabela foi criada:

```bash
curl -X GET "https://gqbxasjoxxslpaxjqfeg.supabase.co/rest/v1/tags?select=*&limit=1" \
  -H "apikey: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

Deve retornar: `[]` (array vazio) com HTTP 200.

## Estrutura da Tabela

```sql
public.tags
├── id           UUID (PK, auto-generated)
├── user_id      UUID (FK → auth.users)
├── name         TEXT NOT NULL
├── color        TEXT DEFAULT '#357266'
├── created_at   TIMESTAMP
└── updated_at   TIMESTAMP

Constraints:
└── unique_user_tag_name: (user_id, name)
```

## Tabela de Relacionamento

```sql
public.transaction_tags
├── id             UUID (PK)
├── transaction_id UUID (FK → transactions)
├── tag_id         UUID (FK → tags)
└── created_at     TIMESTAMP

Constraints:
└── unique_transaction_tag: (transaction_id, tag_id)
```

---

**Status:** Aguardando execução no Supabase Dashboard
