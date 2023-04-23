package me.jadenp.notranks;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class ConfigOptionsTest {

    @Test
    void stripPlaceholders() {
    }

    @Test
    void matchItem() {
    }

    @Test
    void containsAll() {

        String[] requirements = new String[]{"Hello ", "! How is ", "going?"};
        String str = "Hello Not_Jaden! How is NotRanks going?";
        assertThat(ConfigOptions.containsAll(requirements, str), CoreMatchers.is(true));
        requirements = new String[]{"uwu, ", "wh", "at's ", "this"};
        str = "uwu, what's this?";
        assertThat(ConfigOptions.containsAll(requirements, str), CoreMatchers.is(true));
        requirements = new String[]{"xynx", "zzz ", "abcd", "fsf"};
        str = "abcd";
        assertThat(ConfigOptions.containsAll(requirements, str), CoreMatchers.is(false));
        requirements = new String[]{"test"};
        str = "test";
        assertThat(ConfigOptions.containsAll(requirements, str), CoreMatchers.is(true));
    }
}