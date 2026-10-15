package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CoralError
import com.example.ui.theme.EmeraldGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminSettingsScreen(
    viewModel: AppViewModel,
    onBackClick: () -> Unit = {},
    onNavigate: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val storeName by viewModel.storeName.collectAsState()
    val storePhone by viewModel.storePhone.collectAsState()
    val storeVatNumber by viewModel.storeVatNumber.collectAsState()
    val storeLogoUrl by viewModel.storeLogoUrl.collectAsState()
    val supportWhatsappNumber by viewModel.supportWhatsappNumber.collectAsState()
    val subscriptionBannerText by viewModel.subscriptionBannerText.collectAsState()
    val currentBranch by viewModel.currentBranch.collectAsState()
    val currencySymbol by viewModel.currencySymbol.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val cashierPhone by viewModel.cashierPhone.collectAsState()
    val cashierAddress by viewModel.cashierAddress.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val branchesList by viewModel.branches.collectAsState(emptyList())

    // Local editable state
    var editStoreName by remember(storeName) { mutableStateOf(storeName) }
    var editStorePhone by remember(storePhone) { mutableStateOf(storePhone) }
    var editStoreVatNumber by remember(storeVatNumber) { mutableStateOf(storeVatNumber) }
    var editStoreLogoUrl by remember(storeLogoUrl) { mutableStateOf(storeLogoUrl) }
    var editSupportWhatsapp by remember(supportWhatsappNumber) { mutableStateOf(supportWhatsappNumber) }
    var editBannerText by remember(subscriptionBannerText) { mutableStateOf(subscriptionBannerText) }
    var editCurrencySymbol by remember(currencySymbol) { mutableStateOf(currencySymbol) }
    var editCashierName by remember(currentUser) { mutableStateOf(currentUser) }
    var editCashierPhone by remember(cashierPhone) { mutableStateOf(cashierPhone) }
    var editCashierAddress by remember(cashierAddress) { mutableStateOf(cashierAddress) }

    val presetLogos = listOf(
        Triple("preset:diamond", "الجوهرة الذهبية (افتراضي)", Icons.Default.Diamond),
        Triple("preset:store", "المتجر الملكي", Icons.Default.Storefront),
        Triple("preset:account", "المؤسسة المالية", Icons.Default.AccountBalance),
        Triple("preset:calc", "المحاسبة الذكية", Icons.Default.Calculate),
        Triple("preset:building", "إدارة الأعمال", Icons.Default.Business),
        Triple("preset:crown", "النجمة الذهبية", Icons.Default.Stars)
    )

    val commonCurrencies = listOf(
        "شيكل" to "₪",
        "ريال" to "ر.س",
        "دولار" to "$",
        "جنيه" to "ج.م",
        "دينار أردني" to "د.أ",
        "درهم إماراتي" to "د.إ",
        "دينار كويتي" to "د.ك",
        "ريال عماني" to "ر.ع",
        "ريال قطري" to "ر.ق",
        "دينار بحريني" to "د.ب",
        "ليرة تركية" to "₺",
        "يورو" to "€"
    )

    fun applyAndSaveAll() {
        viewModel.storeName.value = editStoreName
        viewModel.storePhone.value = editStorePhone
        viewModel.storeVatNumber.value = editStoreVatNumber
        viewModel.storeLogoUrl.value = editStoreLogoUrl
        viewModel.currentUser.value = editCashierName
        viewModel.cashierPhone.value = editCashierPhone
        viewModel.cashierAddress.value = editCashierAddress
        viewModel.updateCurrencySymbol(editCurrencySymbol.ifBlank { "شيكل" })
        viewModel.updateSupportWhatsappNumber(editSupportWhatsapp)
        viewModel.updateSubscriptionBannerText(editBannerText)
        viewModel.saveUserSettings()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Brush, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("تخصيص هوية وإعدادات التطبيق", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "رجوع")
                    }
                },
                actions = {
                    Button(
                        onClick = { applyAndSaveAll() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("حفظ", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(MaterialTheme.colorScheme.primary, shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Palette, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("لوحة تخصيص النظام والهوية التجارية", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("قم بضبط اسم المنشأة، الشعار، العملة الافتراضية، المظهر، نصوص الإعلانات وقوالب الفواتير لتناسب علامتك التجارية بدقة.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f), lineHeight = 16.sp)
                    }
                }
            }

            // SECTION 1: Store & Brand Identity
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Storefront, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("الهوية التجارية وبيانات المنشأة", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Store Name Field
                    OutlinedTextField(
                        value = editStoreName,
                        onValueChange = { editStoreName = it },
                        label = { Text("اسم المتجر / المؤسسة التجارية:") },
                        placeholder = { Text("مثال: مؤسسة الكنز للمحاسبة والتجارة") },
                        leadingIcon = { Icon(Icons.Default.Business, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Logo Selection
                    Text("شعار وأيقونة التطبيق (Logo Branding):", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    
                    // Live Logo Preview Box
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        AppLogo(size = 56.dp, logoUrl = editStoreLogoUrl)
                        Column(modifier = Modifier.weight(1f)) {
                            Text("المعاينة المباشرة للشعار", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(
                                if (editStoreLogoUrl.startsWith("preset:")) "أيقونة جاهزة: ${presetLogos.find { it.first == editStoreLogoUrl }?.second ?: "مخصصة"}"
                                else if (editStoreLogoUrl.isNotBlank()) "شعار مخصص عبر الرابط"
                                else "الجوهرة الذهبية الافتراضية",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                    }

                    // Preset Logo Chooser
                    Text("اختر نمط الأيقونة والشعار:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(presetLogos) { (presetKey, label, icon) ->
                            val isSelected = editStoreLogoUrl == presetKey || (presetKey == "preset:diamond" && editStoreLogoUrl.isBlank())
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent),
                                modifier = Modifier
                                    .clickable { editStoreLogoUrl = presetKey }
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    AppLogo(size = 36.dp, logoUrl = presetKey)
                                    Text(label, fontSize = 10.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                }
                            }
                        }
                    }

                    // Custom URL Logo Input
                    OutlinedTextField(
                        value = if (editStoreLogoUrl.startsWith("preset:")) "" else editStoreLogoUrl,
                        onValueChange = { editStoreLogoUrl = it },
                        label = { Text("أو أدخل رابط صورة شعار مخصص (URL):") },
                        placeholder = { Text("https://example.com/logo.png") },
                        leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        supportingText = { Text("إذا تُرك فارغاً أو تم اختيار نمط أعلاه، سيتم استخدام الأيقونة الجاهزة.") }
                    )

                    // Phone & VAT
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = editStorePhone,
                            onValueChange = { editStorePhone = it },
                            label = { Text("هاتف المنشأة:") },
                            placeholder = { Text("05xxxxxxxx") },
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = editStoreVatNumber,
                            onValueChange = { editStoreVatNumber = it },
                            label = { Text("الرقم الضريبي:") },
                            placeholder = { Text("300xxxxxxx") },
                            leadingIcon = { Icon(Icons.Default.Pin, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // SECTION 2: Currency & Financial Configuration
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.AttachMoney, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("العملة والخيارات المالية", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    Text("العملة الافتراضية المعتمدة في النظام:", fontWeight = FontWeight.Bold, fontSize = 12.sp)

                    // Currency Quick Selector Chips
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(commonCurrencies) { (name, symbol) ->
                            val isSelected = editCurrencySymbol == name || editCurrencySymbol == symbol
                            FilterChip(
                                selected = isSelected,
                                onClick = { editCurrencySymbol = name },
                                label = { Text("$name ($symbol)", fontSize = 11.sp) },
                                leadingIcon = if (isSelected) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                } else null
                            )
                        }
                    }

                    // Custom Currency Input
                    OutlinedTextField(
                        value = editCurrencySymbol,
                        onValueChange = { editCurrencySymbol = it },
                        label = { Text("رمز أو اسم العملة المخصص:") },
                        placeholder = { Text("مثال: شيكل أو ₪ أو ريال") },
                        leadingIcon = { Icon(Icons.Default.MonetizationOn, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Live Price Preview Box
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("معاينة شكل المبالغ في الفواتير والتقارير:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                            Text(
                                "1,450.00 $editCurrencySymbol",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // SECTION 3: Appearance & Theme Customization
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.ColorLens, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("المظهر والثيم (Theme & UI)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Dark Mode Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(
                                if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                                contentDescription = null,
                                tint = if (isDarkMode) Color(0xFFFFD54F) else MaterialTheme.colorScheme.primary
                            )
                            Column {
                                Text("الوضع الليلي (Dark Mode)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("تفعيل النمط الداكن المريح للعين في جميع شاشات التطبيق", fontSize = 10.sp, color = Color.Gray)
                            }
                        }
                        Switch(
                            checked = isDarkMode,
                            onCheckedChange = { viewModel.toggleDarkMode(it) }
                        )
                    }

                    // Font & Typography Info
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.TextFields, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                        Text("الخط المعتمد للنظام: IBM Plex Sans Arabic (عالي الوضوح والقراءة)", fontSize = 11.sp, color = Color.Gray)
                    }
                }
            }

            // SECTION 4: Subscription & Announcement Ticker
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Campaign, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("شريط الإعلانات والاشتراكات المتحرك", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    OutlinedTextField(
                        value = editBannerText,
                        onValueChange = { editBannerText = it },
                        label = { Text("نص الشريط المتحرك (يظهر أعلى شاشات التطبيق):") },
                        placeholder = { Text("أدخل الجملة أو الإعلان الذي يظهر للمستخدمين...") },
                        leadingIcon = { Icon(Icons.Default.Announcement, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )

                    // Live Banner Preview Box
                    Text("معاينة حية للشريط المتحرك:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFFFFD54F)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = Color(0xFFF57F17), modifier = Modifier.size(20.dp))
                            Text(
                                text = editBannerText.ifBlank { viewModel.defaultSubscriptionBannerText },
                                fontSize = 11.sp,
                                color = Color(0xFFE65100),
                                fontWeight = FontWeight.Medium,
                                maxLines = 2
                            )
                        }
                    }

                    // WhatsApp Support Number
                    OutlinedTextField(
                        value = editSupportWhatsapp,
                        onValueChange = { editSupportWhatsapp = it },
                        label = { Text("رقم واتساب الدعم الفني وتنشيط الاشتراكات:") },
                        placeholder = { Text("201034859313") },
                        leadingIcon = { Icon(Icons.Default.SupportAgent, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        supportingText = { Text("يستخدم في نافذة تفعيل الحسابات والتواصل مع المشرف.") }
                    )

                    // Reset to default button
                    TextButton(
                        onClick = {
                            editBannerText = viewModel.defaultSubscriptionBannerText
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("استعادة النص الترحيبي الافتراضي", fontSize = 11.sp)
                    }
                }
            }

            // SECTION 5: Receipt & Invoice Print Customization
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("تخصيص طباعة الفواتير والإيصالات", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    OutlinedTextField(
                        value = editCashierName,
                        onValueChange = { editCashierName = it },
                        label = { Text("اسم الكاشير / البائع الافتراضي:") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editCashierPhone,
                        onValueChange = { editCashierPhone = it },
                        label = { Text("هاتف الكاشير / نقطة البيع:") },
                        leadingIcon = { Icon(Icons.Default.PhoneIphone, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editCashierAddress,
                        onValueChange = { editCashierAddress = it },
                        label = { Text("عنوان المنشأة / الفرع المطبوع بالفاتورة:") },
                        placeholder = { Text("مثال: شارع الملك فيصل - الفرع الرئيسي") },
                        leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Receipt Preview Sample Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("=== ترويسة الفاتورة التجريبية ===", fontSize = 10.sp, color = Color.Gray)
                            Text(editStoreName.ifBlank { "مؤسسة الكنز للمحاسبة" }, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            if (editStoreVatNumber.isNotBlank()) {
                                Text("الرقم الضريبي: $editStoreVatNumber", fontSize = 11.sp)
                            }
                            if (editStorePhone.isNotBlank()) {
                                Text("هاتف: $editStorePhone", fontSize = 11.sp)
                            }
                            if (editCashierAddress.isNotBlank()) {
                                Text(editCashierAddress, fontSize = 10.sp, color = Color.Gray)
                            }
                            Text("--------------------------------", fontSize = 10.sp, color = Color.Gray)
                            Text("شكراً لتعاملكم معنا ونسعد بخدمتكم دائماً", fontSize = 11.sp, color = EmeraldGreen, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            // SECTION 6: Branches & Quick Admin Hub
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("روابط الإدارة المتقدمة والفروع", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Current Active Branch Selector
                    var expandedBranch by remember { mutableStateOf(false) }
                    Text("الفرع النشط حالياً:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { expandedBranch = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Store, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("الفرع الحالي: $currentBranch ", fontWeight = FontWeight.Bold)
                        }
                        DropdownMenu(
                            expanded = expandedBranch,
                            onDismissRequest = { expandedBranch = false }
                        ) {
                            branchesList.forEach { b ->
                                DropdownMenuItem(
                                    text = { Text(b) },
                                    onClick = {
                                        viewModel.currentBranch.value = b
                                        expandedBranch = false
                                        viewModel.showToast("تم تحويل الفرع النشط إلى: $b")
                                    }
                                )
                            }
                        }
                    }

                    // Admin Quick Links Grid
                    Text("الانتقال السريع لأقسام المشرف:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { onNavigate("admin_branches") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Store, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("إدارة الفروع", fontSize = 11.sp)
                        }
                        OutlinedButton(
                            onClick = { onNavigate("admin_firebase") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("السحابة والمزامنة", fontSize = 11.sp)
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { onNavigate("admin_users") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.People, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("إدارة المشتركين", fontSize = 11.sp)
                        }
                        OutlinedButton(
                            onClick = { onNavigate("backup_restore") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("النسخ الاحتياطي", fontSize = 11.sp)
                        }
                    }
                }
            }

            // Admin Security & Password Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("أمان مدير النظام وكلمة المرور", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    Text(
                        "كلمة مرور الأدمن المعتمدة للتحكم الكامل بالنظام وإدارة المستخدمين والمشتركين.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val currentAdminPass by viewModel.adminPassword.collectAsState()
                    var editAdminPass by remember(currentAdminPass) { mutableStateOf(currentAdminPass) }
                    var passVisible by remember { mutableStateOf(false) }

                    OutlinedTextField(
                        value = editAdminPass,
                        onValueChange = { editAdminPass = it },
                        label = { Text("كلمة مرور الأدمن (الحالية: mmnn12345)") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { passVisible = !passVisible }) {
                                Icon(
                                    imageVector = if (passVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null
                                )
                            }
                        },
                        visualTransformation = if (passVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = { viewModel.updateAdminPassword(editAdminPass.trim()) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تحديث كلمة مرور الأدمن", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // BIG SAVE BUTTON
            Button(
                onClick = { applyAndSaveAll() },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("حفظ وتطبيق جميع التعديلات والتخصيصات ", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
