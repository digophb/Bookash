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
| 000 | `000_create_categories_table.sql` | Cria tabela `categories` | ✅ Aplicada |
| 001 | `001_add_is_archived_to_accounts.sql` | Adiciona coluna `is_archived` à tabela accounts | ✅ Aplicada |
| 002 | `002_create_tags_table.sql` | Cria tabela `tags` com RLS para usuários | ✅ Aplicada |
| 002 | `002_create_app_settings.sql` | Cria tabela `app_settings` | ✅ Aplicada |
| 002 | `002_multi_user_isolation.sql` | Configura RLS para isolamento de usuários | ✅ Aplicada |
| 003 | `003_add_user_id_isolation.sql` | Adiciona user_id para isolamento | ✅ Aplicada |
| 003 | `003_add_user_id_to_categories.sql` | Adiciona user_id às categorias | ✅ Aplicada |
| 004 | `004_categories_default_personal.sql` | Categorias padrão + personalizadas | ✅ Aplicada |
| 005 | `005_create_default_account.sql` | Conta padrão 'Carteira' para novos usuários | ⏳ Pendente |
| 006 | `006_create_users_table.sql` | Tabela public.users + trigger automático | ⏳ Pendente |

## Checklist de Release

Antes de fazer release do app, verifique:

- [x] Todas as migrations foram aplicadas
- [x] Tabelas estão com schema correto
- [x] Índices foram criados

## Boas Práticas

- ✅ Nomear com prefixo numérico: `001_`, `002_`, etc.
- ✅ Usar `IF NOT EXISTS` para idempotência
- ✅ Testar em ambiente de staging antes
- ❌ Nunca modificar migration já aplicada em produção
