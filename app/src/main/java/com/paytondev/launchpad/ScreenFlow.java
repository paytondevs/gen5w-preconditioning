package com.paytondev.launchpad;

final class ScreenFlow {
    enum Page { PRECONDITIONING, HOME, ADVANCED, BROWSER }
    static final Page[] TABS = {Page.PRECONDITIONING, Page.HOME, Page.ADVANCED};
    static Page back(Page page, Page defaultPage) {
        if (page == Page.BROWSER) return Page.ADVANCED;
        return page == defaultPage ? null : defaultPage;
    }
    static Page fromPreference(String value) {
        for (Page tab : TABS) if (tab.name().equals(value)) return tab;
        return Page.PRECONDITIONING;
    }
}
