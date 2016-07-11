package org.zalando.riptide.stream;

/*
 * ⁣​
 * Riptide: Stream
 * ⁣⁣
 * Copyright (C) 2015 - 2016 Zalando SE
 * ⁣⁣
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ​⁣
 */

import static org.hamcrest.Matchers.instanceOf;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.zalando.riptide.model.AccountBody;

import com.fasterxml.jackson.databind.ObjectMapper;

public class StreamConverterTest {

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void shouldRead() throws Exception {
        final StreamConverter unit = new StreamConverter();
        Assert.assertTrue(unit.canRead(List.class, null));
        Assert.assertTrue(unit.canRead(List[].class, null));
        Assert.assertTrue(unit.canRead(AccountBody.class, null));
        Assert.assertTrue(unit.canRead(AccountBody[].class, null));
    }

    @Test
    public void shouldReadStreams() throws Exception {
        final StreamConverter unit = new StreamConverter();
        Assert.assertTrue(unit.canRead(Streams.streamOf(List.class).getType(), null, null));
        Assert.assertTrue(unit.canRead(Streams.streamOf(List[].class).getType(), null, null));
        Assert.assertTrue(unit.canRead(Streams.streamOf(AccountBody.class).getType(), null, null));
        Assert.assertTrue(unit.canRead(Streams.streamOf(AccountBody[].class).getType(), null, null));
    }

    @Test
    public void shouldReadCreateStream() throws Exception {
        Type type = Streams.streamOf(AccountBody.class).getType();
        final StreamConverter unit = new StreamConverter(new ObjectMapper().findAndRegisterModules());
        HttpInputMessage input = new HttpInputMessage() {

            @Override
            public HttpHeaders getHeaders() {
                return null;
            }

            @Override
            public InputStream getBody() throws IOException {
                return new StreamFilter(new ClassPathResource("account-sequence.json").getInputStream());
            }
        };
        input.getHeaders();

        @SuppressWarnings("unchecked")
        Stream<Object> stream = (Stream<Object>) unit.read(type, null, input);

        @SuppressWarnings("unchecked")
        final Consumer<? super Object> verifier = mock(Consumer.class);

        stream.forEach(verifier);
        
        verify(verifier).accept(new AccountBody("1234567890", "Acme Corporation"));
        verify(verifier).accept(new AccountBody("1234567891", "Acme Company"));
        verify(verifier).accept(new AccountBody("1234567892", "Acme GmbH"));
        verify(verifier).accept(new AccountBody("1234567893", "Acme SE"));
        verify(verifier, times(4)).accept(any(AccountBody.class));

    }

    @Test
    public void shouldFailReadOnIOException() throws Exception {
        exception.expect(HttpMessageNotReadableException.class);
        exception.expectCause(instanceOf(IOException.class));

        final StreamConverter unit = new StreamConverter();
        HttpInputMessage input = new HttpInputMessage() {

            @Override
            public HttpHeaders getHeaders() {
                return null;
            }

            @Override
            public InputStream getBody() throws IOException {
                throw new IOException();
            }
        };
        input.getHeaders();
        unit.read(Streams.streamOf(Object.class).getType(), null, input);
    }
}