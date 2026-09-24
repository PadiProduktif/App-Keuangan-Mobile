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
    val tanggal: Long = System.currentTimeMillis()
)

@Entity(tableName = "anggaran")
data class Anggaran(
    @PrimaryKey val kategori: String,
    val batas: Long
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
}

@Database(entities = [Transaksi::class, Anggaran::class], version = 1, exportSchema = false)
abstract class AppDb : RoomDatabase() {
    abstract fun dao(): FinanceDao

    companion object {
        @Volatile private var inst: AppDb? = null
        fun get(c: Context): AppDb = inst ?: synchronized(this) {
            inst ?: Room.databaseBuilder(c.applicationContext, AppDb::class.java, "keuangan.db")
                .build().also { inst = it }
        }
    }
}
