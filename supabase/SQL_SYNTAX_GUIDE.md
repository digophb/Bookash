# Guia de Sintaxe SQL para Supabase

## Regras Importantes

### 1. Strings SEMPRE com aspas simples
```sql
-- CORRETO
INSERT INTO table (name) VALUES ('Valor');
DEFAULT '09:00:00'

-- ERRADO
INSERT INTO table (name) VALUES (Valor);
DEFAULT 09:00:00
```

### 2. Nomes de políticas SEM aspas e SEM espaços
```sql
-- CORRETO
CREATE POLICY reminders_select_policy ON public.reminders ...

-- ERRADO
CREATE POLICY "Users can view their own reminders" ON public.reminders ...
CREATE POLICY Users can view their own reminders ON public.reminders ...
```

### 3. Nomes de tabelas/colunas SEM aspas (ou com aspas duplas se necessário)
```sql
-- CORRETO
CREATE TABLE public.reminders (...)
ALTER TABLE public.accounts ADD COLUMN include_in_balance BOOLEAN

-- EVITAR (só usar se tiver caracteres especiais)
CREATE TABLE "MyTable" (...)
```

### 4. Valores DEFAULT
```sql
-- CORRETO
DEFAULT true
DEFAULT false
DEFAULT NOW()
DEFAULT gen_random_uuid()
DEFAULT 'string_value'  -- strings precisam de aspas

-- ERRADO
DEFAULT 'true'   -- boolean não precisa de aspas
DEFAULT string_value  -- string sem aspas
```

### 5. CHECK constraints - evitar ou usar sintaxe simples
```sql
-- CORRETO (simples)
recurrence_type TEXT

-- EVITAR (pode causar erros de parsing)
recurrence_type TEXT CHECK (recurrence_type IN ('daily', 'weekly'))

-- Se precisar, validar no app
```

### 6. Comentários COMMENT
```sql
-- CORRETO
COMMENT ON TABLE public.reminders IS 'Descricao simples';
COMMENT ON COLUMN public.accounts.include_in_balance IS 'Descricao';

-- ERRADO (acentos podem causar problemas)
COMMENT ON TABLE public.reminders IS 'Descrição com acentos';
```

## Template de Migration

```sql
-- Migration: Nome da migration
-- Date: YYYY-MM-DD

-- Criar tabela
CREATE TABLE IF NOT EXISTS public.nome_tabela (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE NOT NULL,
    nome TEXT NOT NULL,
    valor DECIMAL(12,2),
    ativo BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Criar indices
CREATE INDEX IF NOT EXISTS idx_nome_tabela_user_id ON public.nome_tabela(user_id);

-- Habilitar RLS
ALTER TABLE public.nome_tabela ENABLE ROW LEVEL SECURITY;

-- Criar politicas RLS (nomes simples, sem aspas)
CREATE POLICY nome_tabela_select_policy ON public.nome_tabela FOR SELECT USING (auth.uid() = user_id);
CREATE POLICY nome_tabela_insert_policy ON public.nome_tabela FOR INSERT WITH CHECK (auth.uid() = user_id);
CREATE POLICY nome_tabela_update_policy ON public.nome_tabela FOR UPDATE USING (auth.uid() = user_id);
CREATE POLICY nome_tabela_delete_policy ON public.nome_tabela FOR DELETE USING (auth.uid() = user_id);
```

## Checklist antes de enviar SQL

- [ ] Strings com aspas simples: `'valor'`
- [ ] Nomes de políticas simples: `tabela_select_policy`
- [ ] Boolean sem aspas: `DEFAULT true`
- [ ] COMMENT sem acentos ou caracteres especiais
- [ ] Sem CHECK constraints complexas (validar no app)
- [ ] Testar sintaxe antes de enviar ao usuário
