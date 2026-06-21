package com.club.dto;

import java.util.List;

public class ActivityCreateDTO {
    private String title;
    private String description;
    private String process;
    private String location;
    private String startTime;
    private String endTime;
    private Integer maxCount;
    private java.math.BigDecimal budget;
    private String poster;
    private List<Integer> coHostClubIds;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getProcess() { return process; }
    public void setProcess(String process) { this.process = process; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    public Integer getMaxCount() { return maxCount; }
    public void setMaxCount(Integer maxCount) { this.maxCount = maxCount; }

    public java.math.BigDecimal getBudget() { return budget; }
    public void setBudget(java.math.BigDecimal budget) { this.budget = budget; }

    public String getPoster() { return poster; }
    public void setPoster(String poster) { this.poster = poster; }

    public List<Integer> getCoHostClubIds() { return coHostClubIds; }
    public void setCoHostClubIds(List<Integer> coHostClubIds) { this.coHostClubIds = coHostClubIds; }
}
