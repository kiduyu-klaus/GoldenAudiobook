package com.example.goldenaudiobook.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Navigation item model for drawer navigation
 */
public class NavItem implements Serializable {
    private String id;
    private String title;
    private String url;
    private String icon;
    private int order;
    private boolean isHeader;
    private boolean isCategory;
    private List<NavItem> subItems;

    public NavItem() {
        this.subItems = new ArrayList<>();
    }

    public NavItem(String title, String url, boolean isHeader) {
        this();
        this.title = title;
        this.url = url;
        this.isHeader = isHeader;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public boolean isHeader() {
        return isHeader;
    }

    public void setHeader(boolean header) {
        isHeader = header;
    }

    public boolean isCategory() {
        return isCategory;
    }

    public void setCategory(boolean category) {
        isCategory = category;
    }

    public List<NavItem> getSubItems() {
        return subItems;
    }

    public void setSubItems(List<NavItem> subItems) {
        this.subItems = subItems;
    }

    public void addSubItem(NavItem subItem) {
        if (this.subItems == null) {
            this.subItems = new ArrayList<>();
        }
        this.subItems.add(subItem);
    }

    public boolean hasSubItems() {
        return subItems != null && !subItems.isEmpty();
    }

    @Override
    public String toString() {
        return "NavItem{" +
                "title='" + title + '\'' +
                ", url='" + url + '\'' +
                ", isHeader=" + isHeader +
                '}';
    }
}
