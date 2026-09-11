package com.ishhf.almanara

import com.google.firebase.firestore.FirebaseFirestore

/** يفحص جوجل فايرستور إذا في نسخة أجدد من التطبيق، ويرجع رابط تحميلها إذا في */
object UpdateChecker {

    fun check(currentVersionCode: Int, onUpdateAvailable: (String, String) -> Unit, onUpToDate: () -> Unit) {
        FirebaseFirestore.getInstance().collection("app_version").document("current")
            .get()
            .addOnSuccessListener { doc ->
                val latestVersionCode = doc.getLong("versionCode")?.toInt() ?: currentVersionCode
                val latestVersionName = doc.getString("versionName") ?: ""
                val apkUrl = doc.getString("apkUrl") ?: ""
                if (latestVersionCode > currentVersionCode && apkUrl.isNotEmpty()) {
                    onUpdateAvailable(latestVersionName, apkUrl)
                } else {
                    onUpToDate()
                }
            }
            .addOnFailureListener {
                onUpToDate()
            }
    }
}
