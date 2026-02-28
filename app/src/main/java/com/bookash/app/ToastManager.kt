package com.bookash.app

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

/**
 * ToastManager - Sistema de notificações toast customizado
 * 
 * Características:
 * - Toasts empilhados (máximo 3 simultâneos)
 * - Animações de entrada/saída
 * - Auto-dismiss após 4 segundos
 * - Cores por tipo (SUCCESS, WARNING, ERROR, INFO)
 */
object ToastManager {

    private const val TAG = "ToastManager"
    private const val MAX_TOASTS = 3
    private const val DEFAULT_DURATION: Long = 4000

    private val activeToasts = mutableListOf<View>()
    private val handler = Handler(Looper.getMainLooper())

    /**
     * Exibe um toast customizado
     */
    fun show(
        context: Context,
        message: String,
        type: ToastType = ToastType.SUCCESS,
        duration: Long = DEFAULT_DURATION
    ) {
        Log.d(TAG, "show() called - message: '$message', type: $type")

        // Garantir que está na main thread
        if (Looper.myLooper() == Looper.getMainLooper()) {
            showToastInternal(context, message, type, duration)
        } else {
            handler.post {
                showToastInternal(context, message, type, duration)
            }
        }
    }

    private fun showToastInternal(
        context: Context,
        message: String,
        type: ToastType,
        duration: Long
    ) {
        Log.d(TAG, "showToastInternal() - starting")

        // Encontrar o container root
        val activity = context as? android.app.Activity
        if (activity == null) {
            Log.e(TAG, "Context não é uma Activity - abortando")
            return
        }

        val rootView = activity.window?.decorView as? ViewGroup
        if (rootView == null) {
            Log.e(TAG, "Não foi possível obter decorView - abortando")
            return
        }

        Log.d(TAG, "rootView obtido: ${rootView::class.simpleName}")

        // Criar container de toasts se não existir
        var toastContainer = rootView.findViewWithTag<FrameLayout>("toast_container")
        if (toastContainer == null) {
            Log.d(TAG, "Criando novo toast_container")
            toastContainer = FrameLayout(context).apply {
                tag = "toast_container"
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                ).apply {
                    gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                    bottomMargin = dpToPx(context, 100f)
                    leftMargin = dpToPx(context, 16f)
                    rightMargin = dpToPx(context, 16f)
                }
            }
            rootView.addView(toastContainer)
            Log.d(TAG, "toast_container adicionado ao rootView")
        } else {
            Log.d(TAG, "toast_container já existe")
        }

        // Limitar quantidade de toasts visíveis
        if (activeToasts.size >= MAX_TOASTS) {
            Log.d(TAG, "Limite de toasts atingido, removendo o mais antigo")
            val oldestToast = activeToasts.firstOrNull()
            oldestToast?.let { dismissToast(it, immediate = true) }
        }

        // Criar o toast view
        val toastView = createToastView(context, message, type)
        Log.d(TAG, "Toast view criado")

        // Adicionar à lista de toasts ativos
        activeToasts.add(toastView)

        // Adicionar ao container
        val toastLayoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = dpToPx(context, 8f)
        }
        toastContainer.addView(toastView, toastLayoutParams)
        Log.d(TAG, "Toast adicionado ao container. Total ativos: ${activeToasts.size}")

        // Animação de entrada
        try {
            val enterAnimation = AnimationUtils.loadAnimation(context, R.anim.toast_enter)
            toastView.startAnimation(enterAnimation)
            Log.d(TAG, "Animação de entrada iniciada")
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao carregar animação de entrada", e)
        }

        // Auto-dismiss
        handler.postDelayed({
            Log.d(TAG, "Auto-dismiss triggered")
            dismissToast(toastView)
        }, duration)
    }

    /**
     * Exibe toast de sucesso (verde)
     */
    fun showSuccess(context: Context, message: String) {
        show(context, message, ToastType.SUCCESS)
    }

    /**
     * Exibe toast de aviso (laranja)
     */
    fun showWarning(context: Context, message: String) {
        show(context, message, ToastType.WARNING)
    }

    /**
     * Exibe toast de erro (vermelho)
     */
    fun showError(context: Context, message: String) {
        show(context, message, ToastType.ERROR)
    }

    /**
     * Exibe toast informativo (azul)
     */
    fun showInfo(context: Context, message: String) {
        show(context, message, ToastType.INFO)
    }

    /**
     * Dismiss um toast específico
     */
    private fun dismissToast(toastView: View, immediate: Boolean = false) {
        Log.d(TAG, "dismissToast() - immediate: $immediate")
        
        if (immediate) {
            removeToast(toastView)
        } else {
            try {
                val exitAnimation = AnimationUtils.loadAnimation(toastView.context, R.anim.toast_exit)
                exitAnimation.setAnimationListener(object : android.view.animation.Animation.AnimationListener {
                    override fun onAnimationStart(animation: android.view.animation.Animation?) {}
                    override fun onAnimationRepeat(animation: android.view.animation.Animation?) {}
                    override fun onAnimationEnd(animation: android.view.animation.Animation?) {
                        removeToast(toastView)
                    }
                })
                toastView.startAnimation(exitAnimation)
            } catch (e: Exception) {
                Log.e(TAG, "Erro na animação de saída", e)
                removeToast(toastView)
            }
        }
    }

    /**
     * Remove o toast da view e da lista
     */
    private fun removeToast(toastView: View) {
        Log.d(TAG, "removeToast() - removendo da lista")
        activeToasts.remove(toastView)
        
        val parent = toastView.parent as? ViewGroup
        if (parent != null) {
            parent.removeView(toastView)
            Log.d(TAG, "Toast removido do container. Total ativos: ${activeToasts.size}")
        } else {
            Log.w(TAG, "Toast não tinha parent")
        }
    }

    /**
     * Cria a view do toast
     */
    private fun createToastView(context: Context, message: String, type: ToastType): View {
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.custom_toast, null, false)

        // Configurar ícone e background
        val container = view.findViewById<LinearLayout>(R.id.toastContainer)
        val icon = view.findViewById<ImageView>(R.id.toastIcon)
        val messageView = view.findViewById<TextView>(R.id.toastMessage)

        messageView.text = message

        Log.d(TAG, "createToastView() - type: $type")

        when (type) {
            ToastType.SUCCESS -> {
                container.setBackgroundResource(R.drawable.bg_toast_success)
                icon.setImageResource(R.drawable.ic_toast_check)
            }
            ToastType.WARNING -> {
                container.setBackgroundResource(R.drawable.bg_toast_warning)
                icon.setImageResource(R.drawable.ic_toast_alert)
            }
            ToastType.ERROR -> {
                container.setBackgroundResource(R.drawable.bg_toast_error)
                icon.setImageResource(R.drawable.ic_toast_error)
            }
            ToastType.INFO -> {
                container.setBackgroundResource(R.drawable.bg_toast_info)
                icon.setImageResource(R.drawable.ic_toast_info)
            }
        }

        // Tornar clicável para dismiss manual
        view.setOnClickListener {
            Log.d(TAG, "Toast clicado - dismiss manual")
            dismissToast(view)
        }

        return view
    }

    /**
     * Converte dp para pixels
     */
    private fun dpToPx(context: Context, dp: Float): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }

    /**
     * Limpa todos os toasts ativos
     */
    fun clearAll() {
        Log.d(TAG, "clearAll() - limpando ${activeToasts.size} toasts")
        activeToasts.toList().forEach { dismissToast(it, immediate = true) }
    }
}
