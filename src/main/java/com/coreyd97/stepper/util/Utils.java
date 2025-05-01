package com.coreyd97.stepper.util;

import javax.swing.*;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.URL;

public class Utils {
    public static ImageIcon loadImage(String filename, int width, int height){
        ClassLoader cldr = Utils.class.getClassLoader();
        URL imageURLMain = cldr.getResource(filename);

        if(imageURLMain != null) {
            Image scaled = new ImageIcon(imageURLMain).getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
            ImageIcon scaledIcon = new ImageIcon(scaled);
            BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = (Graphics2D) bufferedImage.getGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.drawImage(scaledIcon.getImage(), null, null);
            return new ImageIcon(bufferedImage);
        }
        return null;
    }

    /**
     * Converts a hotkey in the style of burp into the syntax used by java swing's key stroke.
     * 
     * For example, the hotkey {@code Ctrl+Alt+N}
     * is converted into the string {@code control alt N}
     * The result of this function can be used to create valid {@code KeyStroke} instance.
     * @param burpHotKey A string representing a hotkey as used by Burp
     * @return The same hotkey but in Java Swing's representation
     */
    public static String burpHotKeyToSwing(String burpHotKey) {
        return burpHotKey
            .replace("+", " ") // Expand whitespace
            .replace("Ctrl", "control") // Expand control modifier
            .replace("Shift", "shift") // Expand shift modifier
            .replace("Alt", "alt"); // Expand alt modifier
    }
}
