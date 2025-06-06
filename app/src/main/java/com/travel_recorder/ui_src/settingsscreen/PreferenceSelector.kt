package com.travel_recorder.ui_src.settingsscreen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import kotlinx.coroutines.launch
import com.travel_recorder.R

@Composable
internal fun PreferenceSelector(
    title: String,
    value: Long,
    unit : String,
    onUpdate: suspend (Int) -> Unit,
    modifier: Modifier = Modifier,
    validValues: IntRange? = null,
) {
    var showEdit by rememberSaveable { mutableStateOf(false) }
    val summaryText = stringResource(R.string.current_value_template).format(value, unit)
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier) {
        PreferenceItem(
            title = title,
            summary = summaryText,
            onClick = { showEdit = true },
            modifier = modifier,
        )
        Column(modifier = Modifier.fillMaxHeight()) {}
    }
    if (showEdit) {
        var editValue by rememberSaveable(stateSaver = TextFieldValue.Saver) {
            mutableStateOf(TextFieldValue(value.toString()))
        }
        val isValidValue by remember {
            derivedStateOf {
                validValues?.contains(editValue.text.toIntOrNull()) ?: true
            }
        }
        AlertDialog(
            onDismissRequest = { showEdit = false },
            title = { Text(title) },
            text = {
                val editFocusRequester = remember { FocusRequester() }
                SideEffect {
                    editFocusRequester.requestFocus()
                }
                TextField(
                    value = editValue,
                    onValueChange = { newEditValue ->
                        editValue = cleanUIntInput(newEditValue, editValue) ?: return@TextField
                    },
                    supportingText = {
                        if (validValues != null) {
                            Text(
                                stringResource(R.string.valid_range_template)
                                    .format(validValues.first, unit, validValues.last, unit)
                            )
                        }
                    },
                    isError = !isValidValue,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .focusRequester(editFocusRequester)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            onUpdate(editValue.text.toInt())
                        }
                        showEdit = false
                    },
                    enabled = isValidValue,
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showEdit = false }
                ) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

private fun cleanUIntInput(newValue: TextFieldValue, oldValue: TextFieldValue): TextFieldValue? {
    if (newValue.text == oldValue.text) return newValue
    val newText = newValue.text.filter { it.isDigit() }.trimStart { it == '0' }
    if (newText != oldValue.text) return newValue.copy(text = newText)
    return null
}
