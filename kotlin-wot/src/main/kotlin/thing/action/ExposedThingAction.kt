package ai.ancf.lmos.wot.thing.action

import ai.ancf.lmos.wot.thing.ActionAffordance
import ai.ancf.lmos.wot.thing.Thing
import ai.ancf.lmos.wot.thing.form.Form
import ai.ancf.lmos.wot.thing.schema.DataSchema
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include.*
import com.fasterxml.jackson.annotation.JsonProperty
import org.slf4j.LoggerFactory


class ExposedThingAction<I, O>(
    action: ThingAction<I, O>,
    @JsonIgnore
    private val thing: Thing,
    private val state: ActionState<I, O> = ActionState()
) : ActionAffordance<I, O> by action {

    /**
     * Invokes the method and executes the handler defined in [.state]. `input`
     * contains the request payload. `options` can contain additional data (for example,
     * the query parameters when using COAP/HTTP).
     *
     * @param input
     * @param options
     * @return
     */
    suspend fun invoke(
        input: I,
        options: Map<String, Map<String, Any>> = emptyMap()
    ): O? {
        log.debug("'{}' has Action state of '{}': {}", thing.id, title, state)
        return if (state.handler != null) {
            log.debug(
                "'{}' calls registered handler for Action '{}' with input '{}' and options '{}'",
                thing.id, title, input, options
            )
            try {
                // Use the handler as a suspending function directly
                state.handler.invoke(input, options).also { output ->
                    if (output == null) {
                        log.warn(
                            "'{}': Called registered handler for Action '{}' returned null. This can cause problems.",
                            thing.id, title
                        )
                    }
                }
            } catch (e: Exception) {
                log.error("'{}' handler invocation for Action '{}' failed with exception", thing.id, title, e)
                throw e
            }
        } else {
            log.debug("'{}' has no handler for Action '{}'", thing.id, title)
            null
        }
    }

    companion object {
        private val log: org.slf4j.Logger = LoggerFactory.getLogger(ExposedThingAction::class.java)
    }

    class ActionState<I, O>(val handler: (suspend (input: I, options: Map<String, Map<String, Any>>) -> O?)? = null)
}

data class ThingAction<I, O>(
    @JsonInclude(NON_EMPTY)
    override var title: String? = null,

    @JsonInclude(NON_EMPTY)
    override var description: String? = null,

    @JsonInclude(NON_EMPTY)
    override var descriptions: MutableMap<String, String>? = null,

    @JsonInclude(NON_EMPTY)
    override var uriVariables: MutableMap<String, Map<String, Any>>? = null,

    @JsonInclude(NON_EMPTY)
    override var forms: MutableList<Form>? = null,

    @JsonProperty("@type")
    @JsonInclude(NON_EMPTY)
    override var objectType: String? = null,

    @JsonInclude(NON_NULL)
    override var input: DataSchema<I>? = null,

    @JsonInclude(NON_NULL)
    override var output: DataSchema<O>? = null,
    @JsonInclude(NON_DEFAULT)
    override val safe: Boolean = false,
    @JsonInclude(NON_DEFAULT)
    override val idempotent: Boolean = false,
    @JsonInclude(NON_NULL)
    override val synchronous: Boolean? = null,
    @JsonInclude(NON_EMPTY)
    override var titles: MutableMap<String, String>? = null

) : ActionAffordance<I, O>