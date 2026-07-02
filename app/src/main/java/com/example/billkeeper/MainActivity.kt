package com.example.billkeeper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/* ================================================================
   记账本 — 单文件全栈 (Room + Compose)
   - 支出导入（选类别填金额）
   - 支出总览（分类汇总）
   - 收入手动录入
   - 底部实时汇总条
   ================================================================ */

// ═══════════════ 1. Room 实体 ═══════════════

@Entity(tableName = "bills")
data class BillItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val category: String,
    val amount: Double,
    val date: Long,
    val note: String = ""
)

@Entity(tableName = "incomes")
data class IncomeItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val source: String,
    val amount: Double,
    val date: Long,
    val note: String = ""
)

// ═══════════════ 2. 分类汇总数据类 ═══════════════

data class CategorySummary(
    val category: String,
    val total: Double
)

// ═══════════════ 3. DAO ═══════════════

@Dao
interface BillDao {
    @Insert suspend fun insert(bill: BillItem)

    @Query("SELECT * FROM bills ORDER BY date DESC")
    fun getAll(): Flow<List<BillItem>>

    @Query("SELECT category, SUM(amount) as total FROM bills GROUP BY category ORDER BY total DESC")
    fun getCategorySummary(): Flow<List<CategorySummary>>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM bills")
    fun getTotalExpense(): Flow<Double>
}

@Dao
interface IncomeDao {
    @Insert suspend fun insert(income: IncomeItem)

    @Query("SELECT * FROM incomes ORDER BY date DESC")
    fun getAll(): Flow<List<IncomeItem>>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM incomes")
    fun getTotalIncome(): Flow<Double>
}

// ═══════════════ 4. 数据库 ═══════════════

@Database(entities = [BillItem::class, IncomeItem::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun billDao(): BillDao
    abstract fun incomeDao(): IncomeDao
}

// ═══════════════ 5. Repository ═══════════════

class LedgerRepository(private val db: AppDatabase) {
    val allBills: Flow<List<BillItem>> = db.billDao().getAll()
    val allIncomes: Flow<List<IncomeItem>> = db.incomeDao().getAll()
    val categorySummary: Flow<List<CategorySummary>> = db.billDao().getCategorySummary()
    val totalExpense: Flow<Double> = db.billDao().getTotalExpense()
    val totalIncome: Flow<Double> = db.incomeDao().getTotalIncome()

    suspend fun insertBill(bill: BillItem) = db.billDao().insert(bill)
    suspend fun insertIncome(income: IncomeItem) = db.incomeDao().insert(income)
}

// ═══════════════ 6. ViewModel ═══════════════

class LedgerViewModel(private val repo: LedgerRepository) : ViewModel() {
    val allBills = repo.allBills
    val allIncomes = repo.allIncomes
    val categorySummary = repo.categorySummary
    val totalExpense = repo.totalExpense
    val totalIncome = repo.totalIncome

    fun addBill(category: String, amount: Double, note: String) {
        viewModelScope.launch {
            repo.insertBill(BillItem(category = category, amount = amount, date = System.currentTimeMillis(), note = note))
        }
    }

    fun addIncome(source: String, amount: Double, note: String) {
        viewModelScope.launch {
            repo.insertIncome(IncomeItem(source = source, amount = amount, date = System.currentTimeMillis(), note = note))
        }
    }
}

// ═══════════════ 7. 常量 ═══════════════

val EXPENSE_CATEGORIES = listOf("餐饮", "交通", "购物", "娱乐", "住房", "日用", "医疗", "教育", "其他")
val INCOME_SOURCES = listOf("工资", "兼职", "投资", "理财", "红包", "报销", "其他")

val CATEGORY_COLORS = mapOf(
    "餐饮" to Color(0xFFE57373),
    "交通" to Color(0xFF64B5F6),
    "购物" to Color(0xFFFFB74D),
    "娱乐" to Color(0xFFBA68C8),
    "住房" to Color(0xFF4DB6AC),
    "日用" to Color(0xFFAED581),
    "医疗" to Color(0xFFBCAAA4),
    "教育" to Color(0xFFFFD54F),
    "其他" to Color(0xFF90A4AE)
)

// ═══════════════ 8. Compose UI ═══════════════

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "billkeeper.db").build()
        val repo = LedgerRepository(db)
        val vm = LedgerViewModel(repo)

        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Color(0xFF1B5E20),
                    secondary = Color(0xFF2E7D32),
                    surface = Color(0xFFF5F5F5),
                    background = Color(0xFFFAFAFA)
                )
            ) {
                BillKeeperApp(vm)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillKeeperApp(vm: LedgerViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("支出总览", "导入账单", "录入收入")

    val bills by vm.allBills.collectAsState(initial = emptyList())
    val incomes by vm.allIncomes.collectAsState(initial = emptyList())
    val catSummary by vm.categorySummary.collectAsState(initial = emptyList())
    val totalExp by vm.totalExpense.collectAsState(initial = 0.0)
    val totalInc by vm.totalIncome.collectAsState(initial = 0.0)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("\uD83D\uDCB0 记账本", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1B5E20),
                    titleContentColor = Color.White
                )
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp,
                color = Color.White
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BottomSummaryItem("总收入", totalInc, Color(0xFF2E7D32))
                    BottomSummaryItem("总支出", totalExp, Color(0xFFC62828))
                    BottomSummaryItem("结余", totalInc - totalExp,
                        if (totalInc - totalExp >= 0) Color(0xFF2E7D32) else Color(0xFFC62828))
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = Color(0xFF1B5E20)
            ) {
                tabs.forEachIndexed { idx, title ->
                    Tab(
                        selected = selectedTab == idx,
                        onClick = { selectedTab = idx },
                        text = { Text(title, fontWeight = if (selectedTab == idx) FontWeight.Bold else FontWeight.Normal) }
                    )
                }
            }
            when (selectedTab) {
                0 -> ExpenseSummaryTab(catSummary, totalExp, bills.size)
                1 -> ImportBillTab(vm)
                2 -> AddIncomeTab(vm)
            }
        }
    }
}

