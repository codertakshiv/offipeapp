package com.offipe.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.io.File
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory

@Database(
    entities = [TransactionEntity::class, LastBalanceEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun lastBalanceDao(): LastBalanceDao

    companion object {
        fun create(context: Context, passphrase: ByteArray): AppDatabase {
            val sqlCipherPassphrase = passphrase.toSqlCipherPassphrase()
            passphrase.fill(0)
            return try {
                migrateLegacyDatabase(context, sqlCipherPassphrase)
                val factory = SupportFactory(sqlCipherPassphrase, null, true)
                Room.databaseBuilder(context, AppDatabase::class.java, DATABASE_NAME)
                    .openHelperFactory(factory)
                    .addMigrations(MIGRATION_1_2)
                    .build()
            } catch (exception: Exception) {
                sqlCipherPassphrase.fill(0)
                throw exception
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `last_balance` " +
                        "(`id` INTEGER NOT NULL, `text` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`id`))"
                )
            }
        }

        private fun migrateLegacyDatabase(context: Context, passphrase: ByteArray) {
            val migrationPreferences = context.getSharedPreferences(
                MIGRATION_PREFERENCES_NAME,
                Context.MODE_PRIVATE
            )
            if (migrationPreferences.getBoolean(MIGRATION_COMPLETE_KEY, false)) return

            SQLiteDatabase.loadLibs(context)
            val databaseFile = context.getDatabasePath(DATABASE_NAME)
            if (!databaseFile.exists()) {
                markMigrationComplete(migrationPreferences)
                return
            }

            val legacyDatabase = tryOpenDatabase(databaseFile, LEGACY_PASSPHRASE.toCharArray())

            if (legacyDatabase != null) {
                val newPassphrase = passphrase.toPassphraseChars()
                try {
                    legacyDatabase.changePassword(newPassphrase)
                } catch (exception: Exception) {
                    throw IllegalStateException("Unable to migrate encrypted database", exception)
                } finally {
                    newPassphrase.fill('\u0000')
                    legacyDatabase.close()
                }

                val migratedDatabase = tryOpenDatabase(databaseFile, passphrase.toPassphraseChars())
                    ?: throw IllegalStateException("Encrypted database migration verification failed")
                migratedDatabase.close()
                markMigrationComplete(migrationPreferences)
                return
            }

            val currentDatabase = tryOpenDatabase(databaseFile, passphrase.toPassphraseChars())
            if (currentDatabase != null) {
                currentDatabase.close()
                markMigrationComplete(migrationPreferences)
                return
            }

            deleteDatabaseFiles(databaseFile)
            markMigrationComplete(migrationPreferences)
        }

        private fun tryOpenDatabase(file: File, passphrase: CharArray): SQLiteDatabase? {
            var database: SQLiteDatabase? = null
            return try {
                val openedDatabase = SQLiteDatabase.openDatabase(
                    file.absolutePath,
                    passphrase,
                    null,
                    SQLiteDatabase.OPEN_READWRITE
                )
                database = openedDatabase
                openedDatabase.rawQuery("SELECT count(*) FROM sqlite_master", null).use { cursor ->
                    check(cursor.moveToFirst())
                }
                openedDatabase
            } catch (_: Exception) {
                runCatching { database?.close() }
                null
            } finally {
                passphrase.fill('\u0000')
            }
        }

        private fun deleteDatabaseFiles(databaseFile: File) {
            val files = listOf(
                databaseFile,
                File("${databaseFile.path}-wal"),
                File("${databaseFile.path}-shm"),
                File("${databaseFile.path}-journal")
            )
            check(files.all { !it.exists() || it.delete() }) {
                "Unable to remove unreadable encrypted database"
            }
        }

        private fun markMigrationComplete(preferences: android.content.SharedPreferences) {
            check(preferences.edit().putBoolean(MIGRATION_COMPLETE_KEY, true).commit()) {
                "Unable to persist database migration status"
            }
        }

        private fun ByteArray.toSqlCipherPassphrase(): ByteArray {
            val hexDigits = "0123456789abcdef"
            return ByteArray(size * 2).also { encodedPassphrase ->
                forEachIndexed { index, byte ->
                    val value = byte.toInt() and 0xff
                    encodedPassphrase[index * 2] = hexDigits[value ushr 4].code.toByte()
                    encodedPassphrase[index * 2 + 1] = hexDigits[value and 0x0f].code.toByte()
                }
            }
        }

        private fun ByteArray.toPassphraseChars(): CharArray = CharArray(size) { index ->
            (this[index].toInt() and 0xff).toChar()
        }

        private const val DATABASE_NAME = "offipe.db"
        private const val LEGACY_PASSPHRASE = "offipe_secure_db"
        private const val MIGRATION_PREFERENCES_NAME = "offipe_database_migration"
        private const val MIGRATION_COMPLETE_KEY = "legacy_key_migration_complete"
    }
}