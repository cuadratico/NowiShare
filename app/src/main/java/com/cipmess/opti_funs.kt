package com.cipmess

import android.app.Activity
import android.app.Dialog
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.airbnb.lottie.LottieAnimationView
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import androidx.core.graphics.drawable.toDrawable


fun dialog (context: Activity, layout: Int, extra: (dialog: Dialog) -> Unit): Pair<Dialog, View> {
    val view = LayoutInflater.from(context).inflate(layout, null)

    return Pair(Dialog(context).apply {
        setContentView(view)
        window!!.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        window!!.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        extra(this)
    }, view)
}

fun load (context: Activity, info_text: String): Dialog {
    val (dialog, load_view) = dialog(context, R.layout.load) {
        it.setCancelable(false)
    }

    val info = load_view.findViewById<TextView>(R.id.info)

    info.text = info_text

    dialog.show()

    return dialog
}

fun biometric (context: FragmentActivity, toast_text: String, auth: () -> Unit) {

    BiometricPrompt(context, ContextCompat.getMainExecutor(context), object: BiometricPrompt.AuthenticationCallback (){
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            super.onAuthenticationSucceeded(result)
            auth()
        }

        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            super.onAuthenticationError(errorCode, errString)
            Toast.makeText(context, toast_text, Toast.LENGTH_SHORT).show()
        }
    }).authenticate(
        BiometricPrompt.PromptInfo.Builder().apply {
            setTitle("Authenticate yourself")
            setAllowedAuthenticators(BiometricManager.Authenticators.DEVICE_CREDENTIAL or BiometricManager.Authenticators.BIOMETRIC_STRONG)
        }.build()
    )

}

fun derive_key (pref: SharedPreferences, password: String, salt: ByteArray, algo: String = pref.getString("algo", "AES").toString()): SecretKey {

    return SecretKeySpec(
        SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(PBEKeySpec(password.toCharArray(), salt, 600_000, 256)).encoded,
        algo
    )
}