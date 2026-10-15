import re

with open('app/src/main/java/com/example/ui/AppUI.kt', 'r') as f:
    content = f.read()

bad_triple2 = r'\s*Triple\("الورديات ومراقبة الصندوق اليومي 🕒", Icons\.Default\.HistoryToggleOff\),'
content = re.sub(bad_triple2, '', content)

with open('app/src/main/java/com/example/ui/AppUI.kt', 'w') as f:
    f.write(content)
