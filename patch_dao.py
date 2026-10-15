import sys

def patch():
    with open('app/src/main/java/com/example/data/AppDao.kt', 'r') as f:
        content = f.read()

    new_content = """    @Delete
    suspend fun deleteCustomerTransaction(tx: CustomerTransaction)

    // --- Employees ---
    @Query("SELECT * FROM employees ORDER BY name ASC")
    fun getAllEmployees(): Flow<List<Employee>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmployee(employee: Employee): Long

    @Update
    suspend fun updateEmployee(employee: Employee)

    @Query("UPDATE employees SET balance = balance + :balanceDelta WHERE id = :id")
    suspend fun updateEmployeeBalance(id: Int, balanceDelta: Double)

    // --- Expenses ---
    @Query("SELECT * FROM expenses ORDER BY date DESC")
    fun getAllExpenses(): Flow<List<Expense>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense): Long
}"""
    
    content = content.replace("    @Delete\n    suspend fun deleteCustomerTransaction(tx: CustomerTransaction)\n}", new_content)

    with open('app/src/main/java/com/example/data/AppDao.kt', 'w') as f:
        f.write(content)

patch()
