package com.eenth.blocker

import android.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

sealed class PairResult {
    object Success : PairResult()
    object AlreadyTakenByOther : PairResult()
    object AlreadyPairedToThis : PairResult()
    object NotApproved : PairResult()
    data class Error(val message: String) : PairResult()
}

sealed class VerifyResult {
    object Valid : VerifyResult()
    object NotRegistered : VerifyResult()
    object WrongDevice : VerifyResult()
    data class Error(val message: String) : VerifyResult()
}

class TagRepository {

    companion object {
        private const val SUPABASE_URL = "https://xaggbhcuddhvplxgcszb.supabase.co"
        private const val SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InhhZ2diaGN1ZGRodnBseGdjc3piIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODM4NzQ0NjAsImV4cCI6MjA5OTQ1MDQ2MH0.WX235Jy7WF4kFIvSXB8PyoYoEojbO8brEBI7H4wSdhY"
        private const val TAG = "TagRepository"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val jsonType = "application/json".toMediaType()

    /**
     * Try to pair a tag to this device.
     * - If tag isn't registered: registers it → Success
     * - If tag is registered to this device: AlreadyPairedToThis
     * - If tag is registered to another device: AlreadyTakenByOther
     */
    fun pairTag(tagUid: String, deviceId: String): PairResult {
        return try {
            // Check if tag is in the approved list
            if (!isTagApproved(tagUid)) {
                Log.d(TAG, "Tag not approved: $tagUid")
                return PairResult.NotApproved
            }

            // First check if tag is already registered
            val existing = getRegistration(tagUid)

            if (existing != null) {
                val existingDevice = existing.optString("device_id", "")
                if (existingDevice == deviceId) {
                    PairResult.AlreadyPairedToThis
                } else {
                    PairResult.AlreadyTakenByOther
                }
            } else {
                // Register the tag
                val body = JSONObject().apply {
                    put("tag_uid", tagUid)
                    put("device_id", deviceId)
                    put("tag_name", "My Brick")
                }.toString()

                val request = Request.Builder()
                    .url("$SUPABASE_URL/rest/v1/tag_registrations")
                    .addHeader("apikey", SUPABASE_KEY)
                    .addHeader("Authorization", "Bearer $SUPABASE_KEY")
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "return=minimal")
                    .post(body.toRequestBody(jsonType))
                    .build()

                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    Log.d(TAG, "Tag paired on server: $tagUid → $deviceId")
                    PairResult.Success
                } else {
                    val errorBody = response.body?.string() ?: "Unknown error"
                    Log.e(TAG, "Pair failed: ${response.code} $errorBody")
                    // Could be a duplicate key conflict (race condition)
                    if (response.code == 409) {
                        PairResult.AlreadyTakenByOther
                    } else {
                        PairResult.Error("Server error: ${response.code}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Pair error: ${e.message}")
            PairResult.Error("Network error: ${e.message}")
        }
    }

    /**
     * Verify a tag belongs to this device.
     */
    fun verifyTag(tagUid: String, deviceId: String): VerifyResult {
        return try {
            val existing = getRegistration(tagUid)

            if (existing == null) {
                VerifyResult.NotRegistered
            } else {
                val existingDevice = existing.optString("device_id", "")
                if (existingDevice == deviceId) {
                    VerifyResult.Valid
                } else {
                    VerifyResult.WrongDevice
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Verify error: ${e.message}")
            VerifyResult.Error("Network error: ${e.message}")
        }
    }

    /**
     * Unpair a tag (only works if device_id matches via RLS).
     */
    fun unpairTag(tagUid: String, deviceId: String): Boolean {
        return try {
            val request = Request.Builder()
                .url("$SUPABASE_URL/rest/v1/tag_registrations?tag_uid=eq.$tagUid")
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer $SUPABASE_KEY")
                .addHeader("x-device-id", deviceId)
                .delete()
                .build()

            val response = client.newCall(request).execute()
            Log.d(TAG, "Unpair response: ${response.code}")
            response.isSuccessful
        } catch (e: Exception) {
            Log.e(TAG, "Unpair error: ${e.message}")
            false
        }
    }

    /**
     * Update tag name on server.
     */
    fun updateTagName(tagUid: String, deviceId: String, tagName: String): Boolean {
        return try {
            val body = JSONObject().apply {
                put("tag_name", tagName)
            }.toString()

            val request = Request.Builder()
                .url("$SUPABASE_URL/rest/v1/tag_registrations?tag_uid=eq.$tagUid")
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer $SUPABASE_KEY")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=minimal")
                .addHeader("x-device-id", deviceId)
                .patch(body.toRequestBody(jsonType))
                .build()

            val response = client.newCall(request).execute()
            Log.d(TAG, "Update tag name response: ${response.code}")
            response.isSuccessful
        } catch (e: Exception) {
            Log.e(TAG, "Update tag name error: ${e.message}")
            false
        }
    }

    private fun getRegistration(tagUid: String): JSONObject? {
        val request = Request.Builder()
            .url("$SUPABASE_URL/rest/v1/tag_registrations?tag_uid=eq.$tagUid&select=*")
            .addHeader("apikey", SUPABASE_KEY)
            .addHeader("Authorization", "Bearer $SUPABASE_KEY")
            .get()
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) return null

        val body = response.body?.string() ?: return null
        val arr = JSONArray(body)
        return if (arr.length() > 0) arr.getJSONObject(0) else null
    }

    private fun isTagApproved(tagUid: String): Boolean {
        return try {
            val request = Request.Builder()
                .url("$SUPABASE_URL/rest/v1/approved_tags?tag_uid=eq.$tagUid&select=tag_uid")
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer $SUPABASE_KEY")
                .get()
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.e(TAG, "Approved check failed: ${response.code}")
                return false // Deny if check fails
            }

            val body = response.body?.string() ?: return false
            val arr = JSONArray(body)
            arr.length() > 0
        } catch (e: Exception) {
            Log.e(TAG, "Approved check error: ${e.message}")
            false // Deny if offline/error
        }
    }

    fun registerApprovedTag(tagUid: String): Boolean {
        return try {
            val body = JSONObject().apply {
                put("tag_uid", tagUid)
            }.toString()

            val request = Request.Builder()
                .url("$SUPABASE_URL/rest/v1/approved_tags")
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer $SUPABASE_KEY")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=minimal")
                .post(body.toRequestBody(jsonType))
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful || response.code == 409) {
                Log.d(TAG, "Tag approved: $tagUid (${response.code})")
                true
            } else {
                Log.e(TAG, "Approve failed: ${response.code} ${response.body?.string()}")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Approve error: ${e.message}")
            false
        }
    }
}
