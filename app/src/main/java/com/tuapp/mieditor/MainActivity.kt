package com.tuapp.mieditor

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import io.github.rosemoe.sora.widget.CodeEditor

class MainActivity : AppCompatActivity() {
    private lateinit var editor: CodeEditor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        editor = CodeEditor(this)
        setContentView(editor)

        // Texto de ejemplo inicial
        editor.setText("// Escribe tu codigo aqui\nfun main() {\n    println(\"Hola, mundo\")\n}")
        editor.isWordwrap = true
        editor.setTextSize(16f)
        
        // NOTA: El resaltado de sintaxis con TextMateLanguage requiere
        // la carga de archivos .json de gramáticas en assets. 
        // Por ahora, lo dejamos sin resaltado para asegurar la compilación.
    }

    override fun onDestroy() {
        super.onDestroy()
        editor.release()
    }
}
