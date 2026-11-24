package com.tuapp.mieditor

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import io.github.rosemoe.sora.langs.textmate.registry.FileProviderRegistry
import io.github.rosemoe.sora.langs.textmate.registry.GrammarRegistry
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.langs.textmate.registry.model.ThemeModel
import io.github.rosemoe.sora.widget.CodeEditor
import org.eclipse.tm4e.core.registry.IThemeSource

class MainActivity : AppCompatActivity() {
    private lateinit var editor: CodeEditor
    private var currentUri: Uri? = null

    private val openFileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                currentUri = uri
                loadFileContent(uri)
            }
        }
    }

    private val saveFileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                currentUri = uri
                saveFileContent(uri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 1. Inicializar TextMate (solo una vez por proceso)
        initTextMate()

        val rootLayout = FrameLayout(this)
        editor = CodeEditor(this)
        rootLayout.addView(editor)

        // Botón Abrir
        val btnOpen = FloatingActionButton(this).apply {
            setImageResource(android.R.drawable.ic_menu_search)
            setOnClickListener { openFile() }
        }
        rootLayout.addView(btnOpen, FrameLayout.LayoutParams(150, 150).apply {
            gravity = android.view.Gravity.BOTTOM or android.view.Gravity.END
            bottomMargin = 50; rightMargin = 50
        })

        // Botón Guardar
        val btnSave = FloatingActionButton(this).apply {
            setImageResource(android.R.drawable.ic_menu_save)
            setOnClickListener { saveFile() }
        }
        rootLayout.addView(btnSave, FrameLayout.LayoutParams(150, 150).apply {
            gravity = android.view.Gravity.BOTTOM or android.view.Gravity.END
            bottomMargin = 220; rightMargin = 50
        })

        setContentView(rootLayout)

        editor.setText("// Toca la lupa para abrir un archivo\n")
        editor.isWordwrap = true
        editor.setTextSize(16f)
        
        // Aplicar el esquema de colores de TextMate
        editor.colorScheme = TextMateColorScheme.create(ThemeRegistry.getInstance())
    }

    private fun initTextMate() {
        // Registrar el resolvedor de archivos (assets)
        FileProviderRegistry.getInstance().addFileProvider(
            io.github.rosemoe.sora.langs.textmate.registry.provider.AssetsFileResolver(applicationContext.assets)
        )

        // Cargar y aplicar el tema
        val themeRegistry = ThemeRegistry.getInstance()
        val themeAssetsPath = "textmate/quietlight.json"
        try {
            val inputStream = FileProviderRegistry.getInstance().tryGetInputStream(themeAssetsPath)
            themeRegistry.loadTheme(
                ThemeModel(
                    IThemeSource.fromInputStream(inputStream, themeAssetsPath, null),
                    "quietlight"
                )
            )
            themeRegistry.setTheme("quietlight")
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Cargar las gramáticas (lenguajes)
        try {
            GrammarRegistry.getInstance().loadGrammars("textmate/languages.json")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun openFile() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }
        openFileLauncher.launch(intent)
    }

    private fun saveFile() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/plain"
            putExtra(Intent.EXTRA_TITLE, "nuevo_archivo.kt")
        }
        saveFileLauncher.launch(intent)
    }

    private fun loadFileContent(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val content = inputStream?.bufferedReader()?.use { it.readText() } ?: ""
            editor.setText(content)
            applySyntaxHighlighting(getFileName(uri))
        } catch (e: Exception) {
            Toast.makeText(this, "Error al abrir: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveFileContent(uri: Uri) {
        try {
            contentResolver.openOutputStream(uri)?.use { 
                it.write(editor.text.toString().toByteArray()) 
            }
            Toast.makeText(this, "Archivo guardado", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Error al guardar: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getFileName(uri: Uri): String {
        var name = "desconocido.txt"
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index != -1) name = cursor.getString(index)
            }
        }
        return name
    }

    private fun applySyntaxHighlighting(fileName: String) {
        val ext = fileName.substringAfterLast(".", "").lowercase()
        val scopeName = when (ext) {
            "kt" -> "source.kotlin"
            "java" -> "source.java"
            "xml" -> "text.xml"
            "html" -> "text.html.basic"
            "js" -> "source.js"
            "css" -> "source.css"
            else -> return
        }
        
        try {
            // El segundo parámetro 'true' habilita el autocompletado integrado de TextMate
            val language = TextMateLanguage.create(scopeName, true)
            editor.setEditorLanguage(language)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
