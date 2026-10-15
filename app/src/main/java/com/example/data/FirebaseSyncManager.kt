package com.example.data

import android.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

object FirebaseSyncManager {
    private const val TAG = "FirebaseSyncManager"
    
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    data class AuthResult(val uid: String, val idToken: String)

    /**
     * Authenticates with Firebase Auth via REST API.
     */
    fun signIn(apiKey: String, email: String, pass: String): AuthResult {
        val url = "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=$apiKey"
        val jsonPayload = JSONObject().apply {
            put("email", email)
            put("password", pass)
            put("returnSecureToken", true)
        }.toString()

        val request = Request.Builder()
            .url(url)
            .post(jsonPayload.toRequestBody(JSON_MEDIA_TYPE))
            .build()

        client.newCall(request).execute().use { response ->
            val body = response.body?.string() ?: throw IOException("Empty response body")
            if (!response.isSuccessful) {
                val errorMsg = try {
                    JSONObject(body).getJSONObject("error").getString("message")
                } catch (e: Exception) {
                    "خطأ غير معروف في المصادقة"
                }
                throw IOException(translateAuthError(errorMsg))
            }
            val jsonObj = JSONObject(body)
            return AuthResult(
                uid = jsonObj.getString("localId"),
                idToken = jsonObj.getString("idToken")
            )
        }
    }

    /**
     * Registers a new user with Firebase Auth via REST API.
     */
    fun signUp(apiKey: String, email: String, pass: String): AuthResult {
        val url = "https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=$apiKey"
        val jsonPayload = JSONObject().apply {
            put("email", email)
            put("password", pass)
            put("returnSecureToken", true)
        }.toString()

        val request = Request.Builder()
            .url(url)
            .post(jsonPayload.toRequestBody(JSON_MEDIA_TYPE))
            .build()

        client.newCall(request).execute().use { response ->
            val body = response.body?.string() ?: throw IOException("Empty response body")
            if (!response.isSuccessful) {
                val errorMsg = try {
                    JSONObject(body).getJSONObject("error").getString("message")
                } catch (e: Exception) {
                    "خطأ أثناء إنشاء الحساب بالسحابة"
                }
                throw IOException(translateAuthError(errorMsg))
            }
            val jsonObj = JSONObject(body)
            return AuthResult(
                uid = jsonObj.getString("localId"),
                idToken = jsonObj.getString("idToken")
            )
        }
    }

    /**
     * Translate Firebase REST errors to friendly, specific Arabic messages.
     */
    fun translateAuthError(error: String): String {
        val upper = error.uppercase()
        return when {
            upper.contains("INVALID_LOGIN_CREDENTIALS") || upper.contains("INVALID_PASSWORD") -> "كلمة المرور غير صحيحة، يرجى التأكد وإعادة المحاولة"
            upper.contains("EMAIL_NOT_FOUND") || upper.contains("USER_NOT_FOUND") -> "رقم الجوال أو الحساب غير مسجل لدينا، يرجى إنشاء حساب جديد"
            upper.contains("EMAIL_EXISTS") -> "رقم الجوال أو الحساب مسجل بالفعل، يرجى تسجيل الدخول مباشرة"
            upper.contains("USER_DISABLED") -> "تم إيقاف هذا الحساب من قبل الإدارة"
            upper.contains("WEAK_PASSWORD") -> "كلمة المرور قصيرة جداً (يجب أن تتكون من 6 أحرف أو أرقام على الأقل)"
            upper.contains("INVALID_EMAIL") -> "صيغة رقم الجوال أو البريد غير صالحة"
            upper.contains("TOO_MANY_ATTEMPTS") -> "تم إيقاف المحاولات مؤقتاً بسبب تكرار كلمة المرور الخاطئة. يرجى الانتظار دقيقة والمحاولة ثانية"
            upper.contains("OPERATION_NOT_ALLOWED") -> "تسجيل الدخول بهذه الطريقة غير مفعل حالياً"
            upper.contains("NETWORK") || upper.contains("TIMEOUT") || upper.contains("UNABLE TO RESOLVE") -> "تعذر الاتصال بالخادم، يرجى التحقق من اتصال الإنترنت"
            else -> "حدث خطأ أثناء المصادقة: $error"
        }
    }

