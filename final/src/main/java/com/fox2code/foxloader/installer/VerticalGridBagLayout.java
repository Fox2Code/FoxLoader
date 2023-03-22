package com.fox2code.foxloader.installer;

import java.awt.*;

public class VerticalGridBagLayout extends GridBagLayout {
    public VerticalGridBagLayout() {
        this.defaultConstraints.gridy = GridBagConstraints.RELATIVE;
        this.defaultConstraints.gridheight = GridBagConstraints.RELATIVE;
        this.defaultConstraints.gridwidth = 0;
        this.defaultConstraints.fill = GridBagConstraints.HORIZONTAL;
        this.defaultConstraints.anchor = GridBagConstraints.NORTH;
        this.defaultConstraints.weightx = 1;
        this.defaultConstraints.weighty = 1;
    }
}
