package com.example.keuangan

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "transaksi")
data class Transaksi(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val judul: String,
    val jumlah: Long,
    val isPemasukan: Boolean,
    val kategori: String,
    val tanggal: Long = System.currentTimeMillis(),
    val akunId: Long = 0
)

@Entity(tableName = "anggaran")
data class Anggaran(
    @PrimaryKey val kategori: String,
    val batas: Long
)

@Entity(tableName = "kategori")
data class Kategori(
    @PrimaryKey val nama: String
)

@Entity(tableName = "akun")
data class Akun(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nama: String,
    val saldoAwal: Long = 0
)

@Dao
interface FinanceDao {
    @Query("SELECT * FROM transaksi ORDER BY tanggal DESC")
    fun semuaTransaksi(): Flow<List<Transaksi>>
    @Insert suspend fun tambah(t: Transaksi)
    @Delete suspend fun hapus(t: Transaksi)

    @Query("SELECT * FROM anggaran")
    fun semuaAnggaran(): Flow<List<Anggaran>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun simpanAnggaran(a: Anggaran)

    @Query("SELECT * FROM kategori ORDER BY nama")
    fun semuaKategori(): Flow<List<Kategori>>
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun tambahKategori(k: Kategori)
    @Delete suspend fun hapusKategori(k: Kategori)

    @Query("SELECT * FROM akun ORDER BY id")
    fun semuaAkun(): Flow<List<Akun>>
    @Insert suspend fun tambahAkun(a: Akun): Long
    @Delete suspend fun hapusAkun(a: Akun)
}

@Database(
    entities = [Transaksi::class, Anggaran::class, Kategori::class, Akun::class],
    version = 2,
    exportSchema = false
)
abstract class AppDb : RoomDatabase() {
    abstract fun dao(): FinanceDao

    companion object {
        @Volatile private var inst: AppDb? = null
        fun get(c: Context): AppDb = inst ?: synchronized(this) {
            inst ?: Room.databaseBuilder(c.applicationContext, AppDb::class.java, "keuangan.db")
                .fallbackToDestructiveMigration()
                .build().also { inst = it }
        }
    }
}
