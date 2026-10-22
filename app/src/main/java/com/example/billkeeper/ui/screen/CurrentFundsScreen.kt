package com.example.billkeeper.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.billkeeper.data.model.centsToYuanText
import com.example.billkeeper.data.model.formatCurrency
import com.example.billkeeper.data.model.isValidYuanInput
import com.example.billkeeper.data.model.parseYuanToCents
import com.example.billkeeper.viewmodel.LedgerViewModel

@Composable
fun CurrentFundsScreen(vm: LedgerViewModel) {
    val currentFundsCents by vm.remainingFundsCents.collectAsStateWithLifecycle()
    var editedAmountText by rememberSaveable { mutableStateOf<String?>(null) }
    val amountText = editedAmountText ?: centsToYuanText(currentFundsCents)
    var amountError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .navigationBarsPadding()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "当前现有资金",
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = formatCurrency(currentFundsCents),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
        OutlinedTextField(
            value = amountText,
            onValueChange = {
                if (isValidYuanInput(it)) {
                    editedAmountText = it
                    amountError = null
                }
            },
            label = { Text("现有资金") },
            leadingIcon = { Text("¥") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            isError = amountError != null,
            supportingText = { amountError?.let { Text(it) } },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = {
                val amountCents = parseYuanToCents(amountText)
                if (amountCents == null) {
                    amountError = "请输入大于 0 的有效金额"
                } else {
                    vm.updateCurrentFunds(amountCents)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("保存")
        }
    }
}
