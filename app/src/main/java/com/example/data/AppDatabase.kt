package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException

@Database(
    entities = [
        Product::class,
        Category::class,
        Invoice::class,
        InvoiceItem::class,
        Customer::class,
        LedgerEntry::class,
        Shift::class,
        AuditLog::class,
        SystemUser::class,
        AdminAlert::class,
        Supplier::class,
        SupplierTransaction::class,
        CustomerTransaction::class,
        Bank::class,
        Employee::class,
        Expense::class,
        AppFeedback::class
    ],
    version = 19,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_18_19 = object : androidx.room.migration.Migration(18, 19) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE expenses ADD COLUMN paymentMethod TEXT NOT NULL DEFAULT 'الصندوق'")
                db.execSQL("ALTER TABLE expenses ADD COLUMN checkNumber TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE expenses ADD COLUMN checkBankName TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE expenses ADD COLUMN checkInvoiceId INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE expenses ADD COLUMN partyType TEXT NOT NULL DEFAULT 'مصروف عام'")
                db.execSQL("ALTER TABLE expenses ADD COLUMN partyName TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE expenses ADD COLUMN customerId INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE expenses ADD COLUMN supplierId INTEGER DEFAULT NULL")
            }
        }

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "smart_pos_database"
                )
                    .addMigrations(MIGRATION_18_19)
                    .addCallback(AppDatabaseCallback(scope))
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class AppDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    try {
                        populateDatabase(database.appDao())
                    } catch (e: Exception) {
                        if (e is CancellationException) throw e
                        android.util.Log.e("DatabaseCallback", "Failed to populate database on create", e)
                    }
                }
            }
        }

        override fun onDestructiveMigration(db: SupportSQLiteDatabase) {
            super.onDestructiveMigration(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    try {
                        populateDatabase(database.appDao())
                    } catch (e: Exception) {
                        if (e is CancellationException) throw e
                        android.util.Log.e("DatabaseCallback", "Failed to populate database on migration", e)
                    }
                }
            }
        }

        suspend fun populateDatabase(dao: AppDao) {
            try {
                if (dao.getCategoriesCount() == 0) {
                    dao.insertCategory(Category(id = 1, nameAr = "مشروبات ساخنة وباردة", nameEn = "Beverages"))
                    dao.insertCategory(Category(id = 2, nameAr = "مأكولات وسناكس", nameEn = "Food & Snacks"))
                    dao.insertCategory(Category(id = 3, nameAr = "منتجات ألبان", nameEn = "Dairy Products"))
                    dao.insertCategory(Category(id = 4, nameAr = "منظفات وعناية", nameEn = "Cleaning & Care"))
                    dao.insertCategory(Category(id = 5, nameAr = "إلكترونيات واكسسوارات", nameEn = "Electronics"))
                }

                if (dao.getProductsCount() == 0) {
                    dao.insertProduct(Product(
                        id = 1,
                        nameAr = "قهوة إسبريسو ممتازة",
                        nameEn = "Espresso Coffee",
                        barcode = "6281001",
                        pricePurchase = 8.0,
                        priceSale = 15.0,
                        quantity = 50,
                        unit = "كوب",
                        minQuantityAlert = 5,
                        category = "مشروبات ساخنة وباردة"
                    ))
                    dao.insertProduct(Product(
                        id = 2,
                        nameAr = "مياه معدنية طبيعية 500 مل",
                        nameEn = "Mineral Water 500ml",
                        barcode = "6281002",
                        pricePurchase = 1.0,
                        priceSale = 2.0,
                        quantity = 120,
                        unit = "حبة",
                        minQuantityAlert = 10,
                        category = "مشروبات ساخنة وباردة"
                    ))
                    dao.insertProduct(Product(
                        id = 3,
                        nameAr = "عصير برتقال طازج",
                        nameEn = "Fresh Orange Juice",
                        barcode = "6281003",
                        pricePurchase = 6.0,
                        priceSale = 12.0,
                        quantity = 40,
                        unit = "كوب",
                        minQuantityAlert = 5,
                        category = "مشروبات ساخنة وباردة"
                    ))
                    dao.insertProduct(Product(
                        id = 4,
                        nameAr = "ساندويتش تشيز كيك فاخر",
                        nameEn = "Cheesecake Slice",
                        barcode = "6281004",
                        pricePurchase = 12.0,
                        priceSale = 22.0,
                        quantity = 30,
                        unit = "قطعة",
                        minQuantityAlert = 5,
                        category = "مأكولات وسناكس"
                    ))
                    dao.insertProduct(Product(
                        id = 5,
                        nameAr = "شيبس مقرمش مشكل",
                        nameEn = "Crispy Chips",
                        barcode = "6281005",
                        pricePurchase = 3.0,
                        priceSale = 5.0,
                        quantity = 80,
                        unit = "كيس",
                        minQuantityAlert = 10,
                        category = "مأكولات وسناكس"
                    ))
                    dao.insertProduct(Product(
                        id = 6,
                        nameAr = "شاحن هاتف سريع Type-C",
                        nameEn = "Fast Type-C Charger",
                        barcode = "6281006",
                        pricePurchase = 25.0,
                        priceSale = 45.0,
                        quantity = 25,
                        unit = "قطعة",
                        minQuantityAlert = 3,
                        category = "إلكترونيات واكسسوارات"
                    ))
                }

                if (dao.getSystemUsersCount() == 0) {
                    dao.insertSystemUser(SystemUser(
                        uid = "admin1",
                        name = "مدير النظام",
                        email = "admin@smartpos.com",
                        phone = "01000000000",
                        role = "ADMIN",
                        isActive = true,
                        isSubscribed = true,
                        storeName = "مؤسسة الكنز للمحاسبة التجارية"
                    ))
                    dao.insertSystemUser(SystemUser(
                        uid = "cashier1",
                        name = "كاشير المبيعات",
                        email = "cashier@smartpos.com",
                        phone = "0591234567",
                        role = "CASHIER",
                        isActive = true,
                        isSubscribed = true,
                        storeName = "مؤسسة الكنز للمحاسبة التجارية"
                    ))
                }

                dao.insertCustomer(Customer(
                    id = 1,
                    name = "عميل نقدي افتراضي",
                    phone = "0590000000",
                    email = "customer@smartpos.com",
                    points = 10,
                    type = "عادي"
                ))
            } catch (e: Exception) {
                android.util.Log.e("DatabaseSeed", "Error seeding data", e)
            }
        }
    }
}
