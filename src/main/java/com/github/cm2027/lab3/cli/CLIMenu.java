package com.github.cm2027.lab3.cli;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

public class CLIMenu {

    protected static final Scanner scanner = new Scanner(System.in);

    protected void printHeader(String title) {
        int width = 60;
        System.out.println("\n" + "=".repeat(width));
        System.out.println(centerText(title, width));
        System.out.println("=".repeat(width));
    }

    protected void printDivider() {
        System.out.println("-".repeat(60));
    }

    private String centerText(String text, int width) {
        int padding = (width - text.length()) / 2;
        return " ".repeat(Math.max(0, padding)) + text;
    }

    protected void printMenuOptions(String... options) {
        System.out.println();
        for (int i = 0; i < options.length; i++) {
            System.out.println((i + 1) + ". " + options[i]);
        }
        System.out.println("0. Back");
        System.out.print("\nSelect option: ");
    }

    protected int getIntInput(String prompt) {
        while (true) {
            try {
                System.out.print(prompt);
                String input = scanner.nextLine().trim();
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number.");
            }
        }
    }

    protected String getStringInput(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    protected boolean getConfirmation(String prompt) {
        while (true) {
            System.out.print(prompt + " (y/n): ");
            String input = scanner.nextLine().trim().toLowerCase();
            if (input.equals("y") || input.equals("yes")) {
                return true;
            } else if (input.equals("n") || input.equals("no")) {
                return false;
            }
            System.out.println("Please enter 'y' or 'n'.");
        }
    }

    protected void printSuccess(String message) {
        System.out.println("\n✓ " + message);
    }

    protected void printError(String message) {
        System.out.println("\n✗ Error: " + message);
    }

    protected void printInfo(String message) {
        System.out.println("\nℹ " + message);
    }

    protected void pauseForUser() {
        System.out.print("\nPress Enter to continue...");
        scanner.nextLine();
    }

    protected void clearScreen() {
        for (int i = 0; i < 2; i++) {
            System.out.println();
        }
    }

    protected int selectFromList(String title, String[] items) {
        printHeader(title);
        for (int i = 0; i < items.length; i++) {
            System.out.println((i + 1) + ". " + items[i]);
        }
        System.out.println("0. Cancel");

        while (true) {
            int choice = getIntInput("\nSelect: ");
            if (choice >= 0 && choice <= items.length) {
                return choice;
            }
            System.out.println("Invalid selection. Try again.");
        }
    }

    protected String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) return "N/A";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        return dateTime.format(formatter);
    }
}