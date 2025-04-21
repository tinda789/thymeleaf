package com.project.userauthservice.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectFileDTO {

    private Long id;
    private String fileName;
    private String fileType;
    private String fileUrl;
    private Long projectId;
    private String uploadedAt; // Có thể để String hoặc LocalDateTime tùy cách bạn xử lý JSON
}
