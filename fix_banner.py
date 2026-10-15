import re

with open('app/src/main/java/com/example/ui/AppUI.kt', 'r') as f:
    content = f.read()

bad_banner = """                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(AmberAlert)
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    "النسخة التجريبية (Trial) - يرجى تفعيل الاشتراك عبر الأدمن",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )"""

good_banner = """                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Brush.horizontalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.secondary
                                    )
                                ))
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Stars, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "نسخة تجريبية (Trial) ✨ استمتع بتجربة النظام الآن، ولا تنسَ تفعيل اشتراكك لضمان استمرار الخدمة.",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    modifier = Modifier.weight(1f).basicMarquee(
                                        iterations = Int.MAX_VALUE,
                                        velocity = 30.dp
                                    )
                                )"""

content = content.replace(bad_banner, good_banner)

with open('app/src/main/java/com/example/ui/AppUI.kt', 'w') as f:
    f.write(content)
