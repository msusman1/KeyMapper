package io.github.sds100.keymapper

import android.app.Service
import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.fragment.app.Fragment
import androidx.room.Room
import io.github.sds100.keymapper.data.*
import io.github.sds100.keymapper.data.db.AppDatabase
import io.github.sds100.keymapper.data.db.DefaultDataStoreManager
import io.github.sds100.keymapper.data.db.IDataStoreManager
import io.github.sds100.keymapper.data.repository.*
import kotlinx.coroutines.runBlocking

/**
 * Created by sds100 on 17/05/2020.
 */
object ServiceLocator {

    private val lock = Any()
    private var database: AppDatabase? = null

    @Volatile
    private var keymapRepository: DefaultKeymapRepository? = null

    @Volatile
    private var deviceInfoRepository: DeviceInfoRepository? = null

    @Volatile
    private var dataStoreManager: IDataStoreManager? = null

    @Volatile
    private var fingerprintMapRepository: FingerprintMapRepository? = null

    @Volatile
    private var fileRepository: FileRepository? = null

    @Volatile
    private var systemActionRepository: SystemActionRepository? = null

    @Volatile
    private var packageRepository: DefaultPackageRepository? = null

    @Volatile
    private var appUpdateManager: AppUpdateManager? = null

    @Volatile
    private var globalPreferences: IGlobalPreferences? = null

    @Volatile
    var backupManager: IBackupManager? = null

    fun keymapRepository(context: Context): DefaultKeymapRepository {
        synchronized(this) {
            return keymapRepository ?: createKeymapRepository(context)
        }
    }

    fun deviceInfoRepository(context: Context): DeviceInfoRepository {
        synchronized(this) {
            return deviceInfoRepository ?: createDeviceInfoRepository(context)
        }
    }

    //TODO make private
    fun preferenceDataStore(context: Context): IDataStoreManager {
        synchronized(this) {
            return dataStoreManager ?: createPreferenceDataStore(context)
        }
    }

    fun globalPreferences(context: Context): IGlobalPreferences {
        synchronized(this) {
            return globalPreferences ?: createGlobalPreferences(context)
        }
    }

    fun fingerprintMapRepository(context: Context): FingerprintMapRepository {
        synchronized(this) {
            return fingerprintMapRepository
                ?: createFingerprintMapRepository(context)
        }
    }

    fun fileRepository(context: Context): FileRepository {
        synchronized(this) {
            return fileRepository ?: createFileRepository(context)
        }
    }

    fun systemActionRepository(context: Context): SystemActionRepository {
        synchronized(this) {
            return systemActionRepository ?: createSystemActionRepository(context)
        }
    }

    fun packageRepository(context: Context): PackageRepository {
        synchronized(this) {
            return packageRepository ?: createPackageRepository(context)
        }
    }

    fun appUpdateManager(context: Context): AppUpdateManager {
        synchronized(this) {
            return appUpdateManager ?: createAppUpdateManager(context)
        }
    }

    fun backupManager(context: Context): IBackupManager {
        synchronized(this) {
            return backupManager ?: createBackupManager(context)
        }
    }

    @VisibleForTesting
    fun resetKeymapRepository() {
        synchronized(lock) {
            runBlocking {
                keymapRepository?.deleteAll()
                deviceInfoRepository?.deleteAll()
            }

            database?.apply {
                clearAllTables()
                close()
            }

            database = null
            keymapRepository = null
            deviceInfoRepository = null
        }
    }

    private fun createKeymapRepository(context: Context): DefaultKeymapRepository {
        val database = database ?: createDatabase(context.applicationContext)
        keymapRepository = DefaultKeymapRepository(
            database.keymapDao(),
            (context.applicationContext as MyApplication).appCoroutineScope)
        return keymapRepository!!
    }

    private fun createDeviceInfoRepository(context: Context): DeviceInfoRepository {
        val database = database ?: createDatabase(context.applicationContext)
        deviceInfoRepository = DefaultDeviceInfoRepository(
            database.deviceInfoDao(),
            (context.applicationContext as MyApplication).appCoroutineScope)
        return deviceInfoRepository!!
    }

    private fun createPreferenceDataStore(context: Context): IDataStoreManager {
        return dataStoreManager
            ?: DefaultDataStoreManager(context.applicationContext).also {
                this.dataStoreManager = it
            }
    }

    private fun createFingerprintMapRepository(context: Context): FingerprintMapRepository {
        val dataStore = preferenceDataStore(context).fingerprintGestureDataStore
        val scope = (context.applicationContext as MyApplication).appCoroutineScope

        return fingerprintMapRepository
            ?: DefaultFingerprintMapRepository(dataStore, scope).also {
                this.fingerprintMapRepository = it
            }
    }

    private fun createSystemActionRepository(context: Context): SystemActionRepository {
        return systemActionRepository
            ?: DefaultSystemActionRepository(context.applicationContext).also {
                this.systemActionRepository = it
            }
    }

    private fun createPackageRepository(context: Context): PackageRepository {
        return packageRepository
            ?: DefaultPackageRepository(context.packageManager).also {
                this.packageRepository = it
            }
    }

    private fun createFileRepository(context: Context): FileRepository {
        return fileRepository
            ?: FileRepository(context.applicationContext).also {
                this.fileRepository = it
            }
    }

    private fun createAppUpdateManager(context: Context): AppUpdateManager {
        val preferenceDataStore = preferenceDataStore(context)

        return appUpdateManager ?: AppUpdateManager(preferenceDataStore).also {
            this.appUpdateManager = it
        }
    }

    private fun createGlobalPreferences(context: Context): IGlobalPreferences {
        val dataStore = preferenceDataStore(context).globalPreferenceDataStore

        return globalPreferences
            ?: GlobalPreferences(
                dataStore,
                (context.applicationContext as MyApplication).appCoroutineScope
            ).also {
                this.globalPreferences = it
            }
    }

    private fun createBackupManager(context: Context): IBackupManager {
        return backupManager ?: BackupManager(
            keymapRepository(context),
            fingerprintMapRepository(context),
            deviceInfoRepository(context),
            (context.applicationContext as MyApplication).appCoroutineScope,
            (context.applicationContext as MyApplication),
            globalPreferences(context)
        ).also {
            this.backupManager = it
        }
    }

    private fun createDatabase(context: Context): AppDatabase {
        val result = Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        ).addMigrations(
            AppDatabase.MIGRATION_1_2,
            AppDatabase.MIGRATION_2_3,
            AppDatabase.MIGRATION_3_4,
            AppDatabase.MIGRATION_4_5,
            AppDatabase.MIGRATION_5_6,
            AppDatabase.MIGRATION_6_7,
            AppDatabase.MIGRATION_7_8,
            AppDatabase.MIGRATION_8_9,
            AppDatabase.MIGRATION_9_10).build()
        /* REMINDER!!!! Need to migrate fingerprint maps and other stuff???
         * Keep this note at the bottom */
        database = result
        return result
    }

    fun release() {
        synchronized(this) {
            packageRepository = null
            systemActionRepository = null
        }
    }
}

val Context.globalPreferences: IGlobalPreferences
    get() = ServiceLocator.globalPreferences(this)

val Service.globalPreferences: IGlobalPreferences
    get() = ServiceLocator.globalPreferences(this)

val Fragment.globalPreferences: IGlobalPreferences
    get() = ServiceLocator.globalPreferences(requireContext())