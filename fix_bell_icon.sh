#!/bin/bash
sed -i 's/val hasUnreadNotifications = lowStockProducts.isNotEmpty() || isExpiringSoon/val hasUnreadNotifications = lowStockProducts.isNotEmpty() || isExpiringSoon || (userAccount?.role == "ADMIN" \&\& pendingFeedbacks.isNotEmpty()) || (userAccount?.role != "ADMIN" \&\& userRepliedFeedbacks.isNotEmpty())/g' app/src/main/java/com/example/ui/AppUI.kt
