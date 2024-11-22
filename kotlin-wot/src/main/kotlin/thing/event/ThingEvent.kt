package ai.ancf.lmos.wot.thing.event

import ai.ancf.lmos.wot.thing.Type
import ai.ancf.lmos.wot.thing.form.Form
import ai.ancf.lmos.wot.thing.schema.DataSchema
import ai.ancf.lmos.wot.thing.schema.EventAffordance
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY
import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL
import com.fasterxml.jackson.annotation.JsonProperty
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@JsonIgnoreProperties(ignoreUnknown = true)
data class ThingEvent<T, S, C>(
    @JsonInclude(NON_EMPTY)
    override var title: String? = null,

    @SerialName("@type")
    @JsonProperty("@type")
    @JsonInclude(NON_NULL)
    override var objectType: Type? = null,

    @JsonInclude(NON_NULL)
    override var data: DataSchema<T>? = null,

    @JsonInclude(NON_EMPTY)
    override var description: String? = null,

    @JsonInclude(NON_EMPTY)
    override var descriptions: MutableMap<String, String>? = null,

    @JsonInclude(NON_EMPTY)
    override var uriVariables: MutableMap<String, DataSchema<@Contextual Any>>? = null,

    @JsonInclude(NON_EMPTY)
    override var forms: MutableList<Form> = mutableListOf(),

    @JsonInclude(NON_EMPTY)
    override var subscription: DataSchema<@Contextual S>? = null,

    @JsonInclude(NON_EMPTY)
    override var cancellation: DataSchema<@Contextual C>? = null,

    @JsonInclude(NON_EMPTY)
    override var titles: MutableMap<String, String>? = null

) : EventAffordance<T, S, C>