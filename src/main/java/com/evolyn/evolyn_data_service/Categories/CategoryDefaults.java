package com.evolyn.evolyn_data_service.Categories;

import java.util.List;

/**
 * The starter set every user gets. The names match the fixed list the app used before
 * categories were editable, so existing transactions map onto them one-to-one.
 */
final class CategoryDefaults {

    private CategoryDefaults() {}

    record Default(String type, String name, String icon, String color, String description) {}

    static final String FALLBACK_ICON = "bx bx-purchase-tag-alt";
    static final String FALLBACK_COLOR = "#64748b";

    static final List<Default> ALL = List.of(
            new Default("EXPENSE", "Food & Dining", "bx bx-restaurant", "#f97316", "Restaurants, groceries, cafes"),
            new Default("EXPENSE", "Transport", "bx bx-car", "#f59e0b", "Fuel, public transport, cabs"),
            new Default("EXPENSE", "Shopping", "bx bx-shopping-bag", "#6366f1", "Clothes, electronics, online orders"),
            new Default("EXPENSE", "Entertainment", "bx bx-movie-play", "#ec4899", "Movies, music, subscriptions"),
            new Default("EXPENSE", "Housing", "bx bx-home", "#8b5cf6", "Rent, maintenance, repairs"),
            new Default("EXPENSE", "Health", "bx bx-plus-medical", "#10b981", "Medicine, doctor, fitness, insurance"),
            new Default("EXPENSE", "Education", "bx bx-book", "#3b82f6", "Courses, books, fees"),
            new Default("EXPENSE", "Utilities", "bx bx-bulb", "#eab308", "Electricity, water, phone, internet"),
            new Default("EXPENSE", "Investments", "bx bx-line-chart", "#14b8a6", "SIPs, stocks, deposits"),
            new Default("EXPENSE", "Others", "bx bx-dots-horizontal-rounded", "#64748b", "Anything that doesn't fit elsewhere"),
            new Default("INCOME", "Salary", "bx bx-briefcase", "#22c55e", "Monthly pay"),
            new Default("INCOME", "Freelance", "bx bx-laptop", "#16a34a", "Side projects, contract work"),
            new Default("INCOME", "Interest", "bx bx-coin-stack", "#15803d", "Bank and deposit interest"),
            new Default("INCOME", "Refund", "bx bx-undo", "#0d9488", "Money returned to you"),
            new Default("INCOME", "Gift", "bx bx-gift", "#059669", "Gifts and cash received"),
            new Default("INCOME", "Others", "bx bx-dots-horizontal-rounded", "#64748b", "Any other income"));
}
