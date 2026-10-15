import re

with open('app/src/main/java/com/example/ui/AppViewModel.kt', 'r') as f:
    content = f.read()

content = content.replace("يرجى الانتظار لحين موافقة وتفعيل الحساب من قبل مدير النظام للتمكن من الدخول.", "يمكنك الآن تسجيل الدخول مباشرة برقم هاتفك.")
content = content.replace("يرجى الانتظار لحين مراجعة الحساب وتفعيله من قبل مدير النظام للتمكن من الدخول.", "يمكنك الآن تسجيل الدخول مباشرة برقم هاتفك.")

with open('app/src/main/java/com/example/ui/AppViewModel.kt', 'w') as f:
    f.write(content)
