package com.example.keuangan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

val KATEGORI = listOf("Makanan", "Transportasi", "Belanja", "Tagihan", "Hiburan", "Lainnya")
val WARNA = listOf(
    Color(0xFFE57373), Color(0xFF64B5F6), Color(0xFF81C784),
    Color(0xFFFFB74D), Color(0xFFBA68C8), Color(0xFF90A4AE)
)
val HIJAU = Color(0xFF2E7D32)
val MERAH = Color(0xFFC62828)

fun rp(n: Long): String = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    .apply { maximumFractionDigits = 0 }.format(n)

fun dalamBulan(t: Long, offset: Int): Boolean {
    val a = Calendar.getInstance().apply { add(Calendar.MONTH, offset) }
    val b = Calendar.getInstance().apply { timeInMillis = t }
    return a[Calendar.YEAR] == b[Calendar.YEAR] && a[Calendar.MONTH] == b[Calendar.MONTH]
}

fun labelBulan(offset: Int): String {
    val c = Calendar.getInstance().apply { add(Calendar.MONTH, offset) }
    return SimpleDateFormat("MMMM yyyy", Locale("id")).format(c.time)
}

fun buatCsv(list: List<Transaksi>): String {
    val f = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val baris = list.map {
        val judul = it.judul.replace("\"", "\"\"")
        val jenis = if (it.isPemasukan) "Pemasukan" else "Pengeluaran"
        "${f.format(Date(it.tanggal))},\"$judul\",${it.kategori},$jenis,${it.jumlah}"
    }
    return (listOf("Tanggal,Judul,Kategori,Jenis,Jumlah") + baris).joinToString("\n")
}

class MainActivity : ComponentActivity() {
    private val vm: FinanceViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AppScreen(vm) }
    }
}

@Composable
fun AppScreen(vm: FinanceViewModel) {
    val sistem = isSystemInDarkTheme()
    var gelap by rememberSaveable { mutableStateOf(sistem) }
    MaterialTheme(colorScheme = if (gelap) darkColorScheme() else lightColorScheme()) {
        Surface(Modifier.fillMaxSize()) { Beranda(vm, gelap) { gelap = !gelap } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Beranda(vm: FinanceViewModel, gelap: Boolean, ganti: () -> Unit) {
    val semua by vm.transaksi.collectAsStateWithLifecycle()
    val anggaran by vm.anggaran.collectAsStateWithLifecycle()
    var tab by remember { mutableIntStateOf(0) }
    var offset by rememberSaveable { mutableIntStateOf(0) }
    var dialogTambah by remember { mutableStateOf(false) }
    val bulan = semua.filter { dalamBulan(it.tanggal, offset) }
    val context = LocalContext.current
    val ekspor = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) {
            context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(buatCsv(bulan)) }
            Toast.makeText(context, "CSV tersimpan", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Keuanganku") },
                actions = {
                    TextButton(onClick = { ekspor.launch("keuangan_${labelBulan(offset).replace(' ', '_')}.csv") }) { Text("CSV") }
                    TextButton(onClick = ganti) { Text(if (gelap) "Terang" else "Gelap") }
                }
            )
        },
        floatingActionButton = {
            if (tab == 0) FloatingActionButton(onClick = { dialogTambah = true }) {
                Icon(Icons.Default.Add, "Tambah")
            }
        }
    ) { pad ->
        Column(Modifier.padding(pad)) {
            TabRow(selectedTabIndex = tab) {
                listOf("Transaksi", "Anggaran", "Laporan").forEachIndexed { i, s ->
                    Tab(selected = tab == i, onClick = { tab = i }, text = { Text(s) })
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { offset -= 1 }) { Text("‹") }
                Text(labelBulan(offset), style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = { offset += 1 }, enabled = offset < 0) { Text("›") }
            }
            when (tab) {
                0 -> TabTransaksi(semua, bulan, vm)
                1 -> TabAnggaran(bulan, anggaran, vm)
                else -> TabLaporan(bulan)
            }
        }
    }
    if (dialogTambah) DialogTambah(tutup = { dialogTambah = false }, simpan = { vm.tambah(it) })
}

@Composable
fun TabTransaksi(semua: List<Transaksi>, bulan: List<Transaksi>, vm: FinanceViewModel) {
    val masuk = bulan.filter { it.isPemasukan }.sumOf { it.jumlah }
    val keluar = bulan.filter { !it.isPemasukan }.sumOf { it.jumlah }
    val saldo = semua.sumOf { if (it.isPemasukan) it.jumlah else -it.jumlah }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Saldo", style = MaterialTheme.typography.labelMedium)
                    Text(rp(saldo), style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.height(8.dp))
                    Text("Masuk: ${rp(masuk)}", color = HIJAU)
                    Text("Keluar: ${rp(keluar)}", color = MERAH)
                }
            }
        }
        items(bulan, key = { it.id }) { t ->
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(start = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
                        Text(t.judul, style = MaterialTheme.typography.titleMedium)
                        val tgl = SimpleDateFormat("d MMM yyyy", Locale("id")).format(Date(t.tanggal))
                        Text("${t.kategori} • $tgl", style = MaterialTheme.typography.bodySmall)
                    }
                    Text(
                        (if (t.isPemasukan) "+" else "-") + rp(t.jumlah),
                        color = if (t.isPemasukan) HIJAU else MERAH
                    )
                    IconButton(onClick = { vm.hapus(t) }) { Icon(Icons.Default.Delete, "Hapus") }
                }
            }
        }
    }
}

