package com.root.apigateway.exception;


import com.root.apigateway.utils.CommonUtil;
import com.root.commondependencies.constants.ExceptionConstants;
import com.root.commondependencies.exception.ValidationException;
import com.root.commondependencies.vo.ErrorVO;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.reactive.error.DefaultErrorAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;

import java.util.Map;

@Component
public class GlobalErrorAttributes extends DefaultErrorAttributes {

    @Override
    public Map<String, Object> getErrorAttributes(ServerRequest request, ErrorAttributeOptions options) {
        Exception ex = (Exception) getError(request);

        ErrorVO error = new ErrorVO();
        if (ex instanceof ValidationException) {
            ValidationException validationException = (ValidationException) ex;
            error.setErrorCode(String.valueOf(validationException.getErrorCode()));
            error.setErrorMsg(validationException.getErrorMessage());
            error.setUrl(validationException.getUrl());
        } else {
            error.setErrorCode(HttpStatus.BAD_REQUEST.toString());
            error.setErrorMsg(ExceptionConstants.INTERNAL_ERROR);
        }

        return CommonUtil.getMapFromObject(error);
    }
}