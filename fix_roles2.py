import re

with open('app/src/main/java/com/example/ui/AppUI.kt', 'r') as f:
    content = f.read()

# Fix mock roles block (second attempt, matching the exact spacing found)
old_roles = """      if (userAccount?.role != "ADMIN") {
        Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
          ) {
          
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("محاكاة تبديل صلاحيات الموظفين والورديات:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text("يتيح هذا الخيار محاكاة صلاحيات الإدارة والترحيل المحاسبي والكاشير:", fontSize = 11.sp, color = Color.Gray)
            
            val roles = listOf(
            Triple("كاشير المبيعات", "كاشير", "يقتصر على البيع وفتح/إغلاق ورديته الخاصة فقط"),
            Triple("المدير المالي (الأستاذ عادل)", "محاسب", "التحكم بالقيود المزدوجة ومطالعة ميزان المراجعة وقائمة الأرباح"),
            Triple("المدير العام (م. سلمان)", "مدير", "له صلاحيات مطلقة على المستودع والموظفين والتقارير المتقدمة")
            )
              
            roles.forEach { (user, role, desc) ->
              OutlinedButton(
              onClick = { viewModel.currentUser.value = user; viewModel.currentUserRole.value = role },
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(12.dp),
              border = BorderStroke(
              1.dp,
              if (viewModel.currentUser.value == user) MaterialTheme.colorScheme.primary else Color.LightGray
              )
                ) {
                
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
                  Text(user, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                  Text(desc, fontSize = 10.sp, color = Color.Gray)
                }
              }
            }
          }
        }
      }"""

content = content.replace(old_roles, "")

with open('app/src/main/java/com/example/ui/AppUI.kt', 'w') as f:
    f.write(content)

print("AppUI mock roles removed completely.")
