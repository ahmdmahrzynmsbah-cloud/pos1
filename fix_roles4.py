import re

with open('app/src/main/java/com/example/ui/AppUI.kt', 'r') as f:
    content = f.read()

# Let's find exactly where it is now. We know it starts with 'محاكاة تبديل صلاحيات الموظفين والورديات:'
# and ends after the roles.forEach loop. 
# We need to find the Card that contains it and remove the whole Card.

# Let's just remove the Triple lines from roles and replace it with an empty list, and hide the whole UI manually.
old_roles = """            val roles = listOf(
            Triple("كاشير المبيعات", "كاشير", "يقتصر على البيع وفتح/إغلاق ورديته الخاصة فقط"),
            Triple("المدير المالي (الأستاذ عادل)", "محاسب", "التحكم بالقيود المزدوجة ومطالعة ميزان المراجعة وقائمة الأرباح"),
            Triple("المدير العام (م. سلمان)", "مدير", "له صلاحيات مطلقة على المستودع والموظفين والتقارير المتقدمة")
            )"""

new_roles = """            val roles = emptyList<Triple<String, String, String>>()"""

content = content.replace(old_roles, new_roles)

# Also hide the title
content = content.replace('Text("محاكاة تبديل صلاحيات الموظفين والورديات:", fontWeight = FontWeight.Bold, fontSize = 14.sp)', '// Text removed')
content = content.replace('Text("يتيح هذا الخيار محاكاة صلاحيات الإدارة والترحيل المحاسبي والكاشير:", fontSize = 11.sp, color = Color.Gray)', '// Text removed')

with open('app/src/main/java/com/example/ui/AppUI.kt', 'w') as f:
    f.write(content)

print("AppUI mock roles hidden.")
