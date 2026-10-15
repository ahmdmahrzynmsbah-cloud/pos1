import re

with open('app/src/main/java/com/example/ui/AppViewModel.kt', 'r') as f:
    content = f.read()

content = content.replace('val defaultProj = "fox-tech-pos-cloud"', 'val defaultProj = "project-0085a85d-f44a-4bfb-966"')
content = content.replace('val defaultKey = "AIzaSyBwO7Z_FoxTechKey_201034859313"', 'val defaultKey = "AIzaSyAluuuhYseJLdaTyFUXnZ2OCg0QYkD9y-I"')
content = content.replace('val defaultUrl = "https://fox-tech-pos-default-rtdb.firebaseio.com"', 'val defaultUrl = "https://project-0085a85d-f44a-4bfb-966.firebaseio.com"')

# Also update the isDefaultKey check so it doesn't trigger the local fallback anymore, or we can just leave it since the key changed.
# Actually, the user wants to use REAL firebase now, so we should make sure the app uses it.
# The condition is: val isDefaultKey = apiKey.isBlank() || apiKey == "AIzaSyBwO7Z_FoxTechKey_201034859313" || apiKey.contains("FoxTechKey")
# This means with the new key, isDefaultKey will be false, and it will use real Firebase Auth and Firestore!

with open('app/src/main/java/com/example/ui/AppViewModel.kt', 'w') as f:
    f.write(content)
