import sys

def patch():
    with open('app/src/main/java/com/example/ui/AppViewModel.kt', 'r') as f:
        content = f.read()

    new_states = """
    // --- Employees & Expenses ---
    val allEmployees = repository.allEmployees.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allExpenses = repository.allExpenses.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addEmployee(name: String, phone: String, role: String, salary: Double) {
        viewModelScope.launch {
            repository.insertEmployee(Employee(name = name, phone = phone, role = role, salary = salary))
            logAction("إضافة موظف", "تم إضافة الموظف: $name")
        }
    }

    fun updateEmployee(employee: Employee) {
        viewModelScope.launch {
            repository.updateEmployee(employee)
            logAction("تعديل موظف", "تم تعديل الموظف: ${employee.name}")
        }
    }

    fun addExpense(type: String, amount: Double, notes: String, employeeId: Int? = null) {
        viewModelScope.launch {
            repository.insertExpense(Expense(type = type, amount = amount, notes = notes, employeeId = employeeId))
            if (type == "رواتب وأجور" && employeeId != null) {
                // If it's salary, we might need to update employee balance or something, but usually just adding expense is fine.
                // We will leave employee balance for future updates if needed.
            }
            // Add a ledger entry
            repository.insertLedgerEntry(
                LedgerEntry(
                    description = "سند صرف: $type - $notes",
                    debitAccount = "المصروفات",
                    creditAccount = "الخزينة/البنك",
                    amount = amount
                )
            )
            logAction("سند صرف", "تم تسجيل سند صرف: $type بمبلغ $amount")
            showToast("تم حفظ سند الصرف بنجاح")
        }
    }
"""
    
    # Let's insert this near the end of the ViewModel, before the last closing brace
    content = content.rstrip()
    if content.endswith("}"):
        content = content[:-1]
    
    content += new_states + "\n}\n"
    
    with open('app/src/main/java/com/example/ui/AppViewModel.kt', 'w') as f:
        f.write(content)

patch()
