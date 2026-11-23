package com.example.biodontos.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import com.example.biodontos.core.designsystem.biodontosTextFieldColors
import com.example.biodontos.domain.model.BogotaLocalities

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalityDropdownField(
    selectedLocality: String,
    onLocalityChange: (String) -> Unit,
    label: String = "Localidad",
    dropdownTestTag: String,
    customInputTestTag: String,
    optionTestTagPrefix: String = "${dropdownTestTag}_option_",
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var isOtherMode by remember {
        mutableStateOf(
            selectedLocality.isNotBlank() && !BogotaLocalities.isOfficialLocality(selectedLocality)
        )
    }

    LaunchedEffect(selectedLocality) {
        when {
            BogotaLocalities.isOfficialLocality(selectedLocality) -> isOtherMode = false
            selectedLocality.isNotBlank() -> isOtherMode = true
        }
    }

    val dropdownDisplayValue = when {
        isOtherMode -> BogotaLocalities.OTHER_OPTION
        else -> selectedLocality
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = dropdownDisplayValue,
                onValueChange = {},
                readOnly = true,
                singleLine = true,
                label = { Text(label) },
                placeholder = { Text("Seleccione localidad") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth()
                    .testTag(dropdownTestTag),
                colors = biodontosTextFieldColors(),
                shape = RoundedCornerShape(12.dp)
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.heightIn(max = 280.dp)
            ) {
                BogotaLocalities.ALL_OPTIONS.forEach { option ->
                    val isOther = option == BogotaLocalities.OTHER_OPTION
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = if (isOther) "Otra (ingresar manualmente)" else option,
                                fontWeight = if (isOther) FontWeight.Bold else FontWeight.Normal,
                                color = BiodontosDark,
                                fontSize = 14.sp
                            )
                        },
                        onClick = {
                            expanded = false
                            if (isOther) {
                                isOtherMode = true
                                if (BogotaLocalities.isOfficialLocality(selectedLocality)) {
                                    onLocalityChange("")
                                }
                            } else {
                                isOtherMode = false
                                onLocalityChange(option)
                            }
                        },
                        modifier = Modifier.testTag("$optionTestTagPrefix$option")
                    )
                }
            }
        }

        AnimatedVisibility(visible = isOtherMode) {
            OutlinedTextField(
                value = if (BogotaLocalities.isOfficialLocality(selectedLocality)) "" else selectedLocality,
                onValueChange = onLocalityChange,
                label = { Text("Especifique la localidad") },
                placeholder = { Text("Ej: Soacha, Chía, Cajicá...") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(customInputTestTag),
                colors = biodontosTextFieldColors(),
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}
