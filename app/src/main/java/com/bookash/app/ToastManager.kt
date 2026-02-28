package com.bookash.app

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.lifecycle.findViewTreeLifecycleOwner

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
        // Encontrar o container root
        val rootView = (context as? android.app.Activity)?.window?.decorView as? ViewGroup
            ?: return

        // Criar container de toasts se não existir
        var toastContainer = rootView.findViewWithTag<FrameLayout>("toast_container")
        if (toastContainer == null) {
            toastContainer = FrameLayout(context).apply {
                tag = "toast_container"
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                ).apply {
                    gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                    bottomMargin = dpToPx(context, 80f)
                    leftMargin = dpToPx(context, 16f)
                    rightMargin = dpToPx(context, 16f)
                }
            }
            rootView.addView(toastContainer)
        }

        // Limitar quantidade de toasts visíveis
        if (activeToasts.size >= MAX_TOASTS) {
            val oldestToast = activeToasts.firstOrNull()
            oldestToast?.let { dismissToast(it, immediate = true) }
        }

        // Criar o toast view
        val toastView = createToastView(context, message, type)

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

        // Animação de entrada
        val enterAnimation = AnimationUtils.loadAnimation(context, R.anim.toast_enter)
        toastView.startAnimation(enterAnimation)

        // Auto-dismiss
        handler.postDelayed({
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
        if (immediate) {
            removeToast(toastView)
        } else {
            val exitAnimation = AnimationUtils.loadAnimation(toastView.context, R.anim.toast_exit)
            exitAnimation.setAnimationListener(object : android.view.animation.Animation.AnimationListener {
                override fun onAnimationStart(animation: android.view.animation.Animation?) {}
                override fun onAnimationRepeat(animation: android.view.animation.Animation?) {}
                override fun onAnimationEnd(animation: android.view.animation.Animation?) {
                    removeToast(toastView)
                }
            })
            toastView.startAnimation(exitAnimation)
        }
    }

    /**
     * Remove o toast da view e da lista
     */
    private fun removeToast(toastView: View) {
        activeToasts.remove(toastView)
        (toastView.parent as? ViewGroup)?.removeView(toastView)
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
        activeToasts.toList().forEach { dismissToast(it, immediate = true) }
    }
}