    /**
     * Helper to map a flat key-value map into a Firestore document structure.
     */
    private fun mapToFirestoreJson(map: Map<String, Any?>): JSONObject {
        val fieldsJson = JSONObject()
        for ((key, value) in map) {
            if (value == null) continue
            val valueJson = JSONObject()
            when (value) {
                is String -> valueJson.put("stringValue", value)
                is Boolean -> valueJson.put("booleanValue", value)
                is Int -> valueJson.put("integerValue", value.toString())
                is Long -> valueJson.put("integerValue", value.toString())
                is Double -> valueJson.put("doubleValue", value)
                is Float -> valueJson.put("doubleValue", value.toDouble())
                else -> valueJson.put("stringValue", value.toString())
            }
            fieldsJson.put(key, valueJson)
        }
        return JSONObject().put("fields", fieldsJson)
    }

    /**
     * Helper to parse Firestore document fields back to a flat Map<String, Any?>.
     */
    private fun firestoreJsonToMap(docObj: JSONObject): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>()
        if (docObj.has("fields")) {
            val fields = docObj.getJSONObject("fields")
            val keys = fields.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val valObj = fields.getJSONObject(key)
                when {
                    valObj.has("stringValue") -> map[key] = valObj.getString("stringValue")
                    valObj.has("booleanValue") -> map[key] = valObj.getBoolean("booleanValue")
                    valObj.has("integerValue") -> map[key] = valObj.getString("integerValue").toLong()
                    valObj.has("doubleValue") -> map[key] = valObj.getDouble("doubleValue")
                }
            }
        }
        return map
    }

    /**
     * Checks if Firebase configuration is missing or set to dummy/placeholder defaults.
     */
    fun isPlaceholderConfig(projectId: String?, apiKey: String?): Boolean {
        val proj = projectId?.trim().orEmpty()
        val key = apiKey?.trim().orEmpty()
        return proj.isBlank() || key.isBlank() ||
               proj == "project-0085a85d-f44a-4bfb-966" ||
               proj == "fox-tech-pos-cloud" ||
               proj.contains("placeholder") ||
               key == "AIzaSyAluuuhYseJLdaTyFUXnZ2OCg0QYkD9y-I" ||
               key == "AIzaSyBwO7Z_FoxTechKey_201034859313" ||
               key.contains("FoxTechKey")
    }

    /**
     * Uploads/Overwrites a single document in a collection.
     */
    fun saveDocument(projectId: String, apiKey: String, collection: String, docId: String, data: Map<String, Any?>, idToken: String? = null) {
        val url = "https://firestore.googleapis.com/v1/projects/$projectId/databases/(default)/documents/$collection/$docId?key=$apiKey"
        val payload = mapToFirestoreJson(data).toString()

        val requestBuilder = Request.Builder()
            .url(url)
            .patch(payload.toRequestBody(JSON_MEDIA_TYPE))

        if (!idToken.isNullOrBlank()) {
            requestBuilder.addHeader("Authorization", "Bearer $idToken")
        }

        val request = requestBuilder.build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val body = response.body?.string() ?: ""
                Log.w(TAG, "Firestore sync: Could not save document $docId in $collection (Status: ${response.code}). Security rules or authorization may be required.")
                throw IOException("فشل حفظ البيانات سحابياً: ${response.code}")
            }
        }
    }

    /**
     * Deletes a single document in a collection.
     */
    fun deleteDocument(projectId: String, apiKey: String, collection: String, docId: String, idToken: String? = null) {
        val url = "https://firestore.googleapis.com/v1/projects/$projectId/databases/(default)/documents/$collection/$docId?key=$apiKey"
        val requestBuilder = Request.Builder()
            .url(url)
            .delete()

        if (!idToken.isNullOrBlank()) {
            requestBuilder.addHeader("Authorization", "Bearer $idToken")
        }

        val request = requestBuilder.build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful && response.code != 404) {
                val body = response.body?.string() ?: ""
                Log.w(TAG, "Firestore sync: Could not delete document $docId in $collection (Status: ${response.code})")
                throw IOException("فشل حذف البيانات من السحابة: ${response.code}")
            }
        }
    }

    /**
     * Downloads all documents in a Firestore collection.
     */
    fun getCollection(projectId: String, apiKey: String, collection: String, idToken: String? = null): List<Pair<String, Map<String, Any?>>> {
        val url = "https://firestore.googleapis.com/v1/projects/$projectId/databases/(default)/documents/$collection?key=$apiKey"
        val requestBuilder = Request.Builder()
            .url(url)
            .get()

        if (!idToken.isNullOrBlank()) {
            requestBuilder.addHeader("Authorization", "Bearer $idToken")
        }

        val request = requestBuilder.build()

        client.newCall(request).execute().use { response ->
            val body = response.body?.string() ?: return emptyList()
            if (!response.isSuccessful) {
                if (response.code == 404) return emptyList()
                Log.w(TAG, "Firestore sync: Could not fetch collection $collection (Status: ${response.code}). Security rules or authorization may be required.")
                throw IOException("فشل جلب البيانات السحابية: ${response.code}")
            }
            
            val result = mutableListOf<Pair<String, Map<String, Any?>>>()
            val jsonObj = JSONObject(body)
            if (jsonObj.has("documents")) {
                val docsArray = jsonObj.getJSONArray("documents")
                for (i in 0 until docsArray.length()) {
                    val docObj = docsArray.getJSONObject(i)
                    val fullPath = docObj.getString("name")
                    val docId = fullPath.substringAfterLast("/")
                    val fieldsMap = firestoreJsonToMap(docObj)
                    result.add(Pair(docId, fieldsMap))
                }
            }
            return result
        }
    }
}

