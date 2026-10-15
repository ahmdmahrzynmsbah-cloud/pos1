import sys

def patch():
    with open('app/src/main/java/com/example/ui/InvoiceFormScreen.kt', 'r') as f:
        content = f.read()

    # 1. Initialize variables from AppViewModel
    old_vars_1 = """    var paymentMethod by remember { mutableStateOf("نقدي") } // "نقدي", "آجل", "بطاقة", "شيك\""""
    new_vars_1 = """    val setupPayment = viewModel.setupPaymentMethod.collectAsState().value
    var paymentMethod by remember { mutableStateOf(if (setupPayment.isNotBlank()) setupPayment else "نقدي") } // "نقدي", "آجل", "بطاقة", "شيك\""""
    content = content.replace(old_vars_1, new_vars_1)

    old_vars_2 = """    var checkNumberInput by remember { mutableStateOf("") }
    var checkBankNameInput by remember { mutableStateOf("") }
    val defaultCal = remember { java.util.Calendar.getInstance() }
    var checkDateInput by remember { mutableStateOf(defaultCal.timeInMillis) }"""
    new_vars_2 = """    val setupCheckNum = viewModel.setupCheckNumber.collectAsState().value
    val setupCheckBank = viewModel.setupCheckBankName.collectAsState().value
    val setupCheckDate = viewModel.setupCheckDate.collectAsState().value
    var checkNumberInput by remember(setupCheckNum) { mutableStateOf(setupCheckNum) }
    var checkBankNameInput by remember(setupCheckBank) { mutableStateOf(setupCheckBank) }
    var checkDateInput by remember(setupCheckDate) { mutableStateOf(setupCheckDate) }"""
    content = content.replace(old_vars_2, new_vars_2)

    # 2. Skip dialog if we already have the check details
    old_save_logic = """        } else if (paymentMethod == "شيك") {
            showCheckDetailsDialog = true
        } else {"""
    new_save_logic = """        } else if (paymentMethod == "شيك") {
            if (checkNumberInput.isNotBlank() && checkBankNameInput.isNotBlank()) {
                performActualSave(checkNumberInput, checkBankNameInput, checkDateInput)
            } else {
                showCheckDetailsDialog = true
            }
        } else {"""
    content = content.replace(old_save_logic, new_save_logic)

    with open('app/src/main/java/com/example/ui/InvoiceFormScreen.kt', 'w') as f:
        f.write(content)

patch()
