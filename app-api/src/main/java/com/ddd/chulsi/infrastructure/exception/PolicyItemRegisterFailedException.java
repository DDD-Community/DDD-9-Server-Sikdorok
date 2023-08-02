package com.ddd.chulsi.infrastructure.exception;

import com.ddd.chulsi.infrastructure.exception.message.ErrorMessage;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PolicyItemRegisterFailedException extends ServerException {
    public PolicyItemRegisterFailedException() {
        super(511, ErrorMessage.JSON_DE_SERIALIZED_FAILED);
    }

    public PolicyItemRegisterFailedException(String message) {
        super(511, message, null);
    }
}
