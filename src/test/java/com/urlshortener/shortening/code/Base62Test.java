package com.urlshortener.shortening.code;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Pure unit tests for the {@link Base62} codec (no Spring, no database). */
class Base62Test {

    @Test
    void encodesKnownValues() {
        assertThat(Base62.encode(0)).isEqualTo("0");
        assertThat(Base62.encode(1)).isEqualTo("1");
        assertThat(Base62.encode(61)).isEqualTo("z");
        assertThat(Base62.encode(62)).isEqualTo("10");
    }

    @Test
    void sequenceStartProducesSevenCharCodes() {
        assertThat(Base62.encode(56_800_235_584L)).hasSize(7).isEqualTo("1000000");
    }

    @ParameterizedTest
    @ValueSource(longs = {1L, 61L, 62L, 1000L, 56_800_235_584L, 3_521_614_606_207L, Long.MAX_VALUE})
    void roundTripsEncodeDecode(long value) {
        assertThat(Base62.decode(Base62.encode(value))).isEqualTo(value);
    }

    @Test
    void rejectsNegative() {
        assertThatThrownBy(() -> Base62.encode(-1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void decodeRejectsInvalidCharacter() {
        assertThatThrownBy(() -> Base62.decode("abc$")).isInstanceOf(IllegalArgumentException.class);
    }
}
