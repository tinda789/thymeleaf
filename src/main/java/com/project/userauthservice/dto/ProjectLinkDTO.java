package com.project.userauthservice.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectLinkDTO {

    private Long id;
    private String title;
    private String url;
    private Long projectId;
    private String addedAt;
}