package ai.ancf.lmos.wot.reflection

import ai.ancf.lmos.wot.Wot
import ai.ancf.lmos.wot.reflection.annotations.Action
import ai.ancf.lmos.wot.reflection.annotations.Event
import ai.ancf.lmos.wot.reflection.annotations.Property
import ai.ancf.lmos.wot.reflection.annotations.Thing
import ai.ancf.lmos.wot.reflection.annotations.VersionInfo
import ai.ancf.lmos.wot.thing.ExposedThing
import ai.ancf.lmos.wot.thing.schema.*
import ai.ancf.lmos.wot.thing.thingDescription
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.reflect.*
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

/**
 * This utility class helps in constructing a [ExposedThing] for a given Kotlin class
 * annotated with `@Thing`, `@Property` and `@Event` annotations. It maps class properties and methods
 * to a WoT (Web of Things) description and provides handlers for actions and properties.
 */
object ExposedThingBuilder {

    private val log: Logger = LoggerFactory.getLogger(ExposedThingBuilder::class.java)

    val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        log.error("Caught exception: ${throwable.message}", throwable)
    }

    /**
     * Creates an ExposedThing description for the provided class.
     *
     * @param wot The [Wot] instance used to generate the thing description.
     * @param instance The instance of the class to be described.
     * @param clazz The KClass of the object to be described.
     * @return An [ExposedThing] or null if no description could be generated.
     */
    fun <T : Any> createExposedThing(wot: Wot, instance: T, clazz: KClass<T>): ExposedThing? {
        log.debug("Creating ThingDescription for class: ${clazz.simpleName}")

        // 1. Get the @Thing annotation from the class
        val thingAnnotation = clazz.findAnnotation<Thing>()
        val readOnlyPropertiesMap = mutableMapOf<String, KProperty1<T, *>>()
        val writeOnlyPropertiesMap = mutableMapOf<String, KMutableProperty1<T, *>>()
        val readWritePropertiesMap = mutableMapOf<String, KMutableProperty1<T, *>>()
        val actionsMap = mutableMapOf<String, KFunction<*>>()
        val eventsMap = mutableMapOf<String, KFunction<*>>()
        if (thingAnnotation != null) {
            log.debug("Found @Thing annotation: id=${thingAnnotation.id}, title=${thingAnnotation.title}")
            val exposedThing = wot.produce(thingDescription {
                id = thingAnnotation.id
                title = thingAnnotation.title
                description = thingAnnotation.description
                val versionInfoAnnotation = clazz.findAnnotation<VersionInfo>()
                if(versionInfoAnnotation != null){
                    version = ai.ancf.lmos.wot.thing.schema.VersionInfo(versionInfoAnnotation.instance, versionInfoAnnotation.model)
                }
                // 3. Inspect the properties of the class and find @Property annotations
                clazz.memberProperties.forEach { property ->
                    val propertyAnnotation = property.findAnnotation<Property>()
                    if (propertyAnnotation != null) {
                        log.debug(
                            "Found @Property annotation on property: {}, type={}",
                            property.name,
                            property.returnType
                        )
                        // Check if the property has a setter (is mutable)
                        val isWritable = property is KMutableProperty1
                        when {
                            propertyAnnotation.readOnly -> readOnlyPropertiesMap[property.name] = property
                            propertyAnnotation.writeOnly -> {
                                if(isWritable){
                                    writeOnlyPropertiesMap[property.name] = property as KMutableProperty1<T, *>
                                } else{
                                    throw IllegalArgumentException("Property ${property.name} in Thing '${thingAnnotation.id}' is not writable, but is annotated as writeOnly. Change from val to var.")
                                }
                            }
                            isWritable -> {
                                // If it's writable and neither read-only nor write-only, store it in read-write map
                                readWritePropertiesMap[property.name] = property as KMutableProperty1<T, *>
                            }
                        }

                        val returnType = property.returnType
                        var showConstAndDefault = true
                        var observableProperty = false
                        val classifier = if(isStateFlow(returnType)){
                            showConstAndDefault = false
                            observableProperty = true
                            property.returnType.arguments.firstOrNull()?.type?.classifier
                                ?: throw IllegalArgumentException("StateFlow must have a type argument")
                        }else{
                            returnType.classifier
                        }
                        when (classifier) {
                            Int::class -> intProperty(property.name) {
                                description = propertyAnnotation.description
                                readOnly = propertyAnnotation.readOnly
                                writeOnly = propertyAnnotation.writeOnly
                                title = propertyAnnotation.title
                                observable = observableProperty
                                if(showConstAndDefault){
                                    if(propertyAnnotation.readOnly) {
                                        const = property.get(instance) as Int
                                    } else{
                                        default = property.get(instance) as Int
                                    }
                                }
                            }
                            Double::class -> numberProperty(property.name) {
                                description = propertyAnnotation.description
                                readOnly = propertyAnnotation.readOnly
                                writeOnly = propertyAnnotation.writeOnly
                                title = propertyAnnotation.title
                                observable = observableProperty
                                if(showConstAndDefault){
                                    if(propertyAnnotation.readOnly) {
                                        const = property.get(instance) as Double
                                    } else{
                                        default = property.get(instance) as Double
                                    }
                                }
                            }
                            Float::class -> numberProperty(property.name) {
                                description = propertyAnnotation.description
                                readOnly = propertyAnnotation.readOnly
                                writeOnly = propertyAnnotation.writeOnly
                                title = propertyAnnotation.title
                                observable = observableProperty
                                if(showConstAndDefault){
                                    if(propertyAnnotation.readOnly) {
                                        const = property.get(instance) as Float
                                    } else{
                                        default = property.get(instance) as Float
                                    }
                                }
                            }
                            Long::class -> numberProperty(property.name) {
                                description = propertyAnnotation.description
                                readOnly = propertyAnnotation.readOnly
                                writeOnly = propertyAnnotation.writeOnly
                                title = propertyAnnotation.title
                                observable = observableProperty
                                if(showConstAndDefault){
                                    if(propertyAnnotation.readOnly) {
                                        const = property.get(instance) as Long
                                    } else{
                                        default = property.get(instance) as Long
                                    }
                                }
                            }
                            Number::class -> numberProperty(property.name) {
                                description = propertyAnnotation.description
                                readOnly = propertyAnnotation.readOnly
                                writeOnly = propertyAnnotation.writeOnly
                                title = propertyAnnotation.title
                                observable = observableProperty
                                if(showConstAndDefault){
                                    if(propertyAnnotation.readOnly) {
                                        const = property.get(instance) as Number
                                    } else{
                                        default = property.get(instance) as Number
                                    }
                                }
                            }
                            String::class -> stringProperty(property.name) {
                                description = propertyAnnotation.description
                                readOnly = propertyAnnotation.readOnly
                                writeOnly = propertyAnnotation.writeOnly
                                title = propertyAnnotation.title
                                observable = observableProperty
                                if(showConstAndDefault){
                                    if(propertyAnnotation.readOnly) {
                                        const = property.get(instance) as String
                                    } else{
                                        default = property.get(instance) as String
                                    }
                                }
                            }
                            Boolean::class -> booleanProperty(property.name) {
                                description = propertyAnnotation.description
                                readOnly = propertyAnnotation.readOnly
                                writeOnly = propertyAnnotation.writeOnly
                                title = propertyAnnotation.title
                                observable = observableProperty
                                if(showConstAndDefault){
                                    if(propertyAnnotation.readOnly) {
                                        const = property.get(instance) as Boolean
                                    } else{
                                        default = property.get(instance) as Boolean
                                    }
                                }
                            }
                            List::class -> arrayProperty<Any>(property.name) {
                                description = propertyAnnotation.description
                                readOnly = propertyAnnotation.readOnly
                                title = propertyAnnotation.title
                                observable = observableProperty
                            }
                            IntArray::class -> arrayProperty<Int>(property.name) {
                                description = propertyAnnotation.description
                                readOnly = propertyAnnotation.readOnly
                                writeOnly = propertyAnnotation.writeOnly
                                title = propertyAnnotation.title
                                observable = observableProperty
                                items = IntegerSchema()
                            }
                            DoubleArray::class -> arrayProperty<Number>(property.name) {
                                description = propertyAnnotation.description
                                readOnly = propertyAnnotation.readOnly
                                writeOnly = propertyAnnotation.writeOnly
                                title = propertyAnnotation.title
                                observable = observableProperty
                                items = NumberSchema()
                            }
                            BooleanArray::class -> arrayProperty<Boolean>(property.name) {
                                description = propertyAnnotation.description
                                readOnly = propertyAnnotation.readOnly
                                writeOnly = propertyAnnotation.writeOnly
                                title = propertyAnnotation.title
                                observable = observableProperty
                                items = BooleanSchema()

                            }
                            FloatArray::class -> arrayProperty<Number>(property.name) {
                                description = propertyAnnotation.description
                                readOnly = propertyAnnotation.readOnly
                                writeOnly = propertyAnnotation.writeOnly
                                title = propertyAnnotation.title
                                observable = observableProperty
                                items = NumberSchema()
                            }
                            LongArray::class -> arrayProperty<Number>(property.name) {
                                description = propertyAnnotation.description
                                readOnly = propertyAnnotation.readOnly
                                writeOnly = propertyAnnotation.writeOnly
                                title = propertyAnnotation.title
                                observable = observableProperty
                                items = NumberSchema()
                            }
                            Array<String>::class -> arrayProperty<String>(property.name) {
                                description = propertyAnnotation.description
                                readOnly = propertyAnnotation.readOnly
                                writeOnly = propertyAnnotation.writeOnly
                                title = propertyAnnotation.title
                                observable = observableProperty
                                items = StringSchema()
                            }
                            Array<Number>::class -> arrayProperty<Number>(property.name) {
                                description = propertyAnnotation.description
                                readOnly = propertyAnnotation.readOnly
                                writeOnly = propertyAnnotation.writeOnly
                                title = propertyAnnotation.title
                                observable = observableProperty
                                items = NumberSchema()
                            }
                            Array<Boolean>::class -> arrayProperty<Boolean>(property.name) {
                                description = propertyAnnotation.description
                                readOnly = propertyAnnotation.readOnly
                                writeOnly = propertyAnnotation.writeOnly
                                title = propertyAnnotation.title
                                observable = observableProperty
                                items = BooleanSchema()
                            }
                            Set::class -> arrayProperty<Any>(property.name) {
                                description = propertyAnnotation.description
                                readOnly = propertyAnnotation.readOnly
                                writeOnly = propertyAnnotation.writeOnly
                                title = propertyAnnotation.title
                                observable = observableProperty
                            }
                            Unit::class -> nullProperty(property.name) {
                                description = propertyAnnotation.description
                                readOnly = propertyAnnotation.readOnly
                                writeOnly = propertyAnnotation.writeOnly
                                title = propertyAnnotation.title
                                observable = observableProperty
                            }
                            else -> {
                                // Handle ObjectProperty (sub-properties)
                                objectProperty(property.name) {
                                    description = propertyAnnotation.description
                                    readOnly = propertyAnnotation.readOnly
                                    writeOnly = propertyAnnotation.writeOnly
                                    title = propertyAnnotation.title
                                    observable = observableProperty

                                    // If the property is an object, add its properties recursively
                                    val subProperties = buildObjectSchema(property.returnType)
                                    properties += subProperties.properties
                                    required += subProperties.required
                                }
                            }
                        }
                    }
                }

                // 4. Inspect the functions of the class and find @Action and @Event annotations
                clazz.declaredMemberFunctions.forEach { function ->
                    val actionAnnotation = function.findAnnotation<Action>()
                    val eventAnnotation = function.findAnnotation<Event>()
                    if (actionAnnotation != null) {
                        log.debug("Found @Action annotation on function: ${function.name}")
                        actionsMap[function.name] = function
                        val parameterTypes = function.parameters.drop(1) // Skip the first parameter which is the receiver
                        val inputSchema = generateSchema(parameterTypes)
                        // Handle return type for the output schema
                        val outputSchema = mapTypeToSchema(function.returnType)
                        action<Any, Any>(actionAnnotation.name) {
                            @Suppress("UNCHECKED_CAST")
                            input = inputSchema as DataSchema<Any>?
                            @Suppress("UNCHECKED_CAST")
                            output = outputSchema as DataSchema<Any>?
                            safe = actionAnnotation.safe
                            synchronous = actionAnnotation.synchronous
                            idempotent = actionAnnotation.idempotent
                        }
                    } else if (eventAnnotation != null) {
                        log.debug("Found @Event annotation on function: ${function.name}")
                        eventsMap[function.name] = function
                        // Generate an event stream handler for this function
                        // Extract the generic type (the type of the emitted values from the Flow)
                        val flowType = function.returnType.arguments.firstOrNull()?.type
                            ?: throw IllegalArgumentException("Event function must return a Flow type")
                        val outputSchema = mapTypeToSchema(flowType)
                        event<Any, Any, Any>(eventAnnotation.name) {
                            description = eventAnnotation.description
                            title = eventAnnotation.name
                            @Suppress("UNCHECKED_CAST")
                            data = outputSchema as DataSchema<Any>?
                        }
                    }
                }
            })

            addPropertyHandler(readOnlyPropertiesMap, exposedThing, instance, writeOnlyPropertiesMap, readWritePropertiesMap)
            // Action handlers
            addActionHandler(actionsMap, exposedThing, instance)
            addEventHandler(eventsMap, exposedThing, instance)
            log.debug("Successfully created ThingDescription for class: ${clazz.simpleName}")
            return exposedThing
        }
        log.warn("No @Thing annotation found on class: ${clazz.simpleName}")
        return null
    }

    private fun isStateFlow(type: KType): Boolean {
        return type.classifier == StateFlow::class || type.classifier == MutableStateFlow::class
    }

    internal fun <T : Any> addEventHandler(
        eventsMap: MutableMap<String, KFunction<*>>,
        exposedThing: ExposedThing,
        instance: T
    ) {
        for ((name, function) in eventsMap) {
            log.debug("Setting event handlers for: $name")
            val scope = CoroutineScope(Dispatchers.IO + exceptionHandler)
            exposedThing.setEventSubscribeHandler(name) { _: InteractionOptions ->
                val flow = function.call(instance) as Flow<*>
                scope.launch {
                    flow.collect { value ->
                        val data = DataSchemaValue.toDataSchemaValue(value)
                        exposedThing.emitEvent(name, InteractionInput.Value(data))
                    }
                }
            }
            exposedThing.setEventUnsubscribeHandler(name) {
                log.debug("Unsubscribed from event: $name")
            }
        }
    }

    internal fun <T : Any> addActionHandler(
        actionsMap: MutableMap<String, KFunction<*>>,
        exposedThing: ExposedThing,
        instance: T
    ) {
        for ((name, function) in actionsMap) {
            log.debug("Setting action handler for: $name")
            exposedThing.setActionHandler(name) { input, _ ->
                val args = function.parameters.associateWith { param ->
                    if (param.kind == KParameter.Kind.INSTANCE) instance
                    else toKotlinObject(input.value(), param.type)
                }
                val result = function.callBy(args)
                InteractionInput.Value(DataSchemaValue.toDataSchemaValue(result))
            }
        }
    }

    internal fun <T : Any> addPropertyHandler(
        readOnlyPropertiesMap: MutableMap<String, KProperty1<T, *>>,
        exposedThing: ExposedThing,
        instance: T,
        writeOnlyPropertiesMap: MutableMap<String, KMutableProperty1<T, *>>,
        readWritePropertiesMap: MutableMap<String, KMutableProperty1<T, *>>
    ) {
        for ((name, property) in readOnlyPropertiesMap) {
            log.debug("Setting read property handler for: $name")
            addPropertyReadHandler(property, exposedThing, name, instance)
        }
        for ((name, property) in writeOnlyPropertiesMap) {
            log.debug("Setting write property handler for: $name")
            addPropertyWriteHandler(property, exposedThing, name, instance)
        }
        for ((name, property) in readWritePropertiesMap) {
            log.debug("Setting read and write property handler for: $name")
            addPropertyReadHandler(property, exposedThing, name, instance)
            addPropertyWriteHandler(property, exposedThing, name, instance)
        }
    }

    private fun <T : Any> addPropertyWriteHandler(
        property: KMutableProperty1<T, *>,
        exposedThing: ExposedThing,
        name: String,
        instance: T
    ) {
        exposedThing.setPropertyWriteHandler(name) { input, _ ->
            property.setter.call(instance, toKotlinObject(input.value(), property.returnType))
            InteractionInput.Value(input.value() ?: DataSchemaValue.NullValue)
        }
    }

    private fun <T : Any> addPropertyReadHandler(
        property: KProperty1<T, *>,
        exposedThing: ExposedThing,
        name: String,
        instance: T
    ) {
        if(isStateFlow(property.returnType)){
            // Access the property value
            // Make the property accessible
            val mutableStateFlow = property.getter.call(instance) as? MutableStateFlow<*>
                ?: throw IllegalStateException("Not a MutableStateFlow")
            log.debug("Setting property observe handler for: $name")
            val scope = CoroutineScope(Dispatchers.IO + exceptionHandler)
            exposedThing.setPropertyObserveHandler(name) { _ ->
                scope.launch {
                    mutableStateFlow.collect { value ->
                        exposedThing.emitPropertyChange(
                            name,
                            InteractionInput.Value(DataSchemaValue.toDataSchemaValue(value))
                        )
                    }
                }
                InteractionInput.Value(DataSchemaValue.NullValue)
            }
            exposedThing.setPropertyReadHandler(name) { _ ->
                val value = mutableStateFlow.value
                InteractionInput.Value(DataSchemaValue.toDataSchemaValue(value))
            }
        } else{
            exposedThing.setPropertyReadHandler(name) { _ ->
                val value = property.getter.call(instance)
                InteractionInput.Value(DataSchemaValue.toDataSchemaValue(value))
            }
        }
    }

    /**
     * Generates a schema for the input parameters of an action.
     *
     * @param parameterTypes The list of parameter types to generate a schema for.
     * @return The generated [DataSchema] for the action's parameters.
     */
    private fun generateSchema(parameterTypes: List<KParameter>): DataSchema<out Any> {
        log.debug("Generating schema for parameters: {}", parameterTypes)
        return when {
            parameterTypes.isEmpty() -> {
                log.debug("No parameters found, returning NullSchema")
                NullSchema()
            }
            parameterTypes.size > 1 -> {
                log.debug("Multiple parameters found, building ObjectSchema")
                buildObjectSchema(parameterTypes)
            }
            else -> {
                log.debug("Single parameter found, mapping type to schema")
                val param = parameterTypes.first()
                mapTypeToSchema(param.type)
            }
        }
    }

    /**
     * Builds an [ObjectSchema] for multiple parameters of an action.
     *
     * @param parameterTypes A list of KParameter objects representing the action's parameters.
     * @return The constructed [ObjectSchema] for the parameters.
     */
    internal fun buildObjectSchema(parameterTypes: List<KParameter>): ObjectSchema {
        log.debug("Building ObjectSchema for parameters: {}", parameterTypes)
        val properties: MutableMap<String, DataSchema<*>> = mutableMapOf()
        val required: MutableList<String> = mutableListOf()

        parameterTypes.forEach { param ->
            val schemaForParam = mapTypeToSchema(param.type)
            if (param.isOptional || param.type.isMarkedNullable) {
                properties[param.name!!] = schemaForParam
            } else {
                properties[param.name!!] = schemaForParam
                required.add(param.name!!)
            }
        }

        return ObjectSchema(properties, required)
    }

    private fun buildObjectSchema(type: KType): ObjectSchema {
        log.debug("Building ObjectSchema for type: {}", type)
        if (type.classifier == Map::class) {
            return buildMapSchema(type)
        }

        val kClass = type.classifier as? KClass<*>
            ?: throw IllegalArgumentException("Type is not a valid class: $type")

        val (properties, required) = extractProperties(kClass)
        return ObjectSchema(properties, required)
    }

    private fun buildMapSchema(type: KType): ObjectSchema {
        log.debug("Building MapSchema for type: {}", type)
        val keyType = type.arguments.getOrNull(0)?.type
        val valueType = type.arguments.getOrNull(1)?.type

        val keySchema = if (keyType != null) mapTypeToSchema(keyType) else StringSchema()
        val valueSchema = if (valueType != null) mapTypeToSchema(valueType) else StringSchema()

        return ObjectSchema(
            properties = mutableMapOf(
                "keys" to ArraySchema(items = keySchema),
                "values" to ArraySchema(items = valueSchema)
            )
        )
    }

    private fun extractProperties(kClass: KClass<*>): Pair<MutableMap<String, DataSchema<*>>, MutableList<String>> {
        log.debug("Extracting properties from class: ${kClass.simpleName}")
        val properties: MutableMap<String, DataSchema<*>> = mutableMapOf()
        val required: MutableList<String> = mutableListOf()

        kClass.memberProperties.forEach { property ->
            val returnType = property.returnType
            val schemaForParam = mapTypeToSchema(returnType)
            properties[property.name] = schemaForParam
            if (!returnType.isMarkedNullable) {
                required.add(property.name)
            }
        }
        return Pair(properties, required)
    }

    internal fun mapTypeToSchema(type: KType): DataSchema<out Any> {
        log.debug("Mapping type to schema: {}", type)
        return when (type.classifier) {
            Int::class -> IntegerSchema()
            Float::class -> NumberSchema()
            Double::class -> NumberSchema()
            Long::class -> NumberSchema()
            String::class -> StringSchema()
            Boolean::class -> BooleanSchema()
            List::class, Set::class -> {
                val genericType = type.arguments.firstOrNull()?.type
                val itemSchema = if (genericType != null) {
                    mapTypeToSchema(genericType)
                } else {
                    StringSchema()
                }
                ArraySchema(items = itemSchema)
            }
            IntArray::class -> ArraySchema(items = IntegerSchema())
            FloatArray::class -> ArraySchema(items = NumberSchema())
            DoubleArray::class -> ArraySchema(items = NumberSchema())
            NumberSchema::class -> ArraySchema(items = NumberSchema())
            Array<String>::class -> ArraySchema(items = StringSchema())
            Array<Number>::class -> ArraySchema(items = NumberSchema())
            Array<Boolean>::class -> ArraySchema(items = BooleanSchema())
            Array<Int>::class -> ArraySchema(items = IntegerSchema())
            Array<Float>::class -> ArraySchema(items = NumberSchema())
            Array<Long>::class -> ArraySchema(items = NumberSchema())
            Unit::class -> NullSchema()
            else -> buildObjectSchema(type)
        }
    }

    internal fun toKotlinObject(input: DataSchemaValue?, type: KType): Any? {
        log.debug("Converting DataSchemaValue to Kotlin object: input={}, type={}", input, type)
        if (input == null) return null

        return when (input) {
            is DataSchemaValue.StringValue -> handleStringValue(input, type)
            is DataSchemaValue.IntegerValue -> input.value
            is DataSchemaValue.NumberValue -> input.value
            is DataSchemaValue.BooleanValue -> input.value
            is DataSchemaValue.ArrayValue -> handleArrayValue(input, type)
            is DataSchemaValue.ObjectValue -> handleObjectValue(input, type)
            else -> throw IllegalArgumentException("Unsupported input type: ${input::class}")
        }
    }

    private fun handleStringValue(input: DataSchemaValue.StringValue, type: KType): Any? {
        log.debug("Handling StringValue: input=$input, type=$type")
        val classifier = type.classifier
        return if (classifier is KClass<*> && classifier.java.isEnum) {
            handleEnumString(input, classifier)
        } else {
            input.value
        }
    }

    private fun handleEnumString(input: DataSchemaValue.StringValue, classifier: KClass<*>): Any? {
        log.debug("Handling enum StringValue: input=$input, classifier=$classifier")
        val enumClass = classifier as KClass<out Enum<*>>
        return enumClass.java.enumConstants?.find { it.name == input.value }
            ?: throw IllegalArgumentException("Unknown enum value: ${input.value}")
    }

    private fun handleArrayValue(input: DataSchemaValue.ArrayValue, type: KType): Any? {
        log.debug("Handling ArrayValue: input=$input, type=$type")
        val itemType = type.arguments.firstOrNull()?.type ?: throw IllegalArgumentException("Unknown generic type for collection")
        val mappedItems = input.value.map { item ->
            val dataSchemaValue = DataSchemaValue.toDataSchemaValue(item)
            toKotlinObject(dataSchemaValue, itemType)
        }

        return when (type.classifier) {
            List::class -> mappedItems
            Set::class -> mappedItems.toSet()
            IntArray::class -> mappedItems.map { it as Int }.toIntArray()
            FloatArray::class -> mappedItems.map { it as Float }.toFloatArray()
            DoubleArray::class -> mappedItems.map { it as Double }.toDoubleArray()
            BooleanArray::class -> mappedItems.map { it as Boolean }.toBooleanArray()
            else -> throw IllegalArgumentException("Unsupported array type: ${type.classifier}")
        }
    }

    private fun handleObjectValue(input: DataSchemaValue.ObjectValue, type: KType): Any? {
        log.debug("Handling ObjectValue: input=$input, type=$type")
        val kClass = type.classifier as? KClass<*>
            ?: throw IllegalArgumentException("Type is not a valid class: $type")

        return when {
            kClass == Map::class -> handleMapValue(input, type)
            else -> handleNonMapObject(input, kClass)
        }
    }

    private fun handleMapValue(input: DataSchemaValue.ObjectValue, type: KType): Any? {
        log.debug("Handling MapValue: input=$input, type=$type")
        val keyType = type.arguments.getOrNull(0)?.type ?: throw IllegalArgumentException("Unknown key type for map")
        val valueType = type.arguments.getOrNull(1)?.type ?: throw IllegalArgumentException("Unknown value type for map")

        val mapInstance = mutableMapOf<Any, Any>()
        for ((key, value) in input.value) {
            val keyConverted = toKotlinObject(DataSchemaValue.StringValue(key as String), keyType)
            val valueConverted = toKotlinObject(DataSchemaValue.toDataSchemaValue(value), valueType)
            if (keyConverted != null && valueConverted != null) {
                mapInstance[keyConverted] = valueConverted
            }
        }
        return mapInstance
    }

    private fun handleNonMapObject(input: DataSchemaValue.ObjectValue, kClass: KClass<*>): Any {
        log.debug("Handling non-Map ObjectValue: input={}, kClass={}", input, kClass)
        val constructor = kClass.primaryConstructor
            ?: throw IllegalArgumentException("The class must have a primary constructor")

        val args = constructor.parameters.associateWith { parameter ->
            val propertyType = parameter.type
            val value = input.value[parameter.name]
            val dataSchemaValue = DataSchemaValue.toDataSchemaValue(value)
            toKotlinObject(dataSchemaValue, propertyType)
        }

        return constructor.callBy(args)
    }
}