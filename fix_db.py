import sys

def patch():
    with open('app/src/main/java/com/example/data/AppDatabase.kt', 'r') as f:
        content = f.read()
    
    old_builder = """                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "smart_pos_database"
                )
                    .addCallback(AppDatabaseCallback(scope))
                    .fallbackToDestructiveMigration(true)
                    .fallbackToDestructiveMigrationOnDowngrade(true)
                    .build()"""
                    
    new_builder = """                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "smart_pos_database"
                )
                    .addCallback(AppDatabaseCallback(scope))
                    .fallbackToDestructiveMigration()
                    .build()"""

    content = content.replace(old_builder, new_builder)
    
    with open('app/src/main/java/com/example/data/AppDatabase.kt', 'w') as f:
        f.write(content)

patch()
