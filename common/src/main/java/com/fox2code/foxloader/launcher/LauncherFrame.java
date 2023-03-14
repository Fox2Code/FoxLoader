package com.fox2code.foxloader.launcher;

import javax.swing.*;
import java.awt.*;

public class LauncherFrame {
    private final JFrame jFrame;
    private final JProgressBar jProgressBar;

    public LauncherFrame() {
        Dimension dimension = new Dimension(300, 40);
        this.jProgressBar = new JProgressBar();
        this.jProgressBar.setLayout(null);
        this.jProgressBar.setMinimumSize(dimension);
        this.jProgressBar.setMaximumSize(dimension);
        this.jProgressBar.setValue(0);
        this.jProgressBar.setMaximum(100);
        this.jProgressBar.setString("Loading...");
        this.jProgressBar.setStringPainted(true);
        this.jProgressBar.setBorderPainted(false);
        this.jFrame = new JFrame("FoxLoader");
        this.jFrame.setUndecorated(true);
        this.jFrame.setLayout(new BorderLayout());
        this.jFrame.setMinimumSize(dimension);
        this.jFrame.add(BorderLayout.CENTER, this.jProgressBar);
        this.jFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        this.jFrame.setLocationRelativeTo(null);
        this.jFrame.pack();
        this.jFrame.setResizable(false);
    }

    public void show() {
        this.jFrame.setVisible(true);
    }

    public void hide() {
        this.jFrame.setVisible(false);
        this.jFrame.dispose();
    }

    public void setProgress(int progress, String text) {
        this.jProgressBar.setValue(progress);
        this.jProgressBar.setString(text);
    }

    public static void main(String[] args) throws InterruptedException {
        LauncherFrame launcherFrame = new LauncherFrame();
        launcherFrame.show();
        for (int i = 0; i <= 10; i++) {
            launcherFrame.setProgress(i * 10, i + "/10");
            Thread.sleep(500);
        }
        launcherFrame.hide();
    }
}
