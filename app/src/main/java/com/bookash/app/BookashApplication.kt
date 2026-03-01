package com.bookash.app

import android.app.Application

/**
 * BookashApplication - Classe Application principal
 * 
 * Inicializa componentes globais como UserSession.
 */
class BookashApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Inicializar UserSession globalmente
        UserSession.init(this)
    }
}
