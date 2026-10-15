import re

with open('app/src/main/java/com/example/ui/AppViewModel.kt', 'r') as f:
    content = f.read()

# Fix the login check so subscribed users don't get locked out
content = content.replace(
    "val isExpired = user.trialEndDate != null && System.currentTimeMillis() > user.trialEndDate",
    "val isExpired = user.trialEndDate != null && System.currentTimeMillis() > user.trialEndDate && !user.isSubscribed"
)

with open('app/src/main/java/com/example/ui/AppViewModel.kt', 'w') as f:
    f.write(content)
