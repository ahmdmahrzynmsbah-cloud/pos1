# This script will not run on the phone, but the app code needs to be adjusted
# to clear those old branches. We will modify the loadBranches to clear them if they 
# exactly match the old default template.

import re

with open('app/src/main/java/com/example/ui/AppViewModel.kt', 'r') as f:
    content = f.read()

old_branches = """    if (saved != null) {
      val parsed = saved.split("||").filter { it.isNotBlank() }
      if (parsed.isNotEmpty()) {
         branches.value = parsed
      } else {
         branches.value = listOf("الفرع الرئيسي")
      }
    } else {
      branches.value = listOf("الفرع الرئيسي")
    }"""

new_branches = """    if (saved != null) {
      val parsed = saved.split("||").filter { it.isNotBlank() }
      // Clear out the dummy template ones if they are exactly the ones from the template
      val dummySet = setOf("الفرع الرئيسي (الرياض)", "فرع مكة المكرمة", "فرع جدة (الحمراء)", "فرع الدمام الدولي")
      val isOnlyDummies = parsed.all { dummySet.contains(it) }
      
      if (parsed.isNotEmpty() && !isOnlyDummies) {
         branches.value = parsed.filter { !dummySet.contains(it) }.ifEmpty { listOf("الفرع الرئيسي") }
      } else {
         branches.value = listOf("الفرع الرئيسي")
         // Save the clean list to overwrite the dummy ones
         saveBranches(branches.value)
      }
    } else {
      branches.value = listOf("الفرع الرئيسي")
    }"""

content = content.replace(old_branches, new_branches)

with open('app/src/main/java/com/example/ui/AppViewModel.kt', 'w') as f:
    f.write(content)

print("AppViewModel branches cleanup logic added.")
