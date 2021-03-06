/*
 * Copyright 2018 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.thoughtworks.go.server.domain.user;

import com.thoughtworks.go.config.CaseInsensitiveString;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.thoughtworks.go.server.domain.user.DashboardFilter.DEFAULT_NAME;
import static com.thoughtworks.go.server.domain.user.FilterValidator.*;
import static com.thoughtworks.go.server.domain.user.Filters.WILDCARD_FILTER;
import static org.junit.jupiter.api.Assertions.*;


class FiltersTest {

    private static final String TWENTY_CHAR = "0123456789abcdefghij";
    private static final String NAME_TOO_LONG = TWENTY_CHAR + TWENTY_CHAR + TWENTY_CHAR + TWENTY_CHAR;

    @Test
    void validatesNameFormatOnConstruction() {
        DashboardFilter a = namedWhitelist("¯\\_(ツ)_/¯");

        Throwable e = assertThrows(FilterValidationException.class, () -> new Filters(Collections.singletonList(a)));
        assertEquals(MSG_NAME_FORMAT, e.getMessage());

        DashboardFilter b = namedWhitelist(" filter");

        e = assertThrows(FilterValidationException.class, () -> new Filters(Collections.singletonList(b)));
        assertEquals(MSG_NO_LEADING_TRAILING_SPACES, e.getMessage());

        DashboardFilter c = namedWhitelist("filter ");

        e = assertThrows(FilterValidationException.class, () -> new Filters(Collections.singletonList(c)));
        assertEquals(MSG_NO_LEADING_TRAILING_SPACES, e.getMessage());

        DashboardFilter d = namedWhitelist(NAME_TOO_LONG);
        e = assertThrows(FilterValidationException.class, () -> new Filters(Collections.singletonList(d)));
        assertEquals(MSG_MAX_LENGTH, e.getMessage());
    }

    @Test
    void validatesNameFormatOnDeserialize() {
        final String json = "{ \"filters\": [" +
                "  {\"name\": \"¯\\\\_(\\\\u30C4)_/¯\", \"type\": \"whitelist\", \"pipelines\": []}" +
                "] }";
        Throwable e = assertThrows(FilterValidationException.class, () -> Filters.fromJson(json));
        assertEquals(MSG_NAME_FORMAT, e.getMessage());

        final String json1 = "{ \"filters\": [" +
                "  {\"name\": \" filter\", \"type\": \"whitelist\", \"pipelines\": []}" +
                "] }";
        e = assertThrows(FilterValidationException.class, () -> Filters.fromJson(json1));
        assertEquals(MSG_NO_LEADING_TRAILING_SPACES, e.getMessage());

        final String json2 = "{ \"filters\": [" +
                "  {\"name\": \"filter \", \"type\": \"whitelist\", \"pipelines\": []}" +
                "] }";
        e = assertThrows(FilterValidationException.class, () -> Filters.fromJson(json2));
        assertEquals(MSG_NO_LEADING_TRAILING_SPACES, e.getMessage());

        final String json4 = "{ \"filters\": [" +
                "  {\"name\": \"" + NAME_TOO_LONG + "\", \"type\": \"whitelist\", \"pipelines\": []}" +
                "] }";
        e = assertThrows(FilterValidationException.class, () -> Filters.fromJson(json4));
        assertEquals(MSG_MAX_LENGTH, e.getMessage());
    }

    @Test
    void validatesNamePresenceOnConstruction() {
        DashboardFilter a = namedWhitelist("");

        Throwable e = assertThrows(FilterValidationException.class, () -> new Filters(Collections.singletonList(a)));
        assertEquals(MSG_MISSING_NAME, e.getMessage());

        DashboardFilter b = namedWhitelist(" ");

        e = assertThrows(FilterValidationException.class, () -> new Filters(Collections.singletonList(b)));
        assertEquals(MSG_MISSING_NAME, e.getMessage());

        DashboardFilter c = namedWhitelist(null);

        e = assertThrows(FilterValidationException.class, () -> new Filters(Collections.singletonList(c)));
        assertEquals(MSG_MISSING_NAME, e.getMessage());
    }

    @Test
    void validatesNamePresenceOnDeserialize() {
        final String json = "{ \"filters\": [" +
                "  {\"name\": \"\", \"type\": \"whitelist\", \"pipelines\": []}" +
                "] }";
        Throwable e = assertThrows(FilterValidationException.class, () -> Filters.fromJson(json));
        assertEquals(MSG_MISSING_NAME, e.getMessage());

        final String json1 = "{ \"filters\": [" +
                "  {\"name\": \" \", \"type\": \"whitelist\", \"pipelines\": []}" +
                "] }";
        e = assertThrows(FilterValidationException.class, () -> Filters.fromJson(json1));
        assertEquals(MSG_MISSING_NAME, e.getMessage());

        final String json2 = "{ \"filters\": [" +
                "  {\"type\": \"whitelist\", \"pipelines\": []}" +
                "] }";
        e = assertThrows(FilterValidationException.class, () -> Filters.fromJson(json2));
        assertEquals(MSG_MISSING_NAME, e.getMessage());
    }

    @Test
    void validatesDuplicateNamesOnConstruction() {
        DashboardFilter a = namedWhitelist("one");
        DashboardFilter b = namedBlacklist("one");

        Throwable e = assertThrows(FilterValidationException.class, () -> new Filters(Arrays.asList(a, b)));
        assertEquals("Duplicate filter name: one", e.getMessage());
    }

    @Test
    void validatesDuplicateNamesOnDeserialize() {
        String json = "{ \"filters\": [" +
                "  {\"name\": \"one\", \"type\": \"whitelist\", \"pipelines\": []}," +
                "  {\"name\": \"one\", \"type\": \"whitelist\", \"pipelines\": []}" +
                "] }";
        Throwable e = assertThrows(FilterValidationException.class, () -> Filters.fromJson(json));
        assertEquals("Duplicate filter name: one", e.getMessage());
    }

    @Test
    void validatesPresenceOfDefaultFilterOnConstruction() {
        Throwable e = assertThrows(FilterValidationException.class, () -> Filters.single(namedBlacklist("foo", "bar")));
        assertEquals(MSG_NO_DEFAULT_FILTER, e.getMessage());
    }

    @Test
    void validatesPresenceOfDefaultFilterOnDeserialize() {
        final String json = "{ \"filters\": [" +
                "  {\"name\": \"foo\", \"type\": \"whitelist\", \"pipelines\": [\"bar\"]}" +
                "] }";

        Throwable e = assertThrows(FilterValidationException.class, () -> Filters.fromJson(json));
        assertEquals(MSG_NO_DEFAULT_FILTER, e.getMessage());
    }

    @Test
    void defaultFilterNameIsAlwaysTitleCase() {
        String json = "{ \"filters\": [{\"name\": \"deFauLt\", \"type\": \"whitelist\"}] }";
        assertEquals(DEFAULT_NAME, Filters.fromJson(json).named(DEFAULT_NAME).name());

        String expected = "{\"filters\":[{\"name\":\"" + DEFAULT_NAME + "\",\"pipelines\":[],\"type\":\"whitelist\"}]}";
        assertEquals(expected, Filters.toJson(Filters.single(namedWhitelist("defAUlT"))));
    }

    @Test
    void fromJson() {
        String json = "{ \"filters\": [{\"name\": \"Default\", \"type\": \"whitelist\", \"pipelines\": [\"p1\"]}] }";
        final Filters filters = Filters.fromJson(json);
        assertEquals(1, filters.filters().size());
        final DashboardFilter first = filters.filters().get(0);
        assertEquals(first.name(), DEFAULT_NAME);
        assertTrue(first instanceof WhitelistFilter);
        assertEquals(1, ((WhitelistFilter) first).pipelines().size());
        assertTrue(((WhitelistFilter) first).pipelines().contains(new CaseInsensitiveString("p1")));
    }

    @Test
    void toJson() {
        List<DashboardFilter> views = new ArrayList<>();
        views.add(WILDCARD_FILTER);
        views.add(namedBlacklist("Cool Pipelines", "Pipely McPipe"));
        final Filters filters = new Filters(views);

        assertEquals("{\"filters\":[" +
                "{\"name\":\"" + DEFAULT_NAME + "\",\"pipelines\":[],\"type\":\"blacklist\"}," +
                "{\"name\":\"Cool Pipelines\",\"pipelines\":[\"Pipely McPipe\"],\"type\":\"blacklist\"}" +
                "]}", Filters.toJson(filters));
    }

    @Test
    void equalsIsStructuralEquality() {
        final Filters a = Filters.single(blacklist("p1", "p2"));
        final Filters b = Filters.single(blacklist("p1", "p2"));
        final Filters c = Filters.single(blacklist("p1", "p3"));

        assertEquals(a, b);
        assertNotEquals(a, c);

        final Filters d = Filters.single(whitelist("p1", "p2"));
        final Filters e = Filters.single(whitelist("p1", "p2"));
        final Filters f = Filters.single(whitelist("p1", "p3"));

        assertEquals(d, e);
        assertNotEquals(d, f);

        assertNotEquals(a, d);

        assertEquals(Filters.defaults(), Filters.single(blacklist()));
    }


    private DashboardFilter namedWhitelist(String name, String... pipelines) {
        return new WhitelistFilter(name, CaseInsensitiveString.list(pipelines));
    }

    private DashboardFilter whitelist(String... pipelines) {
        return namedWhitelist(DEFAULT_NAME, pipelines);
    }

    private DashboardFilter namedBlacklist(String name, String... pipelines) {
        return new BlacklistFilter(name, CaseInsensitiveString.list(pipelines));
    }

    private DashboardFilter blacklist(String... pipelines) {
        return namedBlacklist(DEFAULT_NAME, pipelines);
    }
}