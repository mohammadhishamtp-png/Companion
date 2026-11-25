package com.adhdcompanion.focus.data.remote

import com.adhdcompanion.focus.data.prefs.SecurePrefsManager
import com.adhdcompanion.focus.model.BlockCategory
import com.adhdcompanion.focus.model.DayBlock
import com.adhdcompanion.focus.model.MicroStep
import com.adhdcompanion.focus.model.PlannerInput
import com.adhdcompanion.focus.model.UnblockerResponse
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * The AI Planner & Supporter. Every prompt sent from here carries the same
 * baked-in tone rules: warm, calm, peer-like, zero guilt-tripping, never an
 * overwhelming list, always one concrete low-friction physical action first.
 */
class GeminiRepository(private val prefs: SecurePrefsManager) {

    private val gson = Gson()

    private val api: GeminiApiService by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl(GeminiApiService.BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GeminiApiService::class.java)
    }

    private val primaryModel = "gemini-4-hydrogen"
    private val fallbackModel = "gemini-3.5-flash-lite"
    private val ttsModel = "gemini-3.8-flash-tts"

    private fun activeTextModel(): String =
        if (prefs.useFlashLiteFallbackModel) fallbackModel else primaryModel

    companion object {
        const val SYSTEM_PERSONA = """
You are a warm, calm, peer-like executive-dysfunction companion for someone
with ADHD who is trying to transition out of coding hyperfocus into studying.
Rules you always follow, no exceptions:
- Never lecture, never guilt-trip, never say things like "you should have" or "just focus".
- Never dump a huge list on someone who is overwhelmed or frozen.
- Always give exactly one concrete, tiny, physical first action before anything else.
- Keep language short, plain, conversational — like a supportive friend texting, not a coach.
- Assume the person is smart and capable; they are stuck, not broken.
- Keep every response strictly in the JSON schema you are given. No prose outside the JSON.
"""
    }

    // ---------------------------------------------------------------
    // Day planning
    // ---------------------------------------------------------------

    private data class PlannerBlockDto(
        val title: String,
        val why: String,
        @SerializedName("micro_step") val microStep: String,
        @SerializedName("start_minute_of_day") val startMinuteOfDay: Int,
        @SerializedName("duration_minutes") val durationMinutes: Int,
        val category: String,
        @SerializedName("is_decompression_buffer") val isDecompressionBuffer: Boolean = false
    )

    private data class PlannerDto(val blocks: List<PlannerBlockDto>)

    suspend fun generateDayPlan(input: PlannerInput): Result<List<DayBlock>> = withContext(Dispatchers.IO) {
        runCatching {
            val apiKey = requireApiKey()
            val prompt = """
Build a realistic, low-friction day schedule for someone with ADHD transitioning
from coding hyperfocus into studying.

Available hours today: ${input.availableHours}
Current energy level: ${input.energyLevel}
Goals / required blocks (meds, meditation, study topics, etc.): ${input.goalsAndBlocks}

Rules:
- Include short decompression buffers between demanding blocks (5-15 min), marked is_decompression_buffer=true.
- Each block needs a one-sentence personal "why" explaining why it matters right now, not generically.
- Each block needs one tiny physical micro_step to start it (e.g. "open the textbook to page 42 and read one paragraph").
- category must be one of: STUDY, CODING, MEDS, MEDITATION, MEAL, MOVEMENT, BUFFER, OTHER.
- start_minute_of_day is minutes since midnight (e.g. 9:30am = 570).
- Do not overload the day. Favor fewer, well-spaced blocks over a packed list.

Respond ONLY with JSON matching this schema:
{"blocks": [{"title": string, "why": string, "micro_step": string, "start_minute_of_day": int, "duration_minutes": int, "category": string, "is_decompression_buffer": bool}]}
"""
            val dto = callJson<PlannerDto>(prompt, apiKey)
            dto.blocks.map {
                DayBlock(
                    title = it.title,
                    why = it.why,
                    microStep = it.microStep,
                    startMinuteOfDay = it.startMinuteOfDay,
                    durationMinutes = it.durationMinutes,
                    category = runCatching { BlockCategory.valueOf(it.category.uppercase()) }
                        .getOrDefault(BlockCategory.OTHER),
                    isDecompressionBuffer = it.isDecompressionBuffer
                )
            }
        }
    }

    // ---------------------------------------------------------------
    // Stuck / Unblocker
    // ---------------------------------------------------------------

    private data class UnblockerDto(
        @SerializedName("calm_message") val calmMessage: String,
        @SerializedName("why_reminder") val whyReminder: String,
        @SerializedName("micro_steps") val microSteps: List<String>
    )

    suspend fun generateUnblockerResponse(
        whatTheyAreAvoiding: String,
        currentBlockWhy: String?
    ): Result<UnblockerResponse> = withContext(Dispatchers.IO) {
        runCatching {
            val apiKey = requireApiKey()
            val prompt = """
The user is stuck/frozen and avoiding their current task. Here is what they typed
or said about what they're avoiding or why they're stuck:

"$whatTheyAreAvoiding"

${if (currentBlockWhy != null) "Their current block's why is: \"$currentBlockWhy\"" else ""}

Respond with:
1. A short calm_message (1-3 sentences) that is non-judgmental and guilt-free.
2. A one-line why_reminder connecting back to their goal.
3. 3-5 ultra-small, purely physical micro_steps they can check off one at a time
   (e.g. "sit down at the desk", "open the notes app", "write one sentence").
   The first micro_step must be the smallest possible physical action.

Respond ONLY with JSON matching this schema:
{"calm_message": string, "why_reminder": string, "micro_steps": [string]}
"""
            val dto = callJson<UnblockerDto>(prompt, apiKey)
            UnblockerResponse(
                calmMessage = dto.calmMessage,
                whyReminder = dto.whyReminder,
                microSteps = dto.microSteps.mapIndexed { i, text -> MicroStep(id = i, text = text) }
            )
        }
    }

    // ---------------------------------------------------------------
    // Spoken briefing script (text only; SpeechManager handles audio)
    // ---------------------------------------------------------------

    suspend fun generateBriefingScript(
        currentBlock: DayBlock?,
        nextBlock: DayBlock?
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val apiKey = requireApiKey()
            val prompt = """
Write a short, warm, natural-sounding spoken briefing (like a caring friend
talking out loud, NOT a robotic announcement) covering:
- What the current block is (${currentBlock?.title ?: "nothing scheduled right now"})
- Why it matters right now (${currentBlock?.why ?: ""})
- The tiny first step to start (${currentBlock?.microStep ?: ""})
- A brief, light mention of what's coming next (${nextBlock?.title ?: "nothing scheduled yet"})

Keep it under 60 words, conversational, no bullet points, no headers — just
something pleasant to listen to out loud. Do not overwhelm with extra detail.

Respond ONLY with JSON matching this schema:
{"script": string}
"""
            val dto = callJson<BriefingDto>(prompt, apiKey)
            dto.script
        }
    }

    private data class BriefingDto(val script: String)

    // ---------------------------------------------------------------
    // Internal helpers
    // ---------------------------------------------------------------

    private suspend inline fun <reified T> callJson(prompt: String, apiKey: String): T {
        val request = GeminiRequest(
            contents = listOf(GeminiContent(role = "user", parts = listOf(GeminiPart(text = prompt)))),
            systemInstruction = GeminiContent(parts = listOf(GeminiPart(text = SYSTEM_PERSONA))),
            generationConfig = GeminiGenerationConfig(
                temperature = 0.8,
                maxOutputTokens = 1024,
                responseMimeType = "application/json"
            )
        )
        val model = activeTextModel()
        val response = try {
            api.generateContent(model, apiKey, request)
        } catch (primaryFailure: Exception) {
            // Automatic fallback to the lighter model if the primary one fails
            // (unavailable, rate-limited, or not yet enabled on the key).
            if (model != fallbackModel) {
                api.generateContent(fallbackModel, apiKey, request)
            } else {
                throw primaryFailure
            }
        }
        val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            ?: throw IllegalStateException("Gemini returned no content")
        return gson.fromJson(text, T::class.java)
    }

    private fun requireApiKey(): String {
        val key = prefs.geminiApiKey
        require(!key.isNullOrBlank()) { "No Gemini API key set. Add one in Settings." }
        return key
    }

    /**
     * Calls Gemini's TTS-capable model and returns raw base64 PCM/MP3 audio
     * bytes (mimeType included) for SpeechManager to play, or null if the
     * call fails for any reason — SpeechManager then falls back to the
     * built-in Android TextToSpeech engine automatically.
     */
    suspend fun generateSpeechAudio(text: String): GeminiInlineData? = withContext(Dispatchers.IO) {
        val key = prefs.geminiApiKey ?: return@withContext null
        runCatching {
            val request = GeminiRequest(
                contents = listOf(GeminiContent(role = "user", parts = listOf(GeminiPart(text = text)))),
                generationConfig = GeminiGenerationConfig(
                    temperature = null,
                    maxOutputTokens = null,
                    responseModalities = listOf("AUDIO"),
                    speechConfig = GeminiSpeechConfig(
                        voiceConfig = GeminiVoiceConfig(
                            prebuiltVoiceConfig = GeminiPrebuiltVoiceConfig(voiceName = "Kore")
                        )
                    )
                )
            )
            val response = api.generateSpeech(ttsModel, key, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.inlineData
        }.getOrNull()
    }
}
