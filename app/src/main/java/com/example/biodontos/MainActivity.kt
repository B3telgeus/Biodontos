package com.example.biodontos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.example.biodontos.core.designsystem.DentalTheme
import com.example.biodontos.presentation.navigation.BiodontosNavGraph

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DentalTheme {
                // Creamos el controlador de rutas
                val navController = rememberNavController()
                // Iniciamos la navegación de tu app
                BiodontosNavGraph(navController = navController)
            }
        }
    }
}