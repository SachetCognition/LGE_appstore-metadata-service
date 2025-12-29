/*
 * If not stated otherwise in this file or this component's LICENSE file the
 * following copyright and licenses apply:
 *
 * Copyright 2022 Liberty Global Technology Services BV
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.lgi.appstore.metadata.api.error;

import com.lgi.appstore.metadata.model.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;
    private WebRequest webRequest;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
        MockHttpServletRequest request = new MockHttpServletRequest();
        webRequest = new ServletWebRequest(request);
    }

    @Test
    void shouldHandleMaintainerNotFoundException() {
        MaintainerNotFoundException exception = new MaintainerNotFoundException("test-code");

        ResponseEntity<Object> response = exceptionHandler.handleNotFound(exception, webRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isInstanceOf(ErrorResponse.class);
        ErrorResponse errorResponse = (ErrorResponse) response.getBody();
        assertThat(errorResponse.getMessage()).contains("test-code");
    }

    @Test
    void shouldHandleApplicationAlreadyExistsException() {
        ApplicationAlreadyExistsException exception = new ApplicationAlreadyExistsException("App already exists");

        ResponseEntity<Object> response = exceptionHandler.handleConflict(exception, webRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isInstanceOf(ErrorResponse.class);
    }

    @Test
    void shouldHandleMaintainerAlreadyExistsException() {
        MaintainerAlreadyExistsException exception = new MaintainerAlreadyExistsException("test-code");

        ResponseEntity<Object> response = exceptionHandler.handleConflict(exception, webRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isInstanceOf(ErrorResponse.class);
    }

    @Test
    void shouldHandleMandatoryFieldForNativeAppNotFound() {
        MandatoryFieldForNativeAppNotFound exception = new MandatoryFieldForNativeAppNotFound("Missing field");

        ResponseEntity<Object> response = exceptionHandler.handleBadRequest(exception, webRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isInstanceOf(ErrorResponse.class);
    }

    @Test
    void shouldHandleUnsupportedApplicationTypeException() {
        UnsupportedApplicationTypeException exception = new UnsupportedApplicationTypeException();

        ResponseEntity<Object> response = exceptionHandler.handleBadRequest(exception, webRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isInstanceOf(ErrorResponse.class);
    }

    @Test
    void shouldHandleMethodArgumentTypeMismatchWithRequiredType() {
        MethodArgumentTypeMismatchException exception = new MethodArgumentTypeMismatchException(
                "invalid", Integer.class, "paramName", null, new IllegalArgumentException("Invalid value"));

        ResponseEntity<Object> response = exceptionHandler.handleMethodArgumentTypeMismatch(exception, webRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isInstanceOf(ErrorResponse.class);
        ErrorResponse errorResponse = (ErrorResponse) response.getBody();
        assertThat(errorResponse.getMessage()).contains("paramName");
        assertThat(errorResponse.getMessage()).contains("Integer");
    }

    @Test
    void shouldHandleMethodArgumentTypeMismatchWithoutRequiredType() {
        MethodArgumentTypeMismatchException exception = new MethodArgumentTypeMismatchException(
                "invalid", null, "paramName", null, new IllegalArgumentException("Invalid value"));

        ResponseEntity<Object> response = exceptionHandler.handleMethodArgumentTypeMismatch(exception, webRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isInstanceOf(ErrorResponse.class);
        ErrorResponse errorResponse = (ErrorResponse) response.getBody();
        assertThat(errorResponse.getMessage()).contains("paramName");
        assertThat(errorResponse.getMessage()).contains("invalid type");
    }

    @Test
    void shouldHandleJsonExceptionWithInvalidJsonString() {
        JsonException exception = new JsonException("target", "{invalid json}", new RuntimeException("Parse error"));

        ResponseEntity<Object> response = exceptionHandler.handleJsonException(exception, webRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isInstanceOf(ErrorResponse.class);
    }

    @Test
    void shouldHandleJsonExceptionWithoutInvalidJsonString() {
        JsonException exception = new JsonException("target", null, new RuntimeException("Parse error"));

        ResponseEntity<Object> response = exceptionHandler.handleJsonException(exception, webRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isInstanceOf(ErrorResponse.class);
    }

    @Test
    void shouldHandleMissingPathVariable() throws NoSuchMethodException {
        MissingPathVariableException exception = new MissingPathVariableException(
                "pathVar", 
                new org.springframework.core.MethodParameter(
                        GlobalExceptionHandlerTest.class.getMethod("setUp"), -1));

        ResponseEntity<Object> response = exceptionHandler.handleMissingPathVariable(
                exception, new HttpHeaders(), HttpStatus.BAD_REQUEST, webRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isInstanceOf(ErrorResponse.class);
    }

    @Test
    void shouldHandleMissingServletRequestParameter() {
        MissingServletRequestParameterException exception = 
                new MissingServletRequestParameterException("paramName", "String");

        ResponseEntity<Object> response = exceptionHandler.handleMissingServletRequestParameter(
                exception, new HttpHeaders(), HttpStatus.BAD_REQUEST, webRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isInstanceOf(ErrorResponse.class);
        ErrorResponse errorResponse = (ErrorResponse) response.getBody();
        assertThat(errorResponse.getMessage()).contains("paramName");
    }

    @Test
    void shouldHandleMissingServletRequestPart() {
        MissingServletRequestPartException exception = 
                new MissingServletRequestPartException("partName");

        ResponseEntity<Object> response = exceptionHandler.handleMissingServletRequestPart(
                exception, new HttpHeaders(), HttpStatus.BAD_REQUEST, webRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isInstanceOf(ErrorResponse.class);
    }

    @Test
    void shouldHandleMethodArgumentNotValid() {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "testObject");
        bindingResult.addError(new FieldError("testObject", "fieldName", "must not be null"));
        
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(null, bindingResult);

        ResponseEntity<Object> response = exceptionHandler.handleMethodArgumentNotValid(
                exception, new HttpHeaders(), HttpStatus.BAD_REQUEST, webRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isInstanceOf(ErrorResponse.class);
        ErrorResponse errorResponse = (ErrorResponse) response.getBody();
        assertThat(errorResponse.getMessage()).contains("fieldName");
    }

    @Test
    void shouldHandleGenericException() {
        Exception exception = new RuntimeException("Something went wrong");

        ResponseEntity<Object> response = exceptionHandler.handleAll(exception, webRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isInstanceOf(ErrorResponse.class);
        ErrorResponse errorResponse = (ErrorResponse) response.getBody();
        assertThat(errorResponse.getMessage()).isEqualTo("Something went wrong");
    }

    @Test
    void shouldHandleExceptionWithNullMessage() {
        Exception exception = new RuntimeException((String) null);

        ResponseEntity<Object> response = exceptionHandler.handleAll(exception, webRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isInstanceOf(ErrorResponse.class);
        ErrorResponse errorResponse = (ErrorResponse) response.getBody();
        assertThat(errorResponse.getMessage()).isEqualTo("Details not available");
    }

    @Test
    void shouldHandleExceptionWithBlankMessage() {
        Exception exception = new RuntimeException("   ");

        ResponseEntity<Object> response = exceptionHandler.handleAll(exception, webRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isInstanceOf(ErrorResponse.class);
        ErrorResponse errorResponse = (ErrorResponse) response.getBody();
        assertThat(errorResponse.getMessage()).isEqualTo("Details not available");
    }
}
