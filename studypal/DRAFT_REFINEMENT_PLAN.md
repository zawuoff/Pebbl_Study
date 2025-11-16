# Draft Refinement Prompt Plan

## Current Behavior
- `VoiceSessionViewModel.finishSession()` (`app/src/main/java/com/fouwaz/studypal/ui/viewmodel/VoiceSessionViewModel.kt:203`) aggregates `VoiceStreamEntity` records and calls `AiRepository.generateDraft`.
- `AiRepository.generateDraft` (`app/src/main/java/com/fouwaz/studypal/data/repository/AiRepository.kt:86`) builds a combined conversation string and prompts OpenRouter GPT-4o mini with instructions to create a 400–600 word academic synthesis, leading the model to add large amounts of new content.

## Desired Behavior
- Final drafts must stay true to the user’s spoken ideas, only applying light formatting, grammar fixes, paragraph organization, and minimal connective phrases (e.g., “however,” “additionally”).
- The AI should not introduce new ideas, examples, statistics, or expand beyond the user’s original material.

## Implementation Steps
- Update conversation payload construction in `VoiceSessionViewModel.finishSession()` (`app/src/main/java/com/fouwaz/studypal/ui/viewmodel/VoiceSessionViewModel.kt:229`) so user speech segments are explicitly labeled (e.g., “UserIdea #n”) and AI follow-up questions are clearly marked or omitted before passing the list to `generateDraft`.
- Modify `AiRepository.generateDraft` (`app/src/main/java/com/fouwaz/studypal/data/repository/AiRepository.kt:112`) to:
  - Replace the current system prompt with one that enforces fidelity to user ideas and allows only formatting/grammar improvements.
  - Update the user prompt template to restate these constraints and clarify that AI questions are context only.
  - Lower `temperature` to about 0.2 and reduce `maxTokens` to roughly 700–900 to discourage expansive completions.
- Optionally add a safeguard in the same function (after receiving the response) to compare draft length with total user word count and flag or truncate if the ratio exceeds an agreed multiplier (for now set it to 1.8×) before saving to Room.
