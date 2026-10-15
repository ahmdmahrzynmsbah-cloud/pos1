#!/bin/bash
sed -i '/Text("لا توجد إشعارات أو تنبيهات نشطة حالياً/c \
                      if (userAccount?.role == "ADMIN" && pendingFeedbacks.isNotEmpty()) { \
                        Text("لديك ${pendingFeedbacks.size} طلبات/اقتراحات جديدة بانتظار المراجعة", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp) \
                      } else if (userAccount?.role != "ADMIN" && userRepliedFeedbacks.isNotEmpty()) { \
                        Text("يوجد ردود من الإدارة على ${userRepliedFeedbacks.size} من طلباتك", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp) \
                      } else { \
                        Text("لا توجد إشعارات أو تنبيهات نشطة حالياً.", color = Color.Gray, fontSize = 12.sp) \
                      }' app/src/main/java/com/example/ui/AppUI.kt
