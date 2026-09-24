package com.example.keuangan

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FinanceViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = AppDb.get(app).dao()

    val transaksi = dao.semuaTransaksi()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val anggaran = dao.semuaAnggaran()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun tambah(t: Transaksi) = viewModelScope.launch { dao.tambah(t) }
    fun hapus(t: Transaksi) = viewModelScope.launch { dao.hapus(t) }
    fun simpanAnggaran(kategori: String, batas: Long) =
        viewModelScope.launch { dao.simpanAnggaran(Anggaran(kategori, batas)) }
}
