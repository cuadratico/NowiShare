package com.cipmess

import android.util.Log
import androidx.core.graphics.toColorInt
import com.google.android.material.progressindicator.LinearProgressIndicator
import kotlin.math.log2
fun entropy(pass: String, porgress: LinearProgressIndicator){

    val mayusculas_l = ('A'..'Z').joinToString("").toList()
    val numeros_l = (0..9).joinToString ("").toList()
    val minusculas_l = ('a'..'z').joinToString("").toList()

    var pesos = charArrayOf()

    var simbolos = 0
    var minusculas = 0
    var mayusculas = 0
    var numeros = 0

    for (valor in pass) {

        if (!pesos.contains(valor)) {
            pesos = pesos.plus(valor)
        }

        if (minusculas_l.contains(valor)) {
            if (minusculas != 26 ) {minusculas += 26}
        }else if (mayusculas_l.contains(valor)) {
            if (mayusculas != 26) {mayusculas += 26}
        }else if (numeros_l.contains(valor)) {
            if (numeros != 9) {numeros += 10}
        }else {
            simbolos ++
        }
    }


    val final = pass.length * log2(((simbolos + mayusculas + minusculas + numeros) + (pesos.size * 2)).toDouble())

    Log.e("final", final.toString())

    pesos.fill('0')

    porgress.progress = final.toInt()

    when (final) {
        in 0.0..40.0 -> porgress.setIndicatorColor("#aa4040".toColorInt())
        in 40.0..60.0 -> porgress.setIndicatorColor("#c9a23e".toColorInt())
        else -> porgress.setIndicatorColor("#40aa47".toColorInt())
    }
}