package com.cristiancogollo.biblion

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.cristiancogollo.biblion.ui.theme.BiblionBluePrimary
import com.cristiancogollo.biblion.ui.theme.BiblionGoldPrimary
import com.cristiancogollo.biblion.ui.theme.BiblionGoldSoft

@Composable
fun AboutBiblionDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = BiblionGoldPrimary)
                    Text(
                        "Sobre nosotros",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    "Biblion es una app cristiana enfocada en facilitar la lectura bíblica, la búsqueda de versículos y el estudio personal con herramientas simples y claras."
                )

                Text("Equipo:", fontWeight = FontWeight.SemiBold)

                Text("Cristian Felipe Cogollo Rodríguez — Co-fundador & Lead Developer")
                Text("Luis Eduardo Jaimes Hernandez — Co-fundador & Lead Developer")
                Text("Anderson Geovanny Duarte Largo — Co-fundador & Estrategia / Alianzas")
                Text(
                    buildAnnotatedString {
                        withStyle(
                            style = SpanStyle(
                                fontWeight = FontWeight.Bold,
                                color = BiblionBluePrimary
                            )
                        ) {
                            append("Estado Actual: ")
                        }

                        append("Biblion se encuentra en desarrollo activo. Seguimos mejorando funciones, diseño y experiencia para la comunidad.")
                    }
                )

                Text(
                    buildAnnotatedString {
                        withStyle(
                            style = SpanStyle(
                                fontWeight = FontWeight.Bold,
                                color = BiblionGoldPrimary
                            )
                        ) {
                            append("IMPORTANTE: ")
                        }
                        withStyle(
                            style = SpanStyle(
                                color = BiblionGoldSoft
                            )
                        ) {
                            append("La app se encuentra en desarrollo y puede seguir cambiando con nuevas actualizaciones.")
                        }
                    }
                )

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("CERRAR", color = BiblionGoldPrimary)
                }
            }
        }
    }
}

@Composable
fun BiblionComingSoonDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logobiblionsinletras),
                    contentDescription = "Logo de Biblion",
                    modifier = Modifier.size(72.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Muy pronto estará disponible esta opción ✨",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    "Gracias por caminar con Biblion. ¡Lo mejor para tu tiempo con Dios está en camino!",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("ENTENDIDO", color = BiblionGoldPrimary)
                }
            }
        }
    }
}
