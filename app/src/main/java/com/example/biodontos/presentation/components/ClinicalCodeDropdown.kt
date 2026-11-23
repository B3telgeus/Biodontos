package com.example.biodontos.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.biodontos.core.designsystem.BiodontosDark
import com.example.biodontos.core.designsystem.BiodontosErrorRed
import com.example.biodontos.core.designsystem.BiodontosGreyText
import com.example.biodontos.core.designsystem.Zafiro
import com.example.biodontos.core.designsystem.biodontosTextFieldColors
import com.example.biodontos.domain.model.Cie10Code
import com.example.biodontos.domain.model.CupsCode
import com.example.biodontos.domain.model.DentalClinicalCodes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> SearchableCodeDropdown(
    selectedItem: T?,
    onSelect: (T?) -> Unit,
    searchItems: (String) -> List<T>,
    itemKey: (T) -> String,
    itemCode: (T) -> String,
    itemDescription: (T) -> String,
    itemDisplayLabel: (T) -> String,
    itemSecondaryText: ((T) -> String)? = null,
    label: String,
    placeholder: String,
    clearContentDescription: String,
    noMatchesText: String,
    testTag: String,
    optionTestTagPrefix: String = "${testTag}_option_",
    isError: Boolean = false,
    supportingText: String? = null,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf(selectedItem?.let(itemDisplayLabel).orEmpty()) }

    LaunchedEffect(selectedItem) {
        query = selectedItem?.let(itemDisplayLabel).orEmpty()
    }

    val filtered = remember(query, selectedItem) {
        val selectedLabel = selectedItem?.let(itemDisplayLabel).orEmpty()
        val searchTerm = if (selectedItem != null && query == selectedLabel) "" else query
        searchItems(searchTerm)
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { input ->
                query = input
                expanded = true
                val selectedLabel = selectedItem?.let(itemDisplayLabel).orEmpty()
                if (selectedItem != null && input != selectedLabel) {
                    onSelect(null)
                }
            },
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            singleLine = true,
            isError = isError,
            supportingText = supportingText?.let { { Text(it, color = BiodontosErrorRed) } },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = {
                        query = ""
                        onSelect(null)
                        expanded = true
                    }) {
                        Icon(Icons.Default.Close, contentDescription = clearContentDescription)
                    }
                } else {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                }
            },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryEditable)
                .fillMaxWidth()
                .testTag(testTag),
            colors = biodontosTextFieldColors(),
            shape = RoundedCornerShape(12.dp)
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.heightIn(max = 260.dp)
        ) {
            if (filtered.isEmpty()) {
                DropdownMenuItem(
                    text = { Text(noMatchesText, fontSize = 12.sp, color = BiodontosGreyText) },
                    onClick = { expanded = false },
                    enabled = false
                )
            } else {
                filtered.forEach { item ->
                    DropdownMenuItem(
                        text = {
                            Column(modifier = Modifier.padding(vertical = 2.dp)) {
                                Text(
                                    text = itemCode(item),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Zafiro
                                )
                                Text(
                                    text = itemDescription(item),
                                    fontSize = 13.sp,
                                    color = BiodontosDark
                                )
                                val secondary = itemSecondaryText?.invoke(item)
                                if (!secondary.isNullOrBlank()) {
                                    Text(
                                        text = secondary,
                                        fontSize = 11.sp,
                                        color = BiodontosGreyText
                                    )
                                }
                            }
                        },
                        onClick = {
                            query = itemDisplayLabel(item)
                            onSelect(item)
                            expanded = false
                        },
                        modifier = Modifier.testTag("$optionTestTagPrefix${itemKey(item)}")
                    )
                }
            }
        }
    }
}

@Composable
fun Cie10CodeDropdown(
    selectedCode: Cie10Code?,
    onSelect: (Cie10Code?) -> Unit,
    isError: Boolean = false,
    supportingText: String? = null,
    testTag: String,
    optionTestTagPrefix: String = "${testTag}_option_",
    modifier: Modifier = Modifier
) {
    SearchableCodeDropdown(
        selectedItem = selectedCode,
        onSelect = onSelect,
        searchItems = DentalClinicalCodes::searchCie10,
        itemKey = { it.code },
        itemCode = { it.code },
        itemDescription = { it.description },
        itemDisplayLabel = { it.displayLabel },
        label = "Diagnóstico / Tratamiento CIE-10 *",
        placeholder = "Buscar código o diagnóstico (Ej: K02.1 Caries)",
        clearContentDescription = "Limpiar CIE-10",
        noMatchesText = "Sin coincidencias CIE-10",
        testTag = testTag,
        optionTestTagPrefix = optionTestTagPrefix,
        isError = isError,
        supportingText = supportingText,
        modifier = modifier
    )
}

@Composable
fun CupsCodeDropdown(
    selectedCode: CupsCode?,
    onSelect: (CupsCode?) -> Unit,
    isError: Boolean = false,
    supportingText: String? = null,
    testTag: String,
    optionTestTagPrefix: String = "${testTag}_option_",
    modifier: Modifier = Modifier
) {
    SearchableCodeDropdown(
        selectedItem = selectedCode,
        onSelect = onSelect,
        searchItems = DentalClinicalCodes::searchCups,
        itemKey = { it.code },
        itemCode = { it.code },
        itemDescription = { it.description },
        itemDisplayLabel = { it.displayLabel },
        itemSecondaryText = { it.category },
        label = "Procedimiento CUPS (Opcional)",
        placeholder = "Buscar código o procedimiento (Ej: 232102 Resina)",
        clearContentDescription = "Limpiar CUPS",
        noMatchesText = "Sin coincidencias CUPS",
        testTag = testTag,
        optionTestTagPrefix = optionTestTagPrefix,
        isError = isError,
        supportingText = supportingText,
        modifier = modifier
    )
}
