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

        editor.setText("// Escribe tu codigo aqui\nfun main() {\n    println(\"Hola, mundo\")\n}")
        editor.isWordwrap = true
        editor.setTextSize(16f)
    }
}