// Extension and helper methods to convert models to/from flat maps for Firestore REST API
fun AuditLog.toMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "timestamp" to timestamp,
    "employeeName" to employeeName,
    "action" to action,
    "details" to details
)

fun auditLogFromMap(id: Int, map: Map<String, Any?>): AuditLog = AuditLog(
    id = id,
    timestamp = (map["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis(),
    employeeName = map["employeeName"] as? String ?: "",
    action = map["action"] as? String ?: "",
    details = map["details"] as? String ?: ""
)

fun SupplierTransaction.toMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "supplierId" to supplierId,
    "date" to date,
    "transactionType" to transactionType,
    "amount" to amount,
    "note" to note
)

fun supplierTransactionFromMap(id: Int, map: Map<String, Any?>): SupplierTransaction = SupplierTransaction(
    id = id,
    supplierId = (map["supplierId"] as? Number)?.toInt() ?: 0,
    date = (map["date"] as? Number)?.toLong() ?: System.currentTimeMillis(),
    transactionType = map["transactionType"] as? String ?: "شراء",
    amount = (map["amount"] as? Number)?.toDouble() ?: 0.0,
    note = map["note"] as? String ?: ""
)

fun InvoiceItem.toMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "invoiceId" to invoiceId,
    "productId" to productId,
    "productNameAr" to productNameAr,
    "productNameEn" to productNameEn,
    "price" to price,
    "quantity" to quantity,
    "total" to total
)

fun invoiceItemFromMap(id: Int, map: Map<String, Any?>): InvoiceItem = InvoiceItem(
    id = id,
    invoiceId = (map["invoiceId"] as? Number)?.toInt() ?: 0,
    productId = (map["productId"] as? Number)?.toInt() ?: 0,
    productNameAr = map["productNameAr"] as? String ?: "",
    productNameEn = map["productNameEn"] as? String ?: "",
    price = (map["price"] as? Number)?.toDouble() ?: 0.0,
    quantity = (map["quantity"] as? Number)?.toInt() ?: 0,
    total = (map["total"] as? Number)?.toDouble() ?: 0.0
)

fun Product.toMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "nameAr" to nameAr,
    "nameEn" to nameEn,
    "barcode" to barcode,
    "pricePurchase" to pricePurchase,
    "priceSale" to priceSale,
    "quantity" to quantity,
    "unit" to unit,
    "minQuantityAlert" to minQuantityAlert,
    "category" to category,
    "imageResName" to imageResName
)

fun productFromMap(id: Int, map: Map<String, Any?>): Product = Product(
    id = id,
    nameAr = map["nameAr"] as? String ?: "",
    nameEn = map["nameEn"] as? String ?: "",
    barcode = map["barcode"] as? String ?: "",
    pricePurchase = (map["pricePurchase"] as? Number)?.toDouble() ?: 0.0,
    priceSale = (map["priceSale"] as? Number)?.toDouble() ?: 0.0,
    quantity = (map["quantity"] as? Number)?.toInt() ?: 0,
    unit = map["unit"] as? String ?: "",
    minQuantityAlert = (map["minQuantityAlert"] as? Number)?.toInt() ?: 0,
    category = map["category"] as? String ?: "",
    imageResName = map["imageResName"] as? String ?: ""
)

fun Invoice.toMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "customerId" to customerId,
    "invoiceDate" to invoiceDate,
    "subtotal" to subtotal,
    "discount" to discount,
    "taxAmount" to taxAmount,
    "total" to total,
    "paymentMethod" to paymentMethod,
    "paidAmount" to paidAmount,
    "changeAmount" to changeAmount,
    "cashierName" to cashierName,
    "branchName" to branchName,
    "isPurchase" to isPurchase
)

