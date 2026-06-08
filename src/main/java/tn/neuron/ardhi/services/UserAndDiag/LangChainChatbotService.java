package tn.neuron.ardhi.services.UserAndDiag;

import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

import tn.neuron.ardhi.utils.UserAndDiag.AppConfig;

public class LangChainChatbotService {

    private static final String API_KEY = AppConfig.get("groq.api.key");
    private static final String API_URL = "https://api.groq.com/openai/v1";
    // We will use the same model for both chatbots as requested
    private static final String MODEL_NAME = "llama-3.3-70b-versatile";

    interface Assistant {
        // Allows defining a system message context per chat session
        @SystemMessage("{{systemContext}}")
        String chat(@MemoryId String memoryId, @V("systemContext") String systemContext,
                @UserMessage String userMessage);
    }

    private final Assistant assistant;

    public LangChainChatbotService() {
        // Create the underlying chat model (Groq is compatible with the OpenAI API)
        ChatLanguageModel model = OpenAiChatModel.builder()
                .baseUrl(API_URL)
                .apiKey(API_KEY)
                .modelName(MODEL_NAME)
                .timeout(java.time.Duration.ofSeconds(60))
                .build();

        // Use declarative AiServices which manages memory automatically
        this.assistant = AiServices.builder(Assistant.class)
                .chatLanguageModel(model)
                // Using MessageWindowChatMemory to keep the last N messages
                // We provide a ChatMemoryProvider to create a separate memory for each
                // @MemoryId
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(20))
                .build();
    }

    /**
     * Sends a message to the AI and gets a response, maintaining conversation
     * history.
     * 
     * @param sessionId     A unique ID for the current chat session (e.g.
     *                      "diag_123")
     * @param systemContext The initial system instructions to give the AI context.
     * @param userMessage   The message typed by the user.
     * @return The AI's response text.
     */
    public String chat(String sessionId, String systemContext, String userMessage) {
        try {
            return assistant.chat(sessionId, systemContext, userMessage);
        } catch (Exception e) {
            System.err.println("Erreur LangChain4j AI: " + e.getMessage());
            e.printStackTrace();
            return "Désolé, je rencontre une erreur de connexion à mon intelligence. (" + e.getMessage() + ")";
        }
    }
}
