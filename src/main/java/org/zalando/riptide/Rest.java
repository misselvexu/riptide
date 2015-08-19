package org.zalando.riptide;

/*
 * ⁣​
 * riptide
 * ⁣⁣
 * Copyright (C) 2015 Zalando SE
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

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

public final class Rest {

    private final RestTemplate template;

    private Rest(RestTemplate template) {
        this.template = template;
    }

    public Dispatcher execute(HttpMethod method, URI url) {
        return execute(method, url, HttpEntity.EMPTY);
    }

    public Dispatcher execute(HttpMethod method, URI url, HttpHeaders headers) {
        return execute(method, url, new HttpEntity<>(headers));
    }

    public Dispatcher execute(HttpMethod method, URI url, Object body) {
        return execute(method, url, new HttpEntity<>(body));
    }

    public Dispatcher execute(HttpMethod method, URI url, HttpHeaders headers, Object body) {
        return execute(method, url, new HttpEntity<>(body, headers));
    }

    private <T> Dispatcher execute(HttpMethod method, URI url, HttpEntity<T> entity) {
        final Callback<T> callback = new Callback<>(template.getMessageConverters(), entity);
        final ClientHttpResponse response = execute(method, url, callback);
        return new Dispatcher(template, response);
    }

    /**
     * Returns the {@link ClientHttpResponse} as reported by the underlying {@link RestTemplate}.
     * <p>
     * Note: When used with a <i>OAuth2RestTemplate</i> this method catches the exception containing the buffered
     * response thrown by the {@link OAuth2CompatibilityResponseErrorHandler} and continues with normal dispatching.
     * </p>
     */
    private <T> ClientHttpResponse execute(final HttpMethod method, final URI url, final Callback<T> callback) {
        try {
            return template.execute(url, method, callback, BufferingClientHttpResponseWrapper::buffer);
        } catch (AlreadyConsumedResponseException e) {
            return e.getResponse();
        }
    }

    public static Rest create(RestTemplate template) {
        return new Rest(template);
    }

}