fun invoiceFromMap(id: Int, map: Map<String, Any?>): Invoice = Invoice(
    id = id,
    customerId = (map["customerId"] as? Number)?.toInt(),
    invoiceDate = (map["invoiceDate"] as? Number)?.toLong() ?: System.currentTimeMillis(),
    subtotal = (map["subtotal"] as? Number)?.toDouble() ?: 0.0,
    discount = (map["discount"] as? Number)?.toDouble() ?: 0.0,
    taxAmount = (map["taxAmount"] as? Number)?.toDouble() ?: 0.0,
    total = (map["total"] as? Number)?.toDouble() ?: 0.0,
    paymentMethod = map["paymentMethod"] as? String ?: "",
    paidAmount = (map["paidAmount"] as? Number)?.toDouble() ?: 0.0,
    changeAmount = (map["changeAmount"] as? Number)?.toDouble() ?: 0.0,
    cashierName = map["cashierName"] as? String ?: "",
    branchName = map["branchName"] as? String ?: "",
    isPurchase = map["isPurchase"] as? Boolean ?: false
)

fun Customer.toMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "name" to name,
    "phone" to phone,
    "email" to email,
    "points" to points,
    "type" to type
)

fun customerFromMap(id: Int, map: Map<String, Any?>): Customer = Customer(
    id = id,
    name = map["name"] as? String ?: "",
    phone = map["phone"] as? String ?: "",
    email = map["email"] as? String ?: "",
    points = (map["points"] as? Number)?.toInt() ?: 0,
    type = map["type"] as? String ?: "عادي"
)

fun SystemUser.toMap(): Map<String, Any?> = mapOf(
    "uid" to uid,
    "name" to name,
    "email" to email,
    "phone" to phone,
    "role" to role,
    "registrationDate" to registrationDate,
    "trialEndDate" to trialEndDate,
    "isSubscribed" to isSubscribed,
    "isActive" to isActive,
    "storeName" to storeName,
    "storePhone" to storePhone,
    "storeVatNumber" to storeVatNumber,
    "currentBranch" to currentBranch,
    "currencySymbol" to currencySymbol,
    "cashierName" to cashierName,
    "cashierPhone" to cashierPhone,
    "cashierAddress" to cashierAddress,
    "canAccessReports" to canAccessReports,
    "canAccessInventory" to canAccessInventory,
    "canAccessAccounting" to canAccessAccounting,
    "canAccessAdvancedTools" to canAccessAdvancedTools,
    "canAddProducts" to canAddProducts,
    "canDeleteProducts" to canDeleteProducts,
    "canApplyDiscounts" to canApplyDiscounts,
    "canViewProfits" to canViewProfits
)

fun systemUserFromMap(uid: String, map: Map<String, Any?>): SystemUser = SystemUser(
    uid = uid,
    name = map["name"] as? String ?: "",
    email = map["email"] as? String ?: "",
    phone = map["phone"] as? String ?: "",
    role = map["role"] as? String ?: "CUSTOMER",
    registrationDate = (map["registrationDate"] as? Number)?.toLong() ?: System.currentTimeMillis(),
    trialEndDate = (map["trialEndDate"] as? Number)?.toLong(),
    isSubscribed = map["isSubscribed"] as? Boolean ?: false,
    isActive = map["isActive"] as? Boolean ?: true,
    storeName = map["storeName"] as? String ?: "مؤسسة الكنز للمحاسبة التجارية",
    storePhone = map["storePhone"] as? String ?: (map["phone"] as? String ?: ""),
    storeVatNumber = map["storeVatNumber"] as? String ?: "",
    currentBranch = (map["currentBranch"] as? String ?: "الفرع الرئيسي").replace(" (الرياض)", ""),
    currencySymbol = map["currencySymbol"] as? String ?: "شيكل",
    cashierName = map["cashierName"] as? String ?: "كاشير المبيعات",
    cashierPhone = map["cashierPhone"] as? String ?: (map["phone"] as? String ?: ""),
    cashierAddress = (map["cashierAddress"] as? String ?: "").replace("الرياض، المملكة العربية السعودية", "").replace("الرياض", "").trim(),
    canAccessReports = map["canAccessReports"] as? Boolean ?: true,
    canAccessInventory = map["canAccessInventory"] as? Boolean ?: true,
    canAccessAccounting = map["canAccessAccounting"] as? Boolean ?: true,
    canAccessAdvancedTools = map["canAccessAdvancedTools"] as? Boolean ?: false,
    canAddProducts = map["canAddProducts"] as? Boolean ?: true,
    canDeleteProducts = map["canDeleteProducts"] as? Boolean ?: false,
    canApplyDiscounts = map["canApplyDiscounts"] as? Boolean ?: false,
    canViewProfits = map["canViewProfits"] as? Boolean ?: false
)