@Composable
fun BottomSummaryItem(label: String, amount: Double, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 12.sp, color = Color.Gray)
        Text("\u00A5%.2f".format(amount), fontSize = 17.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

// ───────────────────── Tab 0: 支出总览 ─────────────────────

@Composable
fun ExpenseSummaryTab(summary: List<CategorySummary>, totalExp: Double, billCount: Int) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1B5E20)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("总支出", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                    Text(
                        "\u00A5 %.2f".format(totalExp),
                        color = Color.White,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text("共 $billCount 笔账单", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                }
            }
        }

        item {
            Text("按分类汇总", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(top = 4.dp))
        }

        if (summary.isEmpty()) {
            item { Text("暂无支出记录", color = Color.Gray, modifier = Modifier.padding(vertical = 20.dp)) }
        } else {
            items(summary) { cat -> CategoryRow(cat) }
        }
    }
}

@Composable
fun CategoryRow(cat: CategorySummary) {
    val color = CATEGORY_COLORS[cat.category] ?: Color.Gray
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).background(color, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(cat.category.first().toString(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                Spacer(Modifier.width(12.dp))
                Text(cat.category, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }
            Text("\u00A5 %.2f".format(cat.total), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
        }
    }
}

// ───────────────────── Tab 1: 导入账单 ─────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportBillTab(vm: LedgerViewModel) {
    var selectedCategory by remember { mutableStateOf(EXPENSE_CATEGORIES[0]) }
    var amountText by remember { mutableStateOf("") }
    var noteText by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    val bills by vm.allBills.collectAsState(initial = emptyList())

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("添加支出", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(Modifier.height(12.dp))

                    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                        OutlinedTextField(
                            value = selectedCategory, onValueChange = {}, readOnly = true,
                            label = { Text("类别") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            EXPENSE_CATEGORIES.forEach { cat ->
                                DropdownMenuItem(text = { Text(cat) }, onClick = { selectedCategory = cat; expanded = false })
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = amountText, onValueChange = { amountText = it },
                        label = { Text("金额") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        leadingIcon = { Text("\u00A5") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = noteText, onValueChange = { noteText = it },
                        label = { Text("备注（可选）") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = {
                            val amt = amountText.toDoubleOrNull()
                            if (amt != null && amt > 0) { vm.addBill(selectedCategory, amt, noteText); amountText = ""; noteText = "" }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("记一笔")
                    }
                }
            }
        }

        item { Text("账单记录  (${bills.size})", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(top = 4.dp)) }

        if (bills.isEmpty()) {
            item { Text("还没有账单", color = Color.Gray, modifier = Modifier.padding(vertical = 16.dp)) }
        } else {
            items(bills) { bill -> BillRow(bill) }
        }
    }
}

@Composable
fun BillRow(bill: BillItem) {
    val color = CATEGORY_COLORS[bill.category] ?: Color.Gray
    val df = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(36.dp).background(color, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                Text(bill.category.first().toString(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(bill.category, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                if (bill.note.isNotBlank()) Text(bill.note, fontSize = 12.sp, color = Color.Gray)
                Text(df.format(Date(bill.date)), fontSize = 11.sp, color = Color.LightGray)
            }
            Text("- \u00A5%.2f".format(bill.amount), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
        }
    }
}

// ───────────────────── Tab 2: 录入收入 ─────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddIncomeTab(vm: LedgerViewModel) {
    var selectedSource by remember { mutableStateOf(INCOME_SOURCES[0]) }
    var amountText by remember { mutableStateOf("") }
    var noteText by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    val incomes by vm.allIncomes.collectAsState(initial = emptyList())

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("录入收入", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(Modifier.height(12.dp))

                    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                        OutlinedTextField(
                            value = selectedSource, onValueChange = {}, readOnly = true,
                            label = { Text("来源") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            INCOME_SOURCES.forEach { src ->
                                DropdownMenuItem(text = { Text(src) }, onClick = { selectedSource = src; expanded = false })
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = amountText, onValueChange = { amountText = it },
                        label = { Text("金额") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        leadingIcon = { Text("\u00A5") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = noteText, onValueChange = { noteText = it },
                        label = { Text("备注（可选）") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = {
                            val amt = amountText.toDoubleOrNull()
                            if (amt != null && amt > 0) { vm.addIncome(selectedSource, amt, noteText); amountText = ""; noteText = "" }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("记一笔")
                    }
                }
            }
        }

        item { Text("收入记录  (${incomes.size})", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(top = 4.dp)) }

        if (incomes.isEmpty()) {
            item { Text("还没有收入记录", color = Color.Gray, modifier = Modifier.padding(vertical = 16.dp)) }
        } else {
            items(incomes) { income -> IncomeRow(income) }
        }
    }
}

@Composable
fun IncomeRow(income: IncomeItem) {
    val df = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(36.dp).background(Color(0xFF2E7D32), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.TrendingUp, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(income.source, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                if (income.note.isNotBlank()) Text(income.note, fontSize = 12.sp, color = Color.Gray)
                Text(df.format(Date(income.date)), fontSize = 11.sp, color = Color.LightGray)
            }
            Text("+ \u00A5%.2f".format(income.amount), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
        }
    }
}
