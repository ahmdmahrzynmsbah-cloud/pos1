import re

with open("app/src/main/java/com/example/data/AppRepository.kt", "r") as f:
    content = f.read()

content = content.replace(
    'logAudit(cashierName, "فاتورة شراء", "تم حفظ فاتورة شراء #$invoiceId وتوريد المنتجات للمخزن بإجمالي $total للمورد $supplierName")',
    'logAudit(cashierName, if (isReturn) "مرتجع شراء" else "فاتورة شراء", if (isReturn) "تم حفظ فاتورة مرتجع مشتريات #$invoiceId وخصم المنتجات من المخزن بإجمالي $total للمورد $supplierName" else "تم حفظ فاتورة شراء #$invoiceId وتوريد المنتجات للمخزن بإجمالي $total للمورد $supplierName")'
)

with open("app/src/main/java/com/example/data/AppRepository.kt", "w") as f:
    f.write(content)
