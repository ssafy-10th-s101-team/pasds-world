package world.pasds.back.common.exception;

import lombok.Getter;

public enum ExceptionCode {
    MEMBER_NOT_FOUND(404, "Member not found"),
    MEMBER_EXISTS(409, "Member exists"),
    MEMBER_UNAUTHORIZED(401, "Unauthorized member control"),

    EMAIL_EXISTS(409, "Email exists"),
    EMAIL_NOT_FOUND(404, "Email not found"),
    EMAIL_INVALID_FORMAT(400, "Email invalid format"),

    PASSWORD_SAME(409, "Password is same as before"),
    PASSWORD_INVALID_FORMAT(400, "Password invalid format"),
    PASSWORD_CONFIRM_INVALID(400, "Password confirm invalid"),

    NICKNAME_INVALID_FORMAT(400,"Nickname invalid format"),

    ORGANIZATION_NOT_FOUND(404, "Organization not found"),
    ORGANIZATION_UNAUTHORIZED(401, "Unauthorized organization control"),

    TEAM_NOT_FOUND(404, "Team not found"),
    TEAM_UNAUTHORIZED(401, "Unauthorized team control"),

    PRIVATE_DATA_UNAUTHORIZED(401, "Unauthorized PrivateData control"),

    KEY_ERROR(500,"key error"),

    TOTP_CODE_GENERATION_ERROR(500,"Totp code generation error"),

    TOTP_CODE_NOT_SAME(409, "Totp code not same"),

    BAD_REQUEST(400, "Bad Request"),
    FORBIDDEN(503, "Forbidden"),

    ROLE_NOT_FOUND(404, "Role not found"),


    TEAM_NAME_CONFLICT(403, "Team name already exists"),

    INTERNAL_SERVER_ERROR(500,"Something wrong in server");

    @Getter
    private int status;

    @Getter
    private String message;

    ExceptionCode(int code, String message) {
        this.status = code;
        this.message = message;
    }
}