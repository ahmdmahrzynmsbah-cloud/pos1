import re

with open('app/src/main/java/com/example/ui/AuthScreen.kt', 'r') as f:
    content = f.read()

# We need to change the logic in validate:
# Remove email requirements if not admin.
# For !isLogin: require name, businessName, phone.
# For isLogin: if !isAdmin require phone, if isAdmin require email.

# Let's completely rewrite validate() inside the file.
new_validate = """    fun validate(): Boolean {
        var isValid = true
        emailError = null
        passwordError = null
        nameError = null
        businessNameError = null
        phoneError = null

        if (password.isBlank()) {
            passwordError = "كلمة المرور مطلوبة"
            isValid = false
        } else if (password.length < 6) {
            passwordError = "كلمة المرور يجب أن تكون 6 أحرف على الأقل"
            isValid = false
        }

        if (isAdmin) {
            if (email.isBlank()) {
                emailError = "اسم المستخدم أو البريد مطلوب"
                isValid = false
            } else if (email.length < 3) {
                emailError = "الاسم قصير جداً"
                isValid = false
            }
        } else {
            if (phone.isBlank()) {
                phoneError = "رقم الجوال مطلوب"
                isValid = false
            } else if (phone.length < 8) {
                phoneError = "رقم الجوال غير صحيح"
                isValid = false
            }
        }

        if (!isLogin) {
            if (name.isBlank()) {
                nameError = "الاسم الكامل مطلوب"
                isValid = false
            }
            if (businessName.isBlank()) {
                businessNameError = "اسم النشاط التجاري مطلوب"
                isValid = false
            }
        }
        return isValid
    }"""

content = re.sub(r'    fun validate\(\): Boolean \{.*?    \}', new_validate, content, flags=re.DOTALL)

# Now for the UI elements:
# Let's extract the phone field completely out of the `!isLogin` AnimatedVisibility block.
# And we'll use AnimatedVisibility(visible = isAdmin) for the email field.

# The phone field is currently inside AnimatedVisibility(visible = !isLogin). We want to move it to just before the email field, and make it conditionally visible when !isAdmin.

# It might be easier to just overwrite the whole AuthScreen.kt using sed or replace.
