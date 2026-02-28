# Supabase Migrations

Este diretório contém scripts SQL que devem ser executados manualmente no **Supabase Dashboard**.

## ⚠️ Importante

As migrations NÃO são aplicadas automaticamente. O desenvolvedor deve executá-las manualmente.

## Como Aplicar

1. Acesse: https://supabase.com/dashboard
2. Selecione o projeto **Bookash**
3. Vá em **SQL Editor**
4. Cole o conteúdo do arquivo `.sql`
5. Clique em **Run**

## Migrations

| Ordem | Arquivo | Descrição | Status |
|-------|---------|-----------|--------|
| 001 | `001_add_is_archived_to_accounts.sql` | Adiciona coluna `is_archived` à tabela accounts | ⏳ Pendente |
| 002 | `002_create_tags_table.sql` | Cria tabela `tags` com RLS para usuários | ⏳ Pendente |

## Checklist de Release

Antes de fazer release do app, verifique:

- [ ] Todas as migrations foram aplicadas
- [ ] Tabelas estão com schema correto
- [ ] Índices foram criados

## Boas Práticas

- ✅ Nomear com prefixo numérico: `001_`, `002_`, etc.
- ✅ Usar `IF NOT EXISTS` para idempotência
- ✅ Testar em ambiente de staging antes
- ❌ Nunca modificar migration já aplicada em produção
