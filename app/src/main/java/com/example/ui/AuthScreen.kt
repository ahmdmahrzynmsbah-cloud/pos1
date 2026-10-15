package com.example.ui

import androidx.compose.foundation.layout.systemBarsPadding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AuthScreen(viewModel: AppViewModel) {
  val storeName by viewModel.storeName.collectAsState("الكنز للمحاسبة")
  val storeLogoUrl by viewModel.storeLogoUrl.collectAsState("")
  var isLogin by remember { mutableStateOf(true) }
  var isAdmin by remember { mutableStateOf(false) }
  var email by remember { mutableStateOf("") }
  var phone by remember { mutableStateOf("") }
  var name by remember { mutableStateOf("") }
  var businessName by remember { mutableStateOf("") }
  var password by remember { mutableStateOf("") }
  var securityWord by remember { mutableStateOf("") }
  var passwordVisible by remember { mutableStateOf(false) }
  
  var showForgotPassword by remember { mutableStateOf(false) }
  var forgotEmail by remember { mutableStateOf("") }
  var forgotSecurityWord by remember { mutableStateOf("") }
  var newPassword by remember { mutableStateOf("") }
  var forgotError by remember { mutableStateOf<String?>(null) }
  
  var emailError by remember { mutableStateOf<String?>(null) }
  var passwordError by remember { mutableStateOf<String?>(null) }
  var nameError by remember { mutableStateOf<String?>(null) }
  var businessNameError by remember { mutableStateOf<String?>(null) }
  var phoneError by remember { mutableStateOf<String?>(null) }
  
  var selectedCountry by remember { mutableStateOf(com.example.data.allCountryCodes.find { it.code == "+970" } ?: com.example.data.allCountryCodes.first()) }
  var showCountryDialog by remember { mutableStateOf(false) }

  val authState by viewModel.authState.collectAsState()

  fun validate(): Boolean {
    var isValid = true
    emailError = null
    passwordError = null
    nameError = null
    businessNameError = null
    phoneError = null

    // Password validation
    val trimmedPassword = password.trim()
    if (password.isBlank()) {
      passwordError = "يرجى كتابة كلمة المرور"
      isValid = false
    } else if (password.length < 6) {
      passwordError = "كلمة المرور قصيرة جداً (يجب أن تتكون من 6 أحرف أو أرقام على الأقل)"
      isValid = false
    } else if (trimmedPassword.isEmpty()) {
      passwordError = "كلمة المرور لا يمكن أن تحتوي على مسافات فارغة فقط"
      isValid = false
    }

    // Admin email / username validation
    if (isAdmin) {
      val trimmedEmail = email.trim()
      if (trimmedEmail.isBlank()) {
        emailError = "يرجى إدخال اسم المستخدم أو البريد الإلكتروني للمدير"
        isValid = false
      } else if (trimmedEmail.contains("@") && !android.util.Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
        emailError = "صيغة البريد الإلكتروني غير صالحة (مثال: admin@example.com)"
        isValid = false
      } else if (!trimmedEmail.contains("@") && trimmedEmail.length < 3) {
        emailError = "اسم المستخدم قصير جداً (3 أحرف على الأقل)"
        isValid = false
      }
    } else {
      // Customer / Cashier phone validation
      val trimmedPhone = phone.trim()
      val cleanPhoneDigits = trimmedPhone.filter { it.isDigit() }
      if (trimmedPhone.isBlank()) {
        phoneError = "يرجى إدخال رقم الجوال"
        isValid = false
      } else if (cleanPhoneDigits.length != trimmedPhone.length) {
        phoneError = "رقم الجوال يجب أن يحتوي على أرقام فقط"
        isValid = false
      } else if (trimmedPhone.length < 8) {
        phoneError = "رقم الجوال قصير جداً (يجب ألا يقل عن 8 أرقام)"
        isValid = false
      } else if (trimmedPhone.length > 15) {
        phoneError = "رقم الجوال طويل جداً (الحد الأقصى 15 رقماً)"
        isValid = false
      }
    }

    // Registration extra fields validation
    if (!isLogin) {
      val trimmedName = name.trim()
      if (trimmedName.isBlank()) {
        nameError = "يرجى كتابة الاسم الكامل لصاحب الحساب"
        isValid = false
      } else if (trimmedName.length < 3) {
        nameError = "الاسم قصير جداً (يرجى إدخال الاسم الثلاثي أو الثنائي على الأقل)"
        isValid = false
      }

      val trimmedBusiness = businessName.trim()
      if (trimmedBusiness.isBlank()) {
        businessNameError = "يرجى كتابة اسم النشاط التجاري أو المحل"
        isValid = false
      } else if (trimmedBusiness.length < 2) {
        businessNameError = "اسم النشاط التجاري قصير جداً"
        isValid = false
      }
    }
    return isValid
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .systemBarsPadding(),
    contentAlignment = Alignment.Center
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier.verticalScroll(rememberScrollState())
    ) {
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .widthIn(max = 480.dp)
          .padding(horizontal = 24.dp, vertical = 16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
      ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        
        AppLogo(size = 80.dp, logoUrl = storeLogoUrl)

        Spacer(modifier = Modifier.height(24.dp))

        Text(
          storeName, 
          style = MaterialTheme.typography.headlineMedium, 
          color = MaterialTheme.colorScheme.primary, 
          fontWeight = FontWeight.Bold,
          textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
          if (isLogin) "مرحباً بك! الدخول مقتصر على المستخدمين المعتمدين من الأدمن" else "تقديم طلب فتح حساب لاعتماده وتفعيله من قِبل الأدمن", 
          style = MaterialTheme.typography.bodyMedium, 
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Prominent Mode Selector: Login vs Register
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .padding(4.dp),
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          Surface(
            onClick = {
              isLogin = true
              isAdmin = false
              emailError = null
              passwordError = null
              nameError = null
              businessNameError = null
              phoneError = null
              viewModel.resetAuthState()
            },
            shape = RoundedCornerShape(10.dp),
            color = if (isLogin) MaterialTheme.colorScheme.primary else Color.Transparent,
            modifier = Modifier.weight(1f)
          ) {
            Row(
              modifier = Modifier.padding(vertical = 10.dp),
              horizontalArrangement = Arrangement.Center,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                Icons.Filled.Lock,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (isLogin) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                "تسجيل الدخول",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = if (isLogin) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          Surface(
            onClick = {
              isLogin = false
              isAdmin = false
              emailError = null
              passwordError = null
              nameError = null
              businessNameError = null
              phoneError = null
              viewModel.resetAuthState()
            },
            shape = RoundedCornerShape(10.dp),
            color = if (!isLogin) MaterialTheme.colorScheme.primary else Color.Transparent,
            modifier = Modifier.weight(1f)
          ) {
            Row(
              modifier = Modifier.padding(vertical = 10.dp),
              horizontalArrangement = Arrangement.Center,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                Icons.Filled.Person,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (!isLogin) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                "طلب اعتماد حساب",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = if (!isLogin) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        AnimatedVisibility(visible = isLogin) {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 16.dp, vertical = 8.dp),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(
                  Icons.Filled.Lock,
                  contentDescription = null,
                  tint = if (isAdmin) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.size(20.dp)
                )
                Text("دخول كمدير النظام (الأدمن)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
              Switch(
                checked = isAdmin, 
                onCheckedChange = { 
                  isAdmin = it
                  emailError = null
                  passwordError = null
                  phoneError = null
                  if (it && email.isBlank()) {
                    email = "admin"
                  }
                },
                colors = SwitchDefaults.colors(
                  checkedThumbColor = MaterialTheme.colorScheme.primary,
                  checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                )
              )
            }

            if (isAdmin) {
              Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(
                  modifier = Modifier.padding(10.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  Icon(Icons.Filled.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                  Text(
                    "مدير النظام: اسم المستخدم: admin | كلمة المرور: mmnn12345",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                  )
                }
              }
            } else {
              Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(
                  modifier = Modifier.padding(10.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  Icon(Icons.Filled.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                  Text(
                    "التطبيق لا يقبل دخول أي مستخدم إلا بعد اعتماده وتفعيله حصراً بواسطة مدير النظام.",
                    fontSize = 10.sp,
                    lineHeight = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(24.dp))

        AnimatedVisibility(
          visible = !isLogin,
          enter = expandVertically(),
          exit = shrinkVertically()
        ) {
          Column {
            OutlinedTextField(
              value = businessName,
              onValueChange = { 
                businessName = it
                businessNameError = null
                if (authState !is AuthState.Idle && authState !is AuthState.Loading) viewModel.resetAuthState()
              },
              label = { Text("اسم النشاط التجاري") },
              leadingIcon = { Icon(Icons.Filled.Store, contentDescription = null) },
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(12.dp),
              singleLine = true,
              isError = businessNameError != null,
              supportingText = { if (businessNameError != null) Text(businessNameError!!) }
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
              value = name,
              onValueChange = { 
                name = it
                nameError = null
                if (authState !is AuthState.Idle && authState !is AuthState.Loading) viewModel.resetAuthState()
              },
              label = { Text("الاسم الكامل") },
              leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(12.dp),
              singleLine = true,
              isError = nameError != null,
              supportingText = { if (nameError != null) Text(nameError!!) }
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
              value = securityWord,
              onValueChange = { securityWord = it },
              label = { Text("كلمة سر الاستعادة (مهمة)") },
              leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(12.dp),
              singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))
          }
        }

        AnimatedVisibility(
          visible = !isAdmin,
          enter = expandVertically(),
          exit = shrinkVertically()
        ) {
          CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            OutlinedTextField(
              value = phone,
              onValueChange = { 
                phone = it
                phoneError = null
                if (authState !is AuthState.Idle && authState !is AuthState.Loading) viewModel.resetAuthState()
              },
              label = { 
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                  Text("رقم الجوال") 
                }
              },
              leadingIcon = { 
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier
                    .padding(start = 12.dp, end = 8.dp)
                    .clickable { showCountryDialog = true }
                ) {
                  Text(selectedCountry.code, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                  Spacer(modifier = Modifier.width(4.dp))
                  Icon(Icons.Filled.ArrowDropDown, contentDescription = "اختر الدولة")
                  Spacer(modifier = Modifier.width(6.dp))
                  Box(
                    modifier = Modifier
                      .height(24.dp)
                      .width(1.dp)
                      .background(MaterialTheme.colorScheme.outlineVariant)
                  )
                }
              },
              placeholder = { Text("59XXXXXXX", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(12.dp),
              singleLine = true,
              isError = phoneError != null,
              supportingText = { if (phoneError != null) Text(phoneError!!) }
            )
          }
        }

        AnimatedVisibility(
          visible = isAdmin,
          enter = expandVertically(),
          exit = shrinkVertically()
        ) {
          OutlinedTextField(
            value = email,
            onValueChange = { 
              email = it
              emailError = null
              if (authState !is AuthState.Idle && authState !is AuthState.Loading) viewModel.resetAuthState()
            },
            label = { Text("اسم المستخدم أو البريد الإلكتروني للمدير") },
            leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            isError = emailError != null,
            supportingText = { if (emailError != null) Text(emailError!!) }
          )
        }

        if (showCountryDialog) {
          var searchQuery by remember { mutableStateOf("") }
          androidx.compose.ui.window.Dialog(onDismissRequest = { showCountryDialog = false }) {
            Surface(
              shape = RoundedCornerShape(16.dp),
              color = MaterialTheme.colorScheme.surface,
              modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 500.dp)
            ) {
              Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                  value = searchQuery,
                  onValueChange = { searchQuery = it },
                  label = { Text("ابحث عن الدولة أو الرمز") },
                  modifier = Modifier.fillMaxWidth(),
                  leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                  singleLine = true,
                  shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                val filteredCountries = com.example.data.allCountryCodes.filter {
                  it.name.contains(searchQuery, ignoreCase = true) || it.code.contains(searchQuery)
                }
                androidx.compose.foundation.lazy.LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f, fill = false)) {
                  items(filteredCountries.size) { index ->
                    val country = filteredCountries[index]
                    Row(
                      modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                          selectedCountry = country
                          showCountryDialog = false
                        }
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Text(country.name, style = MaterialTheme.typography.bodyLarge)
                      }
                      Text(country.code, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    if (index < filteredCountries.size - 1) {
                      HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    }
                  }
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
          value = password,
          onValueChange = { 
            password = it
            passwordError = null
            if (authState !is AuthState.Idle && authState !is AuthState.Loading) viewModel.resetAuthState()
          },
          label = { Text(if (isAdmin) "كلمة مرور الأدمن (الافتراضية: mmnn12345)" else "كلمة المرور") },
          leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
          trailingIcon = {
            IconButton(onClick = { passwordVisible = !passwordVisible }) {
              Icon(
                imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                contentDescription = if (passwordVisible) "إخفاء كلمة المرور" else "إظهار كلمة المرور"
              )
            }
          },
          visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp),
          singleLine = true,
          isError = passwordError != null,
          supportingText = { if (passwordError != null) Text(passwordError!!) }
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        // Display Success State after Registration
        AnimatedVisibility(visible = authState is AuthState.RegistrationSuccess) {
          val successMsg = (authState as? AuthState.RegistrationSuccess)?.message ?: ""
          Column {
            Card(
              colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
              ),
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(12.dp),
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
            ) {
              Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.Center
                ) {
                  Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(
                    text = "تم إنشاء الحساب بنجاح!",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                  )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                  text = successMsg,
                  color = MaterialTheme.colorScheme.onPrimaryContainer,
                  style = MaterialTheme.typography.bodyMedium,
                  textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                  onClick = {
                    isLogin = true
                    viewModel.resetAuthState()
                  },
                  shape = RoundedCornerShape(8.dp),
                  colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                  Text("تسجيل الدخول الآن", fontWeight = FontWeight.Bold)
                }
              }
            }
            Spacer(modifier = Modifier.height(16.dp))
          }
        }

        // Display Error State
        AnimatedVisibility(visible = authState is AuthState.Error) {
          val msg = (authState as? AuthState.Error)?.message ?: ""
          Column {
            Card(
              colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f)
              ),
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(12.dp),
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
            ) {
              Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(
                  imageVector = Icons.Filled.ErrorOutline,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.error,
                  modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                  text = msg,
                  color = MaterialTheme.colorScheme.onErrorContainer,
                  style = MaterialTheme.typography.bodyMedium,
                  fontWeight = FontWeight.Medium
                )
              }
            }
            Spacer(modifier = Modifier.height(16.dp))
          }
        }

        if (authState is AuthState.Loading) {
          CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        } else {
          if (!isLogin) {
            Card(
              shape = RoundedCornerShape(12.dp),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
              modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            ) {
              Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Icon(
                  Icons.Filled.Lock,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(22.dp)
                )
                Text(
                  text = "تنبيه هام: التطبيق لا يقبل أي مستخدم إلا بعد موافقة واعتماد مدير النظام (الأدمن) حصراً.",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Medium,
                  lineHeight = 16.sp,
                  color = MaterialTheme.colorScheme.onSurface
                )
              }
            }
          }

          Button(
            onClick = {
              if (validate()) {
                val role = if (isAdmin) "ADMIN" else "CUSTOMER"
                val identifier = if (isAdmin) email else "${selectedCountry.code}${phone}"
                val actualEmail = if (identifier.contains("@")) identifier else "$identifier@smartpos.com"
                
                if (isLogin) {
                  viewModel.login(actualEmail, password, isAdmin)
                } else {
                  viewModel.register(name, actualEmail, identifier, password, role, businessName, securityWord)
                }
              }
            },
            modifier = Modifier
              .fillMaxWidth()
              .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
          ) {
            Text(
              if (isLogin) (if (isAdmin) "تسجيل الدخول كمدير النظام" else "تسجيل الدخول") else "إرسال طلب الحساب للاعتماد من الأدمن",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold
            )
          }

          if (isLogin) {
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = { showForgotPassword = true }) {
              Text("هل نسيت كلمة المرور؟")
            }
          }
        }

        if (showForgotPassword) {
          androidx.compose.ui.window.Dialog(onDismissRequest = { showForgotPassword = false }) {
            Surface(
              shape = RoundedCornerShape(20.dp),
              color = MaterialTheme.colorScheme.surface,
              tonalElevation = 6.dp,
              modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
              Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
              ) {
                Icon(
                  imageVector = Icons.Filled.Lock,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                  "استعادة كلمة المرور",
                  style = MaterialTheme.typography.titleLarge,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  "أدخل كلمة سر الاستعادة التي قمت بتحديدها أثناء إنشاء حسابك",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))
                OutlinedTextField(
                  value = forgotEmail,
                  onValueChange = { forgotEmail = it; forgotError = null },
                  label = { Text("البريد الإلكتروني أو رقم الهاتف") },
                  leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                  shape = RoundedCornerShape(12.dp),
                  modifier = Modifier.fillMaxWidth(),
                  singleLine = true
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                  value = forgotSecurityWord,
                  onValueChange = { forgotSecurityWord = it; forgotError = null },
                  label = { Text("كلمة سر الاستعادة") },
                  leadingIcon = { Icon(Icons.Filled.CheckCircle, contentDescription = null) },
                  shape = RoundedCornerShape(12.dp),
                  modifier = Modifier.fillMaxWidth(),
                  singleLine = true
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                  value = newPassword,
                  onValueChange = { newPassword = it; forgotError = null },
                  label = { Text("كلمة المرور الجديدة") },
                  leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                  shape = RoundedCornerShape(12.dp),
                  keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                  visualTransformation = PasswordVisualTransformation(),
                  modifier = Modifier.fillMaxWidth(),
                  singleLine = true
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                if (forgotError != null) {
                  Text(
                    forgotError!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                  )
                  Spacer(modifier = Modifier.height(12.dp))
                }
                
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                  OutlinedButton(
                    onClick = { showForgotPassword = false },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                  ) {
                    Text("إلغاء")
                  }
                  Button(
                    onClick = {
                      if (forgotEmail.isBlank() || forgotSecurityWord.isBlank() || newPassword.isBlank()) {
                        forgotError = "يرجى ملء جميع الحقول المطلوبة"
                      } else if (newPassword.trim().length < 6) {
                        forgotError = "كلمة المرور الجديدة يجب أن تكون 6 أحرف أو أرقام على الأقل"
                      } else {
                        viewModel.resetPassword(forgotEmail.trim(), forgotSecurityWord.trim(), newPassword.trim()) {
                          showForgotPassword = false
                          forgotEmail = ""
                          forgotSecurityWord = ""
                          newPassword = ""
                          viewModel.showToast("تم إعادة تعيين كلمة المرور بنجاح! يمكنك الدخول الآن")
                        }
                      }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1.5f)
                  ) {
                    Text("تأكيد وتغيير", fontWeight = FontWeight.Bold)
                  }
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        TextButton(
          onClick = { 
            isLogin = !isLogin 
            isAdmin = false
            emailError = null
            passwordError = null
            nameError = null
            businessNameError = null
            phoneError = null
            viewModel.resetAuthState()
          }
        ) {
          Text(
            if (isLogin) "ليس لديك حساب؟ إنشاء حساب جديد" else "لديك حساب بالفعل؟ تسجيل الدخول",
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
          )
        }
      }
    }
    
    val uriHandler = LocalUriHandler.current
    
    Spacer(modifier = Modifier.height(16.dp))
    
    // Luxury Branding & Support Frame
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 2.dp),
      shape = RoundedCornerShape(20.dp),
      colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
      ),
      border = BorderStroke(
        1.5.dp,
        MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
      ),
      elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        // Company Name & Website Link (Clickable)
        Surface(
          onClick = {
            try {
              uriHandler.openUri("https://www.daiftech.com")
            } catch (e: Exception) {
              viewModel.showToast("تعذر فتح الموقع")
            }
          },
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.primary.copy(alpha = 0.09f),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 10.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.Center
            ) {
              Icon(
                imageVector = Icons.Default.Business,
                contentDescription = "الشركة",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(19.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "إنتاج وتوزيع شركة الضيف للحلول التقنية",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
              )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.Center
            ) {
              Icon(
                imageVector = Icons.Default.Language,
                contentDescription = "موقع الشركة",
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(14.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "www.daiftech.com",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.secondary,
                textAlign = TextAlign.Center
              )
              Spacer(modifier = Modifier.width(4.dp))
              Icon(
                imageVector = Icons.Default.OpenInNew,
                contentDescription = "زيارة",
                tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f),
                modifier = Modifier.size(12.dp)
              )
            }
          }
        }

        // WhatsApp Numbers Section
        Text(
          text = "أرقام التواصل وخدمة العملاء عبر واتساب ",
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          // Number 1 (+970599678555)
          Surface(
            onClick = {
              try {
                uriHandler.openUri("https://wa.me/970599678555")
              } catch (e: Exception) {
                viewModel.showToast("تعذر فتح تطبيق واتساب")
              }
            },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF25D366).copy(alpha = 0.12f),
            border = BorderStroke(1.dp, Color(0xFF25D366).copy(alpha = 0.45f))
          ) {
            Row(
              modifier = Modifier.padding(vertical = 9.dp, horizontal = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.Center
            ) {
              Icon(
                imageVector = Icons.Default.Chat,
                contentDescription = "واتساب",
                tint = Color(0xFF25D366),
                modifier = Modifier.size(17.dp)
              )
              Spacer(modifier = Modifier.width(5.dp))
              Text(
                text = "+970599678555",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
              )
            }
          }

          // Number 2 (+970599272016)
          Surface(
            onClick = {
              try {
                uriHandler.openUri("https://wa.me/970599272016")
              } catch (e: Exception) {
                viewModel.showToast("تعذر فتح تطبيق واتساب")
              }
            },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF25D366).copy(alpha = 0.12f),
            border = BorderStroke(1.dp, Color(0xFF25D366).copy(alpha = 0.45f))
          ) {
            Row(
              modifier = Modifier.padding(vertical = 9.dp, horizontal = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.Center
            ) {
              Icon(
                imageVector = Icons.Default.Chat,
                contentDescription = "واتساب",
                tint = Color(0xFF25D366),
                modifier = Modifier.size(17.dp)
              )
              Spacer(modifier = Modifier.width(5.dp))
              Text(
                text = "+970599272016",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
              )
            }
          }
        }
      }
    }
    
    Spacer(modifier = Modifier.height(20.dp))
  }
}
}
