package com.SheriffAnya.justRaces.objects;

import java.util.ArrayList;
import java.util.List;

public class Cast {

    private final String name;
    private int maxSize;
    private List<String> members;
    private String color;

    // Базовые характеристики
    private double health;
    private float size;

    // Особенности дыхания
    private boolean canBreatheUnderwater;
    private boolean needsWaterToBreathe;
    private float underwaterMiningSpeed;

    // Постоянные эффекты
    private List<String> permanentEffects;

    public Cast(String name) {
        this.name = name;
        this.maxSize = 4;
        this.members = new ArrayList<>();
        this.color = "&f";
        this.health = 20.0;
        this.size = 1.0f;

        // Особенности дыхания
        this.canBreatheUnderwater = false;
        this.needsWaterToBreathe = false;
        this.underwaterMiningSpeed = 1.0f;

        // Эффекты и предметы
        this.permanentEffects = new ArrayList<>();

        // НЕТ ХАРДКОДА - ВСЕ ЗНАЧЕНИЯ ИЗ КОНФИГА
    }

    // Getters
    public String getName() {
        return name;
    }

    public int getMaxSize() {
        return maxSize;
    }

    public List<String> getMembersNames() {
        return new ArrayList<>(members);
    }

    public String getColor() {
        return color;
    }

    public double getHealth() {
        return health;
    }

    public float getSize() {
        return size;
    }

    public boolean canBreatheUnderwater() {
        return canBreatheUnderwater;
    }

    public boolean needsWaterToBreathe() {
        return needsWaterToBreathe;
    }

    public float getUnderwaterMiningSpeed() {
        return underwaterMiningSpeed;
    }

    public List<String> getPermanentEffects() {
        return new ArrayList<>(permanentEffects);
    }

    public int getCurrentSize() {
        return members.size();
    }

    public boolean isFull() {
        return members.size() >= maxSize;
    }

    public boolean isEmpty() {
        return members.isEmpty();
    }

    public boolean hasMember(String playerName) {
        return members.contains(playerName);
    }

    // Setters - ВСЕ ЗНАЧЕНИЯ УСТАНАВЛИВАЮТСЯ ИЗ КОНФИГА
    public void setMaxSize(int maxSize) {
        this.maxSize = Math.max(1, maxSize);
    }

    public void setColor(String color) {
        this.color = (color != null) ? color : "&f";
    }

    public void setHealth(double health) {
        this.health = Math.max(1.0, Math.min(100.0, health));
    }

    public void setSize(float size) {
        this.size = Math.max(0.1f, Math.min(10.0f, size));
    }

    public void setCanBreatheUnderwater(boolean canBreatheUnderwater) {
        this.canBreatheUnderwater = canBreatheUnderwater;
    }

    public void setNeedsWaterToBreathe(boolean needsWaterToBreathe) {
        this.needsWaterToBreathe = needsWaterToBreathe;
    }

    public void setUnderwaterMiningSpeed(float underwaterMiningSpeed) {
        this.underwaterMiningSpeed = Math.max(0.1f, Math.min(5.0f, underwaterMiningSpeed));
    }

    public void setPermanentEffects(List<String> permanentEffects) {
        this.permanentEffects = new ArrayList<>(permanentEffects);
    }

    // Методы управления участниками (по никам)
    public boolean addMember(String playerName) {
        if (isFull() || members.contains(playerName)) {
            return false;
        }
        members.add(playerName);
        return true;
    }

    public boolean removeMember(String playerName) {
        return members.remove(playerName);
    }

    public void clearMembers() {
        members.clear();
    }
}
