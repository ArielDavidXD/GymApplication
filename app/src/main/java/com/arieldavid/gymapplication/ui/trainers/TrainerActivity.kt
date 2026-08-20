package com.arieldavid.gymapplication.ui.trainers

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.arieldavid.gymapplication.local.database.DatabaseProvider
import com.arieldavid.gymapplication.local.entity.TrainerEntity
import com.arieldavid.gymapplication.ui.colores.Cards
import com.arieldavid.gymapplication.ui.colores.TextoPrincipal
import com.arieldavid.gymapplication.ui.colores.TextoSecundario
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext


@Composable
fun TrainerScreen(navController: NavController) {

    val context = LocalContext.current

    var entrenadores by remember {
        mutableStateOf<List<TrainerEntity>>(emptyList())
    }

    LaunchedEffect(Unit) {

        val db =
            DatabaseProvider.getDatabase(context)

        entrenadores =
            db.TrainersDAO().getAll()
        Log.d(
            "TRAINERS_UI",
            "ENTRENADORES ROOM: ${entrenadores.size}"
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        
        Text(
            text = "Nuestros entrenadores",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = TextoPrincipal
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = "Profesionales que te acompañarán durante tu entrenamiento.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextoSecundario
        )

        Spacer(modifier = Modifier.height(24.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            items(entrenadores) { entrenador ->

                TrainerCard(
                    nombre = entrenador.nombre ?: "",
                    telefono = entrenador.telefono ?: "",
                    descripcion = entrenador.descripcion ?: ""
                )
            }
        }
    }

}

@Composable
fun TrainerCard(
    nombre: String,
    telefono: String,
    descripcion: String
) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Cards
        ),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {

        Column(
            Modifier.padding(20.dp)
        ) {

            Text(
                "Entrenador Personal",
                color = TextoSecundario,
                style = MaterialTheme.typography.labelMedium
            )

            Spacer(Modifier.height(4.dp))

            Text(
                nombre,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextoPrincipal
            )

            Spacer(Modifier.height(16.dp))

            Text(
                "Teléfono",
                color = TextoSecundario,
                style = MaterialTheme.typography.labelMedium
            )

            Text(
                telefono,
                color = TextoPrincipal,
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(Modifier.height(16.dp))

            Text(
                "Especialidad",
                color = TextoSecundario,
                style = MaterialTheme.typography.labelMedium
            )

            Text(
                descripcion.ifBlank {
                    "Entrenador disponible para asesorarte."
                },
                color = TextoPrincipal,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}