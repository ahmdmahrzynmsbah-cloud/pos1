import re

with open('app/src/main/java/com/example/ui/AppViewModel.kt', 'r') as f:
    content = f.read()

# Fix default branches
old_branches = """    if (saved != null) {
      branches.value = saved.split("||").filter { it.isNotBlank() }
    } else {
      branches.value = listOf("الفرع الرئيسي (الرياض)", "فرع مكة المكرمة", "فرع جدة (الحمراء)", "فرع الدمام الدولي")
    }"""

new_branches = """    if (saved != null) {
      val parsed = saved.split("||").filter { it.isNotBlank() }
      if (parsed.isNotEmpty()) {
         branches.value = parsed
      } else {
         branches.value = listOf("الفرع الرئيسي")
      }
    } else {
      branches.value = listOf("الفرع الرئيسي")
    }"""

content = content.replace(old_branches, new_branches)

with open('app/src/main/java/com/example/ui/AppViewModel.kt', 'w') as f:
    f.write(content)

print("AppViewModel branches fixed.")
