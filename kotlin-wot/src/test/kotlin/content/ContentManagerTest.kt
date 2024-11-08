package ai.ancf.lmos.wot.content

import ai.ancf.lmos.wot.thing.schema.DataSchemaValue
import ai.ancf.lmos.wot.thing.schema.ObjectSchema
import ai.ancf.lmos.wot.thing.schema.StringSchema
import ai.ancf.lmos.wot.thing.schema.toDataSchemeValue
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.ByteArrayOutputStream
import java.io.ObjectOutputStream
import kotlin.test.assertFailsWith

class ContentManagerTest {

    @BeforeEach
    fun setup() {
        // Reset CODECS and OFFERED maps
        ContentManager.removeCodec("application/json")
    }

    @Test
    fun `addCodec should register a codec and add to OFFERED if offered is true`() {
        // Act
        ContentManager.addCodec(JsonCodec(), true)

        // Assert
        assertTrue(ContentManager.offeredMediaTypes.contains("application/json"))
        assertTrue(ContentManager.isSupportedMediaType("application/json"))
    }

    @Test
    fun `removeCodec should unregister codec and remove from OFFERED set`() {
        // Arrange
        ContentManager.addCodec(JsonCodec(), true)

        // Act
        ContentManager.removeCodec("application/json")

        // Assert
        assertFalse(ContentManager.offeredMediaTypes.contains("application/json"))
        assertFalse(ContentManager.isSupportedMediaType("application/json"))
    }

    @Test
    fun `contentToValue should use codec to convert content to value`() {
        ContentManager.addCodec(JsonCodec(), true)

        // Arrange
        val testContent = Content(type = "application/json", body = """{"key": "value"}""".toByteArray())

        // Act
        val result = ContentManager.contentToValue(testContent, ObjectSchema())

        result as DataSchemaValue.ObjectValue

        // Assert
        assertEquals("value", result.value["key"])
    }

    @Test
    fun `contentToValue should throw exception when codec fails to decode`() {
        // Arrange
        val jsonCodec : ContentCodec = mockk()
        every { jsonCodec.mediaType } returns "application/json"
        ContentManager.addCodec(jsonCodec, true)
        val testContent = Content(type = "application/json", body = """{"key": "value"}""".toByteArray())

        every { jsonCodec.bytesToValue(testContent.body, StringSchema(), any()) } throws ContentCodecException("Decode error")

        // Act & Assert
        val exception = assertThrows<ContentCodecException> {
            ContentManager.contentToValue(testContent, StringSchema())
        }
        assertEquals("Decode error", exception.message)
    }

    @Test
    fun `valueToContent should use codec to serialize value to Content`() {
        // Arrange
        ContentManager.addCodec(JsonCodec(), true)

        val testValue = "value"

        // Act
        val result = ContentManager.valueToContent(testValue, "application/json")
        val stringValue = ContentManager.contentToValue(result, StringSchema())
        stringValue as DataSchemaValue.StringValue
        // Assert
        assertEquals(testValue, stringValue.value)
    }

    @Test
    fun `valueToContent should use codec to serialize data scheme value to Content`() {
        // Arrange
        ContentManager.addCodec(JsonCodec(), true)

        val testValue = "value".toDataSchemeValue()

        // Act
        val result = ContentManager.valueToContent(testValue, "application/json")
        val stringValue = ContentManager.contentToValue(result, StringSchema())
        stringValue as DataSchemaValue.StringValue
        // Assert
        assertEquals("value", stringValue.value)
    }

    @Test
    fun `valueToContent should throw exception when codec fails to encode`() {
        val jsonCodec : ContentCodec = mockk()
        // Arrange
        val testValue = "value to encode"
        every { jsonCodec.mediaType } returns "application/json"
        ContentManager.addCodec(jsonCodec, true)
        every { jsonCodec.valueToBytes(testValue, any()) } throws ContentCodecException("Encode error")

        // Act & Assert
        val exception = assertFailsWith<ContentCodecException> {
            ContentManager.valueToContent(testValue, "application/json")
        }
        assertEquals("Encode error", exception.message)
    }

    @Test
    fun `fallbackBytesToValue should deserialize content using Java deserialization`() {
        // Arrange
        val byteArray = ByteArrayOutputStream().apply {
            ObjectOutputStream(this).writeObject("Fallback value")
        }.toByteArray()
        val content = Content("application/octet-stream", byteArray)

        // Act
        val result = ContentManager.contentToValue(content, StringSchema())

        result as DataSchemaValue.StringValue

        // Assert
        assertEquals("Fallback value", result.value)
    }

    @Test
    fun `fallbackBytesToValue should throw exception on invalid Java deserialization`() {
        // Arrange
        val byteArray = "invalid".toByteArray()
        val content = Content("application/octet-stream", byteArray)

        // Act & Assert
        assertThrows<ContentCodecException> {
            ContentManager.contentToValue(content, StringSchema())
        }
    }

    @Test
    fun `getMediaType should extract correct media type`() {
        // Act
        val result = ContentManager.getMediaType("text/plain; charset=utf-8")

        // Assert
        assertEquals("text/plain", result)
    }

    @Test
    fun `getMediaTypeParameters should extract parameters correctly`() {
        // Act
        val result = ContentManager.getMediaTypeParameters("text/plain; charset=utf-8; boundary=something")

        // Assert
        assertEquals(2, result.size)
        assertEquals("utf-8", result["charset"])
        assertEquals("something", result["boundary"])
    }
}