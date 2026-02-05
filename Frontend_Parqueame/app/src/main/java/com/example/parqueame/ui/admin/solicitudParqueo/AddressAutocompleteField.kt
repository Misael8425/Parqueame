package com.example.parqueame.ui.admin.solicitudParqueo

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun AddressAutocompleteField(
    label: String = "Dirección",
    vm: AutocompleteAddressViewModel,
    value: String,
    onValueChange: (String) -> Unit,
    onAddressResolved: (address: String, lat: Double?, lng: Double?) -> Unit,
    maxResults: Int = 5,
    minCharsToSearch: Int = 3
) {
    val ctx = LocalContext.current
    val suggestions by vm.suggestions.collectAsState()
    val selectedAddress by vm.selectedAddress.collectAsState()
    val selectedLat by vm.selectedLat.collectAsState()
    val selectedLng by vm.selectedLng.collectAsState()

    var showPanel by remember { mutableStateOf(false) }

    // Cuando resolvemos el place → notificamos y cerramos panel
    LaunchedEffect(selectedAddress, selectedLat, selectedLng) {
        selectedAddress?.let { addr ->
            onAddressResolved(addr, selectedLat, selectedLng)
            showPanel = false
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Usa TU LabelledTextField para mantener tipografía/colores
        com.example.parqueame.ui.common_components.LabelledTextField(
            label = label,
            value = value,
            onValueChange = {
                onValueChange(it)
                if (it.length >= minCharsToSearch) {
                    vm.updateQuery(ctx, it)
                    showPanel = true
                } else {
                    showPanel = false
                }
            },
            placeholder = label,
            keyboardType = KeyboardType.Text,
            modifier = Modifier.fillMaxWidth()
        )

        // Panel de sugerencias SIEMPRE debajo (Card + LazyColumn)
        if (showPanel && suggestions.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(8.dp, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                val list = remember(suggestions, maxResults) {
                    suggestions.take(maxResults)
                }
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp)
                        .background(Color.White)
                        .padding(vertical = 4.dp)
                ) {
                    items(list) { s ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    // al seleccionar, resolvemos lat/lng
                                    vm.selectSuggestion(ctx, s)
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = s.primaryText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF111827)
                            )
                            s.secondaryText?.takeIf { it.isNotBlank() }?.let { secondary ->
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = secondary,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF6B7280)
                                )
                            }
                        }
                        Divider(color = Color(0xFFE5E7EB))
                    }
                }
            }
        }
    }
}

