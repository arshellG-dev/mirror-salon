package com.mirror.salon.users;

import jakarta.servlet.http.HttpSession;

public final class CustomerSession {

    private static final String CUSTOMER_ID = "loggedInCustomerId";
    private static final String MOBILE_NO = "loggedInMobileNo";
    private static final String FULL_NAME = "loggedInFullName";

    private CustomerSession() {}

    public static void login(HttpSession session, Customer customer) {
        session.setAttribute(CUSTOMER_ID, customer.getCustomerId());
        session.setAttribute(MOBILE_NO, customer.getMobileNo());
        session.setAttribute(FULL_NAME, customer.getFullName());
    }

    public static void logout(HttpSession session) {
        session.removeAttribute(CUSTOMER_ID);
        session.removeAttribute(MOBILE_NO);
        session.removeAttribute(FULL_NAME);
    }

    public static Integer getCustomerId(HttpSession session) {
        return (Integer) session.getAttribute(CUSTOMER_ID);
    }

    public static String getFullName(HttpSession session) {
        return (String) session.getAttribute(FULL_NAME);
    }

    public static boolean isLoggedIn(HttpSession session) {
        return getCustomerId(session) != null;
    }
}
