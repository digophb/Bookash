package com.bookash.app

import io.github.jan.tennert.supabase.SupabaseClient
import io.github.jan.tennert.supabase.SupabaseClientBuilder
import io.github.jan.tennert.supabase.postgrest.Postgrest
import io.github.jan.tennert.supabase.auth.Auth
import io.github.jan.tennert.supabase.auth.FlowType
import io.github.jan.tennert.supabase.auth.deepLinkOrNull
import io.ktor.client.engine.android.Android

/**
 * Singleton para gerenciar a conexão com o Supabase.
 * 
 * Centraliza a configuração do cliente Supabase SDK oficial,
 * fornecendo acesso ao Postgrest (database) e Auth (autenticação).
 */
object SupabaseClient {
    
    private const val SUPABASE_URL = "https://gqbxasjoxxslpaxjqfeg.supabase.co"
    private const val SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImdxYnhhc2pveHhzbHBheGpxZmVnIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzIwMzA4MTcsImV4cCI6MjA4NzYwNjgxN30.8arAkeAFEsUSTdyJpmafsp8T2yYgWEaZm9fCGnckaWs"
    
    val client: SupabaseClient by lazy {
        SupabaseClientBuilder(
            supabaseUrl = SUPABASE_URL,
            supabaseKey = SUPABASE_KEY
        ) {
            install(Postgrest)
            install(Auth) {
                flowType = FlowType.PKCE
                // Configuração para deep link se necessário no futuro
            }
            httpEngine = Android.create()
        }.build()
    }
    
    /**
     * Acesso direto ao Postgrest para operações CRUD.
     */
    val postgrest: Postgrest
        get() = client.postgrest
    
    /**
     * Acesso direto ao Auth para autenticação.
     */
    val auth: Auth
        get() = client.auth
}
