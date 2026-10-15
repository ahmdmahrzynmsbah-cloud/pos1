import sys

def patch():
    with open('app/src/main/java/com/example/ui/AppViewModel.kt', 'r') as f:
        content = f.read()

    content = content.replace('showToast("تم تنزيل كشف حساب الموظف بنجاح ✅")', 'showToast("تم تنزيل الملف بنجاح ✅")')
    
    with open('app/src/main/java/com/example/ui/AppViewModel.kt', 'w') as f:
        f.write(content)

patch()
