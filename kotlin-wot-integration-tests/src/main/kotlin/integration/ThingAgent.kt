package ai.ancf.lmos.wot.reflection.annotations

import dev.langchain4j.model.azure.AzureOpenAiChatModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow


@Thing(id= "agent", title="Agent",
    description= "A simple agent.")
@VersionInfo(instance = "1.0.0")
class ThingAgent(@Property(name = "modelTemperature", readOnly = true)
                 val modelConfiguration: ModelConfiguration = ModelConfiguration(0.5, 50)) {

    private val messageFlow = MutableSharedFlow<String>(replay = 1) // Replay last emitted value

    private val model: AzureOpenAiChatModel = AzureOpenAiChatModel.builder()
        .apiKey("af12dab9c046453e82dcf4b24af90bca")
        .deploymentName("GPT35T-1106")
        .endpoint("https://gpt4-uk.openai.azure.com/")
        .temperature(modelConfiguration.modelTemperature)
        .build();

    @Property(name = "observableProperty", title = "Observable Property", readOnly = true)
    val observableProperty : MutableStateFlow<String> = MutableStateFlow("Hello World")

    @Action(name = "ask", title = "Ask", description = "Ask the agent a question.")
    suspend fun  ask(message : String) : String {
        val response = model.generate(message)
        messageFlow.emit(response)
        return model.generate(response)
    }

    @Event(name = "messageGenerated", title = "Generated message")
    fun messageGenerated() : Flow<String> {
        return messageFlow
    }
}

data class ModelConfiguration(val modelTemperature: Double, val maxTokens: Int)

