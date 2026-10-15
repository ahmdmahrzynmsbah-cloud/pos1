import re

with open("app/src/main/java/com/example/ui/AppViewModel.kt", "r") as f:
    content = f.read()

content = content.replace(
    'val page = pdfDocument.startPage(pageInfo)',
    'val page = pdfDocument.startPage(pageInfo)\n                val branchNameStr = currentBranch.value'
)

content = content.replace(
    'canvas.drawText("الفرع: ${if(branchName.isEmpty()) "الرئيسي" else branchName}", pageW / 2f, y, paint)',
    'canvas.drawText("الفرع: ${if(branchNameStr.isEmpty()) "الرئيسي" else branchNameStr}", pageW / 2f, y, paint)'
)

with open("app/src/main/java/com/example/ui/AppViewModel.kt", "w") as f:
    f.write(content)
