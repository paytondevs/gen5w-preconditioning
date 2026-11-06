package com.paytondev.launchpad;

/** Battery opens first; Settings has sub-pages that back out to it. */
final class ScreenFlow {
    enum Page { BATTERY, SCHEDULE, SETTINGS, WIFI, SIGNALS, BROWSER }
    static final Page[] MENU = {Page.BATTERY, Page.SCHEDULE, Page.SETTINGS};

    /** The page Back goes to, or null to close. */
    static Page back(Page page) {
        if (page == Page.BATTERY) return null;
        return menu(page) == Page.SETTINGS && page != Page.SETTINGS ? Page.SETTINGS : Page.BATTERY;
    }
    /** The menu item shown as selected for a page. */
    static Page menu(Page page) {
        return page == Page.WIFI || page == Page.SIGNALS || page == Page.BROWSER ? Page.SETTINGS : page;
    }

    private ScreenFlow() { }
}
