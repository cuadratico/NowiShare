package com.cipmess

import android.annotation.SuppressLint
import android.app.ComponentCaller
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.security.GeneralSecurityException

class MainActivity : AppCompatActivity() {

    enum class state { view, edit }
    private var states = state.edit
    private lateinit var pref: SharedPreferences
    private lateinit var load_dialog: Dialog
    private lateinit var text_read: TextView
    private lateinit var delete: ConstraintLayout

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)


        val input_text = findViewById<EditText>(R.id.input_text)
        val input_visi = findViewById<TextInputLayout>(R.id.input_visi)
        text_read = findViewById(R.id.text_read)
        val text_scroll = findViewById<ScrollView>(R.id.text_scroll)

        val view_edit = findViewById<ConstraintLayout>(R.id.edit_view)
        val icon_modi = findViewById<ShapeableImageView>(R.id.icon_info)
        delete = findViewById(R.id.delete)

        val import = findViewById<ShapeableImageView>(R.id.im)
        val export = findViewById<ShapeableImageView>(R.id.ex)

        delete.visibility = View.GONE
        text_scroll.visibility = View.GONE

        pref = EncryptedSharedPreferences.create(this, "s_f",
            MasterKey.Builder(this).apply {
                setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            }.build()
            , EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM)

        input_text.addTextChangedListener {dato ->
            if (dato!!.isEmpty()) {
                delete.visibility = View.GONE
            } else {
                delete.visibility = View.VISIBLE
            }
        }

        fun clear () {
            if (states == state.edit) {
                input_text.setText("")
            } else {
                text_read.text = ""
            }
            delete.visibility = View.GONE
        }

        fun change_state () {
            if (states == state.edit) {
                states = state.view
                icon_modi.setImageResource(R.drawable.edit)

                text_scroll.visibility = View.VISIBLE

                text_read.text = input_text.text.toString()

                input_text.setText("")
                input_visi.visibility = View.GONE

                if (input_text.text.isNotEmpty() || text_read.text.isNotEmpty()) {
                    delete.visibility = View.VISIBLE
                } else {
                    delete.visibility = View.GONE
                }

            } else {
                biometric(this, "Authenticate to edit the file") {
                    states = state.edit
                    icon_modi.setImageResource(R.drawable.read)

                    input_visi.visibility = View.VISIBLE

                    input_text.setText(text_read.text.toString())

                    text_read.text = ""

                    text_scroll.visibility = View.GONE

                }

            }
        }

        delete.setOnClickListener {
            MaterialAlertDialogBuilder(this).apply {
                setMessage("Do you want to delete all the text?")
                setPositiveButton("Delete") {_, _ -> clear() }
                setNegativeButton("No") {_, _ ->}
            }.show()
        }


        view_edit.setOnClickListener {
            change_state()
        }

        export.setOnClickListener {
            val (dialog_export, view_dialog) = dialog(this, R.layout.export_file) {}

            val input_pass = view_dialog.findViewById<EditText>(R.id.input_pass)
            val progress = view_dialog.findViewById<LinearProgressIndicator>(R.id.progress)
            val file_name = view_dialog.findViewById<EditText>(R.id.input_name_f)

            val conf_key = view_dialog.findViewById<ConstraintLayout>(R.id.key_conf)
            val algo_expre = view_dialog.findViewById<TextView>(R.id.algo_expre)
            val export_button = view_dialog.findViewById<ConstraintLayout>(R.id.export_butt)

            algo_expre.text = "${pref.getString("algo", "AES")}-encrypted"

            input_pass.addTextChangedListener {
                if (it!!.isNotEmpty()) {
                    entropy(it.toString(), progress)
                }
            }

            conf_key.setOnClickListener {
                val algo_list = arrayOf("AES", "ChaCha20")

                MaterialAlertDialogBuilder(this).apply {
                    setTitle("Select the encryption algorithm")
                    setItems(algo_list, object : DialogInterface.OnClickListener {
                        override fun onClick(p0: DialogInterface?, posi: Int) {
                            pref.edit().putString("algo", algo_list[posi]).commit()
                            algo_expre.text = "${pref.getString("algo", "AES")}-encrypted"
                        }
                    })
                }.show()
            }

            export_button.setOnClickListener {
                if (input_pass.text.isNotEmpty() && file_name.text.isNotEmpty()) {
                    biometric(this, "Decrypt the file") {
                        load_dialog = load(this@MainActivity, "Encrypting the file...")

                        lifecycleScope.launch (Dispatchers.IO){
                            try {
                                export(this@MainActivity, pref, input_pass.text.toString(), file_name.text.toString(), if (states == state.edit) { input_text.text.toString() } else { text_read.text.toString() })

                                withContext(Dispatchers.Main) {
                                    clear()
                                    dialog_export.dismiss()
                                    load_dialog.dismiss()
                                }

                            } catch (e: Exception) {
                                Log.e("Error encrypting the file", e.toString())
                                withContext(Dispatchers.Main) {
                                    input_pass.setText("")
                                    Toast.makeText(this@MainActivity, "Error encrypting the file", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }

                    }
                }
            }

            if (input_text.text.isNotEmpty() || text_read.text.isNotEmpty()) {
                dialog_export.show()
            } else {
                Toast.makeText(this, "There is no information to export", Toast.LENGTH_SHORT).show()
            }
        }

        fun s_import () {
            states = state.edit
            change_state()
            startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
            }, 1001)
        }
        import.setOnClickListener {
            if (input_text.text.isNotEmpty() || text_read.text.isNotEmpty()) {
                MaterialAlertDialogBuilder(this).apply {
                    setMessage("The file will overwrite all the information you have written")
                    setPositiveButton("No problem") { _, _ ->
                        s_import()
                    }
                    setNegativeButton("One moment") { _, _ -> }
                }.show()
            } else {
                s_import()
            }
        }

        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?, caller: ComponentCaller) {
        super.onActivityResult(requestCode, resultCode, data, caller)

        if (resultCode == -1) {
            val uri = data?.data
            val query = contentResolver.query(uri!!, null, null, null, null)!!

            if (query.moveToFirst()) {
                val position = query.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val name = query.getString(position)

                if (name!!.matches(Regex(".*.ns.*"))) {

                    val json = JSONObject(contentResolver.openInputStream(uri)?.bufferedReader()?.readText())

                    val (import_dialog, import_view) = dialog(this, R.layout.import_file) {}

                    val input_pass = import_view.findViewById<EditText>(R.id.input_pass)
                    val progress = import_view.findViewById<LinearProgressIndicator>(R.id.progress)
                    val import_butt = import_view.findViewById<ConstraintLayout>(R.id.import_butt)

                    input_pass.addTextChangedListener{
                        if (it!!.isNotEmpty()) {
                            entropy(it.toString(), progress)
                        }
                    }


                    import_butt.setOnClickListener {

                        if (json.has("algo") && json.has("salt") && json.has("mess_array")) {

                            biometric(this@MainActivity, "You could not be authenticated") {
                                load_dialog = load(this@MainActivity, "Decrypting the file...")

                                lifecycleScope.launch (Dispatchers.IO) {
                                    try {
                                        val message = import(pref, json, input_pass.text.toString())

                                        withContext(Dispatchers.Main) {
                                            text_read.text = message
                                            delete.visibility = View.VISIBLE
                                            import_dialog.dismiss()
                                        }

                                    }catch (sec_e: GeneralSecurityException) {
                                        Log.e("Decryption error", sec_e.toString())
                                        withContext(Dispatchers.Main) {
                                            input_pass.setText("")
                                            Toast.makeText(applicationContext, "Decryption error", Toast.LENGTH_SHORT).show()
                                        }

                                    }finally {
                                        withContext(Dispatchers.Main) {
                                            load_dialog.dismiss()
                                        }
                                    }
                                }
                            }

                        }else {
                            input_pass.setText("")
                            Toast.makeText(this, "The structure is not correct", Toast.LENGTH_SHORT).show()
                        }

                    }

                    import_dialog.show()

                } else {
                    Toast.makeText(this@MainActivity, "The file name does not match", Toast.LENGTH_SHORT).show()
                }
            }
        }

    }
}