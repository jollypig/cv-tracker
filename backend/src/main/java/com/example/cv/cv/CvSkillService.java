package com.example.cv.cv;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class CvSkillService {

    private final CvRepository cvRepository;
    private final CvSkillGroupRepository groupRepository;
    private final CvSkillRepository skillRepository;

    public CvSkillService(CvRepository cvRepository, CvSkillGroupRepository groupRepository,
                          CvSkillRepository skillRepository) {
        this.cvRepository = cvRepository;
        this.groupRepository = groupRepository;
        this.skillRepository = skillRepository;
    }

    @Transactional(readOnly = true)
    public List<CvSkillGroupResponse> findGroups(UUID cvId) {
        getCv(cvId);
        return groupRepository.findByCvIdOrderBySortOrderAsc(cvId).stream().map(this::toGroup).toList();
    }

    public CvSkillGroupResponse createGroup(UUID cvId, CvSkillGroupRequest request) {
        CvSkillGroup group = new CvSkillGroup(getCv(cvId), request.name().trim());
        group.setSortOrder(request.sortOrder());
        return toGroup(groupRepository.save(group));
    }

    public CvSkillGroupResponse updateGroup(UUID cvId, UUID groupId, CvSkillGroupRequest request) {
        CvSkillGroup group = getGroup(cvId, groupId);
        group.setName(request.name().trim());
        group.setSortOrder(request.sortOrder());
        return toGroup(groupRepository.save(group));
    }

    public void deleteGroup(UUID cvId, UUID groupId) {
        groupRepository.delete(getGroup(cvId, groupId));
    }

    @Transactional(readOnly = true)
    public List<CvSkillResponse> findSkills(UUID cvId, UUID groupId) {
        getGroup(cvId, groupId);
        return skillRepository.findBySkillGroupIdOrderBySortOrderAsc(groupId).stream().map(this::toSkill).toList();
    }

    public CvSkillResponse createSkill(UUID cvId, UUID groupId, CvSkillRequest request) {
        CvSkill skill = new CvSkill(getGroup(cvId, groupId), request.name().trim());
        skill.setLevel(blankToNull(request.level()));
        skill.setSortOrder(request.sortOrder());
        return toSkill(skillRepository.save(skill));
    }

    public CvSkillResponse updateSkill(UUID cvId, UUID groupId, UUID skillId, CvSkillRequest request) {
        CvSkill skill = skillRepository.findByIdAndSkillGroupId(skillId, getGroup(cvId, groupId).getId())
                .orElseThrow(() -> notFound("Skill not found"));
        skill.setName(request.name().trim());
        skill.setLevel(blankToNull(request.level()));
        skill.setSortOrder(request.sortOrder());
        return toSkill(skillRepository.save(skill));
    }

    public void deleteSkill(UUID cvId, UUID groupId, UUID skillId) {
        CvSkillGroup group = getGroup(cvId, groupId);
        CvSkill skill = skillRepository.findByIdAndSkillGroupId(skillId, group.getId())
                .orElseThrow(() -> notFound("Skill not found"));
        skillRepository.delete(skill);
    }

    private CvSkillGroupResponse toGroup(CvSkillGroup group) {
        return new CvSkillGroupResponse(group.getId(), group.getName(), group.getSortOrder(),
                skillRepository.findBySkillGroupIdOrderBySortOrderAsc(group.getId()).stream().map(this::toSkill).toList());
    }

    private CvSkillResponse toSkill(CvSkill skill) {
        return new CvSkillResponse(skill.getId(), skill.getName(), skill.getLevel(), skill.getSortOrder());
    }

    private Cv getCv(UUID cvId) {
        return cvRepository.findById(cvId).orElseThrow(() -> notFound("CV not found"));
    }

    private CvSkillGroup getGroup(UUID cvId, UUID groupId) {
        getCv(cvId);
        return groupRepository.findByIdAndCvId(groupId, cvId).orElseThrow(() -> notFound("Skill group not found"));
    }

    private ResponseStatusException notFound(String message) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, message);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}