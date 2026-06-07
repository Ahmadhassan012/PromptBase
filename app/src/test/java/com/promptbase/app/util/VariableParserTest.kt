package com.promptbase.app.util

import org.junit.Assert.*
import org.junit.Test

class VariableParserTest {

    @Test
    fun `extract simple variable`() {
        val vars = VariableParser.extractVariables("Hello {{ name }}")
        assertEquals(1, vars.size)
        assertEquals("name", vars[0].key)
        assertEquals("name", vars[0].displayName)
        assertNull(vars[0].defaultValue)
    }

    @Test
    fun `extract variable with default`() {
        val vars = VariableParser.extractVariables("Hello {{ name : World }}")
        assertEquals(1, vars.size)
        assertEquals("name", vars[0].key)
        assertEquals("World", vars[0].defaultValue)
    }

    @Test
    fun `extract variable with default no spaces around colon`() {
        val vars = VariableParser.extractVariables("Hello {{ name:World }}")
        assertEquals(1, vars.size)
        assertEquals("name", vars[0].key)
        assertEquals("World", vars[0].defaultValue)
    }

    @Test
    fun `extract multiple variables`() {
        val vars = VariableParser.extractVariables("{{ a }} and {{ b : B }}")
        assertEquals(2, vars.size)
        assertEquals("a", vars[0].key)
        assertEquals("b", vars[1].key)
        assertEquals("B", vars[1].defaultValue)
    }

    @Test
    fun `deduplicate variables with same key`() {
        val vars = VariableParser.extractVariables("{{ name : first }} and {{ name }}")
        assertEquals(1, vars.size)
        assertEquals("name", vars[0].key)
        assertEquals("first", vars[0].defaultValue)
    }

    @Test
    fun `deduplicate picks first default`() {
        val vars = VariableParser.extractVariables("{{ name }} and {{ name : second }}")
        assertEquals(1, vars.size)
        assertEquals("name", vars[0].key)
        assertEquals("second", vars[0].defaultValue)
    }

    @Test
    fun `no match for empty braces`() {
        val vars = VariableParser.extractVariables("Hello {{ }}")
        assertTrue(vars.isEmpty())
    }

    @Test
    fun `no match for unclosed braces`() {
        val vars = VariableParser.extractVariables("Hello {{ name ")
        assertTrue(vars.isEmpty())
    }

    @Test
    fun `preserve case in display name`() {
        val vars = VariableParser.extractVariables("Hello {{ UserName }}")
        assertEquals("username", vars[0].key)
        assertEquals("UserName", vars[0].displayName)
    }

    @Test
    fun `extract with special characters`() {
        val vars = VariableParser.extractVariables("{{ user_name_123 }}")
        assertEquals(1, vars.size)
        assertEquals("user_name_123", vars[0].key)
    }

    @Test
    fun `replace simple variable`() {
        val result = VariableParser.replaceVariables("Hello {{ name }}", mapOf("name" to "World"))
        assertEquals("Hello World", result)
    }

    @Test
    fun `replace uses default when value missing`() {
        val result = VariableParser.replaceVariables("Hello {{ name : World }}", mapOf())
        assertEquals("Hello World", result)
    }

    @Test
    fun `replace empty string when both missing`() {
        val result = VariableParser.replaceVariables("Hello {{ name }}", mapOf())
        assertEquals("Hello ", result)
    }

    @Test
    fun `replace multiple occurrences`() {
        val result = VariableParser.replaceVariables(
            "{{ a }} and {{ b }}",
            mapOf("a" to "X", "b" to "Y")
        )
        assertEquals("X and Y", result)
    }

    @Test
    fun `replace case insensitive key`() {
        val result = VariableParser.replaceVariables("Hello {{ Name }}", mapOf("name" to "World"))
        assertEquals("Hello World", result)
    }

    @Test
    fun `replace with leading trailing whitespace in value`() {
        val result = VariableParser.replaceVariables("Hello {{ name }}", mapOf("name" to "  World  "))
        assertEquals("Hello World", result)
    }

    @Test
    fun `no template returns same string`() {
        val result = VariableParser.replaceVariables("Hello World", mapOf("name" to "X"))
        assertEquals("Hello World", result)
    }

    @Test
    fun `extract variable with extra inner whitespace`() {
        val vars = VariableParser.extractVariables("{{  name  :  val  }}")
        assertEquals(1, vars.size)
        assertEquals("name", vars[0].key)
        assertEquals("val", vars[0].defaultValue)
    }
}
