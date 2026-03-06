package com.bookash.app

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.Log
import android.widget.ImageView
import com.caverock.androidsvg.SVG
import com.caverock.androidsvg.SVGParseException
import java.io.IOException

/**
 * Utilitario para carregar icones SVG dinamicamente usando AndroidSVG.
 * Permite usar SVGs originais de bancos e categorias sem conversao manual.
 */
object SvgLoader {
    private const val TAG = "SvgLoader"

    /**
     * Carrega um SVG da pasta assets e define como drawable do ImageView.
     * @param context Contexto da aplicacao
     * @param imageView ImageView onde o SVG sera exibido
     * @param assetPath Caminho do SVG na pasta assets (ex: "banks/nubank.svg")
     * @param fallbackDrawable Drawable de fallback se o SVG falhar
     */
    fun loadSvg(
        context: Context,
        imageView: ImageView,
        assetPath: String,
        fallbackDrawable: Int? = null
    ) {
        try {
            val inputStream = context.assets.open(assetPath)
            val svg = SVG.getFromInputStream(inputStream)
            
            // Configurar dimensões
            val width = imageView.layoutParams.width.takeIf { it > 0 } ?: 96
            val height = imageView.layoutParams.height.takeIf { it > 0 } ?: 96
            
            svg.documentWidth = width.toFloat()
            svg.documentHeight = height.toFloat()
            
            // Criar drawable e definir no ImageView
            val drawable = svg.createPictureDrawable()
            imageView.setLayerType(ImageView.LAYER_TYPE_SOFTWARE, null)
            imageView.setImageDrawable(drawable)
            
        } catch (e: IOException) {
            Log.e(TAG, "Erro ao carregar SVG: $assetPath", e)
            fallbackDrawable?.let { imageView.setImageResource(it) }
        } catch (e: SVGParseException) {
            Log.e(TAG, "Erro ao fazer parse do SVG: $assetPath", e)
            fallbackDrawable?.let { imageView.setImageResource(it) }
        }
    }

    /**
     * Carrega um SVG da pasta assets e retorna como Drawable.
     * @param context Contexto da aplicacao
     * @param assetPath Caminho do SVG na pasta assets
     * @param width Largura desejada em pixels
     * @param height Altura desejada em pixels
     * @return Drawable do SVG ou null se falhar
     */
    fun getSvgDrawable(
        context: Context,
        assetPath: String,
        width: Int = 96,
        height: Int = 96
    ): Drawable? {
        return try {
            val inputStream = context.assets.open(assetPath)
            val svg = SVG.getFromInputStream(inputStream)
            
            svg.documentWidth = width.toFloat()
            svg.documentHeight = height.toFloat()
            
            svg.createPictureDrawable()
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao carregar SVG: $assetPath", e)
            null
        }
    }

    /**
     * Mapeia ID do banco para caminho do SVG.
     */
    fun getBankSvgPath(bankId: String): String {
        val svgMap = mapOf(
            "nubank" to "banks/nubank.svg",
            "itau" to "banks/itau.svg",
            "bradesco" to "banks/bradesco.svg",
            "bb" to "banks/bb.svg",
            "caixa" to "banks/caixa.svg",
            "santander" to "banks/santander.svg",
            "inter" to "banks/inter.svg",
            "c6" to "banks/c6.svg",
            "neon" to "banks/neon.svg",
            "picpay" to "banks/picpay.svg",
            "mercadopago" to "banks/mercadopago.svg",
            "original" to "banks/original.svg",
            "bmg" to "banks/bmg.svg",
            "safra" to "banks/safra.svg",
            "btg" to "banks/btg.svg",
            "next" to "banks/itau.svg", // Fallback para Itau
            "digio" to "banks/digio.svg",
            "pagseguro" to "banks/pagseguro.svg",
            "banrisul" to "banks/banrisul.svg",
            "votorantim" to "banks/votorantim.svg",
            "nordeste" to "banks/nordeste.svg"
        )
        return svgMap[bankId] ?: "banks/wallet.svg"
    }

    /**
     * Mapeia icone de categoria para caminho do SVG.
     */
    fun getCategorySvgPath(icon: String): String {
        val iconMap = mapOf(
            "restaurant" to "categories/restaurant.svg",
            "directions_car" to "categories/transport.svg",
            "attach_money" to "categories/salary.svg",
            "sports_esports" to "categories/lazer.svg",
            "local_hospital" to "categories/health.svg",
            "school" to "categories/education.svg",
            "home" to "categories/home.svg",
            "more_horiz" to "categories/other.svg"
        )
        return iconMap[icon] ?: "categories/other.svg"
    }
}