@Composable
fun DialogTambah(tutup: () -> Unit, simpan: (Transaksi) -> Unit) {
    var judul by remember { mutableStateOf("") }
    var jumlah by remember { mutableStateOf("") }
    var masuk by remember { mutableStateOf(false) }
    var kat by remember { mutableStateOf(KATEGORI[0]) }
    AlertDialog(
        onDismissRequest = tutup,
        title = { Text("Transaksi baru") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = !masuk, onClick = { masuk = false }, label = { Text("Pengeluaran") })
                    FilterChip(selected = masuk, onClick = { masuk = true }, label = { Text("Pemasukan") })
                }
                OutlinedTextField(judul, { judul = it }, label = { Text("Judul") }, singleLine = true)
                OutlinedTextField(
                    jumlah, { jumlah = it.filter(Char::isDigit) },
                    label = { Text("Jumlah (Rp)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                if (!masuk) {
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        KATEGORI.forEach { k ->
                            FilterChip(selected = kat == k, onClick = { kat = k }, label = { Text(k) })
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val j = jumlah.toLongOrNull() ?: 0L
                if (judul.isNotBlank() && j > 0) {
                    simpan(Transaksi(judul = judul.trim(), jumlah = j, isPemasukan = masuk,
                        kategori = if (masuk) "Pemasukan" else kat))
                    tutup()
                }
            }) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = tutup) { Text("Batal") } }
    )
}

@Composable
fun TabAnggaran(bulan: List<Transaksi>, anggaran: List<Anggaran>, vm: FinanceViewModel) {
    var edit by remember { mutableStateOf<String?>(null) }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(KATEGORI) { k ->
            val pakai = bulan.filter { !it.isPemasukan && it.kategori == k }.sumOf { it.jumlah }
            val batas = anggaran.find { it.kategori == k }?.batas ?: 0L
            val rasio = if (batas > 0) (pakai.toFloat() / batas).coerceAtMost(1f) else 0f
            Card(Modifier.fillMaxWidth().clickable { edit = k }) {
                Column(Modifier.padding(16.dp)) {
                    Text(k, style = MaterialTheme.typography.titleMedium)
                    LinearProgressIndicator(
                        progress = { rasio },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        color = if (batas > 0 && pakai > batas) MERAH else MaterialTheme.colorScheme.primary
                    )
                    Text(
                        if (batas > 0) "${rp(pakai)} dari ${rp(batas)}"
                        else "${rp(pakai)} • ketuk untuk atur batas"
                    )
                }
            }
        }
    }
    edit?.let { k ->
        var teks by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { edit = null },
            title = { Text("Batas $k per bulan") },
            text = {
                OutlinedTextField(
                    teks, { teks = it.filter(Char::isDigit) },
                    label = { Text("Rp") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            },
            confirmButton = {
                TextButton(onClick = { vm.simpanAnggaran(k, teks.toLongOrNull() ?: 0L); edit = null }) {
                    Text("Simpan")
                }
            }
        )
    }
}

@Composable
fun TabLaporan(bulan: List<Transaksi>) {
    val perKat = KATEGORI.map { k -> bulan.filter { !it.isPemasukan && it.kategori == k }.sumOf { it.jumlah } }
    val total = perKat.sum()
    val masuk = bulan.filter { it.isPemasukan }.sumOf { it.jumlah }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Text("Pengeluaran per kategori", style = MaterialTheme.typography.titleMedium) }
        item {
            if (total == 0L) Text("Belum ada pengeluaran bulan ini.")
            else Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(200.dp)) {
                    var mulai = -90f
                    perKat.forEachIndexed { i, v ->
                        val sapu = 360f * v / total
                        drawArc(WARNA[i], mulai, sapu, useCenter = true)
                        mulai += sapu
                    }
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                KATEGORI.forEachIndexed { i, k ->
                    if (perKat[i] > 0) Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(12.dp).background(WARNA[i]))
                        Spacer(Modifier.width(8.dp))
                        Text("$k: ${rp(perKat[i])} (${100 * perKat[i] / total}%)")
                    }
                }
            }
        }
        item { Text("Pemasukan vs pengeluaran", style = MaterialTheme.typography.titleMedium) }
        item {
            val maks = maxOf(masuk, total, 1L).toFloat()
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Batang("Pemasukan", masuk, maks, HIJAU)
                Batang("Pengeluaran", total, maks, MERAH)
            }
        }
    }
}

@Composable
fun Batang(label: String, nilai: Long, maks: Float, warna: Color) {
    Column {
        Text("$label: ${rp(nilai)}")
        Box(Modifier.fillMaxWidth(nilai / maks).height(16.dp).background(warna))
    }
}
