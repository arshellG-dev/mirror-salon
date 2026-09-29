package com.mirror.salon.orders;

import jakarta.servlet.http.HttpSession;

import java.util.LinkedHashMap;
import java.util.Map;

public final class Cart {

    private static final String SESSION_KEY = "cartItems";

    private Cart() {}

    @SuppressWarnings("unchecked")
    public static Map<Integer, Integer> items(HttpSession session) {
        Map<Integer, Integer> items = (Map<Integer, Integer>) session.getAttribute(SESSION_KEY);
        if (items == null) {
            items = new LinkedHashMap<>();
            session.setAttribute(SESSION_KEY, items);
        }
        return items;
    }

    public static void add(HttpSession session, Integer productId, int quantity) {
        items(session).merge(productId, quantity, Integer::sum);
    }

    public static void setQuantity(HttpSession session, Integer productId, int quantity) {
        if (quantity <= 0) {
            items(session).remove(productId);
        } else {
            items(session).put(productId, quantity);
        }
    }

    public static void remove(HttpSession session, Integer productId) {
        items(session).remove(productId);
    }

    public static void clear(HttpSession session) {
        items(session).clear();
    }
}
