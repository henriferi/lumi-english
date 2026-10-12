package dev.lumi;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.StructuredOutputChatOptions;

/** Scripted chat model: returns the queued answers in order and remembers every prompt it received. */
public class FakeChatModel implements ChatModel {

    private final Deque<String> answers = new ArrayDeque<>();
    private final List<Prompt> prompts = new ArrayList<>();

    public synchronized void willAnswer(String... contents) {
        answers.addAll(List.of(contents));
    }

    public synchronized List<Prompt> prompts() {
        return List.copyOf(prompts);
    }

    public synchronized void reset() {
        answers.clear();
        prompts.clear();
    }

    @Override
    public synchronized ChatResponse call(Prompt prompt) {
        prompts.add(prompt);
        String content = answers.isEmpty() ? "" : answers.poll();
        return new ChatResponse(List.of(new Generation(new AssistantMessage(content))));
    }

    /** Behaves like a provider that supports native structured output (as OpenAI does). */
    @Override
    public ChatOptions getOptions() {
        return StructuredOutputChatOptions.builder().build();
    }
}
