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
import io.github.rosemoe.sora.lang.completion.SimpleCompletionItem
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import io.github.rosemoe.sora.text.Content
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.EditorSearcher
import io.github.rosemoe.sora.widget.completion.EditorAutoCompletion
import org.eclipse.tm4e.core.registry.IThemeSource
import org.eclipse.tm4e.core.registry.Registry
import java.io.BufferedReader
import java.io.InputStreamReader

class MainActivity : AppCompatActivity() {
    private lateinit var editor: CodeEditor
    private var currentUri: Uri? = null
    private lateinit var registry: Registry

    // 1. Lanzador para ABRIR archivos
    private val openFileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                currentUri = uri
                loadFileContent(uri)
            }
        }
    }

    // 2. Lanzador para GUARDAR archivos
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
        
        // Configurar la interfaz principal usando un FrameLayout
        val rootLayout = FrameLayout(this)
        editor = CodeEditor(this)
        rootLayout.addView(editor)
        
        // Configurar Botón de Abrir
        val btnOpen = FloatingActionButton(this).apply {
            setImageResource(android.R.drawable.ic_menu_search)
            contentDescription = "Abrir Archivo"
            setOnClickListener { openFile() }
        }
        val lpOpen = FrameLayout.LayoutParams(150, 150).apply {
            gravity = android.view.Gravity.BOTTOM or android.view.Gravity.END
            bottomMargin = 50
            rightMargin = 50
        }
        rootLayout.addView(btnOpen, lpOpen)

        // Configurar Botón de Guardar
        val btnSave = FloatingActionButton(this).apply {
            setImageResource(android.R.drawable.ic_menu_save)
            contentDescription = "Guardar Archivo"
            setOnClickListener { saveFile() }
        }
        val lpSave = FrameLayout.LayoutParams(150, 150).apply {
            gravity = android.view.Gravity.BOTTOM or android.view.Gravity.END
            bottomMargin = 220
            rightMargin = 50
        }
        rootLayout.addView(btnSave, lpSave)

        setContentView(rootLayout)

        // Configuración inicial del editor
        editor.setText("// Toca la lupa para abrir un archivo\n")
        editor.isWordwrap = true
        editor.setTextSize(16f)
        
        // Inicializar motor TextMate para colores
        initTextMate()
        
        // Inicializar Autocompletado
        setupAutoCompletion()
    }

    private fun initTextMate() {
        registry = Registry()
        // Aquí podríamos cargar el tema oscuro por defecto si quisiéramos
        // Por ahora cargamos el registro para que las gramáticas funcionen
    }

    private fun openFile() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*" // Permite abrir cualquier tipo de archivo de código
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
            val reader = BufferedReader(InputStreamReader(inputStream))
            val content = reader.readText()
            reader.close()
            
            editor.setText(content)
            
            // Detectar la extensión para aplicar colores
            val fileName = getFileName(uri)
            applySyntaxHighlighting(fileName)

        } catch (e: Exception) {
            Toast.makeText(this, "Error al abrir: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveFileContent(uri: Uri) {
        try {
            val outputStream = contentResolver.openOutputStream(uri)
            outputStream?.write(editor.text.toString().toByteArray())
            outputStream?.close()
            Toast.makeText(this, "Archivo guardado exitosamente", Toast.LENGTH_SHORT).show()
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
        val extension = fileName.substringAfterLast(".", "").lowercase()
        val scopeName = when (extension) {
            "kt" -> "source.kotlin"
            "java" -> "source.java"
            "xml" -> "text.xml"
            "html" -> "text.html.basic"
            "js" -> "source.js"
            "css" -> "source.css"
            else -> return // Si no es un lenguaje conocido, no aplica color
        }

        try {
            val grammarFile = if (extension == "kt") "kotlin" else if (extension == "js") "javascript" else extension
            val inputStream = assets.open("languages/$grammarFile.tmLanguage.json")
            val grammar = registry.loadGrammar(IThemeSource.fromInputStream(inputStream, "$grammarFile.json", null))
            
            val language = TextMateLanguage.create(scopeName, grammar)
            editor.setEditorLanguage(language)
        } catch (e: Exception) {
            // Si falla el color, no pasa nada, el archivo sigue abierto
            e.printStackTrace()
        }
    }

    private fun setupAutoCompletion() {
        editor.setAutoCompletionProvider(object : EditorAutoCompletion {
            override fun requestCompletion(content: Content, position: Int): List<SimpleCompletionItem> {
                // Autocompletado simple: busca palabras en el texto actual
                val text = content.toString()
                val words = text.split(Regex("[^a-zA-Z0-9_]")).filter { it.length > 2 }.distinct()
                val currentWord = getCurrentWord(content, position)
                return words.filter { it.startsWith(currentWord) && it != currentWord }
                    .map { SimpleCompletionItem(it) }
            }
        })
    }

    private fun getCurrentWord(content: Content, position: Int): String {
        val line = content.getLine(content.getLineForOffset(position))
        val col = position - content.getLineStartOffset(content.getLineForOffset(position))
        var start = col
        while (start > 0 && (line[start - 1].isLetterOrDigit() || line[start - 1] == '_')) {
            start--
        }
        return line.substring(start, col)
    }
}
