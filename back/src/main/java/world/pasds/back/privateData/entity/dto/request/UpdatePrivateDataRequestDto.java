package world.pasds.back.privateData.entity.dto.request;

import lombok.Getter;

import java.util.List;

@Getter
public class UpdatePrivateDataRequestDto {
    private Long teamId;
    private Long privateDataId;
    private String title;
    private String content;
    private String memo;
    private String id;
    private String url;
    private List<Long> roleId;
}