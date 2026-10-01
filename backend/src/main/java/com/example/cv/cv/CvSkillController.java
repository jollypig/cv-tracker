package com.example.cv.cv;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cvs/{cvId}/skill-groups")
public class CvSkillController {

    private final CvSkillService skillService;

    public CvSkillController(CvSkillService skillService) {
        this.skillService = skillService;
    }

    @GetMapping
    public List<CvSkillGroupResponse> findGroups(@PathVariable UUID cvId) {
        return skillService.findGroups(cvId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CvSkillGroupResponse createGroup(@PathVariable UUID cvId,
                                            @Valid @RequestBody CvSkillGroupRequest request) {
        return skillService.createGroup(cvId, request);
    }

    @PutMapping("/{groupId}")
    public CvSkillGroupResponse updateGroup(@PathVariable UUID cvId, @PathVariable UUID groupId,
                                            @Valid @RequestBody CvSkillGroupRequest request) {
        return skillService.updateGroup(cvId, groupId, request);
    }

    @DeleteMapping("/{groupId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteGroup(@PathVariable UUID cvId, @PathVariable UUID groupId) {
        skillService.deleteGroup(cvId, groupId);
    }

    @GetMapping("/{groupId}/skills")
    public List<CvSkillResponse> findSkills(@PathVariable UUID cvId, @PathVariable UUID groupId) {
        return skillService.findSkills(cvId, groupId);
    }

    @PostMapping("/{groupId}/skills")
    @ResponseStatus(HttpStatus.CREATED)
    public CvSkillResponse createSkill(@PathVariable UUID cvId, @PathVariable UUID groupId,
                                       @Valid @RequestBody CvSkillRequest request) {
        return skillService.createSkill(cvId, groupId, request);
    }

    @PutMapping("/{groupId}/skills/{skillId}")
    public CvSkillResponse updateSkill(@PathVariable UUID cvId, @PathVariable UUID groupId,
                                       @PathVariable UUID skillId, @Valid @RequestBody CvSkillRequest request) {
        return skillService.updateSkill(cvId, groupId, skillId, request);
    }

    @DeleteMapping("/{groupId}/skills/{skillId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSkill(@PathVariable UUID cvId, @PathVariable UUID groupId, @PathVariable UUID skillId) {
        skillService.deleteSkill(cvId, groupId, skillId);
    }
}