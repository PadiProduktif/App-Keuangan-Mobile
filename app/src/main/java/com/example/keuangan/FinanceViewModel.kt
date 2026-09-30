package com.example.keuangan

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

val KATEGORI_DEFAULT = listOf("Makanan", "Transportasi", "Belanja", "Tagihan", "Hiburan", "Lainnya")

class FinanceViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = AppDb.get(app).dao()

    val transaksi = dao.semuaTransaksi()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val anggaran = dao.semuaAnggaran()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val kategori = dao.semuaKategori()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val akun = dao.semuaAkun()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            if (dao.semuaKategori().first().isEmpty()) {
                KATEGORI_DEFAULT.forEach { dao.tambahKategori(Kategori(it)) }
            }
            if (dao.semuaAkun().first().isEmpty()) {
                dao.tambahAkun(Akun(nama = "Tunai", saldoAwal = 0))
            }
        }
    }

    fun tambah(t: Transaksi) = viewModelScope.launch { dao.tambah(t) }
    fun hapus(t: Transaksi) = viewModelScope.launch { dao.hapus(t) }

    fun simpanAnggaran(kategori: String, batas: Long) =
        viewModelScope.launch { dao.simpanAnggaran(Anggaran(kategori, batas)) }

    fun tambahKategori(nama: String) =
        viewModelScope.launch { dao.tambahKategori(Kategori(nama)) }

    fun tambahAkun(nama: String, saldoAwal: Long) =
        viewModelScope.launch { dao.tambahAkun(Akun(nama = nama, saldoAwal = saldoAwal)) }

    // Menambah/mengurangi saldo sebuah akun secara manual, dicatat sebagai transaksi
    // "Penyesuaian saldo" supaya tetap tercatat dalam riwayat.
    fun sesuaikanSaldo(akunId: Long, jumlah: Long, tambah: Boolean) = viewModelScope.launch {
        dao.tambah(
            Transaksi(
                judul = "Penyesuaian saldo",
                jumlah = jumlah,
                isPemasukan = tambah,
                kategori = "Penyesuaian saldo",
                akunId = akunId
            )
        )
    }
}
