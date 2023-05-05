package com.fox2code.foxloader.installer;

import javax.swing.*;

public class SelectableTranslatableLabel extends JEditorPane {
    public SelectableTranslatableLabel(String translationKey) {
        this.setContentType("text/plain");
        this.setEditable(false);
        this.setBackground(null);
        this.setText("Hello world");
        TranslateEngine.installOn(this, translationKey);
    }
}
