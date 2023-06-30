package colpo.core;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AttributeTest {

	private Attributes attributes;

	@BeforeEach
	void init() {
		attributes = new Attributes();
	}

	@Test
	void shouldAddWhenNotPresent() {
		attributes.add("aName", "aValue")
			.add("aName1", "aValue1");
		assertEquals("aValue", attributes.name("aName"));
		assertEquals("aValue1", attributes.name("aName1"));
		assertNull(attributes.name("non existent"));
	}

	@Test
	void shouldThrowWhenPresent() {
		attributes.add("aName", "aValue")
			.add("aName1", "aValue1");
		assertThatThrownBy(() -> attributes.add("aName", "anotherValue"))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("'aName' is already present as 'aValue'");
	}
}
