#!/bin/bash
sed -i '/val lowStockProducts = products.filter { it.quantity <= it.minQuantityAlert }/a \
            val feedbacks by viewModel.allFeedbacks.collectAsState(initial = emptyList())\
            val pendingFeedbacks = feedbacks.filter { it.status == "جديد" || it.status == "قيد المراجعة" }\
            val userRepliedFeedbacks = feedbacks.filter { it.adminReply.isNotBlank() && it.status != "جديد" }' app/src/main/java/com/example/ui/AppUI.kt
