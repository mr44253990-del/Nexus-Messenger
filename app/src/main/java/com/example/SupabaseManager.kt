package com.example

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import java.util.UUID

object SupabaseManager {
    private const val SUPABASE_URL = "https://srfztgcdejfaesrvkarg.supabase.co"
    private const val SUPABASE_KEY = "sb_publishable_BcH2xwywnUCVG48LYjPOLQ_8-y2InGA"

    val client: SupabaseClient = createSupabaseClient(SUPABASE_URL, SUPABASE_KEY) {
        install(Storage)
        install(Postgrest)
    }

    suspend fun uploadFile(bucket: String, path: String, byteArray: ByteArray): String {
        val fileName = "${UUID.randomUUID()}_$path"
        client.storage.from(bucket).upload(fileName, byteArray)
        return client.storage.from(bucket).publicUrl(fileName)
    }

    suspend fun deleteFile(bucket: String, url: String) {
        val fileName = url.substringAfterLast("/")
        client.storage.from(bucket).delete(fileName)
    }

    suspend fun fetchTodoItems(): List<TodoItem> =
        client.from("todos")
            .select()
            .decodeList<TodoItem>()
}
