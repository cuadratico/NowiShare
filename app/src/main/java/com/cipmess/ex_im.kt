package com.cipmess

import android.content.ContentValues
import android.content.Context
import android.content.SharedPreferences
import android.os.Environment
import android.provider.MediaStore
import org.json.JSONObject
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.IvParameterSpec

val algo_map = mapOf("AES" to "AES/GCM/NoPadding", "ChaCha20" to "ChaCha20-Poly1305")

fun export(context: Context, pref: SharedPreferences, password: String, file_name: String, message: String) {

    val salt = SecureRandom().generateSeed(16)

    val c = Cipher.getInstance(algo_map.getValue(pref.getString("algo", "AES").toString())).apply {
        init(Cipher.ENCRYPT_MODE, derive_key(pref, password, salt))
    }

    val mess = JSONObject().apply {
        put("message",
            Base64.getEncoder().withoutPadding().encodeToString(c.doFinal(message.toByteArray()))
        )
        put("iv", Base64.getEncoder().withoutPadding().encodeToString(c.iv))
    }

    val file = JSONObject().apply {
        put("algo", pref.getString("algo", "AES"))
        put("salt", Base64.getEncoder().withoutPadding().encodeToString(salt))
        put("mess_array", mess)
    }.toString()


    val file_values = ContentValues().apply {
        put(MediaStore.Files.FileColumns.DISPLAY_NAME, "$file_name.ns")
        put(MediaStore.Files.FileColumns.MIME_TYPE, "application/ns")
        put(MediaStore.Files.FileColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
    }

    context.contentResolver.openOutputStream(context.contentResolver.insert(MediaStore.Files.getContentUri("external"), file_values)!!)!!.write(file.toByteArray())

    salt.fill(0)
}

fun import(pref: SharedPreferences, jsonOb: JSONObject, pass: String): String {

    val mess_array = jsonOb.getJSONObject("mess_array")

    val algo = jsonOb.getString("algo")



    val c = Cipher.getInstance(algo_map.getValue(algo)).apply {
        init(
            Cipher.DECRYPT_MODE,
            derive_key(pref, pass, Base64.getDecoder().decode(jsonOb.getString("salt")), algo),
            if (algo == "AES") {
                GCMParameterSpec(128, Base64.getDecoder().decode(mess_array.getString("iv")))
            } else {
                IvParameterSpec(Base64.getDecoder().decode(mess_array.getString("iv")))
            }
        )
    }

    return String(c.doFinal(Base64.getDecoder().decode(mess_array.getString("message"))))

}