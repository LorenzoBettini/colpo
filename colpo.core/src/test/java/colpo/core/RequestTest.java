package colpo.core;

import static colpo.core.Participant.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class RequestTest {

	@Test
	void testEqualsAndHashCodeEmpty() {
		assertThat(new Request(null, null, null, null))
			.isEqualTo(new Request(null, null, null, null));
	}

	@Test
	void testInvalidFromAndRequesterAreTheSame() {
		// this is fine, it checks indexes only if the first one is > 0
		new Request(index(0), null, null, index(1));
		// this is invalid
		Participant sameIndex = index(1);
		assertThatThrownBy(() -> new Request(sameIndex, null, null, sameIndex))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("requester and from are the same: 1");
	}

	@Test
	void testEqualsAndHashCodeWithIndexes() {
		var r1 = new Request(
			index(1),
			new Attributes(),
			new Attributes(),
			index(2)
		);
		var r2 = new Request(
			index(1),
			new Attributes(),
			new Attributes(),
			index(2)
		);
		assertThat(r1)
			.isEqualTo(r2)
			.hasSameHashCodeAs(r2);
	}

	@Test
	void testEqualsAndHashCodeWithAttributes() {
		var r1 = new Request(
			index(1),
			new Attributes()
				.add("aName", "aValue")
				.add("aName1", "aValue1"),
			new Attributes(),
			index(2)
		);
		var r2 = new Request(
			index(1),
			new Attributes()
				.add("aName", "aValue")
				.add("aName1", "aValue1"),
			new Attributes(),
			index(2)
		);
		var r3 = new Request(
			index(1),
			new Attributes()
				.add("aName", "aValue"),
			new Attributes(),
			index(2)
		);
		assertThat(r1)
			.isEqualTo(r2)
			.hasSameHashCodeAs(r2)
			.isNotEqualTo(r3);
	}

	@Test
	void testEqualsAndHashCodeWithSuchThat() {
		var r1 = new Request(
			index(1),
			new Attributes()
				.add("aName", "aValue")
				.add("aName1", "aValue1"),
			new Attributes(),
			anySuchThat(new Attributes()
				.add("firstName", "Bob"))
		);
		var r2 = new Request(
			index(1),
			new Attributes()
				.add("aName", "aValue")
				.add("aName1", "aValue1"),
			new Attributes(),
			anySuchThat(new Attributes()
				.add("firstName", "Bob"))
		);
		var r3 = new Request(
			index(1),
			new Attributes()
				.add("aName", "aValue"),
			new Attributes(),
			allSuchThat(new Attributes()
				.add("firstName", "Bob"))
		);
		assertThat(r1)
			.isEqualTo(r2)
			.hasSameHashCodeAs(r2)
			.isNotEqualTo(r3);
	}
}
