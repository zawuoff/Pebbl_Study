package com.fouwaz.studypal.data.repository

import com.fouwaz.studypal.data.remote.api.RetrofitClient
import com.fouwaz.studypal.data.remote.model.ChatMessage
import com.fouwaz.studypal.data.remote.model.ChatRequest
import com.fouwaz.studypal.domain.model.FinalDraftConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AiRepository {

    private val api = RetrofitClient.openRouterApi

    private val modelName = "google/gemini-2.5-flash-lite"

    private fun parseThreeQuestions(response: String): List<String> {
        val lines = response.trim().lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        val questions = mutableListOf<String>()

        for (line in lines) {
            val cleaned = line
                .replace(Regex("^\\d+[.):]\\s*"), "")
                .replace(Regex("^[-*•]\\s*"), "")
                .trim()

            if (cleaned.isNotEmpty() && cleaned.contains("?")) {
                questions.add(cleaned)
            }
        }

        if (questions.size != 3) {
            questions.clear()
            for (line in lines) {
                if (line.contains("?")) {
                    questions.add(line.replace(Regex("^\\d+[.):]\\s*"), "").trim())
                }
                if (questions.size == 3) break
            }
        }

        while (questions.size < 3) {
            questions.add("Can you elaborate more on this idea?")
        }

        return questions.take(3)
    }

    suspend fun generateFollowUpQuestions(transcribedText: String): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val messages = listOf(
                ChatMessage(
                    role = "system",
                    content = """
                        You are an academic scholar guiding a student through their own essay or thesis development.
                        You must never write content for the student; your sole purpose is to pose questions that advance their thinking.
                        Rely exclusively on the supplied transcript for context.
                        Produce exactly three thoughtful questions, numbered 1 through 3.
                        Focus primarily on deepening the user's established ideas around structural cognitive change and pragmatic adaptation via intentional redesign.
                        One question may gently steer toward a closely related facet if it supports comprehensive coverage.
                        Keep every question concise, precise, and strictly on topic.
                    """.trimIndent()
                ),
                ChatMessage(
                    role = "user",
                    content = """
                        Based on the transcript below, provide exactly three numbered follow-up questions that help refine or explore adjacent ideas within the established framework of structural cognitive change and pragmatic adaptation.
                        
                        Transcript:
                        $transcribedText
                        
                        Guidelines:
                        - Stay anchored to the student's wording and intent.
                        - Keep questions academically grounded, actionable, and concise.
                        - Do not introduce new topics or examples absent from the transcript.
                    """.trimIndent()
                )
            )

            val request = ChatRequest(
                model = modelName,
                messages = messages,
                temperature = 0.6,
                maxTokens = 300,
                topP = 0.9
            )

            val response = api.createChatCompletion(request)

            if (response.error != null) {
                Result.failure(Exception(response.error.message))
            } else if (response.choices.isNotEmpty()) {
                val raw = response.choices.first().message.content.trim()
                Result.success(parseThreeQuestions(raw))
            } else {
                Result.failure(Exception("No response from AI"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun generateDraft(
        conversation: List<Pair<String, String?>>,
        config: FinalDraftConfig
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val conversationText = buildString {
                conversation.forEachIndexed { index, (response, question) ->
                    append("Exchange ${index + 1}:\n")
                    if (!question.isNullOrBlank()) {
                        append("AI Question: $question\n")
                    }
                    append("Student Response: $response\n\n")
                }
            }

            val additionalInstructions = buildString {
                if (config.includeSummary) {
                    appendLine("- Begin with a single, well-formed summary paragraph (2-3 sentences) that clearly states the student's core idea.")
                }
                appendLine("- Arrange the main body into complete paragraphs that cover every idea from the student, ending with a concluding paragraph that ties the discussion together.")
                if (config.includeHighlights) {
                    appendLine("- Finish with a section titled \"Key Takeaways:\" followed by exactly three bullet points (use \"- \" as the bullet) written as full sentences.")
                }
                append("- Ensure the response ends with a complete sentence and never stops mid bullet or paragraph.")
            }

            val systemPrompt = """
                You are an academic writing assistant who polishes student speech into well-structured drafts without introducing new ideas.
                Follow every rule exactly:
                - Preserve all original meaning, data, and examples from the student.
                - Only improve grammar, clarity, transitions, and paragraph structure.
                - Maintain a ${config.tone.promptDescription}.
                - Apply a ${config.refinementLevel.promptDescription}.
                - Target approximately ${config.wordGoal} words; stay within 10% of this goal while prioritizing complete thoughts.
                - Never end mid-sentence, mid-paragraph, or mid-list item.
                - Obey any required sections such as summaries or key takeaways precisely as instructed.
                - Do not invent new content.
            """.trimIndent()

            val userPrompt = buildString {
                appendLine("Use the conversation below to create the final draft:")
                appendLine(conversationText.trim())
                appendLine()
                appendLine("Instructions:")
                appendLine("- Organize ideas into coherent paragraphs with smooth transitions and a definitive conclusion.")
                appendLine("- Rephrase for clarity but do not add new ideas or external knowledge.")
                appendLine("- Use the AI questions purely as context to understand what the student was responding to; the draft must reflect only the student's contributions.")
                appendLine("- Keep the student's voice while polishing tone and fluency.")
                appendLine("- Stay within ±10% of ${config.wordGoal} words; expand or compress wording as needed to finish every section cleanly.")
                if (additionalInstructions.isNotBlank()) {
                    appendLine(additionalInstructions.trim())
                }
                appendLine("- Before returning the answer, double-check that the draft satisfies every requested section and ends cleanly.")
                append("Return the polished draft as plain text without front matter or metadata.")
            }

            val maxTokens = ((config.wordGoal * 2.2).toInt()).coerceIn(600, 3500)

            val request = ChatRequest(
                model = modelName,
                messages = listOf(
                    ChatMessage(role = "system", content = systemPrompt),
                    ChatMessage(role = "user", content = userPrompt)
                ),
                temperature = 0.2,
                maxTokens = maxTokens,
                topP = 0.9
            )

            val response = api.createChatCompletion(request)

            if (response.error != null) {
                Result.failure(Exception(response.error.message))
            } else if (response.choices.isNotEmpty()) {
                Result.success(response.choices.first().message.content.trim())
            } else {
                Result.failure(Exception("No draft generated"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    data class CombinedLectureOutputs(
        val overview: String,
        val notes: String,
        val summary: String
    )

    suspend fun generateAllOutputs(transcription: String): Result<CombinedLectureOutputs> = withContext(Dispatchers.IO) {
        try {
            android.util.Log.d("AiRepository", "Generating all outputs for transcription of ${transcription.length} chars")

            // Calculate dynamic token limits based on transcription length
            // Rule of thumb: ~4 characters per token for English text
            val transcriptionTokens = transcription.length / 4

            // Allocate tokens dynamically:
            // - Overview: fixed 400 tokens (always same length)
            // - Notes: 60% of output tokens (main content)
            // - Summary: 30% of output tokens (condensed version)
            // - Buffer: 10% for formatting and tags

            // Estimate output needs based on input size
            // For comprehensive coverage: aim for ~50% of input tokens for all outputs
            val baseOutputTokens = (transcriptionTokens * 0.5).toInt()

            val overviewTokens = 400
            val notesTokens = (baseOutputTokens * 0.6).toInt()
            val summaryTokens = (baseOutputTokens * 0.3).toInt()
            val bufferTokens = (baseOutputTokens * 0.1).toInt()

            val totalMaxTokens = (overviewTokens + notesTokens + summaryTokens + bufferTokens)
                .coerceIn(3000, 16000)  // Min 3k, max 16k (API limit)

            android.util.Log.d("AiRepository", "Dynamic token allocation - Transcription: ~$transcriptionTokens tokens, Output: $totalMaxTokens tokens (Overview: $overviewTokens, Notes: $notesTokens, Summary: $summaryTokens)")

            val systemPrompt = """
                You are an expert educational assistant that helps students understand their lectures.
                You will receive a lecture transcription and must generate three different outputs:
                1. Overview (10-11 lines)
                2. Notes (structured markdown)
                3. Summary (comprehensive understanding)

                Critical Rules for ALL outputs:
                - Base ALL content ONLY on the lecture transcription provided
                - Do NOT add new ideas, interpretations, or external knowledge
                - Do NOT invent examples or concepts not mentioned in the lecture
                - Stay strictly within the content boundaries of the transcription
                - Focus on helping students understand what was said, not adding new information
                - For long lectures, ensure you cover ALL major topics and sections - don't skip content
                - Prioritize completeness over verbosity

                IMPORTANT: You MUST return the outputs in this EXACT format:

                [OVERVIEW]
                (10-11 line overview here)
                [/OVERVIEW]

                [NOTES]
                (structured notes in markdown here - cover the ENTIRE lecture)
                [/NOTES]

                [SUMMARY]
                (comprehensive summary here - cover key points from the ENTIRE lecture)
                [/SUMMARY]

                Do not include any other text outside these sections.
            """.trimIndent()

            val userPrompt = """
                Based on the lecture transcription below, generate three outputs following the EXACT format specified:

                1. OVERVIEW (10-11 lines): A brief description of what the lecture covered. Simply state what topics were discussed without explaining concepts in detail.

                2. NOTES (structured markdown): Break the lecture into clear sections using:
                   - Bullet points and subheadings showing hierarchy
                   - Key definitions, examples, or data from the lecture
                   - Brief reflective cues like "→ this connects to..." or "→ key takeaway:"
                   - Keep notes short, legible, and cognitively helpful
                   - IMPORTANT: Cover the ENTIRE lecture, not just the beginning

                3. SUMMARY (comprehensive): A clear summary that:
                   - Highlights main concepts, key arguments, and supporting evidence
                   - Uses simple, clear language with short paragraphs or bullet points
                   - Ends with a brief "What this means" reflection
                   - Helps students understand what was actually said
                   - IMPORTANT: Include key points from the ENTIRE lecture

                Lecture Transcription:
                $transcription

                Remember to use the EXACT format with [OVERVIEW], [NOTES], and [SUMMARY] tags.
            """.trimIndent()

            val request = ChatRequest(
                model = modelName,
                messages = listOf(
                    ChatMessage(role = "system", content = systemPrompt),
                    ChatMessage(role = "user", content = userPrompt)
                ),
                temperature = 0.2,
                maxTokens = totalMaxTokens,  // Dynamic token limit based on input length
                topP = 0.9
            )

            android.util.Log.d("AiRepository", "Sending combined outputs request to API")
            val response = api.createChatCompletion(request)

            if (response.error != null) {
                android.util.Log.e("AiRepository", "API error for combined outputs: ${response.error.message}")
                Result.failure(Exception(response.error.message))
            } else if (response.choices.isNotEmpty()) {
                val content = response.choices.first().message.content.trim()
                android.util.Log.d("AiRepository", "Combined outputs generated successfully: ${content.length} chars")

                // Parse the combined output
                val overviewRegex = """\[OVERVIEW\](.*?)\[/OVERVIEW\]""".toRegex(RegexOption.DOT_MATCHES_ALL)
                val notesRegex = """\[NOTES\](.*?)\[/NOTES\]""".toRegex(RegexOption.DOT_MATCHES_ALL)
                val summaryRegex = """\[SUMMARY\](.*?)\[/SUMMARY\]""".toRegex(RegexOption.DOT_MATCHES_ALL)

                val overview = overviewRegex.find(content)?.groupValues?.get(1)?.trim()
                val notes = notesRegex.find(content)?.groupValues?.get(1)?.trim()
                val summary = summaryRegex.find(content)?.groupValues?.get(1)?.trim()

                if (overview != null && notes != null && summary != null) {
                    android.util.Log.d("AiRepository", "Parsed outputs - Overview: ${overview.length} chars, Notes: ${notes.length} chars, Summary: ${summary.length} chars")
                    Result.success(CombinedLectureOutputs(overview, notes, summary))
                } else {
                    android.util.Log.e("AiRepository", "Failed to parse combined outputs. Overview: ${overview != null}, Notes: ${notes != null}, Summary: ${summary != null}")
                    Result.failure(Exception("Failed to parse combined outputs from AI response"))
                }
            } else {
                android.util.Log.e("AiRepository", "No combined outputs generated - empty choices")
                Result.failure(Exception("No outputs generated"))
            }
        } catch (e: Exception) {
            android.util.Log.e("AiRepository", "Exception generating combined outputs", e)
            Result.failure(e)
        }
    }

    suspend fun generateOverview(transcription: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            android.util.Log.d("AiRepository", "Generating overview for transcription of ${transcription.length} chars")
            val systemPrompt = """
                You are an expert at creating concise overviews of lecture content.
                Your task is to briefly describe what the lecture was about in exactly 10-11 lines.

                Critical Rules:
                - Base your overview ONLY on the transcribed lecture content
                - Do NOT add new ideas, interpretations, or external knowledge
                - Do NOT invent examples or concepts not mentioned in the lecture
                - Simply state what topics were covered in the lecture
                - Be factual and descriptive, not explanatory
                - Write in clear, simple sentences
                - Each line should be a complete sentence
                - Focus on WHAT was discussed, not WHY or HOW
                - If the lecture is very long, ensure you cover the full scope across all 10-11 lines
                - Stay strictly within the content boundaries of the provided transcription
            """.trimIndent()

            val userPrompt = """
                Based solely on the lecture transcription below, write a brief 10-11 line overview describing what the lecture covered.

                Do NOT explain concepts in detail. Do NOT add interpretations. Simply state what topics and ideas were discussed.

                Lecture Transcription:
                $transcription

                Return exactly 10-11 lines describing what the lecture was about.
            """.trimIndent()

            val request = ChatRequest(
                model = modelName,
                messages = listOf(
                    ChatMessage(role = "system", content = systemPrompt),
                    ChatMessage(role = "user", content = userPrompt)
                ),
                temperature = 0.2,
                maxTokens = 400,
                topP = 0.85
            )

            android.util.Log.d("AiRepository", "Sending overview request to API")
            val response = api.createChatCompletion(request)

            if (response.error != null) {
                android.util.Log.e("AiRepository", "API error for overview: ${response.error.message}")
                Result.failure(Exception(response.error.message))
            } else if (response.choices.isNotEmpty()) {
                val content = response.choices.first().message.content.trim()
                android.util.Log.d("AiRepository", "Overview generated successfully: ${content.length} chars")
                Result.success(content)
            } else {
                android.util.Log.e("AiRepository", "No overview generated - empty choices")
                Result.failure(Exception("No overview generated"))
            }
        } catch (e: Exception) {
            android.util.Log.e("AiRepository", "Exception generating overview", e)
            Result.failure(e)
        }
    }

    suspend fun generateFlashcards(transcription: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val systemPrompt = """
                You are a thoughtful learning assistant. Your goal is to convert lecture content into student-friendly, comprehension-based flashcards that reinforce understanding rather than memorization.

                Format each flashcard as:
                Q: [Question]
                A: [Answer]

                Critical Rules:
                - Contain only information directly stated in the lecture (no outside content)
                - Focus on core ideas, key terms, and cause-effect relationships
                - Use simple Q&A format, where:
                  * The question helps the student recall or explain a concept
                  * The answer is concise and uses the lecture's phrasing or meaning
                - Include a few conceptual understanding cards, e.g., "What does this imply?" or "Why is this important?" — but only if the reasoning was present in the lecture
                - Do NOT add trivia-style questions or memorization that doesn't serve comprehension
                - Do NOT add new ideas, concepts, or external knowledge not mentioned in the lecture
                - Do NOT invent examples or explanations beyond what was discussed
                - Base ALL content ONLY on the lecture transcription provided
                - The aim is to help the student actively recall and connect what they've already learned — not to introduce anything new
                - Create 10-15 flashcards to cover important concepts
                - Stay strictly within the content boundaries of the transcription
            """.trimIndent()

            val userPrompt = """
                Convert this lecture transcript or summary into flashcards that:
                - Contain only information directly stated in the lecture (no outside content)
                - Focus on core ideas, key terms, and cause-effect relationships
                - Use simple Q&A format, where:
                  * The question helps the student recall or explain a concept
                  * The answer is concise and uses the lecture's phrasing or meaning
                - Include a few conceptual understanding cards, e.g., "What does this imply?" or "Why is this important?" — but only if the reasoning was present in the lecture
                - Avoid trivia-style questions or memorization that doesn't serve comprehension

                The aim is to help the student actively recall and connect what they've already learned — not to introduce anything new.

                Lecture Transcription:
                $transcription

                Return the flashcards in the Q:/A: format specified above.
            """.trimIndent()

            val request = ChatRequest(
                model = modelName,
                messages = listOf(
                    ChatMessage(role = "system", content = systemPrompt),
                    ChatMessage(role = "user", content = userPrompt)
                ),
                temperature = 0.3,
                maxTokens = 2000,
                topP = 0.9
            )

            val response = api.createChatCompletion(request)

            if (response.error != null) {
                Result.failure(Exception(response.error.message))
            } else if (response.choices.isNotEmpty()) {
                Result.success(response.choices.first().message.content.trim())
            } else {
                Result.failure(Exception("No flashcards generated"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun generateNotes(transcription: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            android.util.Log.d("AiRepository", "Generating notes for transcription of ${transcription.length} chars")
            val systemPrompt = """
                You are a reflective study companion. Your goal is to help students make sense of what they heard, not to add new information.
                Help the student organize the lecture into understandable, connected notes — like a personal knowledge map, not a rewritten essay.

                Critical Rules:
                - Base ALL content ONLY on the lecture transcription provided
                - Break the lecture into clear sections or topics
                - Use bullet points, subheadings, and indentation to show hierarchy and flow
                - Emphasize key definitions, examples, or data that the lecturer used
                - Include brief reflective cues such as "→ this connects to…" or "→ key takeaway:" to help the student think about relationships between ideas
                - Do NOT add any content that was not explicitly mentioned in the lecture
                - Do NOT add new ideas, external examples, or assumptions
                - Keep the notes short, legible, and cognitively helpful
                - The goal is to make understanding and recall easier, not to decorate or rewrite the lecture
                - Use markdown formatting (headings, bold, bullets, indentation)
                - Stay strictly within the content boundaries of the transcription
            """.trimIndent()

            val userPrompt = """
                Based on this lecture transcript, create structured notes that:
                - Break the lecture into clear sections or topics
                - Use bullet points, subheadings, and indentation to show hierarchy and flow
                - Emphasize key definitions, examples, or data that the lecturer used
                - Include brief reflective cues such as "→ this connects to…" or "→ key takeaway:" to help the student think about relationships between ideas
                - Avoid adding any content that was not explicitly mentioned in the lecture

                Keep the notes short, legible, and cognitively helpful — the goal is to make understanding and recall easier, not to decorate or rewrite the lecture.

                Lecture Transcription:
                $transcription

                Return well-structured notes using markdown formatting.
            """.trimIndent()

            val request = ChatRequest(
                model = modelName,
                messages = listOf(
                    ChatMessage(role = "system", content = systemPrompt),
                    ChatMessage(role = "user", content = userPrompt)
                ),
                temperature = 0.2,
                maxTokens = 4000,  // Increased from 1500 to ensure full notes
                topP = 0.9
            )

            android.util.Log.d("AiRepository", "Sending notes request to API")
            val response = api.createChatCompletion(request)

            if (response.error != null) {
                android.util.Log.e("AiRepository", "API error for notes: ${response.error.message}")
                Result.failure(Exception(response.error.message))
            } else if (response.choices.isNotEmpty()) {
                val content = response.choices.first().message.content.trim()
                android.util.Log.d("AiRepository", "Notes generated successfully: ${content.length} chars")
                Result.success(content)
            } else {
                android.util.Log.e("AiRepository", "No notes generated - empty choices")
                Result.failure(Exception("No notes generated"))
            }
        } catch (e: Exception) {
            android.util.Log.e("AiRepository", "Exception generating notes", e)
            Result.failure(e)
        }
    }

    suspend fun generateDetailedNotes(transcription: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val systemPrompt = """
                You are an expert at creating comprehensive, detailed summaries from lecture content.
                Transform the lecture transcription into a thorough, well-explained detailed summary.

                Critical Rules:
                - Base ALL content ONLY on the lecture transcription provided
                - Do NOT add new ideas, concepts, or external knowledge not mentioned in the lecture
                - Do NOT invent examples or explanations beyond what was discussed
                - You MAY add your own interpretation to explain concepts more clearly
                - You MAY rephrase and clarify ideas to make them easier to understand
                - You MAY NOT introduce new topics or ideas not present in the lecture
                - Include ALL concepts, examples, and explanations from the lecture
                - If the lecture is very long, ensure the detailed summary covers everything
                - Use clear structure with headings and subheadings
                - Explain key points thoroughly to aid understanding
                - Aim for 600-1000 words
                - Use markdown formatting (headings, bold, bullets, numbered lists)
                - Ensure completeness while maintaining clarity
                - Think of this as explaining the lecture content in a clearer, more organized way
            """.trimIndent()

            val userPrompt = """
                Create a detailed, comprehensive summary from the following lecture transcription.
                Explain everything covered in the lecture thoroughly and clearly.
                You may interpret and clarify concepts to aid understanding, but do NOT add new topics or ideas.
                Remember: Base everything on the transcription content only.

                $transcription

                Return a thorough detailed summary using markdown formatting.
            """.trimIndent()

            val request = ChatRequest(
                model = modelName,
                messages = listOf(
                    ChatMessage(role = "system", content = systemPrompt),
                    ChatMessage(role = "user", content = userPrompt)
                ),
                temperature = 0.2,
                maxTokens = 3000,
                topP = 0.9
            )

            val response = api.createChatCompletion(request)

            if (response.error != null) {
                Result.failure(Exception(response.error.message))
            } else if (response.choices.isNotEmpty()) {
                Result.success(response.choices.first().message.content.trim())
            } else {
                Result.failure(Exception("No detailed notes generated"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun generateSummary(transcription: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            android.util.Log.d("AiRepository", "Generating summary for transcription of ${transcription.length} chars")
            val systemPrompt = """
                You are an assistant designed to help students understand their lectures through reflection and clarity, not by generating new ideas.
                Your purpose is to create a concise and clear summary of the lecture without adding any new information or opinions.
                The goal is to help students understand and reflect on what was actually said.

                Critical Rules:
                - Keep every point true to what was said in the lecture
                - Do NOT add any external examples, opinions, or assumptions
                - Highlight the main concepts, key arguments, and supporting evidence mentioned
                - Preserve the speaker's tone or intent where relevant
                - Use simple, clear, and structured language with short paragraphs or bullet points
                - End with a brief "What this means" reflection section, rephrasing the lecture's essence in your own words — without adding new knowledge
                - Focus on understanding, not expansion
                - The goal is to make the student see the logic of what's being explained — not to make it sound smarter
                - Base everything ONLY on the transcription provided
                - Stay strictly within the content boundaries of the lecture
            """.trimIndent()

            val userPrompt = """
                Summarize the following lecture content in a way that:
                - Keeps every point true to what was said in the lecture
                - Does not add any external examples, opinions, or assumptions
                - Highlights the main concepts, key arguments, and supporting evidence mentioned
                - Preserves the speaker's tone or intent where relevant
                - Uses simple, clear, and structured language (with short paragraphs or bullet points)
                - Ends with a brief "What this means" reflection section, rephrasing the lecture's essence in your own words — without adding new knowledge

                Focus on understanding, not expansion. The goal is to make the student see the logic of what's being explained — not to make it sound smarter.

                Lecture Transcription:
                $transcription

                Return a clear summary that helps the student understand what was actually said in the lecture.
            """.trimIndent()

            val request = ChatRequest(
                model = modelName,
                messages = listOf(
                    ChatMessage(role = "system", content = systemPrompt),
                    ChatMessage(role = "user", content = userPrompt)
                ),
                temperature = 0.3,
                maxTokens = 2000,  // Increased from 800 to ensure full summary
                topP = 0.9
            )

            android.util.Log.d("AiRepository", "Sending summary request to API")
            val response = api.createChatCompletion(request)

            if (response.error != null) {
                android.util.Log.e("AiRepository", "API error for summary: ${response.error.message}")
                Result.failure(Exception(response.error.message))
            } else if (response.choices.isNotEmpty()) {
                val content = response.choices.first().message.content.trim()
                android.util.Log.d("AiRepository", "Summary generated successfully: ${content.length} chars")
                Result.success(content)
            } else {
                android.util.Log.e("AiRepository", "No summary generated - empty choices")
                Result.failure(Exception("No summary generated"))
            }
        } catch (e: Exception) {
            android.util.Log.e("AiRepository", "Exception generating summary", e)
            Result.failure(e)
        }
    }
}
