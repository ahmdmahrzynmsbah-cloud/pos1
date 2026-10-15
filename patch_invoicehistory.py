import sys

def patch():
    with open('app/src/main/java/com/example/ui/AppUI.kt', 'r') as f:
        content = f.read()

    # 1. Update matchesType block
    old_matches = """                val matchesType = when (selectedFilterType) {
                    "المبيعات" -> !isPurchase && inv.total >= 0
                    "المشتريات" -> isPurchase && inv.total >= 0
                    "مرتجعات" -> inv.total < 0
                    else -> true
                }"""
                
    new_matches = """                val matchesType = when (selectedFilterType) {
                    "المبيعات" -> !isPurchase && inv.total >= 0 && !inv.isDraft
                    "المشتريات" -> isPurchase && inv.total >= 0 && !inv.isDraft
                    "مرتجعات" -> inv.total < 0 && !inv.isDraft
                    "المسودات" -> inv.isDraft
                    else -> !inv.isDraft // "الكل" shows all non-drafts
                }"""
                
    content = content.replace(old_matches, new_matches)

    # 2. Update filterOptions block
    old_options = """                            val filterOptions = listOf(
                                "الكل (${allInvoices.size})" to "الكل",
                                "المبيعات 🛒" to "المبيعات",
                                "المشتريات 📦" to "المشتريات",
                                "مرتجعات 🔄" to "مرتجعات"
                            )"""
                            
    new_options = """                            val filterOptions = listOf(
                                "الكل (${allInvoices.count { !it.isDraft }})" to "الكل",
                                "المبيعات 🛒" to "المبيعات",
                                "المشتريات 📦" to "المشتريات",
                                "مرتجعات 🔄" to "مرتجعات",
                                "مسودات 📝 (${allInvoices.count { it.isDraft }})" to "المسودات"
                            )"""
                            
    content = content.replace(old_options, new_options)
    
    with open('app/src/main/java/com/example/ui/AppUI.kt', 'w') as f:
        f.write(content)

patch()
