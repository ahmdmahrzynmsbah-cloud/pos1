import re

with open('app/src/main/java/com/example/ui/AppUI.kt', 'r') as f:
    content = f.read()

# Fix search placeholder at line 4348
content = content.replace('placeholder = { Text("ابحث باسم المنتج أو قارئ الباركود...") },', 
                          'placeholder = { Text("ابحث بالاسم أو الباركود...", fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },')

# Fix CRM search placeholder at line 9204
content = content.replace('placeholder = { Text(if (crmTab == "customers") "ابحث باسم العميل أو رقم الجوال..." else "ابحث باسم المورد أو الشركة أو الجوال...") },',
                          'placeholder = { Text(if (crmTab == "customers") "بحث باسم العميل أو الجوال..." else "بحث باسم المورد أو الجوال...", fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },')

with open('app/src/main/java/com/example/ui/AppUI.kt', 'w') as f:
    f.write(content)

print("Placeholders fixed.")
