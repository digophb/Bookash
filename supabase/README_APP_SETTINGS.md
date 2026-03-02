# Migration: app_settings table

## Instruções para criar a tabela no Supabase

### Passo a Passo:

1. Acesse o [Supabase Dashboard](https://supabase.com/dashboard)
2. Selecione o projeto Bookash
3. Vá em **SQL Editor** no menu lateral
4. Clique em **New Query**
5. Cole o conteúdo do arquivo `supabase/migrations/002_create_app_settings.sql`
6. Clique em **Run**

### Estrutura da Tabela:

```sql
app_settings
├── id                    UUID (PK)
├── user_id               UUID (FK → auth.users)
├── theme                 VARCHAR(20) -- 'light', 'dark', 'system'
├── language              VARCHAR(10) -- 'pt-BR', 'en-US'
├── notifications_enabled BOOLEAN
├── created_at            TIMESTAMPTZ
└── updated_at            TIMESTAMPTZ
```

### Verificar Criação:

Após executar, verifique se a tabela foi criada:

```sql
SELECT * FROM app_settings LIMIT 1;
```

Deve retornar um array vazio `[]` com sucesso.

---

**Status:** Aguardando execução no Supabase Dashboard

**Criado por:** ACE (Integração)
