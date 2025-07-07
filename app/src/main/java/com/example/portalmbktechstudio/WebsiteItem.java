package com.example.portalmbktechstudio;

public class WebsiteItem {
    private String title;
    private String url;
    private boolean isSelected;

    public WebsiteItem(String title, String url) {
        this.title = title;
        this.url = url;
        this.isSelected = false;
    }

    public String getTitle() {
        return title;
    }

    public String getUrl() {
        return url;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }
}
